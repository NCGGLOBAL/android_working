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
package com.aidapps.agcmall

import com.aidapps.agcmall.common.HNApplication
import com.aidapps.agcmall.delegator.HNCommTran
import com.aidapps.agcmall.delegator.HNCommTranInterface
import com.aidapps.agcmall.delegator.HNSharedPreference
import com.aidapps.agcmall.util.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.util.*

class MyFirebaseInstanceIdService : FirebaseInstanceIdService() {
    private var mHNCommTran: HNCommTran? = null

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        LogUtil.d(TAG, "Refreshed token: $refreshedToken")
        HNSharedPreference.putSharedPreference(applicationContext, "pushtoken", refreshedToken)
        // Once a token is generated, we subscribe to topic.
        // topic 생성
        if (HNSharedPreference.getSharedPreference(applicationContext, "pushtopic") == "") {
            val topic = Random().nextInt(100) + 1 // topic 1 ~ 100의 값으로 임의 지정
            FirebaseMessaging.getInstance().subscribeToTopic(topic.toString())
            HNSharedPreference.putSharedPreference(
                applicationContext,
                "pushtopic",
                topic.toString()
            )
        }
        if (HNSharedPreference.getSharedPreference(this, "deviceId") == "") {
            HNApplication.Companion.mDeviceId = EtcUtil.getRandomKey(16)
            HNSharedPreference.putSharedPreference(
                this,
                "deviceId",
                HNApplication.Companion.mDeviceId
            )
        } else {
            HNApplication.Companion.mDeviceId =
                HNSharedPreference.getSharedPreference(this, "deviceId")
        }

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken)
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
    private fun sendRegistrationToServer(token: String?) {
        LogUtil.e("sendRegistrationToServer : $token")
        Thread(Runnable {
            try {
                val jObj = JSONObject()
                jObj.put("os", "Android")
                jObj.put(
                    "memberKey",
                    HNSharedPreference.getSharedPreference(applicationContext, "memberKey")
                )
                jObj.put(
                    "pushKey",
                    HNSharedPreference.getSharedPreference(applicationContext, "pushtoken")
                )
                jObj.put("deviceId", HNApplication.Companion.mDeviceId)
                mHNCommTran = HNCommTran(object : HNCommTranInterface {
                    override fun recvMsg(tranCode: String?, params: String) {
                        if (tranCode.equals(HNApplication.PUSH_URL)) {
                            LogUtil.e("recv pushRegister : $tranCode : $params");
                        }
                    }
                })
                mHNCommTran!!.sendMsg(HNApplication.Companion.PUSH_URL, jObj)
                return@Runnable
            } catch (localException: Exception) {
                localException.printStackTrace()
            }
        }).start()
    }

    companion object {
        private const val TAG = "MyFirebaseIIDService"
    }
}