package com.creator.comonlive;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.creator.comonlive.common.HNApplication;
import com.creator.comonlive.helpers.Constants;
import com.creator.comonlive.models.Image;
import com.creator.comonlive.util.BitmapUtil;
import com.creator.comonlive.util.EtcUtil;
import com.creator.comonlive.util.LogUtil;
import com.creator.comonlive.util.RealPathUtil;
import com.kakao.auth.Session;
import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CameraActivity extends Activity {
    private String TAG = "CameraActivity";
    private WebView mWebView;
    private String mCallback;
    private CookieManager mCookieManager;
    private String LIVE_URL = HNApplication.URL + "/addon/wlive/TV_live_creator.asp";

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private String mCurrentPhotoPath;       // 촬영된 이미지 경로

    private JSONArray mImgArr = null;

    KSYStreamer mStreamer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activity가 실행 될 때 항상 화면을 켜짐으로 유지한다.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        initWebView();
        initCamera();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("SeongKwon", "============================================");
        Log.d("SeongKwon", "requestCode = " + requestCode);
        Log.d("SeongKwon", "resultCode = " + resultCode);
        Log.d("SeongKwon", "============================================");
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        } else if ((requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA || requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM) && resultCode == RESULT_OK) {
            String result = "";
            try {
                JSONObject jObj = new JSONObject();
                JSONArray jArray = new JSONArray();
                jObj.put("resultcd", "0");              // 0:성공. 1:실패

                ArrayList<Image> selectedImages = (ArrayList<Image>) data.getExtras().get(Constants.INTENT_EXTRA_IMAGES);
                for (int i = 0; i < selectedImages.size(); i++) {
                    JSONObject jObjItem = new JSONObject();

                    // 회전
                    Matrix matrix = new Matrix();
                    matrix.postRotate(BitmapUtil.GetExifOrientation(selectedImages.get(i).path));

                    int dstWidth = 200;
                    int dstHeight = 200;

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    Bitmap src = BitmapFactory.decodeFile(selectedImages.get(i).path, options);
//                        Bitmap resized = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);

                    int width = src.getWidth();
                    int height = src.getHeight();
//                        Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, width / 2, height / 2, matrix, true);
                    Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, dstWidth, dstHeight, matrix, true);

                    jObjItem.put("image", getBase64String(src));
                    jObjItem.put("thumbnail", getBase64String(resized));

                    jArray.put(jObjItem);
                }

                result = jObj.toString();
                Log.d("SeongKwon", result);
            } catch (Exception e) {
                e.printStackTrace();
            }
            executeJavascript(mCallback + "(" + result + ")");
        } else if (requestCode == Constants.REQUEST_ADD_IMAGE && resultCode == RESULT_OK) {
            Log.d("SeongKwon", "!!!!!!!!!!!!!!!!!!! = Constants.REQUEST_ADD_IMAGE = " + Constants.REQUEST_ADD_IMAGE);
            try {
                JSONObject jObj = new JSONObject();
                jObj.put("resultcd", "0");              // 변경사항 있을경우 : 1, 없을경우 : 0 // 이전[0:성공. 1:실패]
                if (data.hasExtra("isChanged")) {
                    if (data.getBooleanExtra("isChanged", false)) {
                        jObj.put("resultcd", "1");
                    }
                }
                if (data.hasExtra("imgArr")) {
                    mImgArr = new JSONArray(data.getStringExtra("imgArr"));
                }
                jObj.put("imgArr", mImgArr);
                jObj.put("token", data.getStringExtra("token"));
                jObj.put("pageGbn", data.getStringExtra("pageGbn"));
                jObj.put("cnt", data.getStringExtra("cnt"));
                executeJavascript(mCallback + "(" + jObj.toString() + ")");

                // TODO 신규등록을 위한 임시저장
                HNApplication.mImgArrForReg = mImgArr.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == Constants.REQUEST_EDIT_IMAGE && resultCode == RESULT_OK) {
            try {
                if (data.hasExtra("imgArr")) {
                    mImgArr = new JSONArray(data.getStringExtra("imgArr"));
                }
                JSONObject jObj = new JSONObject();
                jObj.put("imgArr", mImgArr);
                jObj.put("token", data.getStringExtra("token"));
                jObj.put("pageGbn", data.getStringExtra("pageGbn"));
                jObj.put("cnt", data.getStringExtra("cnt"));
                executeJavascript(mCallback + "(" + jObj.toString() + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == Constants.FILECHOOSER_NORMAL_REQ_CODE) {
            if (mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri result = getResultUri(data);

            Log.d(getClass().getName(), "openFileChooser : "+result);
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else if (requestCode == Constants.FILECHOOSER_LOLLIPOP_REQ_CODE) {
            if (mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Log.e("SeongKwon", getResultUri(data).toString());
            Uri[] results = new Uri[]{getResultUri(data)};

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void initCamera() {
        GLSurfaceView mCameraPreview = (GLSurfaceView)findViewById(R.id.camera_preview);
        // 创建KSYStreamer实例
        mStreamer = new KSYStreamer(this);
// 设置预览View
        mStreamer.setDisplayPreview(mCameraPreview);
    }

    private void initStreamer(String streamUrl) {
// 设置推流url（需要向相关人员申请，测试地址并不稳定！）
        mStreamer.setUrl(streamUrl);
// 设置预览分辨率, 当一边为0时，SDK会根据另一边及实际预览View的尺寸进行计算
        mStreamer.setPreviewResolution(720, 1280);
// 设置推流分辨率，可以不同于预览分辨率（不应大于预览分辨率，否则推流会有画质损失）
        mStreamer.setTargetResolution(720, 1280);
// 设置预览帧率
        mStreamer.setPreviewFps(15);
// 设置推流帧率，当预览帧率大于推流帧率时，编码模块会自动丢帧以适应设定的推流帧率
        mStreamer.setTargetFps(15);
// 设置视频码率，分别为初始平均码率、最高平均码率、最低平均码率，单位为kbps，另有setVideoBitrate接口，单位为bps
//        mStreamer.setVideoKBitrate(600, 800, 400);
        mStreamer.setVideoKBitrate(2048, 2160, 2000);
// 设置音频采样率
        mStreamer.setAudioSampleRate(44100);
// 设置音频码率，单位为kbps，另有setAudioBitrate接口，单位为bps
        mStreamer.setAudioKBitrate(48);
/**
 * 设置编码模式(软编、硬编)，请根据白名单和系统版本来设置软硬编模式，不要全部设成软编或者硬编,白名单可以联系金山云商务:
 * StreamerConstants.ENCODE_METHOD_SOFTWARE
 * StreamerConstants.ENCODE_METHOD_HARDWARE
 */
        mStreamer.setEnableRepeatLastFrame(false); // disable repeat last frame in background
        mStreamer.setEnableAutoRestart(true, 500);

        mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
// 设置屏幕的旋转角度，支持 0, 90, 180, 270
        mStreamer.setRotateDegrees(0);
// 设置开始预览使用前置还是后置摄像头
        mStreamer.setCameraFacing(CameraCapture.FACING_FRONT);

        mStreamer.toggleTorch(false);

        mStreamer.startStream();

        // 切换前后摄像头
//        mStreamer.switchCamera();
// 开关闪光灯

// 设置美颜滤镜，关于美颜滤镜的具体说明请参见专题说明
//        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
//                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
    }

    private void initWebView() {
        mWebView = ((WebView) findViewById(R.id.webView));
        mWebView.setBackgroundColor(0);
        mWebView.setWebViewClient(new HNWebViewClient());
        mWebView.setWebChromeClient(new HNWebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " NINTH"
                + "&deviceId=" + HNApplication.mDeviceId);
        if (Build.VERSION.SDK_INT >= 21) {
            mWebView.getSettings().setMixedContentMode(0);
            this.mCookieManager = CookieManager.getInstance();
            this.mCookieManager.setAcceptCookie(true);
            this.mCookieManager.setAcceptThirdPartyCookies(mWebView, true);
        } else {
            this.mCookieManager = CookieManager.getInstance();
            this.mCookieManager.setAcceptCookie(true);
        }

        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDisplayZoomControls(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        // setSavePassword - default false in android 4.4 and above
        // setPluginState - use plugin deprecate
        // setAppCacheMaxSize - deprecate
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setSupportMultipleWindows(false);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setSupportMultipleWindows(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.addJavascriptInterface(new WebAppInterface(this, mWebView), "android");

        mWebView.setDrawingCacheEnabled(true);
        mWebView.buildDrawingCache();

        mWebView.loadUrl(LIVE_URL);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStreamer != null) {
            // 一般可以在onResume中开启摄像头预览
            mStreamer.startCameraPreview();
            // 调用KSYStreamer的onResume接口
            mStreamer.onResume();
            // 如果onPause中切到了DummyAudio模块，可以在此恢复
            mStreamer.setUseDummyAudioCapture(false);

            // 设置Info回调，可以收到相关通知信息
            mStreamer.setOnInfoListener(new KSYStreamer.OnInfoListener() {
                @Override
                public void onInfo(int what, int msg1, int msg2) {
                    // ...
                }
            });
// 设置错误回调，收到该回调后，一般是发生了严重错误，比如网络断开等，
// SDK内部会停止推流，APP可以在这里根据回调类型及需求添加重试逻辑。
            mStreamer.setOnErrorListener(new KSYStreamer.OnErrorListener() {
                @Override
                public void onError(int what, int msg1, int msg2) {
                    Log.e(TAG, "onError : " + msg1);
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mStreamer != null) {
            mStreamer.onPause();
            // 一般在这里停止摄像头采集
            mStreamer.stopCameraPreview();
            // 如果希望App切后台后，停止录制主播端的声音，可以在此切换为DummyAudio采集，
            // 该模块会代替mic采集模块产生静音数据，同时释放占用的mic资源
            mStreamer.setUseDummyAudioCapture(true);
        }
    }

    @Override
    public void onDestroy() {
        if (mStreamer != null) {
            // 清理相关资源
            mStreamer.release();
        }
        super.onDestroy();
    }

    public class HNWebViewClient extends WebViewClient {
        public static final String INTENT_PROTOCOL_START = "intent:";
        public static final String INTENT_PROTOCOL_INTENT = "#Intent;";
        public static final String INTENT_PROTOCOL_END = ";end;";
        public static final String GOOGLE_PLAY_STORE_PREFIX = "market://details?id=";

        public HNWebViewClient() {
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // LogUtil.d("onPageLoadStopped : " + url);

//            mProgressUtil.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap paramBitmap) {
            super.onPageStarted(view, url, paramBitmap);
            // LogUtil.d("onPageLoadStarted : " + url);

            executeJavascript("localStorage.setItem(\"dv_id\"," + "\"" + HNApplication.mDeviceId + "\")");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);

            Intent intent = null;

            if (url.startsWith("sms:") || url.startsWith("smsto:")) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(i);
                return true;
            } else if (url.startsWith("tel:")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permissionResult = checkSelfPermission(Manifest.permission.CALL_PHONE);

                    if (permissionResult == PackageManager.PERMISSION_DENIED) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(CameraActivity.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 단말기의 \"전화걸기\" 권한이 필요합니다. 계속 하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                // CALL_PHONE 권한을 Android OS에 요청한다.
                                                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                                            }
                                        }
                                    })
                                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(CameraActivity.this, "기능을 취소했습니다", Toast.LENGTH_SHORT).show();
                                        }
                                    }).create().show();
                        }
                    } else {
                        intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
                        startActivity(intent);
                    }
                } else {
                    intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
                    startActivity(intent);
                }
                return true;
            } else if (url.startsWith("mailto:")) {
                intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(intent);
                return true;
            } else if (url != null && url.startsWith("intent://")) {
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                    if (existPackage != null) {
                        startActivity(intent);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                        startActivity(marketIntent);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (url.startsWith(INTENT_PROTOCOL_START)) {
                final int customUrlStartIndex = INTENT_PROTOCOL_START.length();
                final int customUrlEndIndex = url.indexOf(INTENT_PROTOCOL_INTENT);
                if (customUrlEndIndex < 0) {
                    return false;
                } else {
                    final String customUrl = url.substring(customUrlStartIndex, customUrlEndIndex);
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(customUrl));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getBaseContext().startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        final int packageStartIndex = customUrlEndIndex + INTENT_PROTOCOL_INTENT.length();
                        final int packageEndIndex = url.indexOf(INTENT_PROTOCOL_END);

                        final String packageName = url.substring(packageStartIndex, packageEndIndex < 0 ? url.length() : packageEndIndex);
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_STORE_PREFIX + packageName));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getBaseContext().startActivity(i);
                    }
                    return true;
                }
            }
            return false;       // webview replace
        }
    }

    private class WebAppInterface {
        private Context context;
        private WebView webview;

        public WebAppInterface(Context c, WebView w) {
            context = c;
            webview = w;
        }

        @JavascriptInterface
        public void iwebaction(final String inputJsonString) {
            Intent intent = null;
            JSONObject jObj = null;
            JSONObject response = null;
            try {
                LogUtil.d("iwebaction", "inputJsonString.toString() = " + inputJsonString.toString().trim());
                jObj = new JSONObject(inputJsonString.toString().trim());

                String actionCode = jObj.getString("action_code");

                JSONArray actionParamArray = null;
                JSONObject actionParamObj = null;

                // param
                if (jObj.has("action_param")) {
                    actionParamArray = jObj.getJSONArray("action_param");
                    actionParamObj = actionParamArray.getJSONObject(0);
                }

                // callback
                if (jObj.has("callBack")) {
                    mCallback = jObj.getString("callBack");
                }

                if ("ACT1027".equals(actionCode)) {
                    LogUtil.d("ACT1027 - wlive 전, 후면 카메라 제어");
                    int resultcd = 1;
                    if (actionParamObj.has("key_type")) {
                        mStreamer.switchCamera();
                    } else {
                        resultcd = 0;
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("resultcd", resultcd);      //1: 성공, 0: 실패

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                } else if ("ACT1028".equals(actionCode)) {
                    LogUtil.d("ACT1028 - wlive 마이크 제어");
                    int resultcd = 1;
                    if (actionParamObj.has("key_type")) {   //0: 마이크 끄기,1: 켜기
                        if (actionParamObj.getInt("key_type") == 0) {
                            mStreamer.setMuteAudio(true);
                        } else  {
                            mStreamer.setMuteAudio(false);
                        }
                    } else {
                        resultcd = 0;
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("resultcd", resultcd);      //1: 성공, 0: 실패

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                } else if ("ACT1029".equals(actionCode)) {
                    LogUtil.d("ACT1029 - wlive 이미지필터 제어");
                    int resultcd = 1;
                    if (actionParamObj.has("key_type")) {
                        switch (actionParamObj.getInt("key_type")) {
                            case 0:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
                                break;
                            case 1:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT);
                                break;
                            case 2:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_SKINWHITEN);
                                break;
                            case 3:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_ILLUSION);
                                break;
                            case 4:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
                                break;
                            case 5:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_SMOOTH);
                                break;
                            case 6:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT_EXT);
                                break;
                            case 7:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT_SHARPEN);
                                break;
                            case 8:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO);
                                break;
                            case 9:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO1);
                                break;
                            case 10:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO2);
                                break;
                            case 11:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO3);
                                break;
                            case 12:
                                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO4);
                                break;
                        }
                    } else {
                        resultcd = 0;
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("resultcd", resultcd);      //1: 성공, 0: 실패

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                } else if ("ACT1030".equals(actionCode)) {
                    LogUtil.d("ACT1030 - wlive 스트림키 전달 및 송출");
                    int resultcd = 1;
                    String streamUrl = actionParamObj.getString("stream_url");

                    initStreamer(streamUrl);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("resultcd", resultcd);      //1: 성공, 0: 실패

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                } else if ("ACT1031".equals(actionCode)) {
                    // 종료
                    finish();

                } else if ("ACT1015".equals(actionCode)) {
                    LogUtil.d("ACT1015 - 웹뷰 새창");

                    if (actionParamObj.has("url")) {
                        final String request_url = actionParamObj.getString("url");
                        LogUtil.d("url : " + request_url);
                        intent = new Intent(CameraActivity.this, WebViewActivity.class);
                        intent.putExtra("webviewUrl", request_url);
                        startActivity(intent);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void executeJavascript(String script) {
        if (mWebView == null) {
            return;
        }
        final String excuteScript = EtcUtil.EscapeJavaScriptFunctionParameter(script);

        LogUtil.d("excuteScript() : " + excuteScript);

        runOnUiThread(new Runnable() {
            public void run() {
                String prefix = "javascript:";
                String formattedScript = excuteScript;
                if (!excuteScript.startsWith(prefix)) {
                    formattedScript = prefix + excuteScript;
                }
                LogUtil.i("<<executeJavascript>>    " + formattedScript);
                // Build.VERSION_CODES.KITKAT
                if (Build.VERSION.SDK_INT < 19) {
                    mWebView.loadUrl(formattedScript);
                } else {
                    mWebView.evaluateJavascript(formattedScript, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            LogUtil.d("<<onReceiveValue>>    " + value);
                        }
                    });
                }
            }
        });
    }

    public class HNWebChromeClient extends WebChromeClient {

        public HNWebChromeClient() {
        }

        // For Android Version < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, Constants.FILECHOOSER_NORMAL_REQ_CODE);
        }

        // For 3.0 <= Android Version < 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
            openFileChooser(uploadMsg, acceptType, "");
        }

        // For 4.1 <= Android Version < 5.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
            mUploadMessage = uploadFile;
            imageChooser();
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;
            imageChooser();
            return true;
        }

        private void imageChooser() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(getClass().getName(), "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:"+photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if(takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, Constants.FILECHOOSER_LOLLIPOP_REQ_CODE);
        }

        public boolean onJsAlert(WebView paramWebView, String paramString1, String paramString2, final JsResult paramJsResult) {
            new AlertDialog.Builder(CameraActivity.this)
                    .setTitle(getResources().getString(R.string.alert_title))
                    .setMessage(paramString2)
                    .setPositiveButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                            paramJsResult.confirm();
                        }
                    }).setCancelable(false).create().show();
            return true;
        }

        public boolean onJsConfirm(WebView paramWebView, String paramString1, String paramString2, final JsResult paramJsResult) {
            new AlertDialog.Builder(CameraActivity.this)
                    .setTitle(getResources().getString(R.string.alert_title))
                    .setMessage(paramString2).setPositiveButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                    paramJsResult.confirm();
                }
            }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                    paramJsResult.cancel();
                }
            }).setCancelable(false).create().show();
            return true;
        }
    }

    // 사진저장
    private File createImageFile() throws IOException {
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        deleteDir(storageDir.getPath());

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String imageFileName = "JPEG_" + timeStamp + "_";
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );


        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        Log.d("createImageFile()", "mCurrentPhotoPath = " + mCurrentPhotoPath);
//        Toast.makeText(this, mCurrentPhotoPath, Toast.LENGTH_LONG).show();

        return image;
    }

    private String getBase64String(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] imageBytes = baos.toByteArray();

        String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        return base64String;
    }

    private Uri getResultUri(Intent data) {
        Uri result = null;
        if(data == null || TextUtils.isEmpty(data.getDataString())) {
            // If there is not data, then we may have taken a photo
            if(mCameraPhotoPath != null) {
                result = Uri.parse(mCameraPhotoPath);
            }
        } else {
            String filePath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePath = data.getDataString();
            } else {
                filePath = "file:" + RealPathUtil.getRealPath(this, data.getData());
            }
            result = Uri.parse(filePath);
        }

        return result;
    }

}
