package com.admin.withel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;
import com.admin.withel.common.BackPressCloseHandler;
import com.admin.withel.common.HNApplication;
import com.admin.withel.delegator.HNCommTran;
import com.admin.withel.delegator.HNCommTranInterface;
import com.admin.withel.delegator.HNSharedPreference;
import com.admin.withel.helpers.Constants;
import com.admin.withel.models.Image;
import com.admin.withel.util.AlertUtil;
import com.admin.withel.util.BitmapUtil;
import com.admin.withel.util.EtcUtil;
import com.admin.withel.util.LogUtil;
import com.admin.withel.util.NicePayUtility;
import com.admin.withel.util.RealPathUtil;
import com.admin.withel.util.UploadUtil;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebViewActivity extends Activity {
    private Context mContext;
    private WebView mWebView;
    String mWebViewUrl = "";
    private CookieManager mCookieManager;

    private String mCallback;
    private String mCallbackParam;

    private FirebaseMessaging mFirebaseMessaging;
    private String mPushUid = "";
    private String mLandingUrl = "";
    private BackPressCloseHandler mBackPressCloseHandler;
    private IntentIntegrator mIntegrator;
    private int mCameraType = 0;
    private HNCommTran mHNCommTran;

    private ProgressDialog mProgressDialog;  // 처리중

    String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.CALL_PHONE
            Manifest.permission.GET_ACCOUNTS
    };

    // NICE 연동 가이드
    final String ISP_LINK = "market://details?id=kvp.jjy.MispAndroid320";       // ISP 설치 링크
    final String KFTC_LINK = "market://details?id=com.kftc.bankpay.android";    //금융결제원 설치 링크
    final String MERCHANT_URL = "http://web.nicepay.co.kr/smart/mainPay.jsp";   // 가맹점의 결제 요청 페이지 URL
    private String NICE_BANK_URL = "";                                          // 계좌이체 인증후 거래 요청 URL
    // AndroidManaifest.xml에 명시된 값과 동일한 값을 설정하십시요.
    // 스키마 뒤에 ://를 붙여주십시요.
    private String WAP_URL = "nicepaysample" + "://";
    private String BANK_TID = "";

    private String mCurrentPhotoPath;       // 촬영된 이미지 경로
    private ArrayList<Image> mSelectedImages;

    private String mToken = "";             // 이미지 Token
    private JSONArray mImgArr = null;

    private LinearLayout mLlPermission;     // 권한페이지

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private Uri mCapturedImageURI;

    // SNS========================================================================= //
    private static final int SEND_NAVER_MESSAGE = 0;
    private static final int SEND_KAKAO_MESSAGE = 1;
    private static final int SEND_FACEBOOK_MESSAGE = 2;

    private static OAuthLogin mOAuthLoginModule;

    private CallbackManager callbackManager;

    private List<String> permissionNeeds = Arrays.asList("public_profile", "email");
    private Button btnNaverLogin;
    private Button btnFacebookLogin;
    private Button btnKakaoLogin;

    private String mNaverMessage = "";
    private String mFacebookMessage = "";
    private String mKakaoMessage = "";
    // SNS========================================================================= //

    public static WebViewActivity activity;

    public static WebViewActivity getInstance() {
        if (activity == null) {
            activity = new WebViewActivity();
        }
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_webview);
            mContext = WebViewActivity.this;

            mWebViewUrl = getIntent().getStringExtra("webviewUrl");

            // Back Handler
            mBackPressCloseHandler = new BackPressCloseHandler(this);

            // WebView 초기화
            initWebView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("JavascriptInterface")
    private void initWebView() {
        mWebView = ((WebView) findViewById(R.id.webView));
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
        mWebView.getSettings().setDisplayZoomControls(false);
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

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("webview-type", "sub");

        if (!TextUtils.isEmpty(mWebViewUrl)) {
            mWebView.loadUrl(mWebViewUrl, extraHeaders);
        }
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
            new AlertDialog.Builder(mContext)
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
            new AlertDialog.Builder(mContext)
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
            // LogUtil.e("shouldOverrideUrlLoading : " + url);

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
//                        if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
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
                                            Toast.makeText(mContext, "기능을 취소했습니다", Toast.LENGTH_SHORT).show();
                                        }
                                    }).create().show();
//                        }
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
            }

            //=================================== Nice Pay ===================================

            // 웹뷰에서 ispmobile 실행한 경우...
            if (url.startsWith("ispmobile")) {
                if (NicePayUtility.isPackageInstalled(getApplicationContext(), "kvp.jjy.MispAndroid320")) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else {
                    installISP();
                    return true;
                }
            }
            // 웹뷰에서 계좌이체를 실행한 경우...
            else if (url.startsWith("kftc-bankpay")) {
                if (NicePayUtility.isPackageInstalled(getApplicationContext(), "com.kftc.bankpay.android")) {
                    String sub_str_param = "kftc-bankpay://eftpay?";
                    String reqParam = url.substring(sub_str_param.length());
                    try {
                        reqParam = URLDecoder.decode(reqParam, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    reqParam = makeBankPayData(reqParam);

                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.kftc.bankpay.android", "com.kftc.bankpay.android.activity.MainActivity"));
                    intent.putExtra("requestInfo", reqParam);
                    startActivityForResult(intent, 1);

                    return true;
                } else {
                    installKFTC();
                    return true;
                }
            }
            // 웹뷰에서 안심클릭을 실행한 경우...
            else if (url != null && (url.contains("vguard")
                    || url.contains("droidxantivirus")
                    || url.contains("lottesmartpay")
                    || url.contains("smshinhancardusim://")
                    || url.contains("shinhan-sr-ansimclick")
                    || url.contains("v3mobile")
                    || url.endsWith(".apk")
                    || url.contains("smartwall://")
                    || url.contains("appfree://")
                    || url.contains("market://")
                    || url.contains("ansimclick://")
                    || url.contains("ansimclickscard")
                    || url.contains("ansim://")
                    || url.contains("mpocket")
                    || url.contains("mvaccine")
                    || url.contains("market.android.com")
                    || url.startsWith("intent://")
                    || url.contains("samsungpay")
                    || url.contains("droidx3web://")
                    || url.contains("kakaopay")
                    || url.contains("naversearchapp://")
                    || url.contains("http://m.ahnlab.com/kr/site/download"))) {
                try {
                    try {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Log.i("NICE", "intent getDataString +++===>" + intent.getDataString());

                    } catch (URISyntaxException ex) {
                        Log.e("Browser", "Bad URI " + url + ":" + ex.getMessage());
                        return false;
                    }

                    if (url.startsWith("intent")) { //chrome πˆ¡Ø πÊΩƒ

                        if (getPackageManager().resolveActivity(intent, 0) == null) {
                            String packagename = intent.getPackage();
                            if (packagename != null) {
                                uri = Uri.parse("market://search?q=pname:" + packagename);
                                intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                                return true;
                            }
                        }

                        uri = Uri.parse(intent.getDataString());
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);

                        return true;
                    } else { //±∏ πÊΩƒ
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) {
                    Log.i("NICE", e.getMessage());
                    return false;
                }
            }
            // ispmobile에서 결제 완료후 스마트주문 앱을 호출하여 결제결과를 전달하는 경우
            else if (url.startsWith(WAP_URL)) {
                String thisurl = url.substring(WAP_URL.length());
                view.loadUrl(thisurl);
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
            view.loadUrl(url);
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

                // 앱 설치체크
                if ("ACT0001".equals(actionCode)) {
                    LogUtil.d("ACT0001 - 앱 설치체크");

                    int checkInstall = 0;
                    if (actionParamObj.has("packagename")) {
                        try {
                            String packagename = actionParamObj.getString("packagename");
                            LogUtil.d("packagename : " + packagename);

                            PackageManager pm = context.getPackageManager();
                            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
                            checkInstall = 1;
                        } catch (Exception e) {
                            checkInstall = 0;
                        }
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("checkInstall", checkInstall);      // check install - 0 : 미설치, 1 : 설치

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                }

                // 사진첩 or 카메라 호출
                if ("ACT1001".equals(actionCode)) {
                    LogUtil.d("ACT1001 - 앱 데이터 저장 (키체인 저장 및 파일저장)");

                    if (actionParamObj.has("key_type")) {
                        LogUtil.d("mCameraType : " + mCameraType);

                        if (actionParamObj.getInt("key_type") == 0) {      // camera
                            mCameraType = 3;
//                            requestPermission(Constants.REQUEST_SELECT_IMAGE_CAMERA);
                        } else {                                          // album
                            mCameraType = 4;
//                            requestPermission(Constants.REQUEST_SELECT_IMAGE_ALBUM);
                        }
                        if (!hasPermissions(mContext, PERMISSIONS)) {
                            ActivityCompat.requestPermissions(WebViewActivity.this, PERMISSIONS, Constants.PERMISSIONS_MULTIPLE_REQUEST);
                        } else {
                            if (mCameraType == 3) {
                                dispatchTakePictureIntent();
                            } else {
                                galleryAddPic();
                            }
//                            intent = new Intent(getApplicationContext(), SelectImageMethodActivity.class);
//                            startActivityForResult(intent, Constants.REQUEST_CODE);
                        }
                    }
                }
                // QR 호출
                else if ("ACT1002".equals(actionCode)) {
                    LogUtil.d("ACT1002 - 앱 데이터 가져오기 (키체인 및 파일에 있는 정보 가져오기)");

                    if (actionParamObj.has("key_type")) {
                        mCameraType = actionParamObj.getInt("key_type");
                        LogUtil.d("mCameraType : " + mCameraType);
                    }

                    mCameraType = 0;
//                    requestPermission(Constants.REQUEST_CAMERA);
//                    executeJavascript(mCallback + "()");
                }
                // 위쳇페이
                else if ("ACT1003".equals(actionCode)) {
                    LogUtil.d("ACT1003 - 위쳇페이");

                    if (actionParamObj.has("request_url")) {
                        String request_url = actionParamObj.getString("request_url");
                        LogUtil.d("request_url : " + request_url);

                        mWebView.loadUrl(request_url);
                    }
                }
                // Custom Native 카메라 및 사진 라이브러리 호출
                else if ("ACT1011".equals(actionCode)) {
                    LogUtil.d("ACT1011 - Custom Native 카메라 및 사진 라이브러리 호출");

                    mToken = actionParamObj.getString("token");             // 사진 임시저장시 토큰값
                    mImgArr = actionParamObj.getJSONArray("imgArr");        // 이미지 정보
                    String pageGbn = actionParamObj.getString("pageGbn");   // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
                    String cnt = actionParamObj.getString("cnt");
                    LogUtil.d("token : " + mToken);
                    Log.d("SeongKwon", "mImgArr : " + mImgArr.toString());
                    Log.d("SeongKwon", "pageGbn : " + pageGbn);

                    // 신규
                    intent = new Intent(mContext, SelectImageMethodActivity.class);
                    intent.putExtra("token", mToken);
                    intent.putExtra("imgArr", mImgArr.toString());
                    intent.putExtra("pageGbn", pageGbn);
                    intent.putExtra("cnt", cnt);
                    startActivityForResult(intent, Constants.REQUEST_ADD_IMAGE);
                }
                // 사진 임시저장 통신
                else if ("ACT1012".equals(actionCode)) {
                    LogUtil.d("ACT1012 - 사진 임시저장 통신");

                    mToken = actionParamObj.getString("token");     // 사진 임시저장시 토큰값
                    new uploadImagesAsyncTask().execute(mToken);
                }
                // 휴대폰정보 가져오기
                else if ("ACT1013".equals(actionCode)) {
                    LogUtil.d("ACT1013 - 휴대폰정보 가져오기");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("device", "a");                  // 디바이스 구분값 (i : IOS, a : android)
                    jsonObject.put("deviceId", getDeviceId());      // 디바이스 아이디
                    jsonObject.put("version", getAppVersion());     // version

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                }
                // 앱에서 웹뷰 새창
                else if ("ACT1015".equals(actionCode)) {
                    LogUtil.d("ACT1015 - 웹뷰 새창");

                    if (actionParamObj.has("url")) {
                        final String request_url = actionParamObj.getString("url");
                        LogUtil.d("url : " + request_url);
                        intent = new Intent(mContext, WebViewActivity.class);
                        intent.putExtra("webviewUrl", request_url);
                        startActivity(intent);
                    }
                }
                // 팝업을 닫은후(ACT1015)  호출할 function 이름
                else if ("ACT1016".equals(actionCode)) {
                    LogUtil.d("ACT1016 - 팝업을 닫은후(ACT1015)  호출할 function 이름");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("callScript", "");   // 팝업을 닫은후(ACT1015)  호출할 function 이름
                    jsonObject.put("callObj", "");      // 메인 web 페이지에 넘길 파라메터

                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                    finish();
                }
                // 앱종료
                else if ("ACT1018".equals(actionCode)) {
                    LogUtil.d("ACT1018 - 앱종료");

                    finish();
                    System.exit(0);
                }
                // SNS 호출 (snsType = 1:네이버, 2:카카오톡, 3:facebook)
                else if ("ACT1020".equals(actionCode)) {
                    LogUtil.d("ACT1020 - SNS호출");

                    if(actionParamObj.has("snsType")) {
                        if(actionParamObj.getString("snsType").equals("1")) {
                            // 네이버 로그인
                            callNaverLogin();
                        } else if(actionParamObj.getString("snsType").equals("2")) {
                            // 카카오톡 로그인
                            Session session = Session.getCurrentSession();
                            session.addCallback(new SessionCallback());
                            session.open(AuthType.KAKAO_LOGIN_ALL, WebViewActivity.this);
//                            if (session.checkAndImplicitOpen()) {
//                                // 액세스토큰 유효하거나 리프레시 토큰으로 액세스 토큰 갱신을 시도할 수 있는 경우
//                            } else {
//                                // 무조건 재로그인을 시켜야 하는 경우
//                            }

                        } else if(actionParamObj.getString("snsType").equals("3")) {
                            // 페이스북 로그인
                            if (AccessToken.isCurrentAccessTokenActive()) {
                                try {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("accessToken", AccessToken.getCurrentAccessToken());      // getAccessToken
                                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                initFacebookLogin();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getAppVersion() {
            String versionName = "";
            try {
                PackageInfo pi = null;
                try {
                    pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                versionName = pi.versionName;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return versionName;
        }

        @JavascriptInterface
        public String getDeviceId() {
            return HNApplication.mDeviceId;
        }

        @JavascriptInterface
        public String getMemberKey() {
            return HNSharedPreference.getSharedPreference(getApplicationContext(), "memberKey");
        }

        @JavascriptInterface
        public void setMemberKey(String paramString) {
            HNSharedPreference.putSharedPreference(getApplicationContext(), "memberKey", paramString);
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

    /**
     * For NicePay
     * 계좌이체 결과값을 받아와 오류시 해당 메세지를, 성공시에는 결과 페이지를 호출한다.
     */
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

        if (data == null) return;

        if (data.hasExtra("bankpay_value")) {
            String resVal = data.getExtras().getString("bankpay_value");
            String resCode = data.getExtras().getString("bankpay_code");
            Log.i("NICE", "resCode : " + resCode);
            Log.i("NICE", "resVal : " + resVal);

            if ("091".equals(resCode)) {      //계좌이체 결제를 취소한 경우
                AlertUtil.showConfirmDialog(this, "인증 오류", "계좌이체 결제를 취소하였습니다.");
                mWebView.postUrl(MERCHANT_URL, null);
            } else if ("060".equals(resCode)) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "타임아웃");
                mWebView.postUrl(MERCHANT_URL, null);
            } else if ("050".equals(resCode)) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "전자서명 실패");
                mWebView.postUrl(MERCHANT_URL, null);
            } else if ("040".equals(resCode)) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "OTP/보안카드 처리 실패");
                mWebView.postUrl(MERCHANT_URL, null);
            } else if ("030".equals(resCode)) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "인증모듈 초기화 오류");
                mWebView.postUrl(MERCHANT_URL, null);
            } else if ("000".equals(resCode)) { // 성공일 경우
                String postData = "callbackparam2=" + BANK_TID + "&bankpay_code=" + resCode + "&bankpay_value=" + resVal;
                // nice sample
//                mWebView.postUrl(NICE_BANK_URL, EncodingUtils.getBytes(postData, "euc-kr"));
                try {
                    mWebView.postUrl(NICE_BANK_URL, postData.getBytes("euc-kr"));
                } catch (UnsupportedEncodingException e) {
                    System.out.println("Unsupported character set");
                }
            }
        } else if (data.hasExtra("SCAN_RESULT") && data.hasExtra("SCAN_RESULT_FORMAT")) {
            Toast.makeText(this, "[SCAN_RESULT]" + data.getStringExtra("SCAN_RESULT") + "\n"
                    + "[SCAN_RESULT_FORMAT]" + data.getStringExtra("SCAN_RESULT_FORMAT"), Toast.LENGTH_LONG).show();

            String result = "";
            try {
                JSONObject jObj = new JSONObject();
                jObj.put("resultcd", "0");              // 0:성공. 1:실패
                jObj.put("returnCode", data.getStringExtra("SCAN_RESULT"));

                result = jObj.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            executeJavascript(mCallback + "(" + result + ")");
        }
    }

    /**
     * 계좌이체 데이터를 파싱한다. *
     *
     * @param str
     * @return
     */
    private String makeBankPayData(String str) {
        String[] arr = str.split("&");
        String[] parse_temp;
        HashMap<String, String> tempMap = new HashMap<String, String>();
        for (int i = 0; i < arr.length; i++) {
            try {
                parse_temp = arr[i].split("=");
                tempMap.put(parse_temp[0], parse_temp[1]);
            } catch (Exception e) {

            }
        }
        BANK_TID = tempMap.get("user_key");
        NICE_BANK_URL = tempMap.get("callbackparam1");

        return str;
    }

    /**
     * ISP가 설치되지 않았을때 처리를 진행한다.
     */
    private void installISP() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setMessage("ISP결제를 하기 위해서는 ISP앱이 필요합니다.\n설치 페이지로  진행하시겠습니까?");
        d.setTitle("ISP 설치");
        d.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ISP_LINK));
                startActivity(intent);
            }
        });
        d.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                //결제 초기 화면을 요청합니다.
                mWebView.postUrl(MERCHANT_URL, null);

            }
        });
        d.show();
    }

    /**
     * 계좌이체 BANKPAY 설치 진행 안내
     */
    private void installKFTC() {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setMessage("계좌이체 결제를 하기 위해서는 BANKPAY 앱이 필요합니다.\n설치 페이지로  진행하시겠습니까?");
        d.setTitle("계좌이체 BANKPAY 설치");
        d.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(KFTC_LINK));
                startActivity(intent);
            }
        });
        d.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mWebView.postUrl(MERCHANT_URL, null);
            }
        });
        d.show();
    }


    private void sendPushReceiveToServer(String pushUid) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject jObj = new JSONObject();
                    jObj.put("pushUid", "");
                    mHNCommTran = new HNCommTran(new HNCommTranInterface() {
                        @Override
                        public void recvMsg(String tranCode, String params) {
                            if (tranCode.equals(HNApplication.URL + "/m/app/pushReceive.asp")) {
                                LogUtil.e("recv pushRegister : " + tranCode + " : " + params);
                            }
                        }
                    });
                    mHNCommTran.sendMsg(HNApplication.URL + "/m/app/pushReceive.asp", jObj);
                    return;
                } catch (Exception localException) {
                    localException.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        LogUtil.e("sendRegistrationToServer : " + token);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject jObj = new JSONObject();
                    jObj.put("os", "Android");
                    jObj.put("memberKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "memberKey"));
                    jObj.put("pushKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "pushtoken"));
                    jObj.put("deviceId", HNApplication.mDeviceId);

                    mHNCommTran = new HNCommTran(new HNCommTranInterface() {
                        @Override
                        public void recvMsg(String tranCode, String params) {
                            if (tranCode.equals(HNApplication.PUSH_URL)) {
                                LogUtil.e("recv pushRegister : " + tranCode + " : " + params);
                            }
                        }
                    });
                    mHNCommTran.sendMsg(HNApplication.PUSH_URL, jObj);
                    return;
                } catch (Exception localException) {
                    localException.printStackTrace();
                }
            }
        }).start();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                + ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                + ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
//                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.GET_ACCOUNTS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                Snackbar.make(this.findViewById(android.R.id.content),
                        "Please Grant Permissions to upload profile photo",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(
                                            new String[]{
                                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                                    Manifest.permission.CAMERA,
//                                                    Manifest.permission.CALL_PHONE,
                                                    Manifest.permission.GET_ACCOUNTS,
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                            },
                                            Constants.PERMISSIONS_MULTIPLE_REQUEST);
                                }
                            }
                        }).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                    Manifest.permission.CAMERA,
//                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.GET_ACCOUNTS,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            Constants.PERMISSIONS_MULTIPLE_REQUEST);
                }
            }
        } else {
            // write your logic code if permission already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (cameraPermission && writeExternalFile && readExternalFile) {
                        // write your logic here
                        mLlPermission.setVisibility(View.GONE);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(
                                    new String[]{
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                            Manifest.permission.CAMERA,
                                            Manifest.permission.CALL_PHONE,
                                            Manifest.permission.GET_ACCOUNTS,
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                    },
                                    Constants.PERMISSIONS_MULTIPLE_REQUEST);
                        }

                        Snackbar.make(this.findViewById(android.R.id.content),
                                "Please Grant Permissions to upload profile photo",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(
                                                    new String[]{
                                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                                            Manifest.permission.CAMERA,
//                                                            Manifest.permission.CALL_PHONE,
                                                            Manifest.permission.GET_ACCOUNTS,
                                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                                    },
                                                    Constants.PERMISSIONS_MULTIPLE_REQUEST);
                                        }
                                    }
                                }).show();
                    }
                }
                break;
        }
    }

    private void callQR() {
        // zxing init
        mIntegrator = new IntentIntegrator(this);

        mIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
//        mIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        mIntegrator.setPrompt("Scan a barcode");
        if (mCameraType == 0) {
            mIntegrator.setCameraId(1);  // Use a specific camera of the device
        } else {
            mIntegrator.setCameraId(0);  // Use a specific camera of the device
        }
        mIntegrator.setBeepEnabled(false);
        mIntegrator.initiateScan();
    }

    ;

    // 사진촬영
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
//                Uri providerURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
//                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, providerURI);
//                startActivityForResult(takePictureIntent, Constants.REQUEST_SELECT_IMAGE_CAMERA);
            }
        }
    }

    // 사진 앨범선택
    private void galleryAddPic() {
        Intent intent = new Intent(this, AlbumSelectActivity.class);
        startActivityForResult(intent, Constants.REQUEST_SELECT_IMAGE_ALBUM);
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

    // 폴더삭제
    private void deleteDir(String path) {
        File file = new File(path);
        File[] childFileList = file.listFiles();
        for (File childFile : childFileList) {
            if (childFile.isDirectory()) {
                deleteDir(childFile.getAbsolutePath());    //하위 디렉토리 루프
            } else {
                childFile.delete();    //하위 파일삭제
            }
        }

        file.delete();    //root 삭제
    }

    private String encodeImage(String path) {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

//        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        //Base64.de
        return encodedImage;
    }

    public boolean hasPermissions(Context context, String... permissions) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                for (String permission : permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            } else {
                Intent intent = new Intent(getApplicationContext(), SelectImageMethodActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private String getBase64String(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] imageBytes = baos.toByteArray();

        String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        return base64String;
    }

    public class uploadImagesAsyncTask extends AsyncTask<String, Void, String> {
        public String result = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle("알림");
            mProgressDialog.setMessage("처리중입니다.\n잠시만 기다려 주세요.");
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String token = (String) params[0];

            HashMap<String, String> param = new HashMap<String, String>();
            param.put("token", token);
            param.put("service", "GOODSIMGSREG");

            File file = new File(getFilesDir() + "/");
            File[] flist = file.listFiles();
            Log.d("SeongKwon", "*************************************************");
            if (flist.length > 0) {
                mSelectedImages = new ArrayList<Image>();
                for (int i = 0; i < flist.length; i++) {
                    String fname = flist[i].getName();
                    Log.d("SeongKwon", "name = " + fname);

                    long id = -1;
                    String name = fname;
                    String path = file.getAbsolutePath() + "/" + name;
                    boolean isSelected = false;

                    Log.d("SeongKwon", "=========================");
                    Log.d("SeongKwon", "id = " + id);
                    Log.d("SeongKwon", "name = " + name);
                    Log.d("SeongKwon", "path = " + path);
                    Log.d("SeongKwon", "isSelected = " + isSelected);
                    Log.d("SeongKwon", "=========================");

                    if (file.exists() && fname.contains(".jpg")) {
                        mSelectedImages.add(new Image(id, name, path, isSelected, -1));
                    }
                }

                try {
                    result = UploadUtil.upload(mContext, "http://laos.mallshopping.co.kr/m/app/", mSelectedImages, param);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                result = "-1";
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            mProgressDialog.dismiss();
            Log.e("SeongKwon", s);
            if (s == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                builder.setTitle("알림");
                builder.setMessage("사진 등록 중 오류가 발생했습니다.\n다시 시도해 주세요.");
                AlertDialog dialog = builder.create();
                dialog.show();

                return;
            }

            if (s.equals("-1")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                builder.setTitle("알림");
                builder.setMessage("등록 할 사진을 선택해 주세요.");
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                executeJavascript(mCallback + "(" + s + ")");
            }
        }
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

    public boolean isServiceRunningCheck(String serviceName) {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d("SeongKwon", service.service.getClassName());
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //// ====================================================================== ////
    //// ====================================================================== ////
    ////                               SNS LOGIN                                ////
    //// ====================================================================== ////
    //// ====================================================================== ////

    // 네이버 로그인
    public void callNaverLogin() {
        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                mContext
                , getString(R.string.naver_client_id)        // 애플리케이션 등록 후 발급받은 클라이언트 아이디
                , getString(R.string.naver_client_secret)    // 애플리케이션 등록 후 발급받은 클라이언트 시크릿
                , mContext.getResources().getString(R.string.app_name)     // 네이버 앱의 로그인 화면에 표시할 애플리케이션 이름. 모바일 웹의 로그인 화면을 사용할 때는 서버에 저장된 애플리케이션 이름이 표시됩니다.
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );

        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        mOAuthLoginModule.startOauthLoginActivity(WebViewActivity.this, mOAuthLoginHandler);
                    }
                });
            }
        }).start();
    }

    /**
     * OAuthLoginHandler를 startOAuthLoginActivity() 메서드 호출 시 파라미터로 전달하거나 OAuthLoginButton
     객체에 등록하면 인증이 종료되는 것을 확인할 수 있습니다.
     */
    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(final boolean success) {
            if (success) {
                final String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                String tokenType = mOAuthLoginModule.getTokenType(mContext);
            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }

            SendMassgeHandler mMainHandler = new SendMassgeHandler();
            if(mMainHandler != null) {
                Message msg = mMainHandler.obtainMessage();
                msg.what = SEND_NAVER_MESSAGE;
                if (success) {
                    msg.arg1 = 1;
                } else {
                    msg.arg1 = 0;
                }
                mMainHandler.sendMessage(msg);
            }
        };
    };

    private String NaverProfile(String accessToken) {
        String header = "Bearer " + accessToken; // Bearer 다음에 공백 추가
        try {
            String apiURL = "https://openapi.naver.com/v1/nid/me";
            URL url = new URL(apiURL);

            HttpURLConnection con = (HttpURLConnection)url.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", header);
            con.connect();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("accessToken", accessToken);      // getAccessToken
            jsonObject.put("userInfo", response.toString());      // 사용자정보
            executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // Handler 클래스
    class SendMassgeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SEND_NAVER_MESSAGE :
                    if(msg.arg1 == 1) {
                        final String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                        String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                        long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                        String tokenType = mOAuthLoginModule.getTokenType(mContext);
                        Log.e("SeongKwon" , "accessToken = " + accessToken);
                        Log.e("SeongKwon" , "refreshToken = " + refreshToken);
                        Log.e("SeongKwon" , "expiresAt = " + String.valueOf(expiresAt));
                        Log.e("SeongKwon" , "tokenType = " + tokenType);
                        Log.e("SeongKwon" , "state = " + mOAuthLoginModule.getState(mContext).toString());

                        mNaverMessage = "accessToken = " + accessToken + "\n";
                        mNaverMessage += "refreshToken = " + refreshToken + "\n";
                        mNaverMessage += "expiresAt = " + expiresAt + "\n";
                        mNaverMessage += "tokenType = " + tokenType + "\n";
                        mNaverMessage += "state = " + mOAuthLoginModule.getState(mContext).toString() + "\n";

                        try {
                            // 네이버 프로필 정보 요청
//                            new NaverProfile().execute(accessToken);
                            new Thread() {
                                public void run() {
                                    NaverProfile(accessToken);
                                }
                            }.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                        String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                        Log.e("SeongKwon" , "errorCode = " + errorCode);
                        Log.e("SeongKwon" , "errorDesc = " + errorDesc);

                        mNaverMessage += "errorCode = " + errorCode + "\n";
                        mNaverMessage += "errorDesc = " + errorDesc + "\n";
                        // 취소시 : errorCode = user_cancel
                        // 취소시 : errorDesc = user_cancel
                    }
                    break;
                case SEND_KAKAO_MESSAGE :
                    Log.d("SeongKwon", "msg = " + msg.toString());
                    break;
                default:
                    break;
            }
        }
    }

    // 카카오톡 로그인
    private class SessionCallback implements ISessionCallback {
        // 로그인에 성공한 상태
        @Override
        public void onSessionOpened() {
            try {
                requestMe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 로그인에 실패한 상태
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
        }

        // 사용자 정보 요청
        private void requestMe() {
            // 사용자정보 요청 결과에 대한 Callback
            UserManagement.getInstance().me(new MeV2ResponseCallback() {
                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.d("SeongKwon", "SessionCallback :: onSessionClosed : " + errorResult.getErrorMessage());
                }

                @Override
                public void onSuccess(MeV2Response result) {
                    Log.d("SeongKwon", "SessionCallback :: onSuccess");
                    try {
                        String email = result.getKakaoAccount().getEmail();
                        String nickname = result.getNickname();
                        String profileImagePath = result.getProfileImagePath();
                        String thumnailPath = result.getThumbnailImagePath();
                        long id = result.getId();

                        JSONObject jsonAccount = new JSONObject();
                        jsonAccount.put("email", email);
                        jsonAccount.put("nickname", nickname);
                        jsonAccount.put("profileImagePath", profileImagePath);
                        jsonAccount.put("thumnailPath", thumnailPath);
                        jsonAccount.put("id", id);
                        Log.e("SeongKwon", "jsonAccount : " + jsonAccount.toString());

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("accessToken", Session.getCurrentSession().getAccessToken());      // getAccessToken
                        jsonObject.put("userInfo", jsonAccount);      // 사용자정보
                        executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    // 페이스북 로그인
    private void initFacebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(WebViewActivity.this, permissionNeeds);

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("SeongKwon", "onSuccess - getAccessToken : " + loginResult.getAccessToken().getToken());
                Log.d("SeongKwon", "onSuccess - getUserId : " + loginResult.getAccessToken().getUserId());
                Log.d("SeongKwon", "onSuccess - getExpires : " + loginResult.getAccessToken().getExpires());
                Log.d("SeongKwon", "onSuccess - getLastRefresh : " + loginResult.getAccessToken().getLastRefresh());

                // getFbInfo();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("accessToken", loginResult.getAccessToken().getToken());      // getAccessToken
                    executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancel() {
                Log.d("SeongKwon", "onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("SeongKwon", "onError : " + error.getLocalizedMessage());
            }
        });
    }

    private void getFbInfo() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        Log.d("SeongKwon", "====================================0");
        Log.d("SeongKwon", "onSuccess - getToken : " + accessToken.getToken());
        Log.d("SeongKwon", "onSuccess - getUserId : " + accessToken.getUserId());
        Log.d("SeongKwon", "onSuccess - isExpired : " + accessToken.isExpired());
        Log.d("SeongKwon", "onSuccess - getExpires : " + accessToken.getExpires());
        Log.d("SeongKwon", "onSuccess - getLastRefresh : " + accessToken.getLastRefresh());
        Log.d("SeongKwon", "====================================1");

        mFacebookMessage = "Token = " + accessToken.getToken() + "\n";
        mFacebookMessage += "UserId = " + accessToken.getUserId() + "\n";
        mFacebookMessage += "Expires = " + accessToken.getExpires() + "\n";
        mFacebookMessage += "LastRefresh = " + accessToken.getLastRefresh() + "\n";

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            Log.d("SeongKwon", "fb json object: " + object);
                            Log.d("SeongKwon", "fb graph response: " + response);

                            mFacebookMessage += "fb_json_object = " + object + "\n";

                            runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                    alertDialogBuilder.setTitle("알림");
                                    alertDialogBuilder.setMessage(mFacebookMessage)
                                            .setPositiveButton("확인" , new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {}
                                            })
                                            .setCancelable(false)
                                            .create().show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,first_name,last_name,email,gender,birthday"); // id,first_name,last_name,email,gender,birthday,cover,picture.type(large)
        request.setParameters(parameters);
        request.executeAsync();
    }
}