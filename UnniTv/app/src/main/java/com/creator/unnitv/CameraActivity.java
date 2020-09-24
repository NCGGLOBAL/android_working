package com.creator.unnitv;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.creator.unnitv.R;
import com.creator.unnitv.common.HNApplication;
import com.creator.unnitv.delegator.HNSharedPreference;
import com.creator.unnitv.helpers.Constants;
import com.creator.unnitv.util.EtcUtil;
import com.creator.unnitv.util.LogUtil;
import com.creator.unnitv.util.NicePayUtility;
import com.facebook.AccessToken;
import com.kakao.auth.AuthType;
import com.kakao.auth.Session;
import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class CameraActivity extends Activity {
    private WebView mWebView;
    private String mCallback;
    private CookieManager mCookieManager;
    private String LIVE_URL = HNApplication.URL + "/addon/wlive/TV_live_creator.asp";

    KSYStreamer mStreamer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initWebView();
        initCamera();
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
        mStreamer.setPreviewResolution(480, 0);
// 设置推流分辨率，可以不同于预览分辨率（不应大于预览分辨率，否则推流会有画质损失）
        mStreamer.setTargetResolution(480, 0);
// 设置预览帧率
        mStreamer.setPreviewFps(15);
// 设置推流帧率，当预览帧率大于推流帧率时，编码模块会自动丢帧以适应设定的推流帧率
        mStreamer.setTargetFps(15);
// 设置视频码率，分别为初始平均码率、最高平均码率、最低平均码率，单位为kbps，另有setVideoBitrate接口，单位为bps
        mStreamer.setVideoKBitrate(600, 800, 400);
// 设置音频采样率
        mStreamer.setAudioSampleRate(44100);
// 设置音频码率，单位为kbps，另有setAudioBitrate接口，单位为bps
        mStreamer.setAudioKBitrate(48);
/**
 * 设置编码模式(软编、硬编)，请根据白名单和系统版本来设置软硬编模式，不要全部设成软编或者硬编,白名单可以联系金山云商务:
 * StreamerConstants.ENCODE_METHOD_SOFTWARE
 * StreamerConstants.ENCODE_METHOD_HARDWARE
 */
        mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
// 设置屏幕的旋转角度，支持 0, 90, 180, 270
        mStreamer.setRotateDegrees(0);
// 设置开始预览使用前置还是后置摄像头
        mStreamer.setCameraFacing(CameraCapture.FACING_FRONT);

        mStreamer.startStream();

        // 切换前后摄像头
        mStreamer.switchCamera();
// 开关闪光灯
        mStreamer.toggleTorch(true);
// 设置美颜滤镜，关于美颜滤镜的具体说明请参见专题说明
        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
    }

    private void initWebView() {
        mWebView = ((WebView) findViewById(R.id.webView));
        mWebView.setBackgroundColor(0);
        mWebView.setWebViewClient(new HNWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
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
                    // ...
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
}
