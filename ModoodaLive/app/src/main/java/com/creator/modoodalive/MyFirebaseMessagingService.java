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
package com.creator.modoodalive;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.creator.modoodalive.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private String mPushUid = "";
    private String mLandingUrl = "";
    private String mImgUrl = "";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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
        Map<String, String> data = remoteMessage.getData();
        String myCustomKey = data.get("title");
        LogUtil.d(TAG, "myCustomKey: " + myCustomKey);

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            LogUtil.d(TAG, "Message data payload: " + remoteMessage.getData());

            if(remoteMessage.getData().containsKey("pushUid") && remoteMessage.getData().containsKey("url")) {
                mPushUid = remoteMessage.getData().get("pushUid");
                mLandingUrl = remoteMessage.getData().get("url");
            }

            if(remoteMessage.getData().containsKey("imgUrl")) {
                mImgUrl = remoteMessage.getData().get("imgUrl");
            }

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            try {
                LogUtil.d(TAG, "Message Notification title1 : " + remoteMessage.getNotification().getTitle());
                LogUtil.d(TAG, "Message Notification Body1 : " + remoteMessage.getNotification().getBody());
                sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), mImgUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(remoteMessage.getData().containsKey("title") && remoteMessage.getData().containsKey("body")) {
                LogUtil.d(TAG, "Message Notification title2 : " + remoteMessage.getData().get("title"));
                LogUtil.d(TAG, "Message Notification Body2 : " + remoteMessage.getData().get("body"));
                sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), mImgUrl);
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
    private void handleNow() {
        LogUtil.d(TAG, "Short lived task is done.");
    }

    /**
     *
     * @param title
     * @param message
     * @param imgUrl
     */
    private void sendNotification(String title, String message, String imgUrl) {
        new sendNotification(this).execute(title, message, imgUrl);
    }

    private class sendNotification extends AsyncTask<String, Void, Bitmap> {

        Context ctx;
        String title;
        String message;

        public sendNotification(Context context) {
            super();
            this.ctx = context;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            InputStream in;
            title = params[0];
            message = params[1];
            try {
                URL url = new URL(params[2]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                in = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(in);

                return myBitmap;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            try {
                Intent intent = new Intent(ctx, MainActivity.class);
                intent.putExtra("pushUid", mPushUid);
                intent.putExtra("url", mLandingUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

                // Creates an explicit intent for an Activity in your app
                Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Notification.Builder notificationBuilder = null;
                LogUtil.d(TAG, "Message Notification 3");
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LogUtil.d(TAG, "Message Notification 4");
                    notificationBuilder = new Notification.Builder(ctx, MyNotificationManager.Channel.MESSAGE)  // round Image
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher))
                            .setContentTitle(title)
                            .setContentText(message)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);
                    if (result != null) {
                        Notification.BigPictureStyle bigPictureStyle = new Notification.BigPictureStyle()
                                .bigPicture(result)
                                .setBigContentTitle(title)
                                .setSummaryText(message);
                        notificationBuilder.
                                setStyle(bigPictureStyle);
                    } else  {
                        notificationBuilder.
                                setStyle(new Notification.BigTextStyle().bigText(message));
                    }
                } else {
                    LogUtil.d(TAG, "Message Notification 5");
                    notificationBuilder = new Notification.Builder(ctx)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher))
                            .setStyle(new Notification.BigPictureStyle()
                                    .bigPicture(result)
                                    .setBigContentTitle(title)
                                    .setSummaryText(message))
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setContentText(message)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent);
                }
                getManager(ctx).notify(0 /* ID of notification */, notificationBuilder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static android.app.NotificationManager getManager(Context context) {
        return (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

//    public static void deleteChannel(Context context, @Channel String channel) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            getManager(context).deleteNotificationChannel(channel);
//        }
//    }
}