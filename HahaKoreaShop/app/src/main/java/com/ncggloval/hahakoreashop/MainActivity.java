package com.ncggloval.hahakoreashop;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.ncggloval.hahakoreashop.baidu.PushUtils;
import com.ncggloval.hahakoreashop.common.BackPressCloseHandler;
import com.ncggloval.hahakoreashop.common.HNApplication;
import com.ncggloval.hahakoreashop.delegator.HNCommTran;
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference;
import com.ncggloval.hahakoreashop.util.EtcUtil;
import com.ncggloval.hahakoreashop.util.LogUtil;
import com.tencent.mm.sdk.openapi.SendAuth;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private WebView wv;
    private HNCommTran mHNCommTran;

    private String mCallback;

    //    private ProgressUtil mProgressUtil;
    private BackPressCloseHandler mBackPressCloseHandler;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private Context mApplicationContext;
    private boolean mStartedBaidu = false;
    private String mBaiduApiKey;
    private String mGetUserinfo = "";
    private LinearLayout mLlloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            if (HNSharedPreference.getSharedPreference(this, "deviceId").equals("")) {
                HNApplication.mDeviceId = EtcUtil.getRandomKey(16);

                HNSharedPreference.putSharedPreference(this, "deviceId", HNApplication.mDeviceId);
            } else {
                HNApplication.mDeviceId = HNSharedPreference.getSharedPreference(this, "uuid");
            }

            // Back Handler
            mBackPressCloseHandler = new BackPressCloseHandler(this);
            HNApplication.mIsFirstLoading = true;
            mLlloading = (LinearLayout)findViewById(R.id.ll_loading);

            // Progress Dialog
//            mProgressUtil = new ProgressUtil(MainActivity.this);
//            mProgressUtil.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//            mProgressUtil.show();

            // Baidu push
//            mApplicationContext = this.getApplicationContext();
//            mBaiduApiKey = readChinaPushApiKey();
//            LogUtil.e("BaiduApiKey : " + mBaiduApiKey);
//            if (!mStartedBaidu && mBaiduApiKey != null) {
//                LogUtil.e("PushManager.startWork : ");
//                PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, mBaiduApiKey);
//                mStartedBaidu = true;
//            }

            // Wechat
            HNApplication.mWechatApi = WXAPIFactory.createWXAPI(this, HNApplication.APP_ID, true);
            HNApplication.mWechatApi.registerApp(HNApplication.APP_ID);

            // WebView 초기화
            initWebView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (!HNApplication.mWechatUserInfo.equals("")) {
                String success = "1";               // 성공여부 - 1:성공, 2:실패
                String type = mGetUserinfo;         // 타입 - 00:위챗
                String userInfo = HNApplication.mWechatUserInfo;
                String strJavaScript = "userInfomation('" + success + "','" + type + "','" + userInfo + "')";
                this.wv.loadUrl("javascript:" + strJavaScript);  //자바스크립트 함수호출

                // 초기화
                HNApplication.mWechatUserInfo = "";

                Toast.makeText(this, "UserInfo : " + userInfo, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();

        if (PushUtils.ACTION_LOGIN.equals(action)) {
            String accessToken = intent.getStringExtra(PushUtils.EXTRA_ACCESS_TOKEN);
            PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_ACCESS_TOKEN, accessToken);

            // TODO ACCESSTOKEN 추가
            Toast.makeText(this, "Baidu accessToken : " + accessToken, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        this.wv = ((WebView) findViewById(R.id.webView));
        this.wv.setWebViewClient(new HNWebViewClient());
        this.wv.setWebChromeClient(new HNWebChromeClient());
        this.wv.getSettings().setUserAgentString(this.wv.getSettings().getUserAgentString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.wv.getSettings().setMixedContentMode(0);
        }

        this.wv.getSettings().setSupportZoom(true);
        this.wv.getSettings().setDisplayZoomControls(true);
        this.wv.getSettings().setBuiltInZoomControls(true);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            this.wv.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//        } else {
        this.wv.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
//        }

        // setSavePassword - default false in android 4.4 and above
        // setPluginState - use plugin deprecate
        // setAppCacheMaxSize - deprecate
        this.wv.getSettings().setJavaScriptEnabled(true);
        this.wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.wv.getSettings().setAllowFileAccess(true);
        this.wv.getSettings().setAllowContentAccess(true);
        this.wv.getSettings().setLoadsImagesAutomatically(true);
        this.wv.getSettings().setLoadWithOverviewMode(true);
        this.wv.getSettings().setSupportMultipleWindows(false);
        this.wv.getSettings().setUseWideViewPort(true);
        this.wv.getSettings().setDatabaseEnabled(true);
        this.wv.getSettings().setDomStorageEnabled(true);
        this.wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.wv.getSettings().setSupportMultipleWindows(true);
        this.wv.getSettings().setAppCacheEnabled(true);
        this.wv.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        this.wv.addJavascriptInterface(new AndroidBridge(), "android");
        this.wv.loadUrl(HNApplication.URL);
    }

    @Override
    protected void onDestroy() {
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }

        super.onDestroy();
    }

    public class HNWebChromeClient extends WebChromeClient {
        public HNWebChromeClient() {
        }

        public boolean onJsAlert(WebView paramWebView, String paramString1, String paramString2, final JsResult paramJsResult) {
            new AlertDialog.Builder(MainActivity.this)
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
            new AlertDialog.Builder(MainActivity.this)
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
        public HNWebViewClient() {
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            LogUtil.d("onPageLoadStopped : " + url);

            if(HNApplication.mIsFirstLoading) {
                HNApplication.mIsFirstLoading = false;
                mLlloading.setVisibility(View.GONE);
            }
//            mProgressUtil.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap paramBitmap) {
            super.onPageStarted(view, url, paramBitmap);
            LogUtil.d("onPageLoadStarted : " + url);

//            mProgressUtil.show();

            if (!url.startsWith(HNApplication.URL + "/m/goods/content.asp")) {

            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LogUtil.e("shouldOverrideUrlLoading : " + url);
            if (url != null && url.startsWith("intent://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
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
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            view.loadUrl(url);
            return false;
//            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            LogUtil.e("onReceivedError : " + error);
        }
    }

    private class AndroidBridge {
        private AndroidBridge() {
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

                            PackageManager pm = getPackageManager();
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

                // 알리페이
                if ("ACT1024".equals(actionCode)) {
                    LogUtil.d("ACT1024 - 알리페이");

                    if (actionParamObj.has("request_url")) {
                        String request_url = actionParamObj.getString("request_url");
                        LogUtil.d("request_url : " + request_url);

                        MainActivity.this.wv.loadUrl(request_url);
                    }
                }
                // 위챗페이
                else if ("ACT1025".equals(actionCode)) {
                    LogUtil.d("ACT1025 - 위챗페이");

                    if (actionParamObj.has("request_url")) {
                        String request_url = actionParamObj.getString("request_url");
                        LogUtil.d("request_url : " + request_url);

                        MainActivity.this.wv.loadUrl(request_url);
                    }
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
                        intent = new Intent(MainActivity.this, WebViewActivity.class);
                        intent.putExtra("webviewUrl", request_url);
                        startActivity(intent);
                    }
                }
                // 앱종료
                else if ("ACT1018".equals(actionCode)) {
                    LogUtil.d("ACT1018 - 앱종료");

                    finish();
                    System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getAppVersion() {
            try {
                String str = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionName;
                return str;
            } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
            }
            return "";
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

        @JavascriptInterface
        public void getUserInfo(String type) {
            LogUtil.line();
            LogUtil.e("getUserInfo : " + type);
            LogUtil.line();
            try {
                if (!type.equals("00")) {    // 위쳇로그인
                    return;
                }
                mGetUserinfo = type;
                SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";                                  // Authorization scope requested by applications
                req.state = String.valueOf(System.currentTimeMillis());       // Used to indentify applications; returned by WeChat after authentication. none
                boolean requestDone = HNApplication.mWechatApi.sendReq(req);
                LogUtil.e("SendAuth.Req done: " + requestDone);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void executeJavascript(String script) {
        if (this.wv == null) {
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
                    MainActivity.this.wv.loadUrl(formattedScript);
                } else {
                    MainActivity.this.wv.evaluateJavascript(formattedScript, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            LogUtil.d("<<onReceiveValue>>    " + value);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        mBackPressCloseHandler.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (this.wv.canGoBack()) {
                    this.wv.goBack();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

//    public void setToken(String paramString) {
//        if (Build.VERSION.SDK_INT > 8) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
//        }
//
//        HNSharedPreference.putSharedPreference(getApplicationContext(), "gcmKey", paramString);
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    JSONObject jObj = new JSONObject();
//                    jObj.put("os", "Android");
//                    jObj.put("memberKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "memberKey"));
//                    jObj.put("pushKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "gcmKey"));
//                    jObj.put("deviceId", Settings.Secure.getString(MainActivity.this.getContentResolver(), "android_id"));
//                    mHNCommTran = new HNCommTran(new HNCommTranInterface() {
//                        @Override
//                        public void recvMsg(String tranCode, JSONObject params) {
//                            if (tranCode.equals("/pushRegister.asp")) {
//                                LogUtil.e("recv pushRegister");
//                            }
//                        }
//                    });
//                    mHNCommTran.sendMsg("/pushRegister.asp", jObj);
//                    return;
//                } catch (Exception localException) {
//                    localException.printStackTrace();
//                }
//            }
//        }).start();
//    }

    // for Baidu =======================================================================================================================

    /**
     * Convenience method to obtain the Baidu API key defined in AndroidManifest.xml.
     *
     * @return Baidu API key.
     */
    private String readChinaPushApiKey() {
        try {
            return getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString(HNApplication.API_KEY);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.w("Could not retrieve China push API key from AndroidManifest.xml.  Cannot receive push messages!", e);
            return null;
        }
    }

    /**
     * Updates the Baidu event message log which is displayed to users.
     *
     * @param logMessage the new message.
     */
    private void updateMessageLog(String logMessage) {
        Toast.makeText(this, logMessage, Toast.LENGTH_LONG).show();
    }
}