/**
 * Copyright Google Inc. All Rights Reserved.
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
package com.creator.so1n

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import com.creator.so1n.util.LogUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private var mPushUid: String? = ""
    private var mLandingUrl: String? = ""
    private var mImgUrl: String? = ""
    private var mPushType: String? = "default"

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        val data = remoteMessage.data
        val myCustomKey = data["title"]
        LogUtil.d(TAG, "myCustomKey: $myCustomKey")

        // Check if message contains a data payload.
        if (remoteMessage.data.size > 0) {
            LogUtil.d(TAG, "Message data payload: " + remoteMessage.data)
            if (remoteMessage.data.containsKey("pushUid") && remoteMessage.data.containsKey("url")) {
                mPushUid = remoteMessage.data["pushUid"]
                mLandingUrl = remoteMessage.data["url"]
            }
            if (remoteMessage.data.containsKey("imgUrl")) {
                mImgUrl = remoteMessage.data["imgUrl"]
            }
            if (remoteMessage.data.containsKey("push_type")) {
                mPushType = remoteMessage.data["push_type"]
            }
            if ( /* Check if data needs to be processed by long running job */true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow()
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            try {
                LogUtil.d(
                    TAG, "Message Notification title1 : " + remoteMessage.notification!!
                        .title
                )
                LogUtil.d(
                    TAG, "Message Notification Body1 : " + remoteMessage.notification!!
                        .body
                )
                sendNotification(
                    remoteMessage.notification!!.title, remoteMessage.notification!!.body, mImgUrl
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (remoteMessage.data.containsKey("title") && remoteMessage.data.containsKey("body")) {
                LogUtil.d(TAG, "Message Notification title2 : " + remoteMessage.data["title"])
                LogUtil.d(TAG, "Message Notification Body2 : " + remoteMessage.data["body"])
                sendNotification(remoteMessage.data["title"], remoteMessage.data["body"], mImgUrl)
            }
        }
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]
    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    //    private void scheduleJob() {
    //        // [START dispatch_job]
    //        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
    //        Job myJob = dispatcher.newJobBuilder()
    //                .setService(MyJobService.class)
    //                .setTag("my-job-tag")
    //                .build();
    //        dispatcher.schedule(myJob);
    //        // [END dispatch_job]
    //    }
    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        LogUtil.d(TAG, "Short lived task is done.")
    }

    /**
     *
     * @param title
     * @param message
     * @param imgUrl
     */
    private fun sendNotification(title: String?, message: String?, imgUrl: String?) {
        sendNotification(this).execute(title, message, imgUrl)
    }

    private inner class sendNotification(var ctx: Context) : AsyncTask<String?, Void?, Bitmap?>() {
        var title: String? = null
        var message: String? = null

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            try {
                val intent = Intent(ctx, MainActivity::class.java)
                intent.putExtra("pushUid", mPushUid)
                intent.putExtra("url", mLandingUrl)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(
                    ctx,
                    0 /* Request code */,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                // Creates an explicit intent for an Activity in your app
                var defaultSoundUri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (mPushType == "custom_push") {
                    defaultSoundUri = Uri.parse(
                        "android.resource://"
                                + ctx.packageName + "/" + R.raw.custom_push
                    )
                }
                var notificationBuilder: Notification.Builder? = null
                LogUtil.d(TAG, "Message Notification 3")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LogUtil.d(TAG, "Message Notification 4")
                    notificationBuilder = Notification.Builder(
                        ctx,
                        MyNotificationManager.Channel.Companion.MESSAGE
                    ) // round Image
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(
                            BitmapFactory.decodeResource(
                                ctx.resources,
                                R.mipmap.ic_launcher
                            )
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                    if (result != null) {
                        val bigPictureStyle = Notification.BigPictureStyle()
                            .bigPicture(result)
                            .setBigContentTitle(title)
                            .setSummaryText(message)
                        notificationBuilder.style = bigPictureStyle
                    } else {
                        notificationBuilder.style = Notification.BigTextStyle().bigText(message)
                    }
                } else {
                    LogUtil.d(TAG, "Message Notification 5")
                    notificationBuilder = Notification.Builder(ctx)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(
                            BitmapFactory.decodeResource(
                                ctx.resources,
                                R.mipmap.ic_launcher
                            )
                        )
                        .setStyle(
                            Notification.BigPictureStyle()
                                .bigPicture(result)
                                .setBigContentTitle(title)
                                .setSummaryText(message)
                        )
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                }
                getManager(ctx).notify(0 /* ID of notification */, notificationBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg params: String?): Bitmap? {
            val `in`: InputStream
            title = params[0]
            message = params[1]
            try {
                val url = URL(params[2])
                val connection =
                    url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                `in` = connection.inputStream
                return BitmapFactory.decodeStream(`in`)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private fun getManager(context: Context): NotificationManager {
            return context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        } //    public static void deleteChannel(Context context, @Channel String channel) {
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //            getManager(context).deleteNotificationChannel(channel);
        //        }
        //    }
    }
}