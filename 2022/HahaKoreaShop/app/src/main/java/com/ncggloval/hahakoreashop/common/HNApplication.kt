package com.ncggloval.hahakoreashop.common

import com.tencent.mm.sdk.openapi.IWXAPI
import java.net.CookieManager

//import com.tencent.mm.sdk.openapi.IWXAPI;
/**
 * Created by skcrackers on 5/26/16.
 */
object HNApplication {
    var mSigned = true
    var URL = "https://hahakoreashop.com"
    var PUSH_URL = URL + "/m/app/pushRegister.asp"
    var mWechatApi: IWXAPI? = null
    var mWechatToken = ""
    var mWechatUserInfo = ""
    var mDeviceId = ""
    var mIsFirstLoading = true

    // wechat
    const val APP_ID = "wx5cab472ee70264e7"
    const val APP_SECRET = "12c372a4c24f3ae30bc2a642b53c63a8"

    // baidu
    const val API_KEY = "api_key"
    var mCookieManager: CookieManager? = null
    val cookieManager: CookieManager?
        get() {
            if (mCookieManager == null) {
                mCookieManager = CookieManager()
            }
            return mCookieManager
        }
}