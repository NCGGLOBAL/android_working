package com.ncggloval.hahakoreashop

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Notification
import android.content.Context
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.ncggloval.hahakoreashop.common.BackPressCloseHandler
import android.os.Bundle
import com.ncggloval.hahakoreashop.R
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference
import com.ncggloval.hahakoreashop.common.HNApplication
import com.ncggloval.hahakoreashop.util.EtcUtil
import com.ncggloval.hahakoreashop.MainActivity
import com.baidu.android.pushservice.BasicPushNotificationBuilder
import com.baidu.android.pushservice.CustomPushNotificationBuilder
import android.provider.MediaStore.Audio
import com.tencent.mm.sdk.openapi.WXAPIFactory
import android.content.Intent
import android.content.DialogInterface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.Spannable
import com.baidu.ufosdk.UfoSDK
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.text.TextUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import org.json.JSONObject
import org.json.JSONArray
import com.ncggloval.hahakoreashop.WebViewActivity
import com.tencent.mm.sdk.openapi.SendAuth
import android.widget.*
import com.baidu.android.pushservice.PushConstants
import com.baidu.android.pushservice.PushManager
import com.ncggloval.hahakoreashop.util.LogUtil
import java.lang.Exception
import java.net.URISyntaxException
import java.util.HashMap

/*
 * 云推送Demo主Activity。
 * 代码中，注释以Push标注开头的，表示接下来的代码块是Push接口调用示例
 */
class MainActivity : Activity(), OnRequestPermissionsResultCallback {
    // main
    private var mBackPressCloseHandler: BackPressCloseHandler? = null
    private var mLlloading: LinearLayout? = null
    private var wv: WebView? = null
    private var mCallback: String? = null
    private var mGetUserinfo = ""
    private var mLandingUrl: String? = ""
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        Utils.logStringCache = Utils.getLogText(getApplicationContext());
        setContentView(R.layout.activity_main)
        // main
        if (HNSharedPreference.getSharedPreference(this, "deviceId") == "") {
            HNApplication.mDeviceId = EtcUtil.getRandomKey(16)
            HNSharedPreference.putSharedPreference(this, "deviceId", HNApplication.mDeviceId)
        } else {
            HNApplication.mDeviceId = HNSharedPreference.getSharedPreference(this, "deviceId") ?: ""
        }
        Log.e(TAG, "HNApplication.mDeviceId : " + HNApplication.mDeviceId)
        // 启动百度push
        checkStoragePerms(REQ_CODE_INIT_APIKEY)
        /**
         * 以下通知栏设置2选1。使用默认通知时，无需添加以下设置代码。
         */

        // 1.默认通知
        // 若您的应用需要适配Android O（8.x）系统，且将目标版本targetSdkVersion设置为26及以上时：
        // SDK提供设置Android O（8.x）新特性---通知渠道的设置接口。
        // 若不额外设置，SDK将使用渠道名默认值"云推送"；您也可以仿照以下3行代码自定义channelId/channelName。
        // 注：非targetSdkVersion 26的应用无需以下调用且不会生效
        val bBuilder = BasicPushNotificationBuilder()
        bBuilder.setChannelId("testDefaultChannelId")
        bBuilder.setChannelName("testDefaultChannelName")
        // PushManager.setDefaultNotificationBuilder(this, bBuilder); //使自定义channel生效

        // 2.自定义通知
        // 设置自定义的通知样式，具体API介绍见用户手册
        // 请在通知推送界面中，高级设置->通知栏样式->自定义样式，选中并且填写值：1，
        // 与下方代码中 PushManager.setNotificationBuilder(this, 1, cBuilder)中的第二个参数对应
        val cBuilder = CustomPushNotificationBuilder(
            R.layout.notification_custom_builder,
            R.id.notification_icon,
            R.id.notification_title,
            R.id.notification_text
        )
        cBuilder.setNotificationFlags(Notification.FLAG_AUTO_CANCEL)
        cBuilder.setNotificationDefaults(Notification.DEFAULT_VIBRATE)
        cBuilder.setStatusbarIcon(this.applicationInfo.icon)
        cBuilder.setLayoutDrawable(R.drawable.simple_notification_icon)
        cBuilder.setNotificationSound(
            Uri.withAppendedPath(
                Audio.Media.INTERNAL_CONTENT_URI, "6"
            ).toString()
        )
        // 若您的应用需要适配Android O（8.x）系统，且将目标版本targetSdkVersion设置为26及以上时：
        // 可自定义channelId/channelName, 若不设置则使用默认值"Push"；
        // 注：非targetSdkVersion 26的应用无需以下2行调用且不会生效
        cBuilder.setChannelId("testId")
        cBuilder.setChannelName("testName")
        // 推送高级设置，通知栏样式设置为下面的ID，ID应与server下发字段notification_builder_id值保持一致
        PushManager.setNotificationBuilder(this, 1, cBuilder)

        // Back Handler
        mBackPressCloseHandler = BackPressCloseHandler(this)
        HNApplication.mIsFirstLoading = true
        mLlloading = findViewById<View>(R.id.ll_loading) as LinearLayout

        // Wechat
        HNApplication.mWechatApi = WXAPIFactory.createWXAPI(this, HNApplication.APP_ID, true)
        HNApplication.mWechatApi?.registerApp(HNApplication.APP_ID)
        mLandingUrl = intent.getStringExtra("url")

        // WebView 초기화
        initWebView()
    }

    override fun onNewIntent(intent: Intent) {
        mLandingUrl = intent.getStringExtra("url")
        Log.e(TAG, "onNewIntent mLandingUrl : $mLandingUrl")
        loadUrl()
    }

    // 删除tag操作
    private fun deleteTags() {
        val layout = LinearLayout(this@MainActivity)
        layout.orientation = LinearLayout.VERTICAL
        val textviewGid = EditText(this@MainActivity)
        textviewGid.setHint(R.string.tags_hint)
        layout.addView(textviewGid)
        val builder = AlertDialog.Builder(
            this@MainActivity
        )
        builder.setView(layout)
        builder.setPositiveButton(
            R.string.text_btn_delTags
        ) { dialog, which -> // Push: 删除tag调用方式
            val tags = Utils.getTagsList(
                textviewGid
                    .text.toString()
            )
            PushManager.delTags(applicationContext, tags)
        }
        builder.show()
    }

    // 设置标签,以英文逗号隔开
    private fun setTags() {
        val layout = LinearLayout(this@MainActivity)
        layout.orientation = LinearLayout.VERTICAL
        val textviewGid = EditText(this@MainActivity)
        textviewGid.setHint(R.string.tags_hint)
        layout.addView(textviewGid)
        val builder = AlertDialog.Builder(
            this@MainActivity
        )
        builder.setView(layout)
        builder.setPositiveButton(
            R.string.text_btn_setTags
        ) { dialog, which -> // Push: 设置tag调用方式
            val tags = Utils.getTagsList(
                textviewGid
                    .text.toString()
            )
            PushManager.setTags(applicationContext, tags)
        }
        builder.show()
    }

    // api_key 绑定
    private fun initWithApiKey() {
        Log.e(TAG, "initWithApiKey")
        // 开启华为代理，如需开启，请参考华为代理接入文档
        //！！应用需要已经在华为推送官网注册
        PushManager.enableHuaweiProxy(this, true)
        // 开启魅族代理，如需开启，请参考魅族代理接入文档
        //！！需要将mzAppId和mzAppKey修改为自己应用在魅族推送官网申请的APPID和APPKEY
        PushManager.enableMeizuProxy(this, true, mzAppId, mzAppKey)
        // 开启OPPO代理，如需开启，请参考OPPO代理接入文档
        //！！需要将opAppKey和opAppSecret修改为自己应用在OPPO推送官网申请的APPKEY和APPSECRET
        PushManager.enableOppoProxy(this, true, opAppKey, opAppSecret)
        // 开启小米代理，如需开启，请参考小米代理接入文档
        //！！需要将xmAppId和xmAppKey修改为自己应用在小米推送官网申请的APPID和APPKEY
        PushManager.enableXiaomiProxy(this, true, xmAppId, xmAppKey)
        // 开启VIVO代理，如需开启，请参考VIVO代理接入文档
        //！！需要将AndroidManifest.xml中com.vivo.push.api_key和com.vivo.push.app_id修改为自己应用在VIVO推送官网申请的APPKEY和APPID
        PushManager.enableVivoProxy(this, true)
        // Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
        // 这里把apikey存放于manifest文件中，只是一种存放方式，
        // 您可以用自定义常量等其它方式实现，来替换参数中的Utils.getMetaValue(PushDemoActivity.this,
        // "api_key")
//        ！！请将AndroidManifest.xml api_key 字段值修改为自己的 api_key 方可使用 ！！
        //！！ATTENTION：You need to modify the value of api_key to your own in AndroidManifest.xml to use this Demo !!
        PushManager.startWork(
            applicationContext,
            PushConstants.LOGIN_TYPE_API_KEY,
            Utils.getMetaValue(this@MainActivity, "api_key")
        )
        Log.e(TAG, "PushManager.startWork end")
    }

    // 解绑
    private fun unBindForApp() {
        // Push：解绑
        PushManager.stopWork(applicationContext)
    }

    // 列举tag操作
    private fun showTags() {
        // Push：标签列表
        PushManager.listTags(applicationContext)
    }

    // 设置免打扰时段
    private fun setunDistur() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.show()
        val window = alertDialog.window
        window!!.setContentView(R.layout.bpush_setundistur_time)
        val startPicker = window
            .findViewById<View>(R.id.start_time_picker) as TimePicker
        val endPicker = window
            .findViewById<View>(R.id.end_time_picker) as TimePicker
        startPicker.setIs24HourView(true)
        endPicker.setIs24HourView(true)
        startPicker.descendantFocusability = TimePicker.FOCUS_BLOCK_DESCENDANTS
        endPicker.descendantFocusability = TimePicker.FOCUS_BLOCK_DESCENDANTS
        val set = window.findViewById<View>(R.id.btn_set) as Button
        set.setOnClickListener {
            val startHour = startPicker.currentHour
            val startMinute = startPicker.currentMinute
            val endHour = endPicker.currentHour
            val endMinute = endPicker.currentMinute
            if (startHour == 0 && startMinute == 0 && endHour == 0 && endMinute == 0) {
                Toast.makeText(
                    applicationContext, R.string.text_cancel_disturb,
                    Toast.LENGTH_SHORT
                ).show()
            } else if (startHour > endHour
                || startHour == endHour && startMinute > endMinute
            ) {
                setToastText(
                    getString(R.string.text_first_day) + startHour + ":" + startMinute,
                    getString(R.string.text_second_day) + endHour + ":" + endMinute
                )
            } else {
                setToastText(
                    "$startHour:$startMinute", endHour.toString() + ":"
                            + endMinute
                )
            }

            // Push: 设置免打扰时段
            // startHour startMinute：开始 时间 ，24小时制，取值范围 0~23 0~59
            // endHour endMinute：结束 时间 ，24小时制，取值范围 0~23 0~59
            PushManager.setNoDisturbMode(
                applicationContext,
                startHour, startMinute, endHour, endMinute
            )
            alertDialog.cancel()
        }
        val guide = window.findViewById<View>(R.id.btn_guide) as Button
        guide.setOnClickListener {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.text_disturb_title)
                .setMessage(R.string.text_disturb_explain)
                .setPositiveButton(R.string.prompt_confirm, null)
                .show()
        }
        val cancel = window.findViewById<View>(R.id.btn_cancel) as Button
        cancel.setOnClickListener { alertDialog.cancel() }
    }

    private fun setToastText(start: String, end: String) {
        val text = getString(R.string.text_toast, start, end)
        val indexTotal = 13 + start.length
        val indexPosition = indexTotal + 3 + end.length
        val s = SpannableString(text)
        s.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.red)),
            13, indexTotal, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        s.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.red)),
            indexTotal + 3, indexPosition,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST + 1, 1, R.string.prompt_about).setIcon(
            android.R.drawable.ic_menu_info_details
        )
        menu.add(Menu.NONE, Menu.FIRST + 2, 2, R.string.prompt_help).setIcon(
            android.R.drawable.ic_menu_help
        )
        menu.add(Menu.NONE, Menu.FIRST + 3, 3, R.string.prompt_feedback)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (Menu.FIRST + 1 == item.itemId) {
            showAbout()
            return true
        }
        if (Menu.FIRST + 2 == item.itemId) {
            showHelp()
            return true
        }
        if (Menu.FIRST + 3 == item.itemId) {
            showFeedback()
        }
        return false
    }

    // 反馈
    private fun showFeedback() {
        val intent = UfoSDK.getStartFaqIntent(this)
        intent.putExtra("faq_channel", 32918) // faq_channel：设置常见问题来源。
        startActivity(intent)
    }

    // 关于
    private fun showAbout() {
        val alertDialog: Dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.prompt_about).setMessage(R.string.text_about)
            .setPositiveButton(R.string.prompt_confirm) { arg0, arg1 -> Log.i(TAG, "onclick...") }
            .create()
        alertDialog.show()
    }

    // 帮助
    private fun showHelp() {
        val alertDialog: Dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.prompt_help).setMessage(R.string.text_help)
            .setPositiveButton(R.string.prompt_confirm) { arg0, arg1 -> Log.i(TAG, "onclick...") }
            .create()
        alertDialog.show()
    }

    public override fun onResume() {
        super.onResume()
        try {
            if (HNApplication.mWechatUserInfo != "") {
                val success = "1" // 성공여부 - 1:성공, 2:실패
                val type = mGetUserinfo // 타입 - 00:위챗
                val userInfo = HNApplication.mWechatUserInfo
                val strJavaScript = "userInfomation('$success','$type','$userInfo')"
                wv!!.loadUrl("javascript:$strJavaScript") //자바스크립트 함수호출

                // 초기화
                HNApplication.mWechatUserInfo = ""
                Toast.makeText(this, "UserInfo : $userInfo", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onDestroy() {
        Utils.setLogText(applicationContext, Utils.logStringCache)
        CookieSyncManager.createInstance(this)
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
        } else {
            cookieManager.removeAllCookie()
        }
        super.onDestroy()
    }

    private fun checkStoragePerms(requestCode: Int) {
        Log.e(TAG, "checkStoragePerms")
        val writePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                requestCode
            )
        } else {
            initWithApiKey()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_CODE_INIT_APIKEY) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initWithApiKey()
                } else {
                    Toast.makeText(
                        this,
                        "请先授予存储权限", Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                initWithApiKey()
            }
        }
    }

    private fun initWebView() {
        wv = findViewById<View>(R.id.webView) as WebView
        wv!!.webViewClient = HNWebViewClient()
        wv!!.webChromeClient = HNWebChromeClient()
        wv!!.settings.setUserAgentString(wv!!.settings.userAgentString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wv!!.settings.mixedContentMode = 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        wv!!.settings.setSupportZoom(true)
        wv!!.settings.displayZoomControls = false
        wv!!.settings.builtInZoomControls = true
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            this.wv.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//        } else {
        wv!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
        //        }

        // setSavePassword - default false in android 4.4 and above
        // setPluginState - use plugin deprecate
        // setAppCacheMaxSize - deprecate
        wv!!.settings.javaScriptEnabled = true
        wv!!.settings.javaScriptCanOpenWindowsAutomatically = true
        wv!!.settings.allowFileAccess = true
        wv!!.settings.allowContentAccess = true
        wv!!.settings.loadsImagesAutomatically = true
        wv!!.settings.loadWithOverviewMode = true
        wv!!.settings.setSupportMultipleWindows(false)
        wv!!.settings.useWideViewPort = true
        wv!!.settings.databaseEnabled = true
        wv!!.settings.domStorageEnabled = true
        wv!!.settings.javaScriptCanOpenWindowsAutomatically = true
        wv!!.settings.setSupportMultipleWindows(true)
//        wv!!.settings.setAppCacheEnabled(true)
//        wv!!.settings.setAppCachePath(applicationContext.cacheDir.absolutePath)
        wv!!.addJavascriptInterface(AndroidBridge(this, wv!!), "android")
        //        this.wv.loadUrl(HNApplication.URL);
        loadUrl()
    }

    private fun loadUrl() {
        val extraHeaders: MutableMap<String, String> = HashMap()
        extraHeaders["webview-type"] = "main"
        if (!TextUtils.isEmpty(mLandingUrl)) {
            wv!!.loadUrl(mLandingUrl!!, extraHeaders)
            mLandingUrl = ""
        } else {
            wv!!.loadUrl(HNApplication.URL, extraHeaders)
        }
    }

    inner class HNWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            LogUtil.d("onPageLoadStopped : $url")
            if (HNApplication.mIsFirstLoading) {
                HNApplication.mIsFirstLoading = false
                mLlloading!!.visibility = View.GONE
            }
            //            mProgressUtil.dismiss();
        }

        override fun onPageStarted(view: WebView, url: String, paramBitmap: Bitmap) {
            super.onPageStarted(view, url, paramBitmap)
            LogUtil.d("onPageLoadStarted : $url")

//            mProgressUtil.show();
            if (!url.startsWith(HNApplication.URL + "/m/goods/content.asp")) {
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "shouldOverrideUrlLoading : $url")
            }
            if (url != null && url.startsWith("intent://")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage =
                        packageManager.getLaunchIntentForPackage(intent.getPackage()!!)
                    if (existPackage != null) {
                        startActivity(intent)
                    } else {
                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        marketIntent.data = Uri.parse("market://details?id=" + intent.getPackage())
                        startActivity(marketIntent)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    intent?.let { startActivity(it) }
                    return true
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
            view.loadUrl(url)
            return false
            //            return super.shouldOverrideUrlLoading(view, url);
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            LogUtil.e("onReceivedError : $error")
        }
    }

    inner class HNWebChromeClient : WebChromeClient() {
        override fun onJsAlert(
            paramWebView: WebView,
            paramString1: String,
            paramString2: String,
            paramJsResult: JsResult
        ): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(resources.getString(R.string.alert_title))
                .setMessage(paramString2)
                .setPositiveButton(
                    resources.getString(R.string.confirm),
                    DialogInterface.OnClickListener { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() })
                .setCancelable(false).create().show()
            return true
        }

        override fun onJsConfirm(
            paramWebView: WebView,
            paramString1: String,
            paramString2: String,
            paramJsResult: JsResult
        ): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(resources.getString(R.string.alert_title))
                .setMessage(paramString2).setPositiveButton(
                    resources.getString(R.string.confirm),
                    DialogInterface.OnClickListener { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() })
                .setNegativeButton(
                    resources.getString(R.string.cancel),
                    DialogInterface.OnClickListener { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.cancel() })
                .setCancelable(false).create().show()
            return true
        }
    }

    private inner class AndroidBridge(
        private val context: Context,
        private val webview: WebView)
    {
        @JavascriptInterface
        fun iwebaction(inputJsonString: String) {
            var intent: Intent? = null
            var jObj: JSONObject? = null
            val response: JSONObject? = null
            try {
                LogUtil.d(
                    "iwebaction",
                    "inputJsonString.toString() = " + inputJsonString.trim { it <= ' ' })
                jObj = JSONObject(inputJsonString.trim { it <= ' ' })
                val actionCode = jObj.getString("action_code")
                var actionParamArray: JSONArray? = null
                var actionParamObj: JSONObject? = null

                // param
                if (jObj.has("action_param")) {
                    actionParamArray = jObj.getJSONArray("action_param")
                    actionParamObj = actionParamArray.getJSONObject(0)
                }

                // callback
                if (jObj.has("callBack")) {
                    mCallback = jObj.getString("callBack")
                }

                // 앱 설치체크
                if ("ACT0001" == actionCode) {
                    LogUtil.d("ACT0001 - 앱 설치체크")
                    var checkInstall = 0
                    if (actionParamObj!!.has("packagename")) {
                        checkInstall = try {
                            val packagename = actionParamObj.getString("packagename")
                            LogUtil.d("packagename : $packagename")
                            val pm = packageManager
                            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES)
                            1
                        } catch (e: Exception) {
                            0
                        }
                    }
                    val jsonObject = JSONObject()
                    jsonObject.put("checkInstall", checkInstall) // check install - 0 : 미설치, 1 : 설치
                    executeJavascript("$mCallback($jsonObject)")
                }

                // 알리페이
                if ("ACT1024" == actionCode) {
                    LogUtil.d("ACT1024 - 알리페이")
                    if (actionParamObj!!.has("request_url")) {
                        val request_url = actionParamObj.getString("request_url")
                        LogUtil.d("request_url : $request_url")
                        wv!!.loadUrl(request_url)
                    }
                } else if ("ACT1025" == actionCode) {
                    LogUtil.d("ACT1025 - 위챗페이")
                    if (actionParamObj!!.has("request_url")) {
                        val request_url = actionParamObj.getString("request_url")
                        LogUtil.d("request_url : $request_url")
                        wv!!.loadUrl(request_url)
                    }
                } else if ("ACT1013" == actionCode) {
                    LogUtil.d("ACT1013 - 휴대폰정보 가져오기")
                    Log.e(TAG, "ACT1013.mDeviceId : " + HNApplication.mDeviceId)
                    val jsonObject = JSONObject()
                    jsonObject.put("device", "a") // 디바이스 구분값 (i : IOS, a : android)
                    jsonObject.put("deviceId", deviceId) // 디바이스 아이디
                    jsonObject.put("version", appVersion) // version
                    executeJavascript("$mCallback($jsonObject)")
                } else if ("ACT1015" == actionCode) {
                    LogUtil.d("ACT1015 - 웹뷰 새창")
                    if (actionParamObj!!.has("url")) {
                        val request_url = actionParamObj.getString("url")
                        LogUtil.d("url : $request_url")
                        intent = Intent(this@MainActivity, WebViewActivity::class.java)
                        intent.putExtra("webviewUrl", request_url)
                        startActivity(intent)
                    }
                } else if ("ACT1018" == actionCode) {
                    LogUtil.d("ACT1018 - 앱종료")
                    finish()
                    System.exit(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @get:JavascriptInterface
        val appVersion: String
            get() {
                try {
                    return packageManager.getPackageInfo(packageName, 0).versionName
                } catch (localNameNotFoundException: PackageManager.NameNotFoundException) {
                }
                return ""
            }

        @get:JavascriptInterface
        val deviceId: String
            get() = HNApplication.mDeviceId

        @get:JavascriptInterface
        @set:JavascriptInterface
        var memberKey: String?
            get() = HNSharedPreference.getSharedPreference(applicationContext, "memberKey")
            set(paramString) {
                HNSharedPreference.putSharedPreference(applicationContext, "memberKey", paramString)
            }

        @JavascriptInterface
        fun getUserInfo(type: String) {
            LogUtil.line()
            LogUtil.e("getUserInfo : $type")
            LogUtil.line()
            try {
                if (type != "00") {    // 위쳇로그인
                    return
                }
                mGetUserinfo = type
                val req = SendAuth.Req()
                req.scope = "snsapi_userinfo" // Authorization scope requested by applications
                req.state = System.currentTimeMillis()
                    .toString() // Used to indentify applications; returned by WeChat after authentication. none
                val requestDone = HNApplication.mWechatApi?.sendReq(req)
                LogUtil.e("SendAuth.Req done: $requestDone")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun executeJavascript(script: String?) {
        if (wv == null) {
            return
        }
        val excuteScript = EtcUtil.EscapeJavaScriptFunctionParameter(script ?: "")
        LogUtil.d("excuteScript() : $excuteScript")
        runOnUiThread {
            val prefix = "javascript:"
            var formattedScript = excuteScript
            if (!excuteScript.startsWith(prefix)) {
                formattedScript = prefix + excuteScript
            }
            LogUtil.i("<<executeJavascript>>    $formattedScript")
            // Build.VERSION_CODES.KITKAT
            if (Build.VERSION.SDK_INT < 19) {
                wv!!.loadUrl(formattedScript)
            } else {
                wv!!.evaluateJavascript(formattedScript) { value -> LogUtil.d("<<onReceiveValue>>    $value") }
            }
        }
    }

    override fun onBackPressed() {
        // super.onBackPressed();
        mBackPressCloseHandler!!.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> if (wv!!.canGoBack()) {
                wv!!.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQ_CODE_INIT_APIKEY = 0

        /** 魅族代理需要的魅族appid和appkey，请到魅族推送官网申请  */
        private const val mzAppId = ""
        private const val mzAppKey = ""

        /** 小米代理需要的小米appid和appkey，请到小米推送官网申请  */
        private const val xmAppId = ""
        private const val xmAppKey = ""

        /** OPPO代理需要的OPPO appkey和appsecret，请到OPPO推送官网申请  */
        private const val opAppKey = ""
        private const val opAppSecret = ""
    }
}