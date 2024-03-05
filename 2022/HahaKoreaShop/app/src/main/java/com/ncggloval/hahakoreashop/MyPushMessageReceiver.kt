package com.ncggloval.hahakoreashop

import android.app.*
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.*
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import com.baidu.android.pushservice.PushMessageReceiver
import com.ncggloval.hahakoreashop.MainActivity
import com.ncggloval.hahakoreashop.MyPushMessageReceiver
import com.ncggloval.hahakoreashop.common.HNApplication
import com.ncggloval.hahakoreashop.delegator.HNCommTran
import com.ncggloval.hahakoreashop.delegator.HNCommTranInterface
import com.ncggloval.hahakoreashop.util.HNConfig
import com.ncggloval.hahakoreashop.util.LogUtil
import org.json.JSONException
import org.json.JSONObject

/*
 * Push消息处理receiver。请编写您需要的回调函数， 一般来说： onBind是必须的，用来处理startWork返回值；
 *onMessage用来接收透传消息； onSetTags、onDelTags、onListTags是tag相关操作的回调；
 *onNotificationClicked在通知被点击时回调； onUnbind是stopWork接口的返回值回调
 * 返回值中的errorCode，解释如下：
 *0 - Success
 *10001 - Network Problem
 *10101  Integrate Check Error
 *30600 - Internal Server Error
 *30601 - Method Not Allowed
 *30602 - Request Params Not Valid
 *30603 - Authentication Failed
 *30604 - Quota Use Up Payment Required
 *30605 - Data Required Not Found
 *30606 - Request Time Expires Timeout
 *30607 - Channel Token Timeout
 *30608 - Bind Relation Not Found
 *30609 - Bind Number Too Many
 * 当您遇到以上返回错误时，如果解释不了您的问题，请用同一请求的返回值requestId和errorCode联系我们追查问题。
 *
 */
class MyPushMessageReceiver : PushMessageReceiver() {
    private var mHNconfig: HNConfig? = null
    private var mHNCommTran: HNCommTran? = null

    /**
     * 调用PushManager.startWork后，sdk将对push
     * server发起绑定请求，这个过程是异步的。绑定请求的结果通过onBind返回。 如果您需要用单播推送，需要把这里获取的channel
     * id和user id上传到应用server中，再调用server接口用channel id和user id给单个手机或者用户推送。
     *
     * @param context
     * BroadcastReceiver的执行Context
     * @param errorCode
     * 绑定接口返回值，0 - 成功
     * @param appid
     * 应用id。errorCode非0时为null
     * @param userId
     * 应用user id。errorCode非0时为null
     * @param channelId
     * 应用channel id。errorCode非0时为null
     * @param requestId
     * 向服务端发起的请求id。在追查问题时有用；
     * @return none
     */
    override fun onBind(
        context: Context, errorCode: Int, appid: String,
        userId: String, channelId: String, requestId: String
    ) {
        val responseString = ("onBind errorCode=" + errorCode + " appid="
                + appid + " userId=" + userId + " channelId=" + channelId
                + " requestId=" + requestId)
        Log.e(TAG, " (onBind) : 1")
        Log.e(TAG, " (onBind) : $responseString")
        mHNconfig = HNConfig(context)
        mHNconfig?.appId = appid
        mHNconfig?.userId = userId
        mHNconfig?.channelId = channelId
        mHNconfig?.requestId = requestId
        // 임시 조건삭제
//        if (errorCode == 0) {
        // 绑定成功
        LogUtil.d(TAG + " : " + "绑定成功")
        Thread(Runnable {
            try {
                val jsonObject = JSONObject()
                jsonObject.put("os", "Android")
                jsonObject.put("memberKey", mHNconfig?.memberKey)
                jsonObject.put("deviceId", mHNconfig?.deviceId)
                jsonObject.put("appId", mHNconfig?.appId)
                jsonObject.put("userId", mHNconfig?.userId)
                jsonObject.put("channelId", mHNconfig?.channelId)
                jsonObject.put("requestId", mHNconfig?.requestId)
                mHNCommTran = HNCommTran(object : HNCommTranInterface {
                    override fun recvMsg(tranCode: String?, params: String) {
                        if (tranCode.equals(HNApplication.PUSH_URL)) {
                            LogUtil.e("recv pushRegister : $tranCode : $params");
                        }
                    }
                })
                mHNCommTran!!.sendMsg(HNApplication.PUSH_URL, jsonObject)
                return@Runnable
            } catch (localJSONException: JSONException) {
                localJSONException.printStackTrace()
                return@Runnable
            } catch (localException: Exception) {
                localException.printStackTrace()
            }
        }).start()
        //        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString)
    }

    /**
     * 接收透传消息的函数。
     *
     * @param context
     * 上下文
     * @param message
     * 推送的消息
     * @param customContentString
     * 自定义内容,为空或者json字符串
     */
    override fun onMessage(context: Context, message: String, customContentString: String) {
        try {
            val messageString = ("透传消息 onMessage=\"" + message
                    + "\" customContentString=" + customContentString)
            LogUtil.d(TAG, " (onMessage) : 2")
            //            LogUtil.e(TAG, " (onMessage) : " + messageString);
            // 自定义内容获取方式，mykey和myvalue对应透传消息推送时自定义内容中设置的键和值

//            if (!TextUtils.isEmpty(customContentString)) {
//                JSONObject customJson = null;
//                try {
//                    customJson = new JSONObject(customContentString);
//                    String myvalue = null;
//                    if (!customJson.isNull("mykey")) {
//                        myvalue = customJson.getString("mykey");
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }

            // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
//            updateContent(context, messageString);
            sendNotification(context, message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 接收通知到达的函数。
     *
     * @param context
     * 上下文
     * @param title
     * 推送的通知的标题
     * @param description
     * 推送的通知的描述
     * @param customContentString
     * 自定义内容，为空或者json字符串
     */
    override fun onNotificationArrived(
        context: Context, title: String,
        description: String, customContentString: String
    ) {
        val notifyString = ("通知到达 onNotificationArrived  title=\"" + title
                + "\" description=\"" + description + "\" customContent="
                + customContentString)
        LogUtil.d(TAG, " (onNotificationArrived) : 3")
        LogUtil.e(TAG, " (onNotificationArrived) : $notifyString")
        // 自定义内容获取方式，mykey和myvalue对应通知推送时自定义内容中设置的键和值
        if (!TextUtils.isEmpty(customContentString)) {
            var customJson: JSONObject? = null
            try {
                customJson = JSONObject(customContentString)
                var myvalue: String? = null
                if (!customJson.isNull("mykey")) {
                    myvalue = customJson.getString("mykey")
                }
            } catch (e: JSONException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        // 你可以參考 onNotificationClicked中的提示从自定义内容获取具体值
        updateContent(context, notifyString)
    }

    /**
     * 接收通知点击的函数。
     *
     * @param context
     * 上下文
     * @param title
     * 推送的通知的标题
     * @param description
     * 推送的通知的描述
     * @param customContentString
     * 自定义内容，为空或者json字符串
     */
    override fun onNotificationClicked(
        context: Context, title: String,
        description: String, customContentString: String
    ) {
        val notifyString = ("通知点击 onNotificationClicked title=\"" + title + "\" description=\""
                + description + "\" customContent=" + customContentString)
        LogUtil.d(TAG, " (onNotificationClicked) : 4")
        LogUtil.e(TAG, " (onNotificationClicked) : $notifyString")
        var landingUrl = ""
        // 自定义内容获取方式，mykey和myvalue对应通知推送时自定义内容中设置的键和值
        if (!TextUtils.isEmpty(customContentString)) {
            var customJson: JSONObject? = null
            try {
                customJson = JSONObject(customContentString)
                //                String myvalue = null;
                if (!customJson.isNull("url")) {
                    landingUrl = customJson.getString("url")
                }
            } catch (e: JSONException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
//        updateContent(context, notifyString);
//        customContentString = "https://www.google.com";
        val intent = Intent()
        intent.setClass(context.applicationContext, MainActivity::class.java)
        intent.putExtra("url", landingUrl)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.applicationContext.startActivity(intent)
    }

    /**
     * setTags() 的回调函数。
     *
     * @param context
     * 上下文
     * @param errorCode
     * 错误码。0表示某些tag已经设置成功；非0表示所有tag的设置均失败。
     * @param successTags
     * 设置成功的tag
     * @param failTags
     * 设置失败的tag
     * @param requestId
     * 分配给对云推送的请求的id
     */
    override fun onSetTags(
        context: Context, errorCode: Int,
        successTags: List<String>, failTags: List<String>, requestId: String
    ) {
        val responseString = ("onSetTags errorCode=" + errorCode
                + " sucessTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId)
        LogUtil.d(TAG, " (onSetTags) : 5")
        LogUtil.d(TAG, " (onSetTags) : $responseString")

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString)
    }

    /**
     * delTags() 的回调函数。
     *
     * @param context
     * 上下文
     * @param errorCode
     * 错误码。0表示某些tag已经删除成功；非0表示所有tag均删除失败。
     * @param successTags
     * 成功删除的tag
     * @param failTags
     * 删除失败的tag
     * @param requestId
     * 分配给对云推送的请求的id
     */
    override fun onDelTags(
        context: Context,
        errorCode: Int,
        successTags: List<String>,
        failTags: List<String>,
        requestId: String
    ) {
        val responseString = ("onDelTags errorCode=" + errorCode
                + " sucessTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId)
        LogUtil.d(TAG, " (onDelTags) : $responseString")

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString)
    }

    /**
     * listTags() 的回调函数。
     *
     * @param context
     * 上下文
     * @param errorCode
     * 错误码。0表示列举tag成功；非0表示失败。
     * @param tags
     * 当前应用设置的所有tag。
     * @param requestId
     * 分配给对云推送的请求的id
     */
    override fun onListTags(
        context: Context, errorCode: Int, tags: List<String>,
        requestId: String
    ) {
        val responseString = ("onListTags errorCode=" + errorCode + " tags="
                + tags)
        LogUtil.d(TAG, " (onListTags) : 6")
        LogUtil.d(TAG, " (onListTags) : $responseString")

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString)
    }

    /**
     * PushManager.stopWork() 的回调函数。
     *
     * @param context
     * 上下文
     * @param errorCode
     * 错误码。0表示从云推送解绑定成功；非0表示失败。
     * @param requestId
     * 分配给对云推送的请求的id
     */
    override fun onUnbind(context: Context, errorCode: Int, requestId: String) {
        val responseString = ("onUnbind errorCode=" + errorCode
                + " requestId = " + requestId)
        LogUtil.d(TAG, " (onUnbind) : 7")
        LogUtil.d(TAG, " (onUnbind) : $responseString")
        if (errorCode == 0) {
            // 解绑定成功
            Log.d(TAG, "解绑成功")
        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString)
    }

    private fun updateContent(context: Context, content: String) {
        LogUtil.d(TAG, "updateContent content : $content")
        //        String logText = "" + Utils.logStringCache;
//
//        if (!logText.equals("")) {
//            logText += "\n";
//        }
//
//        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH-mm-ss");
//        logText += sDateFormat.format(new Date()) + ": ";
//        logText += content;
//
//        Utils.logStringCache = logText;
//
//        LogUtil.d(TAG, " (updateContent) : 8");
//        LogUtil.e(TAG, " (updateContent) : " + logText);
//        Intent intent = new Intent();
//        intent.setClass(context.getApplicationContext(), MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.getApplicationContext().startActivity(intent);
    }

    private fun sendNotification(context: Context, message: String) {
        try {
            val jsonMessage = JSONObject(message)
            LogUtil.d(TAG, " (onMessage) : pushUid : " + jsonMessage.getString("pushUid"))
            LogUtil.d(TAG, " (onMessage) : title : " + jsonMessage.getString("title"))
            LogUtil.d(TAG, " (onMessage) : description : " + jsonMessage.getString("description"))
            LogUtil.d(TAG, " (onMessage) : url : " + jsonMessage.getString("url"))
            val foregroud = ForegroundCheckTask().execute(context.applicationContext).get()
            var pendingIntent: PendingIntent? = null
            //            if(foregroud)
            pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            //            else
//                pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, SplashActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            val builder = Notification.Builder(context)
            builder.setSmallIcon(R.mipmap.ic_launcher)
            builder.setTicker(jsonMessage.getString("title"))
            builder.setWhen(System.currentTimeMillis())
            builder.setContentTitle(jsonMessage.getString("title"))
            builder.setContentText(jsonMessage.getString("description"))
            builder.setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
            builder.setContentIntent(pendingIntent)
            builder.setAutoCancel(true)
            builder.setPriority(Notification.PRIORITY_MAX)

            // 고유ID로 알림을 생성.
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(jsonMessage.getString("pushUid").toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal inner class ForegroundCheckTask : AsyncTask<Context?, Void?, Boolean>() {
        private fun isAppOnForeground(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            val packageName = context.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                    return true
                }
            }
            return false
        }

        override fun doInBackground(vararg params: Context?): Boolean {
            val context = params[0]?.applicationContext
            return isAppOnForeground(context!!)
        }
    }

    companion object {
        /** TAG to Log  */
        val TAG = MyPushMessageReceiver::class.java.simpleName
    }
}