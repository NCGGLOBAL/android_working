package com.ncggloval.hahakoreashop.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.ncggloval.hahakoreashop.R
import com.ncggloval.hahakoreashop.util.LogUtil
import com.tencent.mm.sdk.openapi.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by skcrackers on 6/2/16.
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {
    private var api: IWXAPI? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_wxentry)
            api = WXAPIFactory.createWXAPI(this, APP_ID, false)

//            SendAuth.Resp resp = new SendAuth.Resp();
//            requestDone = api.sendResp(resp);
//            LogUtil.e("SendAuth.Resp done: " + requestDone);
            val btn_WechatReg = findViewById<View>(R.id.btn_WechatReg) as Button
            btn_WechatReg.setOnClickListener {
                LogUtil.d("registerApp = " + api?.registerApp(APP_ID))
                val req = SendAuth.Req()
                req.scope = "snsapi_userinfo" // Authorization scope requested by applications
                req.state =
                    "none" // Used to indentify applications; returned by WeChat after authentication. none
                val requestDone = api?.sendReq(req)
                LogUtil.e("SendAuth.Req done: $requestDone")
            }
            api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        api!!.handleIntent(intent, this)
    }

    override fun onReq(baseReq: BaseReq) {
        LogUtil.d("baseReq = $baseReq")
        when (baseReq.type) {
            ConstantsAPI.COMMAND_SENDAUTH -> {}
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {}
            else -> {}
        }
    }

    override fun onResp(baseResp: BaseResp) {
        LogUtil.d("baseResp = $baseResp")
        when (baseResp.errCode) {
            BaseResp.ErrCode.ERR_OK -> when (baseResp.type) {
                ConstantsAPI.COMMAND_SENDAUTH -> auth(baseResp)
                ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> finish()
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {}
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {}
            else -> {}
        }
        if (baseResp.type == ConstantsAPI.COMMAND_SENDAUTH) {
            auth(baseResp)
        }
    }

    // http://admin.wechat.com/wiki/index.php?title=User_Profile_via_Web
    private fun auth(resp: BaseResp) {
        val sendResp = resp as SendAuth.Resp
        LogUtil.e("resp.toString = $sendResp")
        LogUtil.e("resp.errCode = " + sendResp.errCode)
        LogUtil.e("resp.state = " + sendResp.state)
        LogUtil.e("resp.token = " + sendResp.token)
        LogUtil.e("resp.token = " + sendResp.userName)
        // Toast.makeText(this, "code = " + sendResp.code, Toast.LENGTH_SHORT).show();
        // final String code = sendResp.code;
        val code = ""
        object : Thread() {
            override fun run() {
                val url: URL
                var reader: BufferedReader? = null
                var s = ""
                try {
                    //secret = getResources().getString(R.string.com_wechat_api_wechat_API_SECRET);
                    //appId = WeChatControl.getInstance().getAppKey(this);
                    url = URL(
                        "https://api.weixin.qq.com/sns/oauth2/access_token?"
                                + "appid=" + APP_ID // The unique ID of the official account
                                + "&secret=" + APP_SECRET // The appsecret of the official account
                                + "&code=" + code // The code parameter obtained in the first step
                                + "&grant_type=authorization_code"
                    )
                    val con = url.openConnection()
                    reader = BufferedReader(
                        InputStreamReader(
                            con.getInputStream()
                        )
                    )
                    var line = reader.readLine().toString()
                    s += line
                    while (reader.readLine().also { line = it } != null) {
                        s = s + line
                    }
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (reader != null) {
                        try {
                            reader.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                LogUtil.i("response: $s")
                val jsonObj: JSONObject
                var accessToken = ""
                var openId = ""
                try {
                    jsonObj = JSONObject(s)
                    accessToken = jsonObj.getString("access_token")
                    openId = jsonObj.getString("openid")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                var br: BufferedReader? = null
                var str = ""
                try {
                    val userUrl = URL(
                        "https://api.weixin.qq.com/sns/userinfo?"
                                + "access_token=" + accessToken
                                + "&openid=" + openId
                    )
                    val conn = userUrl.openConnection()
                    br = BufferedReader(
                        InputStreamReader(
                            conn.getInputStream()
                        )
                    )
                    var tmpStr = br.readLine()
                    str = tmpStr
                    if (br.readLine().also { tmpStr = it } != null) {
                        str += tmpStr
                    }
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                LogUtil.d(str)
            }
        }.start()
    }

    companion object {
        private const val APP_ID = "wxd0e4cd5f2a03bd5b"
        private const val APP_SECRET = "19c89da4fa2c69af27e63d01ea66a39f"
    }
}