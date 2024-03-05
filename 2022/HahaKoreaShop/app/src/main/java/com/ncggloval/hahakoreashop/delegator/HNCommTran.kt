package com.ncggloval.hahakoreashop.delegator

import com.ncggloval.hahakoreashop.common.HNApplication
import kotlin.jvm.JvmOverloads
import android.content.pm.PackageManager
import com.ncggloval.hahakoreashop.delegator.HNSharedPreference
import com.ncggloval.hahakoreashop.R
import android.app.Activity
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler
import com.tencent.mm.sdk.openapi.IWXAPI
import android.os.Bundle
import com.tencent.mm.sdk.openapi.WXAPIFactory
import com.ncggloval.hahakoreashop.wxapi.WXEntryActivity
import com.tencent.mm.sdk.openapi.SendAuth
import android.content.Intent
import com.tencent.mm.sdk.openapi.BaseReq
import com.tencent.mm.sdk.openapi.ConstantsAPI
import com.tencent.mm.sdk.openapi.BaseResp
import org.json.JSONObject
import org.json.JSONException
import android.widget.Toast
import com.ncggloval.hahakoreashop.delegator.HNCommTranInterface
import com.ncggloval.hahakoreashop.delegator.HNCommTran.sendMsgTask
import android.os.AsyncTask
import android.text.TextUtils
import com.ncggloval.hahakoreashop.delegator.HNPlugun
import com.ncggloval.hahakoreashop.delegator.HNGlobalPreference
import android.database.sqlite.SQLiteOpenHelper
import com.ncggloval.hahakoreashop.delegator.HNGlobalPreference.DatabaseHelper
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.content.pm.ApplicationInfo
import com.baidu.ufosdk.UfoSDK
import android.graphics.Bitmap
import android.webkit.WebView
import com.ncggloval.hahakoreashop.delegator.HNCommTran
import android.widget.LinearLayout
import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.CookieSyncManager
import android.webkit.WebChromeClient
import android.webkit.JsResult
import android.content.DialogInterface
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.JavascriptInterface
import org.json.JSONArray
import com.ncggloval.hahakoreashop.WebViewActivity
import com.ncggloval.hahakoreashop.util.EtcUtil
import android.webkit.ValueCallback
import com.ncggloval.hahakoreashop.util.HNConfig
import com.ncggloval.hahakoreashop.MyPushMessageReceiver
import com.ncggloval.hahakoreashop.MainActivity
import com.ncggloval.hahakoreashop.MyPushMessageReceiver.ForegroundCheckTask
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by skcrackers on 5/24/16.
 */
class HNCommTran(private val hnCertmgrTranInterface: HNCommTranInterface) {
    private var mTrCode // Trans Code
            : String? = null

    fun sendMsg(tranCode: String?, jsonParam: JSONObject) {
        mTrCode = tranCode
        println("tran input : $jsonParam")

        // TODO : 유효성 체크
        sendMsgTask().execute(tranCode, jsonParam.toString())
    }

    // AsyncTask<Params,Progress,Result>
    private inner class sendMsgTask : AsyncTask<String?, Void?, String?>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: String?) {
            try {
                println("onPostExecute : $result")
                if (null != result && "" != result) {    // SUCCESS
                    try {
                        // mTrCode, result
                        hnCertmgrTranInterface.recvMsg(mTrCode, result)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg params: String?): String? {
            var result: String? = ""
            try {
//                URL url = new URL(HNApplication.URL + params[0]);
                val url = URL(HNApplication.PUSH_URL)
                println("url : $url")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST" // default GET
                urlConnection.doInput = true // InputStream으로 응답 헤더와 메시지를 v읽어들이겠다는 옵션
                urlConnection.doOutput = true // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
                //                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty(
                    "Cookie",
                    TextUtils.join(";", HNApplication.cookieManager?.cookieStore?.cookies!!)
                )
                try {
                    val os = urlConnection.outputStream
                    os.write(params[1]?.toByteArray(charset("UTF-8")))
                    os.flush()
                    os.close()

                    // Header Cookies
                    val cookies = urlConnection.headerFields["Set-Cookie"]
                    if (cookies != null) {
                        for (cookie in cookies) {
                            println("@COOKIE : " + cookie.split(";\\s*").toTypedArray()[0])
                            HNApplication.cookieManager?.cookieStore?.add(
                                null,
                                HttpCookie.parse(cookie)[0]
                            )
                        }
                    }

                    // Body
                    val `in` = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    var response: String? = ""
                    while (`in`.readLine().also { response = it } != null) {
                        result += response
                    }
                    `in`.close()
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }
    }

    private fun disableConnectionReuseIfNecessary() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false")
        }
    }
}