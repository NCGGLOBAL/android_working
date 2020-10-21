package com.mallup.allflower.common;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kakao.auth.KakaoSDK;
import com.mallup.allflower.MyNotificationManager;

import java.net.CookieManager;

/**
 * Created by skcrackers on 5/26/16.
 */
public class HNApplication extends Application {
    public static final String TAG = HNApplication.class.getSimpleName();
    private RequestQueue mRequestQueue;

    public static boolean mSigned = false;
    public static String URL = "http://modookot.co.kr/m";
    public static String PUSH_URL = "http://modookot.co.kr/m/m/app/pushRegister.asp";
    public static boolean mIsFirstLoading = false;
    private static HNApplication mInstance;
    public static String mDeviceId = "";

    public static String mImgArrForReg = "";

    public static CookieManager mCookieManager;

    public static CookieManager getCookieManager() {
        if (mCookieManager == null) {
            mCookieManager = new CookieManager();
        }

        return mCookieManager;
    }

    /**
     * singleton 애플리케이션 객체를 얻는다.
     * @return singleton 애플리케이션 객체
     */
    public static HNApplication getGlobalApplicationContext() {
        if(mInstance == null)
            throw new IllegalStateException("this application does not inherit com.kakao.GlobalApplication");
        return mInstance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        MyNotificationManager.createChannel(this);

        KakaoSDK.init(new KakaoSDKAdapter());
    }

    public static synchronized HNApplication getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
