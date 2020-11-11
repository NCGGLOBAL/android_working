package com.ncggloval.hahakoreashop.common;

//import com.tencent.mm.sdk.openapi.IWXAPI;

import com.tencent.mm.sdk.openapi.IWXAPI;

import java.net.CookieManager;

/**
 * Created by skcrackers on 5/26/16.
 */
public class HNApplication {
    public static boolean mSigned = true;
    public static String URL = "https://hahakoreashop.com";
    public static IWXAPI mWechatApi;
    public static String mWechatToken = "";
    public static String mWechatUserInfo = "";
    public static String mDeviceId = "";

    public static boolean mIsFirstLoading = true;

    // wechat
    public static final String APP_ID = "wx5cab472ee70264e7";
    public static final String APP_SECRET = "12c372a4c24f3ae30bc2a642b53c63a8";

    // baidu
    public static final String API_KEY = "api_key";

    public static CookieManager mCookieManager;

    public static CookieManager getCookieManager() {
        if (mCookieManager == null) {
            mCookieManager = new CookieManager();
        }

        return mCookieManager;
    }
}
