package com.ncggloval.hahakoreashop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.baidu.ufosdk.UfoSDK
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application is onCreated!")
        Log.e(TAG, "*****UfoSDK.init(this)*****")
        UfoSDK.init(this)
        UfoSDK.openRobotAnswer()

        // 设置用户的头像
        UfoSDK.setCurrentUserIcon(meIconBitmap)
        // 在聊天界面中获取聊天信息的时间间隔
        UfoSDK.setChatThreadTime(10)
        // 设置当前用户名
        // 我的反馈按钮颜色
        UfoSDK.setRootBackgroundColor(resources.getColor(R.color.gray))
    }

    fun stream2ByteArray(`is`: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(16384)
        try {
            while (`is`.read(data, 0, data.size).also { nRead = it } != -1) {
                baos.write(data, 0, nRead)
            }
            baos.flush()
        } catch (e: IOException) {
            Log.i(TAG, "stream2ByteArray fail")
        }
        return baos.toByteArray()
    }

    // TODO Auto-generated catch block
    val meIconBitmap: Bitmap?
        get() {
            val `is`: InputStream
            var bmpMeIcon: Bitmap? = null
            try {
                `is` = assets.open("ufo_res/ufo_defult_me_icon.png")
                val bs = stream2ByteArray(`is`)
                bmpMeIcon = BitmapFactory.decodeByteArray(bs, 0, bs.size, null)
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            return bmpMeIcon
        }

    companion object {
        val TAG = Application::class.java.simpleName
    }
}