package com.creator.hotdeallive.util

import android.content.Context
import android.content.pm.PackageManager
import com.creator.hotdeallive.common.HNApplication
import com.creator.hotdeallive.delegator.HNSharedPreference

/**
 * Created by skcrackers on 8/5/16.
 */
class HNConfig(private val ctx: Context) {
    val appVersion: String
        get() {
            try {
                return ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName.toString()
            } catch (localNameNotFoundException: PackageManager.NameNotFoundException) {
            }
            return ""
        }
    val deviceId: String?
        get() {
            LogUtil.d("getDeviceId() : " + HNApplication.Companion.mDeviceId)
            return HNApplication.Companion.mDeviceId
        }
    var memberKey: String?
        get() = HNSharedPreference.getSharedPreference(ctx, "memberKey")
        set(paramString) {
            HNSharedPreference.putSharedPreference(ctx, "memberKey", paramString)
        }
    var appId: String?
        get() = HNSharedPreference.getSharedPreference(ctx, "appId")
        set(paramString) {
            HNSharedPreference.putSharedPreference(ctx, "appId", paramString)
        }
    var userId: String?
        get() = HNSharedPreference.getSharedPreference(ctx, "userId")
        set(paramString) {
            HNSharedPreference.putSharedPreference(ctx, "userId", paramString)
        }
    var channelId: String?
        get() = HNSharedPreference.getSharedPreference(ctx, "channelId")
        set(paramString) {
            HNSharedPreference.putSharedPreference(ctx, "channelId", paramString)
        }
    var requestId: String?
        get() = HNSharedPreference.getSharedPreference(ctx, "requestId")
        set(paramString) {
            HNSharedPreference.putSharedPreference(ctx, "requestId", paramString)
        }
}