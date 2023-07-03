package com.creator.sweetbotpartner.common

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.multidex.MultiDex
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.creator.sweetbotpartner.MyNotificationManager
import java.net.CookieManager

/**
 * Created by skcrackers on 5/26/16.
 */
class HNApplication : Application() {
    private var mRequestQueue: RequestQueue? = null
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        MyNotificationManager.createChannel(this)

//        KakaoSDK.init(new KakaoSDKAdapter());
    }

    val requestQueue: RequestQueue?
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(applicationContext)
            }
            return mRequestQueue
        }

    fun <T> addToRequestQueue(req: Request<T>, tag: String?) {
        req.tag = if (TextUtils.isEmpty(tag)) TAG else tag
        requestQueue!!.add(req)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        requestQueue!!.add(req)
    }

    fun cancelPendingRequests(tag: Any?) {
        if (mRequestQueue != null) {
            mRequestQueue!!.cancelAll(tag)
        }
    }

    companion object {
        val TAG = HNApplication::class.java.simpleName
        var mSigned = false
        var URL = "https://sweetbot.co.kr/m/partner/sales_day_list.asp"
        var PUSH_URL = URL + "/m/app/pushRegister.asp"
        var UPLOAD_URL = URL + "/m/app/"
        var mIsFirstLoading = false

        @get:Synchronized
        var instance: HNApplication? = null
            private set
        var mDeviceId: String? = ""
        var mImgArrForReg = ""
        const val LIMIT_IMAGE_COUNT = 10
        var mCookieManager: CookieManager? = null
        val cookieManager: CookieManager?
            get() {
                if (mCookieManager == null) {
                    mCookieManager = CookieManager()
                }
                return mCookieManager
            }

        /**
         * singleton 애플리케이션 객체를 얻는다.
         * @return singleton 애플리케이션 객체
         */
        val globalApplicationContext: HNApplication?
            get() {
                checkNotNull(instance) { "this application does not inherit com.kakao.GlobalApplication" }
                return instance
            }
    }
}