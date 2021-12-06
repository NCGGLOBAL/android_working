package com.creator.studiomallup.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.creator.studiomallup.common.HNApplication;
import com.creator.studiomallup.delegator.HNSharedPreference;


/**
 * Created by skcrackers on 8/5/16.
 */
public class HNConfig {
    private Context ctx;

    public HNConfig(Context paramContext)
    {
        this.ctx = paramContext;
    }

    public String getAppVersion() {
        try {
            String str = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
            return str;
        } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
        }
        return "";
    }

    public String getDeviceId() {
        LogUtil.d("getDeviceId() : " + HNApplication.mDeviceId);
        return HNApplication.mDeviceId;
    }

    public String getMemberKey() {
        return HNSharedPreference.getSharedPreference(ctx, "memberKey");
    }
    public void setMemberKey(String paramString) {
        HNSharedPreference.putSharedPreference(ctx, "memberKey", paramString);
    }

    public String getAppId() {
        return HNSharedPreference.getSharedPreference(ctx, "appId");
    }
    public void setAppId(String paramString) {
        HNSharedPreference.putSharedPreference(ctx, "appId", paramString);
    }

    public String getUserId() {
        return HNSharedPreference.getSharedPreference(ctx, "userId");
    }
    public void setUserId(String paramString) {
        HNSharedPreference.putSharedPreference(ctx, "userId", paramString);
    }

    public String getChannelId() {
        return HNSharedPreference.getSharedPreference(ctx, "channelId");
    }
    public void setChannelId(String paramString) {
        HNSharedPreference.putSharedPreference(ctx, "channelId", paramString);
    }

    public String getRequestId() {
        return HNSharedPreference.getSharedPreference(ctx, "requestId");
    }
    public void setRequestId(String paramString) {
        HNSharedPreference.putSharedPreference(ctx, "requestId", paramString);
    }
}
