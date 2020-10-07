package com.mallup.martnara.delegator;

import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.mallup.martnara.common.HNApplication;
import com.mallup.martnara.util.LogUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by skcrackers on 5/24/16.
 */
public class HNCommTran {
    private String mTrCode;             // Trans Code

    private HNCommTranInterface hnCertmgrTranInterface;
    public HNCommTran(HNCommTranInterface event) {
        hnCertmgrTranInterface = event;
    }

    public void sendMsg(String tranCode, JSONObject jsonParam) {
        mTrCode = tranCode;

        System.out.println("tran input : " + jsonParam.toString());

        // TODO : 유효성 체크
        new sendMsgTask().execute(tranCode, jsonParam.toString());
    }

    // AsyncTask<Params,Progress,Result>
    private class sendMsgTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";

            try {
//                URL url = new URL(HNApplication.URL + params[0]);
                URL url = new URL(params[0]);
                LogUtil.d("url : " + url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST"); // default GET
                urlConnection.setDoInput(true);         // InputStream으로 응답 헤더와 메시지를 v읽어들이겠다는 옵션
                urlConnection.setDoOutput(true);        // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
//                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Content-Type", "text/html");
                urlConnection.setRequestProperty("Cookie", TextUtils.join(";", HNApplication.getCookieManager().getCookieStore().getCookies()));

//                urlConnection.connect();
//                LogUtil.d("urlConnection.getResponseCode() : " + urlConnection.getResponseCode());

                try {
                    OutputStream os = urlConnection.getOutputStream();

                    os.write(params[1].getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    // Header Cookies
                    List<String> cookies = urlConnection.getHeaderFields().get("Set-Cookie");
                    if (cookies != null) {
                        for (String cookie : cookies) {
                            System.out.println("@COOKIE : " + cookie.split(";\\s*")[0]);
                            HNApplication.getCookieManager().getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                        }
                    }

                    // Body
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String response = "";
                    while ((response = in.readLine()) != null) {
                        result += response;
                    }
                    in.close();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                LogUtil.d("onPostExecute : " + result);

                if (null != result && !"".equals(result)) {    // SUCCESS
                    try {
                        // mTrCode, result
                        hnCertmgrTranInterface.recvMsg(mTrCode, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void disableConnectionReuseIfNecessary() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }
}
