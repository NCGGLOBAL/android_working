package com.ncggloval.hahakoreashop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.baidu.ufosdk.UfoSDK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Application extends android.app.Application {
    public static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application is onCreated!");
        Log.e(TAG, "*****UfoSDK.init(this)*****");

        UfoSDK.init(this);
        UfoSDK.openRobotAnswer();

        // 设置用户的头像
        UfoSDK.setCurrentUserIcon(getMeIconBitmap());
        // 在聊天界面中获取聊天信息的时间间隔
        UfoSDK.setChatThreadTime(10);
        // 设置当前用户名
        // 我的反馈按钮颜色
        UfoSDK.setRootBackgroundColor(getResources().getColor(R.color.gray));
    }

    public byte[] stream2ByteArray(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                baos.write(data, 0, nRead);
            }
            baos.flush();
        } catch (IOException e) {
            Log.i(TAG, "stream2ByteArray fail");
        }
        return baos.toByteArray();
    }

    public Bitmap getMeIconBitmap() {
        InputStream is;
        Bitmap bmpMeIcon = null;
        try {
            is = getAssets().open("ufo_res/ufo_defult_me_icon.png");
            byte[] bs = this.stream2ByteArray(is);
            bmpMeIcon = BitmapFactory.decodeByteArray(bs, 0, bs.length, null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bmpMeIcon;
    }
}