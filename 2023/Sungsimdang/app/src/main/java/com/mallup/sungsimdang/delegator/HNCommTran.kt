package com.mallup.sungsimdang.delegator

import android.os.AsyncTask
import android.os.Build
import android.text.TextUtils
import com.mallup.sungsimdang.common.HNApplication
import com.mallup.sungsimdang.util.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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

        override fun doInBackground(vararg params: String?): String? {
            var result: String? = ""
            try {
//                URL url = new URL(HNApplication.URL + params[0]);
                val url = URL(params[0])
                LogUtil.d("url : $url")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST" // default GET
                urlConnection.doInput = true // InputStream으로 응답 헤더와 메시지를 v읽어들이겠다는 옵션
                urlConnection.doOutput = true // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
                //                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Content-Type", "text/html")
                urlConnection.setRequestProperty(
                    "Cookie",
                    TextUtils.join(
                        ";",
                        HNApplication.cookieManager?.cookieStore?.cookies!!
                    )
                )

//                urlConnection.connect();
//                LogUtil.d("urlConnection.getResponseCode() : " + urlConnection.getResponseCode());
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
                            HNApplication.cookieManager?.cookieStore
                                ?.add(null, HttpCookie.parse(cookie)[0])
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

        override fun onPostExecute(result: String?) {
            try {
                LogUtil.d("onPostExecute : $result")
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
    }

    private fun disableConnectionReuseIfNecessary() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false")
        }
    }
}