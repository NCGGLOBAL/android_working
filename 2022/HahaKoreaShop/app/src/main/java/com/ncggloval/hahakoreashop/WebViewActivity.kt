package com.ncggloval.hahakoreashop

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ncggloval.hahakoreashop.common.HNApplication
import com.ncggloval.hahakoreashop.delegator.HNCommTran
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference.putSharedPreference
import com.ncggloval.hahakoreashop.util.EtcUtil
import com.ncggloval.hahakoreashop.util.LogUtil
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

class WebViewActivity : AppCompatActivity() {
    private var wv: WebView? = null
    private val mHNCommTran: HNCommTran? = null
    var mWebViewUrl = ""
    private var mCallback: String? = null

    //    private ProgressUtil mProgressUtil;
    private val mApplicationContext: Context? = null
    private val mStartedBaidu = false
    private val mBaiduApiKey: String? = null
    private val mGetUserinfo = ""
    private val mLlloading: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_webview)
            mWebViewUrl = getIntent().getStringExtra("webviewUrl") ?: ""

            // WebView 초기화
            initWebView()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        wv = findViewById(R.id.webView) as WebView?
        wv!!.webViewClient = HNWebViewClient()
        wv!!.webChromeClient = HNWebChromeClient()
        wv!!.settings.setUserAgentString(wv!!.settings.userAgentString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wv!!.settings.mixedContentMode = 0
        }
        wv!!.settings.setSupportZoom(true)
        wv!!.settings.displayZoomControls = true
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
//        wv!!.settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath())
        wv!!.addJavascriptInterface(AndroidBridge(this, wv!!), "android")
        wv!!.settings.setUserAgentString(wv!!.settings.userAgentString + "webview-type=sub")
        if (!TextUtils.isEmpty(mWebViewUrl)) {
            wv!!.loadUrl(mWebViewUrl)
        }
    }

    override fun onDestroy() {
        CookieSyncManager.createInstance(this)
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
        } else {
            cookieManager.removeAllCookie()
        }
        super.onDestroy()
    }

    inner class HNWebChromeClient : WebChromeClient() {
        override fun onJsAlert(
            paramWebView: WebView,
            paramString1: String,
            paramString2: String,
            paramJsResult: JsResult
        ): Boolean {
            AlertDialog.Builder(this@WebViewActivity)
                .setTitle(getResources().getString(R.string.alert_title))
                .setMessage(paramString2)
                .setPositiveButton(
                    getResources().getString(R.string.confirm),
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
            AlertDialog.Builder(this@WebViewActivity)
                .setTitle(getResources().getString(R.string.alert_title))
                .setMessage(paramString2).setPositiveButton(
                    getResources().getString(R.string.confirm),
                    DialogInterface.OnClickListener { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() })
                .setNegativeButton(
                    getResources().getString(R.string.cancel),
                    DialogInterface.OnClickListener { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.cancel() })
                .setCancelable(false).create().show()
            return true
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
            LogUtil.e("shouldOverrideUrlLoading : $url")
            if (url != null && url.startsWith("intent://")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage: Intent? =
                        getPackageManager().getLaunchIntentForPackage(intent?.getPackage()!!)
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
                            val pm: PackageManager = getPackageManager()
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
                        intent = Intent(this@WebViewActivity, WebViewActivity::class.java)
                        intent!!.putExtra("webviewUrl", request_url)
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
                    return this@WebViewActivity.getPackageManager()
                        .getPackageInfo(this@WebViewActivity.getPackageName(), 0).versionName
                } catch (localNameNotFoundException: PackageManager.NameNotFoundException) {
                }
                return ""
            }

        @get:JavascriptInterface
        val deviceId: String?
            get() = HNApplication.mDeviceId

        @get:JavascriptInterface
        @set:JavascriptInterface
        var memberKey: String?
            get() = HNSharedPreference.getSharedPreference(getApplicationContext(), "memberKey")
            set(paramString) {
                putSharedPreference(getApplicationContext(), "memberKey", paramString)
            }
    }

    fun executeJavascript(script: String) {
        if (wv == null) {
            return
        }
        val excuteScript = EtcUtil.EscapeJavaScriptFunctionParameter(script)
        LogUtil.d("excuteScript() : $excuteScript")
        runOnUiThread(Runnable {
            val prefix = "javascript:"
            var formattedScript = excuteScript
            if (!excuteScript!!.startsWith(prefix)) {
                formattedScript = prefix + excuteScript
            }
            LogUtil.i("<<executeJavascript>>    $formattedScript")
            // Build.VERSION_CODES.KITKAT
            if (Build.VERSION.SDK_INT < 19) {
                wv!!.loadUrl(formattedScript!!)
            } else {
                wv!!.evaluateJavascript(formattedScript!!) { value -> LogUtil.d("<<onReceiveValue>>    $value") }
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> if (wv!!.canGoBack()) {
                wv!!.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
    private fun readChinaPushApiKey(): String? {
        return try {
            getPackageManager().getApplicationInfo(
                getPackageName(),
                PackageManager.GET_META_DATA
            ).metaData.getString(HNApplication.API_KEY)
        } catch (e: PackageManager.NameNotFoundException) {
            LogUtil.w(
                "Could not retrieve China push API key from AndroidManifest.xml.  Cannot receive push messages!",
                e
            )
            null
        }
    }

    /**
     * Updates the Baidu event message log which is displayed to users.
     *
     * @param logMessage the new message.
     */
    private fun updateMessageLog(logMessage: String) {
        Toast.makeText(this, logMessage, Toast.LENGTH_LONG).show()
    }
}