package com.creator.sweetbotpartner

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import com.creator.sweetbotpartner.util.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by TedPark on 2018. 2. 3..
 */
object MyNotificationManager {
    private const val GROUP_TED_PARK = "wavayo"
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogUtil.e("================= Create Channel!!!!")
            val group1 = NotificationChannelGroup(GROUP_TED_PARK, GROUP_TED_PARK)
            getManager(context).createNotificationChannelGroup(group1)
            val channelMessage = NotificationChannel(
                Channel.MESSAGE,
                context.getString(R.string.notification_channel_message_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channelMessage.description =
                context.getString(R.string.notification_channel_message_description)
            channelMessage.group = GROUP_TED_PARK
            channelMessage.lightColor = Color.GREEN
            channelMessage.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            getManager(context).createNotificationChannel(channelMessage)
            val channelComment = NotificationChannel(
                Channel.COMMENT,
                context.getString(R.string.notification_channel_comment_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channelComment.description =
                context.getString(R.string.notification_channel_comment_description)
            channelComment.group = GROUP_TED_PARK
            channelComment.lightColor = Color.BLUE
            channelComment.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            getManager(context).createNotificationChannel(channelComment)
            val channelNotice = NotificationChannel(
                Channel.NOTICE,
                context.getString(R.string.notification_channel_notice_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            channelNotice.description =
                context.getString(R.string.notification_channel_notice_description)
            channelNotice.group = GROUP_TED_PARK
            channelNotice.lightColor = Color.RED
            channelNotice.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            getManager(context).createNotificationChannel(channelNotice)
        }
    }

    private fun getManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun deleteChannel(context: Context, @Channel channel: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getManager(context).deleteNotificationChannel(channel)
        }
    }

    fun sendNotification(
        context: Context,
        id: Int,
        @Channel channel: String?,
        title: String?,
        body: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(context, channel)
                .setContentTitle(title)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setStyle(
                    Notification.BigPictureStyle()
                        .bigPicture(
                            BitmapFactory.decodeResource(
                                context.resources,
                                R.drawable.permission_contents
                            )
                        )
                        .setBigContentTitle(title)
                        .setSummaryText(body)
                )
                .setContentText(body)
                .setSmallIcon(smallIcon)
                .setAutoCancel(true)
            getManager(context).notify(id, builder.build())
        } else {
            LogUtil.e("================= Test4")
        }
    }

    private val smallIcon: Int
        private get() = android.R.drawable.stat_notify_chat

    @Retention(RetentionPolicy.SOURCE)
    annotation class Channel {
        companion object {
            var MESSAGE = "message"
            var COMMENT = "comment"
            var NOTICE = "notice"
        }
    }
}