package com.ncggloval.hahakoreashop.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ncggloval.hahakoreashop.R;
import com.ncggloval.hahakoreashop.util.LogUtil;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendAuth;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by skcrackers on 6/2/16.
 */
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private static final String APP_ID = "wxd0e4cd5f2a03bd5b";
    private static final String APP_SECRET = "19c89da4fa2c69af27e63d01ea66a39f";
    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_wxentry);

            api = WXAPIFactory.createWXAPI(this, APP_ID, false);

//            SendAuth.Resp resp = new SendAuth.Resp();
//            requestDone = api.sendResp(resp);
//            LogUtil.e("SendAuth.Resp done: " + requestDone);

            Button btn_WechatReg = (Button)findViewById(R.id.btn_WechatReg);
            btn_WechatReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogUtil.d("registerApp = " + api.registerApp(APP_ID));

                    final SendAuth.Req req = new SendAuth.Req();
                    req.scope = "snsapi_userinfo";      // Authorization scope requested by applications
                    req.state = "none";                 // Used to indentify applications; returned by WeChat after authentication. none
                    boolean requestDone = api.sendReq(req);
                    LogUtil.e("SendAuth.Req done: " + requestDone);
                }
            });
            api.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq baseReq) {
        LogUtil.d("baseReq = " + baseReq);
        switch (baseReq.getType()) {
            case ConstantsAPI.COMMAND_SENDAUTH:
                break;
            case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
                break;
            default:
                break;
        }
    }

    @Override
    public void onResp(BaseResp baseResp) {
        LogUtil.d("baseResp = " + baseResp);
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                switch (baseResp.getType()) {
                    case ConstantsAPI.COMMAND_SENDAUTH:
                        auth(baseResp);
                        break;
                    case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
                        finish();
                        break;
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                break;
            default:
                break;
        }
        if (baseResp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            auth(baseResp);
        }
    }

    // http://admin.wechat.com/wiki/index.php?title=User_Profile_via_Web
    private void auth(BaseResp resp) {
        SendAuth.Resp sendResp = (SendAuth.Resp) resp;
        LogUtil.e("resp.toString = " + sendResp.toString());
        LogUtil.e("resp.errCode = " + sendResp.errCode);
        LogUtil.e("resp.state = " + sendResp.state);
        LogUtil.e("resp.token = " + sendResp.token);
        LogUtil.e("resp.token = " + sendResp.userName);
        // Toast.makeText(this, "code = " + sendResp.code, Toast.LENGTH_SHORT).show();
        // final String code = sendResp.code;
        final String code = "";
        new Thread() {
            public void run() {
                URL url;
                BufferedReader reader = null;
                String s = "";
                try {
                    //secret = getResources().getString(R.string.com_wechat_api_wechat_API_SECRET);
                    //appId = WeChatControl.getInstance().getAppKey(this);

                    url = new URL("https://api.weixin.qq.com/sns/oauth2/access_token?"
                            + "appid=" + APP_ID         // The unique ID of the official account
                            + "&secret=" + APP_SECRET   // The appsecret of the official account
                            + "&code=" + code           // The code parameter obtained in the first step
                            + "&grant_type=authorization_code");
                    URLConnection con = url.openConnection();
                    reader = new BufferedReader(new InputStreamReader(
                            con.getInputStream()));
                    String line = reader.readLine().toString();
                    s += line;
                    while ((line = reader.readLine()) != null) {
                        s = s + line;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                LogUtil.i("response: " + s);
                JSONObject jsonObj;
                String accessToken = "";
                String openId = "";
                try {
                    jsonObj = new JSONObject(s);
                    accessToken = jsonObj.getString("access_token");
                    openId = jsonObj.getString("openid");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                BufferedReader br = null;
                String str = "";
                try {
                    URL userUrl = new URL("https://api.weixin.qq.com/sns/userinfo?"
                            + "access_token=" + accessToken
                            + "&openid=" + openId);
                    URLConnection conn = userUrl.openConnection();
                    br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));
                    String tmpStr = br.readLine();
                    str = tmpStr;
                    if ((tmpStr = br.readLine()) != null) {
                        str += tmpStr;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LogUtil.d(str);

            }

            ;
        }.start();
    }
}