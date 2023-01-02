package com.nechingu.wavayopay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.StringDef;

import com.nechingu.wavayopay.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by TedPark on 2018. 2. 3..
 */

public class MyNotificationManager {

    private static final String GROUP_TED_PARK = "wavayo";

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogUtil.e("================= Create Channel!!!!");
            NotificationChannelGroup group1 = new NotificationChannelGroup(GROUP_TED_PARK, GROUP_TED_PARK);
            getManager(context).createNotificationChannelGroup(group1);

            NotificationChannel channelMessage = new NotificationChannel(Channel.MESSAGE,
                    context.getString(R.string.notification_channel_message_title), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelMessage.setDescription(context.getString(R.string.notification_channel_message_description));
            channelMessage.setGroup(GROUP_TED_PARK);
            channelMessage.setLightColor(Color.GREEN);
            channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager(context).createNotificationChannel(channelMessage);

            NotificationChannel channelComment = new NotificationChannel(Channel.COMMENT,
                    context.getString(R.string.notification_channel_comment_title), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelComment.setDescription(context.getString(R.string.notification_channel_comment_description));
            channelComment.setGroup(GROUP_TED_PARK);
            channelComment.setLightColor(Color.BLUE);
            channelComment.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager(context).createNotificationChannel(channelComment);

            NotificationChannel channelNotice = new NotificationChannel(Channel.NOTICE,
                    context.getString(R.string.notification_channel_notice_title), android.app.NotificationManager.IMPORTANCE_HIGH);
            channelNotice.setDescription(context.getString(R.string.notification_channel_notice_description));
            channelNotice.setGroup(GROUP_TED_PARK);
            channelNotice.setLightColor(Color.RED);
            channelNotice.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager(context).createNotificationChannel(channelNotice);
        }
    }

    private static android.app.NotificationManager getManager(Context context) {
        return (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void deleteChannel(Context context, @Channel String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getManager(context).deleteNotificationChannel(channel);
        }

    }

    public static void sendNotification(Context context, int id, @Channel String channel, String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(context, channel)
                    .setContentTitle(title)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                    .setStyle(new Notification.BigPictureStyle()
                            .bigPicture(BitmapFactory.decodeResource(context.getResources(), R.drawable.permission_contents))
                            .setBigContentTitle(title)
                            .setSummaryText(body))
                    .setContentText(body)
                    .setSmallIcon(getSmallIcon())
                    .setAutoCancel(true);

            getManager(context).notify(id, builder.build());
        } else {
            LogUtil.e("================= Test4");
        }
    }

    private static int getSmallIcon() {
        return android.R.drawable.stat_notify_chat;
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Channel.MESSAGE,
            Channel.COMMENT,
            Channel.NOTICE
    })
    public @interface Channel {
        String MESSAGE = "message";
        String COMMENT = "comment";
        String NOTICE = "notice";
    }

}
