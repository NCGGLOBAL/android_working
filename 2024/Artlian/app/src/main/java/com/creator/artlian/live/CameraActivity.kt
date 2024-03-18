package com.creator.artlian.live

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.net.http.SslError
import android.opengl.GLSurfaceView
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.*
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import android.widget.Toast
import androidx.core.content.FileProvider
import com.creator.artlian.R
import com.creator.artlian.WebViewActivity
import com.creator.artlian.common.HNApplication
import com.creator.artlian.delegator.HNSharedPreference
import com.creator.artlian.helpers.Constants
import com.creator.artlian.models.Image
import com.creator.artlian.util.*
import com.ksyun.media.streamer.capture.CameraCapture
import com.ksyun.media.streamer.capture.camera.CameraTouchHelper
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt
import com.ksyun.media.streamer.kit.KSYStreamer
import com.ksyun.media.streamer.kit.StreamerConstants
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : Activity() {
    private val TAG = "CameraActivity"
    private var mWebView: WebView? = null
    private var mCallback: String? = null
    private var mCookieManager: CookieManager? = null
    private val LIVE_URL: String = HNApplication.Companion.URL + "/addon/wlive/TV_live_creator.asp"
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private var mCurrentPhotoPath // 촬영된 이미지 경로
            : String? = null
    private var mCapturedImageURI: Uri? = null
    private var mImgArr: JSONArray? = null
    var mStreamer: KSYStreamer? = null
    var mCameraPreview: GLSurfaceView? = null
    var mCameraHintView: CameraHintView? = null
    var mMainHandler: Handler? = null

    companion object {
        const val INTENT_PROTOCOL_START = "intent:"
        const val INTENT_PROTOCOL_INTENT = "#Intent;"
        const val INTENT_PROTOCOL_END = ";end;"
        const val GOOGLE_PLAY_STORE_PREFIX = "market://details?id="
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity가 실행 될 때 항상 화면을 켜짐으로 유지한다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_camera)
        initWebView()
        initCamera()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var data: Intent? = data
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SeongKwon", "============================================")
        Log.d("SeongKwon", "requestCode = $requestCode")
        Log.d("SeongKwon", "resultCode = $resultCode")
        Log.d("SeongKwon", "============================================")
        if ((requestCode == Constants.REQUEST_SELECT_IMAGE_CAMERA || requestCode == Constants.REQUEST_SELECT_IMAGE_ALBUM) && resultCode == RESULT_OK) {
            var result = ""
            try {
                val jObj = JSONObject()
                val jArray = JSONArray()
                jObj.put("resultcd", "0") // 0:성공. 1:실패
                val selectedImages =
                    data!!.extras!![Constants.INTENT_EXTRA_IMAGES] as ArrayList<Image>?
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
                if (data!!.hasExtra("isChanged")) {
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
                if (data!!.hasExtra("imgArr")) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (data == null) data = Intent()
                if (data.data == null) data.data = mCapturedImageURI
                mFilePathCallback!!.onReceiveValue(FileChooserParams.parseResult(resultCode, data))
                mFilePathCallback = null
            } else {
                val results = arrayOf(getResultUri(data))
                mFilePathCallback!!.onReceiveValue(results)
                mFilePathCallback = null
            }
        } else if (resultCode == RESULT_OK && requestCode == Constants.REQUEST_GET_FILE) {
            data?.data?.let {
                try {
                    val bitmap = BitmapUtil.uriToBitmap(this, it)
                    val base64String = getBase64String(bitmap!!)

                    val fileName = File(it.path).name
                    val jObj = JSONObject()
                    jObj.put("fName", fileName)
                    jObj.put("fData", base64String)

                    executeJavascript("$mCallback($jObj)")
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (mWebView != null && mWebView!!.canGoBack()) {
            mWebView!!.goBack()
            return
        }
        super.onBackPressed()
    }

    private fun initCamera() {
        mCameraPreview = findViewById<View>(R.id.camera_preview) as GLSurfaceView
        mMainHandler = Handler()
        // 创建KSYStreamer实例
        mStreamer = KSYStreamer(this)
        // 设置预览View
        mStreamer!!.setDisplayPreview(mCameraPreview)
    }

    private fun initStreamer(streamUrl: String) {
// 设置推流url（需要向相关人员申请，测试地址并不稳定！）
        mStreamer!!.url = streamUrl
        // 设置预览分辨率, 当一边为0时，SDK会根据另一边及实际预览View的尺寸进行计算
        mStreamer!!.setPreviewResolution(720, 1280)
        // 设置推流分辨率，可以不同于预览分辨率（不应大于预览分辨率，否则推流会有画质损失）
        mStreamer!!.setTargetResolution(720, 1280)
        // 设置预览帧率
        mStreamer!!.previewFps = 15f
        // 设置推流帧率，当预览帧率大于推流帧率时，编码模块会自动丢帧以适应设定的推流帧率
        mStreamer!!.targetFps = 15f
        // 设置视频码率，分别为初始平均码率、最高平均码率、最低平均码率，单位为kbps，另有setVideoBitrate接口，单位为bps
//        mStreamer.setVideoKBitrate(600, 800, 400);
        mStreamer!!.setVideoKBitrate(2048, 2160, 2000)
        // 设置音频采样率
        mStreamer!!.audioSampleRate = 44100
        // 设置音频码率，单位为kbps，另有setAudioBitrate接口，单位为bps
        mStreamer!!.setAudioKBitrate(48)
        /**
         * 设置编码模式(软编、硬编)，请根据白名单和系统版本来设置软硬编模式，不要全部设成软编或者硬编,白名单可以联系金山云商务:
         * StreamerConstants.ENCODE_METHOD_SOFTWARE
         * StreamerConstants.ENCODE_METHOD_HARDWARE
         */
        mStreamer!!.enableRepeatLastFrame = false // disable repeat last frame in background
        mStreamer!!.setEnableAutoRestart(true, 500)
        mStreamer!!.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE)
        // 设置屏幕的旋转角度，支持 0, 90, 180, 270
        mStreamer!!.rotateDegrees = 0
        // 设置开始预览使用前置还是后置摄像头
        mStreamer!!.cameraFacing = CameraCapture.FACING_FRONT
        mStreamer!!.toggleTorch(false)

        // 触摸对焦和手势缩放功能
//        if (config.mZoomFocus) {
        val cameraTouchHelper = CameraTouchHelper()
        cameraTouchHelper.setCameraCapture(mStreamer!!.cameraCapture)
        mCameraPreview!!.setOnTouchListener(cameraTouchHelper)
        // set CameraHintView to show focus rect and zoom ratio
        mCameraHintView = findViewById(R.id.camera_hint)
        mCameraHintView?.visibility = View.VISIBLE
        cameraTouchHelper.setCameraHintView(mCameraHintView)
        //        }
        if (mMainHandler != null) {
            mMainHandler!!.postDelayed({ mStreamer!!.startStream() }, 100)
        }


        // 切换前后摄像头
//        mStreamer.switchCamera();
// 开关闪光灯

// 设置美颜滤镜，关于美颜滤镜的具体说明请参见专题说明
//        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
//                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
    }

    private fun initWebView() {
        mWebView = findViewById<View>(R.id.webView) as WebView
        mWebView!!.setBackgroundColor(0)
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
        mWebView!!.settings.useWideViewPort = true
        mWebView!!.settings.databaseEnabled = true
        mWebView!!.settings.domStorageEnabled = true
        mWebView!!.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView!!.settings.setSupportMultipleWindows(true)
//        mWebView!!.settings.setAppCacheEnabled(true)
        mWebView!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
//        mWebView!!.settings.setAppCachePath(applicationContext.cacheDir.absolutePath)
        mWebView!!.settings.textZoom = 100
        mWebView!!.addJavascriptInterface(WebAppInterface(this, mWebView!!), "android")
        mWebView!!.isDrawingCacheEnabled = true
        mWebView!!.buildDrawingCache()
        mWebView!!.loadUrl(LIVE_URL)
    }

    public override fun onResume() {
        super.onResume()
        if (mStreamer != null) {
            // 一般可以在onResume中开启摄像头预览
            mStreamer!!.startCameraPreview()
            // 调用KSYStreamer的onResume接口
            mStreamer!!.onResume()
            // 如果onPause中切到了DummyAudio模块，可以在此恢复
            mStreamer!!.setUseDummyAudioCapture(false)

            // 设置Info回调，可以收到相关通知信息
            mStreamer!!.onInfoListener = KSYStreamer.OnInfoListener { what, msg1, msg2 ->
                // ...
            }
            // 设置错误回调，收到该回调后，一般是发生了严重错误，比如网络断开等，
// SDK内部会停止推流，APP可以在这里根据回调类型及需求添加重试逻辑。
            mStreamer!!.onErrorListener =
                KSYStreamer.OnErrorListener { what, msg1, msg2 -> Log.e(TAG, "onError : $msg1") }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mStreamer != null) {
            mStreamer!!.onPause()
            // 一般在这里停止摄像头采集
            mStreamer!!.stopCameraPreview()
            // 如果希望App切后台后，停止录制主播端的声音，可以在此切换为DummyAudio采集，
            // 该模块会代替mic采集模块产生静音数据，同时释放占用的mic资源
            mStreamer!!.setUseDummyAudioCapture(true)
        }
    }

    public override fun onDestroy() {
        if (mMainHandler != null) {
            mMainHandler!!.removeCallbacksAndMessages(null)
            mMainHandler = null
        }
        if (mStreamer != null) {
            mStreamer!!.stopRecord()
            // 清理相关资源
            mStreamer!!.release()
        }
        super.onDestroy()
    }

    inner class HNWebViewClient : WebViewClient(), DownloadListener {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            // LogUtil.d("onPageLoadStopped : " + url);

//            mProgressUtil.dismiss();
        }

        override fun onPageStarted(view: WebView, url: String, paramBitmap: Bitmap?) {
            super.onPageStarted(view, url, paramBitmap)
            // LogUtil.d("onPageLoadStarted : " + url);
            executeJavascript("localStorage.setItem(\"dv_id\"," + "\"" + HNApplication.mDeviceId + "\")")
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            if (HNSharedPreference.getSharedPreference(this@CameraActivity, "isFirstLive") == "") {
                val builder = AlertDialog.Builder(this@CameraActivity)
                builder.setTitle("라이브 방송을 시작 하시겠습니까?")
                builder.setPositiveButton("예") { dialog, id ->
                    handler.proceed()
                    HNSharedPreference.putSharedPreference(this@CameraActivity, "isFirstLive", "Y")
                    dialog.dismiss()
                }
                builder.setNegativeButton("아니오") { dialog, id ->
                    handler.cancel()
                    HNSharedPreference.putSharedPreference(this@CameraActivity, "isFirstLive", "")
                    finish()
                }
                val alertDialog = builder.create()
                alertDialog.show()
            } else {
                handler.proceed()
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
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
                            val dialog = AlertDialog.Builder(this@CameraActivity)
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
                                        this@CameraActivity,
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
            view.setDownloadListener(this)
            return false // webview replace
        }

        override fun onDownloadStart(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimeType: String?,
            contentLength: Long
        ) {
            EtcUtil.downloadFile(url, userAgent, contentDisposition, mimeType, this@CameraActivity)
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
                if ("ACT1027" == actionCode) {
                    LogUtil.d("ACT1027 - wlive 전, 후면 카메라 제어")
                    var resultcd = 1
                    if (actionParamObj!!.has("key_type")) {
                        mStreamer!!.switchCamera()
                    } else {
                        resultcd = 0
                    }
                    val jsonObject = JSONObject()
                    jsonObject.put("resultcd", resultcd) //1: 성공, 0: 실패
                    executeJavascript("$mCallback($jsonObject)")
                } else if ("ACT1028" == actionCode) {
                    LogUtil.d("ACT1028 - wlive 마이크 제어")
                    var resultcd = 1
                    if (actionParamObj!!.has("key_type")) {   //0: 마이크 끄기,1: 켜기
                        if (actionParamObj.getInt("key_type") == 0) {
                            mStreamer!!.setMuteAudio(true)
                        } else {
                            mStreamer!!.setMuteAudio(false)
                        }
                    } else {
                        resultcd = 0
                    }
                    val jsonObject = JSONObject()
                    jsonObject.put("resultcd", resultcd) //1: 성공, 0: 실패
                    executeJavascript("$mCallback($jsonObject)")
                } else if ("ACT1029" == actionCode) {
                    LogUtil.d("ACT1029 - wlive 이미지필터 제어")
                    var resultcd = 1
                    if (actionParamObj!!.has("key_type")) {
                        when (actionParamObj.getInt("key_type")) {
                            0 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE
                            )
                            1 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT
                            )
                            2 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_SKINWHITEN
                            )
                            3 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_ILLUSION
                            )
                            4 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE
                            )
                            5 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_SMOOTH
                            )
                            6 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT_EXT
                            )
                            7 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_SOFT_SHARPEN
                            )
                            8 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO
                            )
                            9 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO1
                            )
                            10 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO2
                            )
                            11 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO3
                            )
                            12 -> mStreamer!!.imgTexFilterMgt.setFilter(
                                mStreamer!!.glRender,
                                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO4
                            )
                        }
                    } else {
                        resultcd = 0
                    }
                    val jsonObject = JSONObject()
                    jsonObject.put("resultcd", resultcd) //1: 성공, 0: 실패
                    executeJavascript("$mCallback($jsonObject)")
                } else if ("ACT1030" == actionCode) {
                    LogUtil.d("ACT1030 - wlive 스트림키 전달 및 송출")
                    val resultcd = 1
                    val streamUrl = actionParamObj!!.getString("stream_url")
                    runOnUiThread { initStreamer(streamUrl) }
                    val jsonObject = JSONObject()
                    jsonObject.put("resultcd", resultcd) //1: 성공, 0: 실패
                    executeJavascript("$mCallback($jsonObject)")
                } else if ("ACT1031" == actionCode) {
                    // 종료
                    finish()
                } else if ("ACT1015" == actionCode) {
                    LogUtil.d("ACT1015 - 웹뷰 새창")
                    if (actionParamObj!!.has("url")) {
                        val request_url = actionParamObj.getString("url")
                        LogUtil.d("url : $request_url")
                        intent = Intent(this@CameraActivity, WebViewActivity::class.java)
                        intent.putExtra("webviewUrl", request_url)
                        startActivity(intent)
                    }
                } else if ("ACT1036" == actionCode) {
                    LogUtil.d("ACT1036 - 스트리밍 화면 캡쳐")
                    mStreamer?.requestScreenShot {
                        it?.let {
                            val base64String = getBase64String(it)
                            val jObj = JSONObject()
                            jObj.put("fData", base64String)
                            executeJavascript("$mCallback($jObj)")
                        }
                    }
                } else if ("ACT1037" == actionCode) {
                    LogUtil.d("ACT1037 - 파일 열기")
                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = "*/*"
                    val intentArray: Array<Intent?>
                    intentArray = contentSelectionIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(chooserIntent, Constants.REQUEST_GET_FILE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                    // File 객체의 URI 를 얻는다.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mCapturedImageURI = FileProvider.getUriForFile(
                            this@CameraActivity,
                            "$packageName.fileprovider",
                            photoFile
                        )
                    } else {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        mCapturedImageURI = Uri.fromFile(photoFile)
                    }
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
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
            AlertDialog.Builder(this@CameraActivity)
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
            AlertDialog.Builder(this@CameraActivity)
                .setTitle(resources.getString(R.string.alert_title))
                .setMessage(paramString2)
                .setPositiveButton(resources.getString(R.string.confirm)) { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.confirm() }
                .setNegativeButton(resources.getString(R.string.cancel)) { paramAnonymousDialogInterface, paramAnonymousInt -> paramJsResult.cancel() }
                .setCancelable(false).create().show()
            return true
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {

            val windowWebview = WebView(this@CameraActivity)
            windowWebview.settings.run {
                javaScriptEnabled = true                        // 자바 스크립트 사용 여부
                setSupportMultipleWindows(true)                 //여러개의 윈도우 사용 여부
                javaScriptCanOpenWindowsAutomatically = true
            }

            val windowDialog = Dialog(this@CameraActivity).apply {

                setContentView(windowWebview)
                val params = window?.attributes?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }

                window?.attributes = params
            }

            windowWebview.webChromeClient = object : WebChromeClient() {

                override fun onCloseWindow(window: WebView?) {
                    windowDialog.dismiss()
                    windowWebview.destroy()
                    window?.destroy()
                }
            }

            windowDialog.setOnDismissListener {
                windowWebview.destroy()
            }

            windowDialog.show()

            (resultMsg?.obj as WebView.WebViewTransport).webView = windowWebview
            resultMsg.sendToTarget()

            return true
        }
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

    private fun getBase64String(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
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
}