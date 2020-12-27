package com.ncggloval.hahakoreashop;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.provider.MediaStore.Audio;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.baidu.android.pushservice.BasicPushNotificationBuilder;
import com.baidu.android.pushservice.CustomPushNotificationBuilder;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.baidu.ufosdk.UfoSDK;
import com.ncggloval.hahakoreashop.common.BackPressCloseHandler;
import com.ncggloval.hahakoreashop.common.HNApplication;
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference;
import com.ncggloval.hahakoreashop.util.EtcUtil;
import com.ncggloval.hahakoreashop.util.LogUtil;
import com.tencent.mm.sdk.openapi.SendAuth;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 云推送Demo主Activity。
 * 代码中，注释以Push标注开头的，表示接下来的代码块是Push接口调用示例
 */
public class MainActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQ_CODE_INIT_APIKEY = 0;

    /** 魅族代理需要的魅族appid和appkey，请到魅族推送官网申请 **/
    private static final String mzAppId = "";
    private static final String mzAppKey = "";

    /** 小米代理需要的小米appid和appkey，请到小米推送官网申请 **/
    private static final String xmAppId = "";
    private static final String xmAppKey = "";

    /** OPPO代理需要的OPPO appkey和appsecret，请到OPPO推送官网申请 **/
    private static final String opAppKey = "";
    private static final String opAppSecret = "";

    // main
    private BackPressCloseHandler mBackPressCloseHandler;
    private LinearLayout mLlloading;

    private WebView wv;
    private String mCallback;
    private String mGetUserinfo = "";

    private String mLandingUrl = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Utils.logStringCache = Utils.getLogText(getApplicationContext());

        setContentView(R.layout.activity_main);
        // 启动百度push
        checkStoragePerms(REQ_CODE_INIT_APIKEY);

        /**
         * 以下通知栏设置2选1。使用默认通知时，无需添加以下设置代码。
         */

        // 1.默认通知
        // 若您的应用需要适配Android O（8.x）系统，且将目标版本targetSdkVersion设置为26及以上时：
        // SDK提供设置Android O（8.x）新特性---通知渠道的设置接口。
        // 若不额外设置，SDK将使用渠道名默认值"云推送"；您也可以仿照以下3行代码自定义channelId/channelName。
        // 注：非targetSdkVersion 26的应用无需以下调用且不会生效
        BasicPushNotificationBuilder bBuilder = new BasicPushNotificationBuilder();
        bBuilder.setChannelId("testDefaultChannelId");
        bBuilder.setChannelName("testDefaultChannelName");
        // PushManager.setDefaultNotificationBuilder(this, bBuilder); //使自定义channel生效

        // 2.自定义通知
        // 设置自定义的通知样式，具体API介绍见用户手册
        // 请在通知推送界面中，高级设置->通知栏样式->自定义样式，选中并且填写值：1，
        // 与下方代码中 PushManager.setNotificationBuilder(this, 1, cBuilder)中的第二个参数对应
        CustomPushNotificationBuilder cBuilder = new CustomPushNotificationBuilder(
                R.layout.notification_custom_builder,
                R.id.notification_icon,
                R.id.notification_title,
                R.id.notification_text);

        cBuilder.setNotificationFlags(Notification.FLAG_AUTO_CANCEL);
        cBuilder.setNotificationDefaults(Notification.DEFAULT_VIBRATE);
        cBuilder.setStatusbarIcon(this.getApplicationInfo().icon);
        cBuilder.setLayoutDrawable(R.drawable.simple_notification_icon);
        cBuilder.setNotificationSound(Uri.withAppendedPath(
                Audio.Media.INTERNAL_CONTENT_URI, "6").toString());
        // 若您的应用需要适配Android O（8.x）系统，且将目标版本targetSdkVersion设置为26及以上时：
        // 可自定义channelId/channelName, 若不设置则使用默认值"Push"；
        // 注：非targetSdkVersion 26的应用无需以下2行调用且不会生效
        cBuilder.setChannelId("testId");
        cBuilder.setChannelName("testName");
        // 推送高级设置，通知栏样式设置为下面的ID，ID应与server下发字段notification_builder_id值保持一致
        PushManager.setNotificationBuilder(this, 1, cBuilder);

        // main
        if (HNSharedPreference.getSharedPreference(this, "deviceId").equals("")) {
            HNApplication.mDeviceId = EtcUtil.getRandomKey(16);

            HNSharedPreference.putSharedPreference(this, "deviceId", HNApplication.mDeviceId);
        } else {
            HNApplication.mDeviceId = HNSharedPreference.getSharedPreference(this, "deviceId");
        }
        Log.e(TAG, "HNApplication.mDeviceId : " + HNApplication.mDeviceId);
        // Back Handler
        mBackPressCloseHandler = new BackPressCloseHandler(this);
        HNApplication.mIsFirstLoading = true;
        mLlloading = (LinearLayout)findViewById(R.id.ll_loading);

        // Wechat
        HNApplication.mWechatApi = WXAPIFactory.createWXAPI(this, HNApplication.APP_ID, true);
        HNApplication.mWechatApi.registerApp(HNApplication.APP_ID);

        mLandingUrl = getIntent().getStringExtra("url");

        // WebView 초기화
        initWebView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mLandingUrl = intent.getStringExtra("url");
        Log.e(TAG, "onNewIntent mLandingUrl : " + mLandingUrl);
        loadUrl();
    }

    // 删除tag操作
    private void deleteTags() {
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText textviewGid = new EditText(MainActivity.this);
        textviewGid.setHint(R.string.tags_hint);
        layout.addView(textviewGid);

        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setView(layout);
        builder.setPositiveButton(R.string.text_btn_delTags,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Push: 删除tag调用方式
                        List<String> tags = Utils.getTagsList(textviewGid
                                .getText().toString());
                        PushManager.delTags(getApplicationContext(), tags);
                    }
                });
        builder.show();
    }

    // 设置标签,以英文逗号隔开
    private void setTags() {
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText textviewGid = new EditText(MainActivity.this);
        textviewGid.setHint(R.string.tags_hint);
        layout.addView(textviewGid);

        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setView(layout);
        builder.setPositiveButton(R.string.text_btn_setTags,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Push: 设置tag调用方式
                        List<String> tags = Utils.getTagsList(textviewGid
                                .getText().toString());
                        PushManager.setTags(getApplicationContext(), tags);
                    }

                });
        builder.show();
    }

    // api_key 绑定
    private void initWithApiKey() {
        Log.e(TAG, "initWithApiKey");
        // 开启华为代理，如需开启，请参考华为代理接入文档
        //！！应用需要已经在华为推送官网注册
        PushManager.enableHuaweiProxy(this, true);
        // 开启魅族代理，如需开启，请参考魅族代理接入文档
        //！！需要将mzAppId和mzAppKey修改为自己应用在魅族推送官网申请的APPID和APPKEY
        PushManager.enableMeizuProxy(this, true, mzAppId, mzAppKey);
        // 开启OPPO代理，如需开启，请参考OPPO代理接入文档
        //！！需要将opAppKey和opAppSecret修改为自己应用在OPPO推送官网申请的APPKEY和APPSECRET
        PushManager.enableOppoProxy(this, true, opAppKey, opAppSecret);
        // 开启小米代理，如需开启，请参考小米代理接入文档
        //！！需要将xmAppId和xmAppKey修改为自己应用在小米推送官网申请的APPID和APPKEY
        PushManager.enableXiaomiProxy(this, true, xmAppId, xmAppKey);
        // 开启VIVO代理，如需开启，请参考VIVO代理接入文档
        //！！需要将AndroidManifest.xml中com.vivo.push.api_key和com.vivo.push.app_id修改为自己应用在VIVO推送官网申请的APPKEY和APPID
        PushManager.enableVivoProxy(this, true);
        // Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
        // 这里把apikey存放于manifest文件中，只是一种存放方式，
        // 您可以用自定义常量等其它方式实现，来替换参数中的Utils.getMetaValue(PushDemoActivity.this,
        // "api_key")
//        ！！请将AndroidManifest.xml api_key 字段值修改为自己的 api_key 方可使用 ！！
        //！！ATTENTION：You need to modify the value of api_key to your own in AndroidManifest.xml to use this Demo !!
        PushManager.startWork(getApplicationContext(),
                PushConstants.LOGIN_TYPE_API_KEY,
                Utils.getMetaValue(MainActivity.this, "api_key"));
        Log.e(TAG, "PushManager.startWork end");
    }

    // 解绑
    private void unBindForApp() {
        // Push：解绑
        PushManager.stopWork(getApplicationContext());
    }

    // 列举tag操作
    private void showTags() {
        // Push：标签列表
        PushManager.listTags(getApplicationContext());
    }

    // 设置免打扰时段
    private void setunDistur() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.bpush_setundistur_time);

        final TimePicker startPicker = (TimePicker) window
                .findViewById(R.id.start_time_picker);
        final TimePicker endPicker = (TimePicker) window
                .findViewById(R.id.end_time_picker);
        startPicker.setIs24HourView(true);
        endPicker.setIs24HourView(true);
        startPicker
                .setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        endPicker
                .setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);

        Button set = (Button) window.findViewById(R.id.btn_set);
        set.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int startHour = startPicker.getCurrentHour();
                int startMinute = startPicker.getCurrentMinute();
                int endHour = endPicker.getCurrentHour();
                int endMinute = endPicker.getCurrentMinute();

                if (startHour == 0 && startMinute == 0 && endHour == 0
                        && endMinute == 0) {
                    Toast.makeText(getApplicationContext(), R.string.text_cancel_disturb,
                            Toast.LENGTH_SHORT).show();
                } else if (startHour > endHour
                        || (startHour == endHour && startMinute > endMinute)) {
                    setToastText(getString(R.string.text_first_day) + startHour + ":" + startMinute,
                            getString(R.string.text_second_day) + endHour + ":" + endMinute);
                } else {
                    setToastText(startHour + ":" + startMinute, endHour + ":"
                            + endMinute);
                }

                // Push: 设置免打扰时段
                // startHour startMinute：开始 时间 ，24小时制，取值范围 0~23 0~59
                // endHour endMinute：结束 时间 ，24小时制，取值范围 0~23 0~59
                PushManager.setNoDisturbMode(getApplicationContext(),
                        startHour, startMinute, endHour, endMinute);

                alertDialog.cancel();
            }

        });
        Button guide = (Button) window.findViewById(R.id.btn_guide);
        guide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.text_disturb_title)
                        .setMessage(R.string.text_disturb_explain)
                        .setPositiveButton(R.string.prompt_confirm, null)
                        .show();
            }
        });

        Button cancel = (Button) window.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });

    }

    private void setToastText(String start, String end) {
        String text = getString(R.string.text_toast, start, end);
        int indexTotal = 13 + start.length();
        int indexPosition = indexTotal + 3 + end.length();
        SpannableString s = new SpannableString(text);
        s.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.red)),
                13, indexTotal, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        s.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.red)),
                indexTotal + 3, indexPosition,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(Menu.NONE, Menu.FIRST + 1, 1, R.string.prompt_about).setIcon(
                android.R.drawable.ic_menu_info_details);

        menu.add(Menu.NONE, Menu.FIRST + 2, 2, R.string.prompt_help).setIcon(
                android.R.drawable.ic_menu_help);

        menu.add(Menu.NONE, Menu.FIRST + 3, 3, R.string.prompt_feedback);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (Menu.FIRST + 1 == item.getItemId()) {
            showAbout();
            return true;
        }
        if (Menu.FIRST + 2 == item.getItemId()) {
            showHelp();
            return true;
        }

        if (Menu.FIRST + 3 == item.getItemId()) {
            showFeedback();
        }
        return false;
    }

    // 反馈
    private void showFeedback() {
        Intent intent = UfoSDK.getStartFaqIntent(this);
        intent.putExtra("faq_channel", 32918);  // faq_channel：设置常见问题来源。
        startActivity(intent);
    }

    // 关于
    private void showAbout() {
        Dialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.prompt_about).setMessage(R.string.text_about)
                .setPositiveButton(R.string.prompt_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.i(TAG, "onclick...");
                    }

                }).create();
        alertDialog.show();
    }

    // 帮助
    private void showHelp() {
        Dialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.prompt_help).setMessage(R.string.text_help)
                .setPositiveButton(R.string.prompt_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.i(TAG, "onclick...");
                    }

                }).create();
        alertDialog.show();
    }

    @Override
    public void onResume() {
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
    public void onDestroy() {
        Utils.setLogText(getApplicationContext(), Utils.logStringCache);
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }

        super.onDestroy();
    }

    private void checkStoragePerms(int requestCode) {
        Log.e(TAG, "checkStoragePerms");
        int writePermission = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        } else {
            initWithApiKey();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_INIT_APIKEY) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initWithApiKey();
                } else {
                    Toast.makeText(this,
                            "请先授予存储权限", Toast.LENGTH_SHORT).show();
                }
            } else {
                initWithApiKey();
            }
        }
    }

    private void initWebView() {
        this.wv = ((WebView) findViewById(R.id.webView));
        this.wv.setWebViewClient(new HNWebViewClient());
        this.wv.setWebChromeClient(new HNWebChromeClient());
        this.wv.getSettings().setUserAgentString(this.wv.getSettings().getUserAgentString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.wv.getSettings().setMixedContentMode(0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        this.wv.getSettings().setSupportZoom(true);
        this.wv.getSettings().setDisplayZoomControls(false);
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
//        this.wv.loadUrl(HNApplication.URL);
        loadUrl();
    }

    private void loadUrl() {
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("webview-type", "main");
        if (!TextUtils.isEmpty(mLandingUrl)) {
            wv.loadUrl(mLandingUrl, extraHeaders);
            mLandingUrl = "";
        } else {
            wv.loadUrl(HNApplication.URL, extraHeaders);
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
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "shouldOverrideUrlLoading : " + url);
            }
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

    public class HNWebChromeClient extends WebChromeClient {
        public HNWebChromeClient() {
        }

        public boolean onJsAlert(WebView paramWebView, String paramString1, String paramString2, final JsResult paramJsResult) {
            new android.support.v7.app.AlertDialog.Builder(MainActivity.this)
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
            new android.support.v7.app.AlertDialog.Builder(MainActivity.this)
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
                    Log.e(TAG, "ACT1013.mDeviceId : " + HNApplication.mDeviceId);
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
}