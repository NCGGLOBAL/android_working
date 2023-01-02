/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creator.gachimoa;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.creator.gachimoa.common.HNApplication;
import com.creator.gachimoa.delegator.HNCommTran;
import com.creator.gachimoa.delegator.HNCommTranInterface;
import com.creator.gachimoa.delegator.HNSharedPreference;
import com.creator.gachimoa.util.EtcUtil;
import com.creator.gachimoa.util.LogUtil;

import org.json.JSONObject;

import java.util.Random;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    private HNCommTran mHNCommTran;

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        LogUtil.d(TAG, "Refreshed token: " + refreshedToken);
        HNSharedPreference.putSharedPreference(getApplicationContext(), "pushtoken", refreshedToken);
        // Once a token is generated, we subscribe to topic.
        // topic 생성
        if (HNSharedPreference.getSharedPreference(getApplicationContext(), "pushtopic").equals("")) {
            int topic = (new Random()).nextInt(100) + 1;          // topic 1 ~ 100의 값으로 임의 지정
            FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(topic));

            HNSharedPreference.putSharedPreference(getApplicationContext(), "pushtopic", String.valueOf(topic));
        }
        if (HNSharedPreference.getSharedPreference(this, "deviceId").equals("")) {
            HNApplication.mDeviceId = EtcUtil.getRandomKey(16);

            HNSharedPreference.putSharedPreference(this, "deviceId", HNApplication.mDeviceId);
        } else {
            HNApplication.mDeviceId = HNSharedPreference.getSharedPreference(this, "deviceId");
        }

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        LogUtil.e("sendRegistrationToServer : " + token);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject jObj = new JSONObject();
                    jObj.put("os", "Android");
                    jObj.put("memberKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "memberKey"));
                    jObj.put("pushKey", HNSharedPreference.getSharedPreference(getApplicationContext(), "pushtoken"));
                    jObj.put("deviceId", HNApplication.mDeviceId);
                    mHNCommTran = new HNCommTran(new HNCommTranInterface() {
                        @Override
                        public void recvMsg(String tranCode, String params) {
                            if (tranCode.equals(HNApplication.PUSH_URL)) {
                                LogUtil.e("recv pushRegister : " + tranCode + " : " + params);
                            }
                        }
                    });
                    mHNCommTran.sendMsg(HNApplication.PUSH_URL, jObj);
                    return;
                } catch (Exception localException) {
                    localException.printStackTrace();
                }
            }
        }).start();
    }
}