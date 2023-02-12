package com.creator.hotsourcelive

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.creator.hotsourcelive.common.BackPressCloseHandler
import com.creator.hotsourcelive.common.HNApplication
import com.creator.hotsourcelive.delegator.HNCommTran
import com.creator.hotsourcelive.delegator.HNSharedPreference
import com.creator.hotsourcelive.helpers.Constants
import com.creator.hotsourcelive.models.Image
import com.creator.hotsourcelive.util.*
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.integration.android.IntentIntegrator
import com.kakao.auth.*
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URISyntaxException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

//import com.nhn.android.naverlogin.OAuthLogin;
//import com.nhn.android.naverlogin.OAuthLoginHandler;
class WebViewActivity : Activity() {
    private var mContext: Context? = null
    private var mWebView: WebView? = null
    var mWebViewUrl: String? = ""
    private var mCookieManager: CookieManager? = null
    private var mCallback: String? = null
    private val mCallbackParam: String? = null
    private val mFirebaseMessaging: FirebaseMessaging? = null
    private val mPushUid = ""
    private val mLandingUrl = ""
    private var mBackPressCloseHandler: BackPressCloseHandler? = null
    private var mIntegrator: IntentIntegrator? = null
    private var mCameraType = 0
    private var mHNCommTran: HNCommTran? = null
    private var mProgressDialog // 처리중
            : ProgressDialog? = null
    var PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,  //            Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,  //            Manifest.permission.CALL_PHONE
        Manifest.permission.GET_ACCOUNTS
    )

    // NICE 연동 가이드
    val ISP_LINK = "market://details?id=kvp.jjy.MispAndroid320" // ISP 설치 링크
    val KFTC_LINK = "market://details?id=com.kftc.bankpay.android" //금융결제원 설치 링크
    val MERCHANT_URL = "http://web.nicepay.co.kr/smart/mainPay.jsp" // 가맹점의 결제 요청 페이지 URL
    private var NICE_BANK_URL: String? = "" // 계좌이체 인증후 거래 요청 URL

    // AndroidManaifest.xml에 명시된 값과 동일한 값을 설정하십시요.
    // 스키마 뒤에 ://를 붙여주십시요.
    private val WAP_URL = "nicepaysample" + "://"
    private var BANK_TID: String? = ""
    private var mCurrentPhotoPath // 촬영된 이미지 경로
            : String? = null
    private var mSelectedImages: ArrayList<Image>? = null
    private var mToken = "" // 이미지 Token
    private var mImgArr: JSONArray? = null
    private val mLlPermission // 권한페이지
            : LinearLayout? = null
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val mCapturedImageURI: Uri? = null

    //    private static OAuthLogin mOAuthLoginModule;
    private var callbackManager: CallbackManager? = null
    private val permissionNeeds = Arrays.asList("public_profile", "email")
    private val btnNaverLogin: Button? = null
    private val btnFacebookLogin: Button? = null
    private val btnKakaoLogin: Button? = null
    private val mNaverMessage = ""
    private var mFacebookMessage = ""
    private val mKakaoMessage = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity가 실행 될 때 항상 화면을 켜짐으로 유지한다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            setContentView(R.layout.activity_webview)
            mContext = this@WebViewActivity
            mWebViewUrl = intent.getStringExtra("webviewUrl")

            // Back Handler
            mBackPressCloseHandler = BackPressCloseHandler(this)

            // WebView 초기화
            initWebView()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        mWebView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mWebView!!.onPause()
    }

    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        mWebView = findViewById<View>(R.id.webView) as WebView
        mWebView!!.webViewClient = HNWebViewClient()
        mWebView!!.webChromeClient = HNWebChromeClient()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        val userAgentString =
            "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/97.0.4692.87 Mobile Safari/535.19 " + "webview-type=sub"
        mWebView!!.settings.setUserAgentString(userAgentString)
        if (Build.VERSION.SDK_INT >= 21) {
            mWebView!!.settings.mixedContentMode = 0
            mCookieManager = CookieManager.getInstance()
            mCookieManager?.setAcceptCookie(true)
            mCookieManager?.setAcceptThirdPartyCookies(mWebView, true)
        } else {
            mCookieManager = CookieManager.getInstance()
            mCookieManager?.setAcceptCookie(true)
        }
        mWebView!!.settings.setSupportZoom(true)
        mWebView!!.settings.displayZoomControls = true
        mWebView!!.settings.builtInZoomControls = true

        // setSavePassword - default false in android 4.4 and above
        // setPluginState - use plugin deprecate
        // setAppCacheMaxSize - deprecate
        mWebView!!.settings.javaScriptEnabled = true
        mWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView!!.settings.allowFileAccess = true
        mWebView!!.settings.allowContentAccess = true
        mWebView!!.settings.loadsImagesAutomatically = true
        mWebView!!.settings.loadWithOverviewMode = true
        mWebView!!.settings.setSupportMultipleWindows(false)
        mWebView!!.settings.useWideViewPort = true
        mWebView!!.settings.databaseEnabled = true
        mWebView!!.settings.domStorageEnabled = true
        mWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView!!.settings.setSupportMultipleWindows(true)
        mWebView!!.settings.setAppCacheEnabled(true)
        mWebView!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView!!.settings.setAppCachePath(applicationContext.cacheDir.absolutePath)
        mWebView!!.settings.textZoom = 100
        mWebView!!.addJavascriptInterface(WebAppInterface(this, mWebView!!), "android")
        mWebView!!.isDrawingCacheEnabled = true
        mWebView!!.buildDrawingCache()
        val extraHeaders: MutableMap<String, String> = HashMap()
        extraHeaders["webview-type"] = "sub"
        if (!TextUtils.isEmpty(mWebViewUrl)) {
            mWebView!!.loadUrl(mWebViewUrl!!, extraHeaders)
        }
    }

    inner class HNWebChromeClient : WebChromeClient() {
        // For Android Version < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
            //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
            mUploadMessage = uploadMsg
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, Constants.FILECHOOSER_NORMAL_REQ_CODE)
        }

        // For 3.0 <= Android Version < 4.1
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String?) {
            //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
            openFileChooser(uploadMsg, acceptType, "")
        }

        // For 4.1 <= Android Version < 5.0
        fun openFileChooser(
            uploadFile: ValueCallback<Uri?>?,
            acceptType: String?,
            capture: String?
        ) {
            mUploadMessage = uploadFile
            imageChooser()
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams
        ): Boolean {
            println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3")
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }
            mFilePathCallback = filePathCallback
            imageChooser()
            return true
        }

        private fun imageChooser() {
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(javaClass.name, "Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile)
                    )
                } else {
                    takePictureIntent = null
                }
            }
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "image/*"
            val intentArray: Array<Intent?>
            intentArray = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            startActivityForResult(chooserIntent, Constants.FILECHOOSER_LOLLIPOP_REQ_CODE)
        }

        override fun onJsAlert(
            paramWebView: WebView,
            paramString1: String,
            paramString2: String,
            paramJsResult: JsResult
        ): Boolean {
            AlertDialog.Builder(mContext!!)
                .setTitle(resources.getString(R.string.alert_title))
                .setMessage(paramString2)
                .setPositiveButton(resources.getString(R.string.confirm)) { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() }
                .setCancelable(false).create().show()
            return true
        }

        override fun onJsConfirm(
            paramWebView: WebView,
            paramString1: String,
            paramString2: String,
            paramJsResult: JsResult
        ): Boolean {
            AlertDialog.Builder(mContext!!)
                .setTitle(resources.getString(R.string.alert_title))
                .setMessage(paramString2)
                .setPositiveButton(resources.getString(R.string.confirm)) { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() }
                .setNegativeButton(resources.getString(R.string.cancel)) { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.cancel() }
                .setCancelable(false).create().show()
            return true
        }
    }

    inner class HNWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            // LogUtil.d("onPageLoadStopped : " + url);

//            mProgressUtil.dismiss();
        }

        override fun onPageStarted(view: WebView, url: String, paramBitmap: Bitmap) {
            super.onPageStarted(view, url, paramBitmap)
            // LogUtil.d("onPageLoadStarted : " + url);
            executeJavascript("localStorage.setItem(\"dv_id\"," + "\"" + HNApplication.Companion.mDeviceId + "\")")
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            // LogUtil.e("shouldOverrideUrlLoading : " + url);
            var uri = Uri.parse(url)
            var intent: Intent? = null
            if (url.startsWith("sms:") || url.startsWith("smsto:")) {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                startActivity(i)
                return true
            } else if (url.startsWith("tel:")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val permissionResult = checkSelfPermission(Manifest.permission.CALL_PHONE)
                    if (permissionResult == PackageManager.PERMISSION_DENIED) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                            val dialog = AlertDialog.Builder(
                                mContext!!
                            )
                            dialog.setTitle("권한이 필요합니다.")
                                .setMessage("이 기능을 사용하기 위해서는 단말기의 \"전화걸기\" 권한이 필요합니다. 계속 하시겠습니까?")
                                .setPositiveButton("네") { dialog, which ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        // CALL_PHONE 권한을 Android OS에 요청한다.
                                        requestPermissions(
                                            arrayOf(Manifest.permission.CALL_PHONE),
                                            1000
                                        )
                                    }
                                }
                                .setNegativeButton("아니요") { dialog, which ->
                                    Toast.makeText(
                                        mContext,
                                        "기능을 취소했습니다",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .create().show()
                        }
                    } else {
                        intent = Intent(Intent.ACTION_CALL, Uri.parse(url))
                        startActivity(intent)
                    }
                } else {
                    intent = Intent(Intent.ACTION_CALL, Uri.parse(url))
                    startActivity(intent)
                }
                return true
            } else if (url.startsWith("mailto:")) {
                intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                startActivity(intent)
                return true
            }

            //=================================== Nice Pay ===================================

            // 웹뷰에서 ispmobile 실행한 경우...
            if (url.startsWith("ispmobile")) {
                return if (NicePayUtility.isPackageInstalled(
                        applicationContext,
                        "kvp.jjy.MispAndroid320"
                    )
                ) {
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } else {
                    installISP()
                    true
                }
            } else if (url.startsWith("kftc-bankpay")) {
                return if (NicePayUtility.isPackageInstalled(
                        applicationContext,
                        "com.kftc.bankpay.android"
                    )
                ) {
                    val sub_str_param = "kftc-bankpay://eftpay?"
                    var reqParam = url.substring(sub_str_param.length)
                    try {
                        reqParam = URLDecoder.decode(reqParam, "utf-8")
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    reqParam = makeBankPayData(reqParam)
                    intent = Intent(Intent.ACTION_MAIN)
                    intent.component = ComponentName(
                        "com.kftc.bankpay.android",
                        "com.kftc.bankpay.android.activity.MainActivity"
                    )
                    intent.putExtra("requestInfo", reqParam)
                    startActivityForResult(intent, 1)
                    true
                } else {
                    installKFTC()
                    true
                }
            } else if (url != null && (url.contains("vguard")
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
                        || url.contains("kakaotalk://")
                        || url.contains("http://m.ahnlab.com/kr/site/download"))
            ) {
                return try {
                    try {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        Log.i("NICE", "intent getDataString +++===>" + intent.dataString)
                    } catch (ex: URISyntaxException) {
                        Log.e("Browser", "Bad URI " + url + ":" + ex.message)
                        return false
                    }
                    if (url.startsWith("intent")) { //chrome πˆ¡Ø πÊΩƒ
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            if (packageManager.resolveActivity(intent, 0) == null) {
                                val pkgName = intent.getPackage()
                                if (pkgName != null) {
                                    uri = Uri.parse("market://search?q=pname:$pkgName")
                                    intent = Intent(Intent.ACTION_VIEW, uri)
                                    startActivity(intent)
                                }
                            } else {
                                uri = Uri.parse(intent.dataString)
                                intent = Intent(Intent.ACTION_VIEW, uri)
                                startActivity(intent)
                            }
                        } else {
                            try {
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                uri = Uri.parse("market://search?q=pname:" + intent.getPackage())
                                intent = Intent(Intent.ACTION_VIEW, uri)
                                startActivity(intent)
                            }
                        }
                        true
                    } else { //±∏ πÊΩƒ
                        intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    }
                } catch (e: Exception) {
                    Log.i("NICE", e.message!!)
                    false
                }
            } else if (url.startsWith(WAP_URL)) {
                val thisurl = url.substring(WAP_URL.length)
                view.loadUrl(thisurl)
                return true
            } else if (url != null && url.startsWith("intent://")) {
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
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
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    intent?.let { startActivity(it) }
                    return true
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            } else if (url.startsWith(Companion.INTENT_PROTOCOL_START)) {
                val customUrlStartIndex = Companion.INTENT_PROTOCOL_START.length
                val customUrlEndIndex = url.indexOf(Companion.INTENT_PROTOCOL_INTENT)
                return if (customUrlEndIndex < 0) {
                    false
                } else {
                    val customUrl = url.substring(customUrlStartIndex, customUrlEndIndex)
                    try {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(customUrl))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        baseContext.startActivity(i)
                    } catch (e: ActivityNotFoundException) {
                        val packageStartIndex =
                            customUrlEndIndex + Companion.INTENT_PROTOCOL_INTENT.length
                        val packageEndIndex = url.indexOf(Companion.INTENT_PROTOCOL_END)
                        val packageName = url.substring(
                            packageStartIndex,
                            if (packageEndIndex < 0) url.length else packageEndIndex
                        )
                        val i = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Companion.GOOGLE_PLAY_STORE_PREFIX + packageName)
                        )
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        baseContext.startActivity(i)
                    }
                    true
                }
            }
            view.loadUrl(url)
            return false // webview replace
        }
    }

    private inner class WebAppInterface(
        private val context: Context,
        private val webview: WebView
    ) {
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
                            val pm = context.packageManager
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

                // 사진첩 or 카메라 호출
                if ("ACT1001" == actionCode) {
                    LogUtil.d("ACT1001 - 앱 데이터 저장 (키체인 저장 및 파일저장)")
                    if (actionParamObj!!.has("key_type")) {
                        LogUtil.d("mCameraType : $mCameraType")
                        mCameraType = if (actionParamObj.getInt("key_type") == 0) {      // camera
                            3
                            //                            requestPermission(Constants.REQUEST_SELECT_IMAGE_CAMERA);
                        } else {                                          // album
                            4
                            //                            requestPermission(Constants.REQUEST_SELECT_IMAGE_ALBUM);
                        }
                        if (!hasPermissions(mContext, *PERMISSIONS)) {
                            ActivityCompat.requestPermissions(
                                this@WebViewActivity,
                                PERMISSIONS,
                                Constants.PERMISSIONS_MULTIPLE_REQUEST
                            )
                        } else {
                            if (mCameraType == 3) {
                                dispatchTakePictureIntent()
                            } else {
                                galleryAddPic()
                            }
                            //                            intent = new Intent(getApplicationContext(), SelectImageMethodActivity.class);
//                            startActivityForResult(intent, Constants.REQUEST_CODE);
                        }
                    }
                } else if ("ACT1002" == actionCode) {
                    LogUtil.d("ACT1002 - 앱 데이터 가져오기 (키체인 및 파일에 있는 정보 가져오기)")
                    if (actionParamObj!!.has("key_type")) {
                        mCameraType = actionParamObj.getInt("key_type")
                        LogUtil.d("mCameraType : $mCameraType")
                    }
                    mCameraType = 0
                    //                    requestPermission(Constants.REQUEST_CAMERA);
//                    executeJavascript(mCallback + "()");
                } else if ("ACT1003" == actionCode) {
                    LogUtil.d("ACT1003 - 위쳇페이")
                    if (actionParamObj!!.has("request_url")) {
                        val request_url = actionParamObj.getString("request_url")
                        LogUtil.d("request_url : $request_url")
                        mWebView!!.loadUrl(request_url)
                    }
                } else if ("ACT1011" == actionCode) {
                    LogUtil.d("ACT1011 - Custom Native 카메라 및 사진 라이브러리 호출")
                    mToken = actionParamObj!!.getString("token") // 사진 임시저장시 토큰값
                    mImgArr = actionParamObj.getJSONArray("imgArr") // 이미지 정보
                    val pageGbn =
                        actionParamObj.getString("pageGbn") // 1 : 신규페이지에서 진입, 2 : 수정페이지에서 진입
                    val cnt = actionParamObj.getString("cnt")
                    LogUtil.d("token : $mToken")
                    Log.d("SeongKwon", "mImgArr : " + mImgArr.toString())
                    Log.d("SeongKwon", "pageGbn : $pageGbn")

                    // 신규
                    intent = Intent(mContext, SelectImageMethodActivity::class.java)
                    intent.putExtra("token", mToken)
                    intent.putExtra("imgArr", mImgArr.toString())
                    intent.putExtra("pageGbn", pageGbn)
                    intent.putExtra("cnt", cnt)
                    startActivityForResult(intent, Constants.REQUEST_ADD_IMAGE)
                } else if ("ACT1012" == actionCode) {
                    LogUtil.d("ACT1012 - 사진 임시저장 통신")
                    mToken = actionParamObj!!.getString("token") // 사진 임시저장시 토큰값
                    uploadImagesAsyncTask().execute(mToken)
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
                        intent = Intent(mContext, WebViewActivity::class.java)
                        intent.putExtra("webviewUrl", request_url)
                        startActivity(intent)
                    }
                } else if ("ACT1016" == actionCode) {
                    LogUtil.d("ACT1016 - 팝업을 닫은후(ACT1015)  호출할 function 이름")
                    val jsonObject = JSONObject()
                    jsonObject.put("callScript", "") // 팝업을 닫은후(ACT1015)  호출할 function 이름
                    jsonObject.put("callObj", "") // 메인 web 페이지에 넘길 파라메터
                    executeJavascript("$mCallback($jsonObject)")
                    finish()
                } else if ("ACT1018" == actionCode) {
                    LogUtil.d("ACT1018 - 앱종료")
                    finish()
                    System.exit(0)
                } else if ("ACT1020" == actionCode) {
                    LogUtil.d("ACT1020 - SNS호출")
                    if (actionParamObj!!.has("snsType")) {
                        if (actionParamObj.getString("snsType") == "1") {
                            // 네이버 로그인
//                            callNaverLogin();
                        } else if (actionParamObj.getString("snsType") == "2") {
                            // 카카오톡 로그인
                            val session = Session.getCurrentSession()
                            session.addCallback(SessionCallback())
                            session.open(AuthType.KAKAO_LOGIN_ALL, this@WebViewActivity)
                            //                            if (session.checkAndImplicitOpen()) {
//                                // 액세스토큰 유효하거나 리프레시 토큰으로 액세스 토큰 갱신을 시도할 수 있는 경우
//                            } else {
//                                // 무조건 재로그인을 시켜야 하는 경우
//                            }
                        } else if (actionParamObj.getString("snsType") == "3") {
                            // 페이스북 로그인
                            if (AccessToken.isCurrentAccessTokenActive()) {
                                try {
                                    val jsonObject = JSONObject()
                                    jsonObject.put(
                                        "accessToken",
                                        AccessToken.getCurrentAccessToken()
                                    ) // getAccessToken
                                    executeJavascript("$mCallback($jsonObject)")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                initFacebookLogin()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @get:JavascriptInterface
        val appVersion: String
            get() {
                var versionName = ""
                try {
                    var pi: PackageInfo? = null
                    try {
                        pi = packageManager.getPackageInfo(packageName, 0)
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                    }
                    versionName = pi!!.versionName
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return versionName
            }

        @get:JavascriptInterface
        val deviceId: String?
            get() = HNApplication.Companion.mDeviceId

        @get:JavascriptInterface
        @set:JavascriptInterface
        var memberKey: String?
            get() = HNSharedPreference.getSharedPreference(applicationContext, "memberKey")
            set(paramString) {
                HNSharedPreference.putSharedPreference(applicationContext, "memberKey", paramString)
            }
    }

    fun executeJavascript(script: String) {
        if (mWebView == null) {
            return
        }
        val excuteScript = EtcUtil.EscapeJavaScriptFunctionParameter(script)
        LogUtil.d("excuteScript() : $excuteScript")
        runOnUiThread {
            val prefix = "javascript:"
            var formattedScript = excuteScript
            if (!excuteScript!!.startsWith(prefix)) {
                formattedScript = prefix + excuteScript
            }
            LogUtil.i("<<executeJavascript>>    $formattedScript")
            // Build.VERSION_CODES.KITKAT
            if (Build.VERSION.SDK_INT < 19) {
                mWebView!!.loadUrl(formattedScript!!)
            } else {
                mWebView!!.evaluateJavascript(formattedScript!!) { value -> LogUtil.d("<<onReceiveValue>>    $value") }
            }
        }
    }

    /**
     * For NicePay
     * 계좌이체 결과값을 받아와 오류시 해당 메세지를, 성공시에는 결과 페이지를 호출한다.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SeongKwon", "============================================")
        Log.d("SeongKwon", "requestCode = $requestCode")
        Log.d("SeongKwon", "resultCode = $resultCode")
        Log.d("SeongKwon", "============================================")
//        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
//            return
//        } else
        if ((requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA || requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM) && resultCode == RESULT_OK) {
            var result = ""
            try {
                val jObj = JSONObject()
                val jArray = JSONArray()
                jObj.put("resultcd", "0") // 0:성공. 1:실패
                val selectedImages =
                    data.extras!![Constants.INTENT_EXTRA_IMAGES] as ArrayList<Image>?
                for (i in selectedImages!!.indices) {
                    val jObjItem = JSONObject()

                    // 회전
                    val matrix = Matrix()
                    matrix.postRotate(
                        BitmapUtil.Companion.GetExifOrientation(selectedImages[i].path).toFloat()
                    )
                    val dstWidth = 200
                    val dstHeight = 200
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 4
                    val src = BitmapFactory.decodeFile(selectedImages[i].path, options)
                    //                        Bitmap resized = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
                    val width = src.width
                    val height = src.height
                    //                        Bitmap resized = Bitmap.createBitmap(src, width / 2, height / 4, width / 2, height / 2, matrix, true);
                    val resized = Bitmap.createBitmap(
                        src,
                        width / 2,
                        height / 4,
                        dstWidth,
                        dstHeight,
                        matrix,
                        true
                    )
                    jObjItem.put("image", getBase64String(src))
                    jObjItem.put("thumbnail", getBase64String(resized))
                    jArray.put(jObjItem)
                }
                result = jObj.toString()
                Log.d("SeongKwon", result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            executeJavascript("$mCallback($result)")
        } else if (requestCode == Constants.REQUEST_ADD_IMAGE && resultCode == RESULT_OK) {
            Log.d(
                "SeongKwon",
                "!!!!!!!!!!!!!!!!!!! = Constants.REQUEST_ADD_IMAGE = " + Constants.REQUEST_ADD_IMAGE
            )
            try {
                val jObj = JSONObject()
                jObj.put("resultcd", "0") // 변경사항 있을경우 : 1, 없을경우 : 0 // 이전[0:성공. 1:실패]
                if (data.hasExtra("isChanged")) {
                    if (data.getBooleanExtra("isChanged", false)) {
                        jObj.put("resultcd", "1")
                    }
                }
                if (data.hasExtra("imgArr")) {
                    mImgArr = JSONArray(data.getStringExtra("imgArr"))
                }
                jObj.put("imgArr", mImgArr)
                jObj.put("token", data.getStringExtra("token"))
                jObj.put("pageGbn", data.getStringExtra("pageGbn"))
                jObj.put("cnt", data.getStringExtra("cnt"))
                executeJavascript("$mCallback($jObj)")

                // TODO 신규등록을 위한 임시저장
                HNApplication.Companion.mImgArrForReg = mImgArr.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (requestCode == Constants.REQUEST_EDIT_IMAGE && resultCode == RESULT_OK) {
            try {
                if (data.hasExtra("imgArr")) {
                    mImgArr = JSONArray(data.getStringExtra("imgArr"))
                }
                val jObj = JSONObject()
                jObj.put("imgArr", mImgArr)
                jObj.put("token", data.getStringExtra("token"))
                jObj.put("pageGbn", data.getStringExtra("pageGbn"))
                jObj.put("cnt", data.getStringExtra("cnt"))
                executeJavascript("$mCallback($jObj)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (requestCode == Constants.FILECHOOSER_NORMAL_REQ_CODE) {
            if (mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            val result = getResultUri(data)
            Log.d(javaClass.name, "openFileChooser : $result")
            mUploadMessage!!.onReceiveValue(result)
            mUploadMessage = null
        } else if (requestCode == Constants.FILECHOOSER_LOLLIPOP_REQ_CODE) {
            if (mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            Log.e("SeongKwon", getResultUri(data).toString())
            val results = arrayOf(getResultUri(data))
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        }
        if (data == null) return
        if (data.hasExtra("bankpay_value")) {
            val resVal = data.extras!!.getString("bankpay_value")
            val resCode = data.extras!!.getString("bankpay_code")
            Log.i("NICE", "resCode : $resCode")
            Log.i("NICE", "resVal : $resVal")
            if ("091" == resCode) {      //계좌이체 결제를 취소한 경우
                AlertUtil.showConfirmDialog(this, "인증 오류", "계좌이체 결제를 취소하였습니다.")
                mWebView?.loadUrl(MERCHANT_URL)
            } else if ("060" == resCode) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "타임아웃")
                mWebView?.loadUrl(MERCHANT_URL)
            } else if ("050" == resCode) {
                mWebView?.loadUrl(MERCHANT_URL)
            } else if ("040" == resCode) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "OTP/보안카드 처리 실패")
                mWebView?.loadUrl(MERCHANT_URL)
            } else if ("030" == resCode) {
                AlertUtil.showConfirmDialog(this, "인증 오류", "인증모듈 초기화 오류")
                mWebView?.loadUrl(MERCHANT_URL)
            } else if ("000" == resCode) { // 성공일 경우
                val postData =
                    "callbackparam2=$BANK_TID&bankpay_code=$resCode&bankpay_value=$resVal"
                // nice sample
//                mWebView.postUrl(NICE_BANK_URL, EncodingUtils.getBytes(postData, "euc-kr"));
                try {
                    mWebView!!.postUrl(NICE_BANK_URL!!, postData.toByteArray(charset("euc-kr")))
                } catch (e: UnsupportedEncodingException) {
                    println("Unsupported character set")
                }
            }
        } else if (data.hasExtra("SCAN_RESULT") && data.hasExtra("SCAN_RESULT_FORMAT")) {
            Toast.makeText(
                this, """
     [SCAN_RESULT]${data.getStringExtra("SCAN_RESULT")}
     [SCAN_RESULT_FORMAT]${data.getStringExtra("SCAN_RESULT_FORMAT")}
     """.trimIndent(), Toast.LENGTH_LONG
            ).show()
            var result = ""
            try {
                val jObj = JSONObject()
                jObj.put("resultcd", "0") // 0:성공. 1:실패
                jObj.put("returnCode", data.getStringExtra("SCAN_RESULT"))
                result = jObj.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            executeJavascript("$mCallback($result)")
        }
    }

    /**
     * 계좌이체 데이터를 파싱한다. *
     *
     * @param str
     * @return
     */
    private fun makeBankPayData(str: String): String {
        val arr = str.split("&").toTypedArray()
        var parse_temp: Array<String>
        val tempMap = HashMap<String, String>()
        for (i in arr.indices) {
            try {
                parse_temp = arr[i].split("=").toTypedArray()
                tempMap[parse_temp[0]] = parse_temp[1]
            } catch (e: Exception) {
            }
        }
        BANK_TID = tempMap["user_key"]
        NICE_BANK_URL = tempMap["callbackparam1"]
        return str
    }

    /**
     * ISP가 설치되지 않았을때 처리를 진행한다.
     */
    private fun installISP() {
        val d = AlertDialog.Builder(this)
        d.setMessage("ISP결제를 하기 위해서는 ISP앱이 필요합니다.\n설치 페이지로  진행하시겠습니까?")
        d.setTitle("ISP 설치")
        d.setPositiveButton("확인") { dialog, which ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ISP_LINK))
            startActivity(intent)
        }
        d.setNegativeButton("아니요") { dialog, which ->
            dialog.cancel()
            //결제 초기 화면을 요청합니다.
            mWebView?.loadUrl(MERCHANT_URL)
        }
        d.show()
    }

    /**
     * 계좌이체 BANKPAY 설치 진행 안내
     */
    private fun installKFTC() {
        val d = AlertDialog.Builder(this)
        d.setMessage("계좌이체 결제를 하기 위해서는 BANKPAY 앱이 필요합니다.\n설치 페이지로  진행하시겠습니까?")
        d.setTitle("계좌이체 BANKPAY 설치")
        d.setPositiveButton("확인") { dialog, which ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KFTC_LINK))
            startActivity(intent)
        }
        d.setNegativeButton("아니요") { dialog, which ->
            dialog.cancel()
            mWebView!!.loadUrl(MERCHANT_URL)
        }
        d.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PERMISSIONS_MULTIPLE_REQUEST -> if (grantResults.size > 0) {
                val cameraPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED
                val writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED
                val readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (cameraPermission && writeExternalFile && readExternalFile) {
                    // write your logic here
                    mLlPermission!!.visibility = View.GONE
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,  //                                            Manifest.permission.CAMERA,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.GET_ACCOUNTS,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ),
                            Constants.PERMISSIONS_MULTIPLE_REQUEST
                        )
                    }
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Please Grant Permissions to upload profile photo",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(
                        "ENABLE"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,  //                                                            Manifest.permission.CAMERA,
                                    //                                                            Manifest.permission.CALL_PHONE,
                                    Manifest.permission.GET_ACCOUNTS,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ),
                                Constants.PERMISSIONS_MULTIPLE_REQUEST
                            )
                        }
                    }.show()
                }
            }
        }
    }

    private fun callQR() {
        // zxing init
        mIntegrator = IntentIntegrator(this)
        mIntegrator!!.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
        //        mIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        mIntegrator!!.setPrompt("Scan a barcode")
        if (mCameraType == 0) {
            mIntegrator!!.setCameraId(1) // Use a specific camera of the device
        } else {
            mIntegrator!!.setCameraId(0) // Use a specific camera of the device
        }
        mIntegrator!!.setBeepEnabled(false)
        mIntegrator!!.initiateScan()
    }

    // 사진촬영
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
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
    private fun galleryAddPic() {
        val intent = Intent(this, AlbumSelectActivity::class.java)
        startActivityForResult(intent, Constants.REQUEST_SELECT_IMAGE_ALBUM)
    }

    // 사진저장
    @Throws(IOException::class)
    private fun createImageFile(): File {
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        deleteDir(storageDir.getPath());

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFileName = "JPEG_" + timeStamp + "_"
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )


        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        Log.d("createImageFile()", "mCurrentPhotoPath = $mCurrentPhotoPath")
        //        Toast.makeText(this, mCurrentPhotoPath, Toast.LENGTH_LONG).show();
        return image
    }

    // 폴더삭제
    private fun deleteDir(path: String) {
        val file = File(path)
        val childFileList = file.listFiles()
        for (childFile in childFileList) {
            if (childFile.isDirectory) {
                deleteDir(childFile.absolutePath) //하위 디렉토리 루프
            } else {
                childFile.delete() //하위 파일삭제
            }
        }
        file.delete() //root 삭제
    }

    private fun encodeImage(path: String): String {
        val imagefile = File(path)
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(imagefile)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        val bm = BitmapFactory.decodeStream(fis)
        val baos = ByteArrayOutputStream()
        bm.compress(CompressFormat.JPEG, 75, baos)
        val b = baos.toByteArray()

//        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        //Base64.de
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission!!
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            } else {
                val intent = Intent(applicationContext, SelectImageMethodActivity::class.java)
                startActivityForResult(intent, Constants.REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun getBase64String(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }

    inner class uploadImagesAsyncTask : AsyncTask<String?, Void?, String?>() {
        var result: String? = ""
        override fun onPreExecute() {
            super.onPreExecute()
            mProgressDialog = ProgressDialog(mContext)
            mProgressDialog!!.setTitle("알림")
            mProgressDialog!!.setMessage("처리중입니다.\n잠시만 기다려 주세요.")
            mProgressDialog!!.show()
        }

        override fun doInBackground(vararg params: String?): String? {
            val param = HashMap<String, String?>()
            param["token"] = params[0]
            param["service"] = "GOODSIMGSREG"
            val file = File("$filesDir/")
            val flist = file.listFiles()
            Log.d("SeongKwon", "*************************************************")
            if (flist.size > 0) {
                mSelectedImages = ArrayList()
                for (i in flist.indices) {
                    val fname = flist[i].name
                    Log.d("SeongKwon", "name = $fname")
                    val id: Long = -1
                    val path = file.absolutePath + "/" + fname
                    val isSelected = false
                    Log.d("SeongKwon", "=========================")
                    Log.d("SeongKwon", "id = $id")
                    Log.d("SeongKwon", "name = $fname")
                    Log.d("SeongKwon", "path = $path")
                    Log.d("SeongKwon", "isSelected = $isSelected")
                    Log.d("SeongKwon", "=========================")
                    if (file.exists() && fname.contains(".jpg")) {
                        mSelectedImages!!.add(
                            Image(
                                id,
                                fname,
                                path,
                                isSelected,
                                -1
                            )
                        )
                    }
                }
                try {
                    result = UploadUtil.upload(
                        mContext,
                        "http://laos.mallshopping.co.kr/m/app/",
                        mSelectedImages!!,
                        param
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                result = "-1"
            }
            return result
        }

        override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
            mProgressDialog!!.dismiss()
            Log.e("SeongKwon", s!!)
            if (s == null) {
                val builder = AlertDialog.Builder(
                    mContext!!
                )
                builder.setPositiveButton(R.string.confirm) { dialog, id -> dialog.dismiss() }
                builder.setTitle("알림")
                builder.setMessage("사진 등록 중 오류가 발생했습니다.\n다시 시도해 주세요.")
                val dialog = builder.create()
                dialog.show()
                return
            }
            if (s == "-1") {
                val builder = AlertDialog.Builder(
                    mContext!!
                )
                builder.setPositiveButton(R.string.confirm) { dialog, id -> dialog.dismiss() }
                builder.setTitle("알림")
                builder.setMessage("등록 할 사진을 선택해 주세요.")
                val dialog = builder.create()
                dialog.show()
            } else {
                executeJavascript("$mCallback($s)")
            }
        }
    }

    private fun getResultUri(data: Intent?): Uri {
        var result: Uri? = null
        if (data == null || TextUtils.isEmpty(data.dataString)) {
            // If there is not data, then we may have taken a photo
            if (mCameraPhotoPath != null) {
                result = Uri.parse(mCameraPhotoPath)
            }
        } else {
            var filePath: String? = ""
            filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                data.dataString
            } else {
                "file:" + RealPathUtil.getRealPath(this, data.data)
            }
            result = Uri.parse(filePath)
        }
        return result!!
    }

    fun isServiceRunningCheck(serviceName: String): Boolean {
        val manager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            Log.d("SeongKwon", service.service.className)
            if (serviceName == service.service.className) {
                return true
            }
        }
        return false
    }
    //// ====================================================================== ////
    //// ====================================================================== ////
    ////                               SNS LOGIN                                ////
    //// ====================================================================== ////
    //// ====================================================================== ////
    // 네이버 로그인
    //    public void callNaverLogin() {
    //        mOAuthLoginModule = OAuthLogin.getInstance();
    //        mOAuthLoginModule.init(
    //                mContext
    //                , getString(R.string.naver_client_id)        // 애플리케이션 등록 후 발급받은 클라이언트 아이디
    //                , getString(R.string.naver_client_secret)    // 애플리케이션 등록 후 발급받은 클라이언트 시크릿
    //                , mContext.getResources().getString(R.string.app_name)     // 네이버 앱의 로그인 화면에 표시할 애플리케이션 이름. 모바일 웹의 로그인 화면을 사용할 때는 서버에 저장된 애플리케이션 이름이 표시됩니다.
    //                //,OAUTH_CALLBACK_INTENT
    //                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
    //        );
    //
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                runOnUiThread(new Runnable(){
    //                    @Override
    //                    public void run() {
    //                        mOAuthLoginModule.startOauthLoginActivity(WebViewActivity.this, mOAuthLoginHandler);
    //                    }
    //                });
    //            }
    //        }).start();
    //    }
    /**
     * OAuthLoginHandler를 startOAuthLoginActivity() 메서드 호출 시 파라미터로 전달하거나 OAuthLoginButton
     * 객체에 등록하면 인증이 종료되는 것을 확인할 수 있습니다.
     */
    //    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
    //        @Override
    //        public void run(final boolean success) {
    //            if (success) {
    //                final String accessToken = mOAuthLoginModule.getAccessToken(mContext);
    //                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
    //                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
    //                String tokenType = mOAuthLoginModule.getTokenType(mContext);
    //            } else {
    //                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
    //                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
    //                Toast.makeText(mContext, "errorCode:" + errorCode + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
    //            }
    //
    //            SendMassgeHandler mMainHandler = new SendMassgeHandler();
    //            if(mMainHandler != null) {
    //                Message msg = mMainHandler.obtainMessage();
    //                msg.what = SEND_NAVER_MESSAGE;
    //                if (success) {
    //                    msg.arg1 = 1;
    //                } else {
    //                    msg.arg1 = 0;
    //                }
    //                mMainHandler.sendMessage(msg);
    //            }
    //        };
    //    };
    //    private String NaverProfile(String accessToken) {
    //        String header = "Bearer " + accessToken; // Bearer 다음에 공백 추가
    //        try {
    //            String apiURL = "https://openapi.naver.com/v1/nid/me";
    //            URL url = new URL(apiURL);
    //
    //            HttpURLConnection con = (HttpURLConnection)url.openConnection();
    //
    //            con.setRequestMethod("GET");
    //            con.setRequestProperty("Authorization", header);
    //            con.connect();
    //
    //            int responseCode = con.getResponseCode();
    //            BufferedReader br;
    //            if(responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
    //                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
    //            } else {  // 에러 발생
    //                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
    //            }
    //            String inputLine;
    //            StringBuffer response = new StringBuffer();
    //            while ((inputLine = br.readLine()) != null) {
    //                response.append(inputLine);
    //            }
    //            br.close();
    //
    //            JSONObject jsonObject = new JSONObject();
    //            jsonObject.put("accessToken", accessToken);      // getAccessToken
    //            jsonObject.put("userInfo", response.toString());      // 사용자정보
    //            executeJavascript(mCallback + "(" + jsonObject.toString() + ")");
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //        return "";
    //    }
    // Handler 클래스
    internal inner class SendMassgeHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                SEND_KAKAO_MESSAGE -> Log.d("SeongKwon", "msg = $msg")
                else -> {}
            }
        }
    }

    // 카카오톡 로그인
    private inner class SessionCallback : ISessionCallback {
        // 로그인에 성공한 상태
        override fun onSessionOpened() {
            try {
                requestMe()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 로그인에 실패한 상태
        override fun onSessionOpenFailed(exception: KakaoException) {
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.message)
        }

        // 사용자 정보 요청
        private fun requestMe() {
            // 사용자정보 요청 결과에 대한 Callback
            UserManagement.getInstance().me(object : MeV2ResponseCallback() {
                override fun onSessionClosed(errorResult: ErrorResult) {
                    Log.d(
                        "SeongKwon",
                        "SessionCallback :: onSessionClosed : " + errorResult.errorMessage
                    )
                }

                override fun onSuccess(result: MeV2Response) {
                    Log.d("SeongKwon", "SessionCallback :: onSuccess")
                    try {
                        val email = result.kakaoAccount.email
                        val nickname = result.nickname
                        val profileImagePath = result.profileImagePath
                        val thumnailPath = result.thumbnailImagePath
                        val id = result.id
                        val jsonAccount = JSONObject()
                        jsonAccount.put("email", email)
                        jsonAccount.put("nickname", nickname)
                        jsonAccount.put("profileImagePath", profileImagePath)
                        jsonAccount.put("thumnailPath", thumnailPath)
                        jsonAccount.put("id", id)
                        Log.e("SeongKwon", "jsonAccount : $jsonAccount")
                        val jsonObject = JSONObject()
                        jsonObject.put(
                            "accessToken",
                            Session.getCurrentSession().accessToken
                        ) // getAccessToken
                        jsonObject.put("userInfo", jsonAccount) // 사용자정보
                        executeJavascript("$mCallback($jsonObject)")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    // 페이스북 로그인
    private fun initFacebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this@WebViewActivity, permissionNeeds)
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(
                        "SeongKwon",
                        "onSuccess - getAccessToken : " + loginResult.accessToken.token
                    )
                    Log.d("SeongKwon", "onSuccess - getUserId : " + loginResult.accessToken.userId)
                    Log.d(
                        "SeongKwon",
                        "onSuccess - getExpires : " + loginResult.accessToken.expires
                    )
                    Log.d(
                        "SeongKwon",
                        "onSuccess - getLastRefresh : " + loginResult.accessToken.lastRefresh
                    )

                    // getFbInfo();
                    try {
                        val jsonObject = JSONObject()
                        jsonObject.put(
                            "accessToken",
                            loginResult.accessToken.token
                        ) // getAccessToken
                        executeJavascript("$mCallback($jsonObject)")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancel() {
                    Log.d("SeongKwon", "onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.d("SeongKwon", "onError : " + error.localizedMessage)
                }
            })
    }

    // id,first_name,last_name,email,gender,birthday,cover,picture.type(large)
    private val fbInfo: Unit
        private get() {
            val accessToken = AccessToken.getCurrentAccessToken()
            Log.d("SeongKwon", "====================================0")
            Log.d("SeongKwon", "onSuccess - getToken : " + accessToken.token)
            Log.d("SeongKwon", "onSuccess - getUserId : " + accessToken.userId)
            Log.d("SeongKwon", "onSuccess - isExpired : " + accessToken.isExpired)
            Log.d("SeongKwon", "onSuccess - getExpires : " + accessToken.expires)
            Log.d("SeongKwon", "onSuccess - getLastRefresh : " + accessToken.lastRefresh)
            Log.d("SeongKwon", "====================================1")
            mFacebookMessage = """
                 Token = ${accessToken.token}
                 
                 """.trimIndent()
            mFacebookMessage += """
                 UserId = ${accessToken.userId}
                 
                 """.trimIndent()
            mFacebookMessage += """
                 Expires = ${accessToken.expires}
                 
                 """.trimIndent()
            mFacebookMessage += """
                 LastRefresh = ${accessToken.lastRefresh}
                 
                 """.trimIndent()
            val request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken()
            ) { `object`, response ->
                try {
                    Log.d("SeongKwon", "fb json object: $`object`")
                    Log.d("SeongKwon", "fb graph response: $response")
                    mFacebookMessage += "fb_json_object = $`object`\n"
                    runOnUiThread {
                        val alertDialogBuilder = AlertDialog.Builder(
                            mContext!!
                        )
                        alertDialogBuilder.setTitle("알림")
                        alertDialogBuilder.setMessage(mFacebookMessage)
                            .setPositiveButton("확인") { dialogInterface, i -> }
                            .setCancelable(false)
                            .create().show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val parameters = Bundle()
            parameters.putString(
                "fields",
                "id,first_name,last_name,email,gender,birthday"
            ) // id,first_name,last_name,email,gender,birthday,cover,picture.type(large)
            request.parameters = parameters
            request.executeAsync()
        }

    companion object {
        // SNS========================================================================= //
        private const val SEND_NAVER_MESSAGE = 0
        private const val SEND_KAKAO_MESSAGE = 1
        private const val SEND_FACEBOOK_MESSAGE = 2

        const val INTENT_PROTOCOL_START = "intent:"
        const val INTENT_PROTOCOL_INTENT = "#Intent;"
        const val INTENT_PROTOCOL_END = ";end;"
        const val GOOGLE_PLAY_STORE_PREFIX = "market://details?id="

        // SNS========================================================================= //
        var activity: WebViewActivity? = null
        val instance: WebViewActivity?
            get() {
                if (activity == null) {
                    activity = WebViewActivity()
                }
                return activity
            }
    }
}