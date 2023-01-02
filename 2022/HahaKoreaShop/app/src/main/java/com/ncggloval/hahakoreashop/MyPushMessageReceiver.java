package com.ncggloval.hahakoreashop;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.pushservice.PushMessageReceiver;
import com.ncggloval.hahakoreashop.MainActivity;
import com.ncggloval.hahakoreashop.R;
import com.ncggloval.hahakoreashop.common.HNApplication;
import com.ncggloval.hahakoreashop.delegator.HNCommTran;
import com.ncggloval.hahakoreashop.delegator.HNCommTranInterface;
import com.ncggloval.hahakoreashop.util.HNConfig;
import com.ncggloval.hahakoreashop.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

public class MyPushMessageReceiver extends PushMessageReceiver {
    /** TAG to Log */
    public static final String TAG = MyPushMessageReceiver.class.getSimpleName();
    private HNConfig mHNconfig;

    private HNCommTran mHNCommTran;

    /**
     * 调用PushManager.startWork后，sdk将对push
     * server发起绑定请求，这个过程是异步的。绑定请求的结果通过onBind返回。 如果您需要用单播推送，需要把这里获取的channel
     * id和user id上传到应用server中，再调用server接口用channel id和user id给单个手机或者用户推送。
     *
     * @param context
     *            BroadcastReceiver的执行Context
     * @param errorCode
     *            绑定接口返回值，0 - 成功
     * @param appid
     *            应用id。errorCode非0时为null
     * @param userId
     *            应用user id。errorCode非0时为null
     * @param channelId
     *            应用channel id。errorCode非0时为null
     * @param requestId
     *            向服务端发起的请求id。在追查问题时有用；
     * @return none
     */
    @Override
    public void onBind(Context context, int errorCode, String appid,
                       String userId, String channelId, String requestId) {
        String responseString = "onBind errorCode=" + errorCode + " appid="
                + appid + " userId=" + userId + " channelId=" + channelId
                + " requestId=" + requestId;
        Log.e(TAG, " (onBind) : 1");
        Log.e(TAG, " (onBind) : " + responseString);

        this.mHNconfig = new HNConfig(context);
        this.mHNconfig.setAppId(appid);
        this.mHNconfig.setUserId(userId);
        this.mHNconfig.setChannelId(channelId);
        this.mHNconfig.setRequestId(requestId);
        // 임시 조건삭제
//        if (errorCode == 0) {
        // 绑定成功
        LogUtil.d(TAG + " : " + "绑定成功");

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("os", "Android");
                    jsonObject.put("memberKey", MyPushMessageReceiver.this.mHNconfig.getMemberKey());
                    jsonObject.put("deviceId", MyPushMessageReceiver.this.mHNconfig.getDeviceId());
                    jsonObject.put("appId", MyPushMessageReceiver.this.mHNconfig.getAppId());
                    jsonObject.put("userId", MyPushMessageReceiver.this.mHNconfig.getUserId());
                    jsonObject.put("channelId", MyPushMessageReceiver.this.mHNconfig.getChannelId());
                    jsonObject.put("requestId", MyPushMessageReceiver.this.mHNconfig.getRequestId());

                    mHNCommTran = new HNCommTran(new HNCommTranInterface() {
                        @Override
                        public void recvMsg(String tranCode, String params) {
                            if (tranCode.equals(HNApplication.PUSH_URL)) {
                                Log.e(TAG, "recv pushRegister : " + params);
                            }
                        }
                    });
                    mHNCommTran.sendMsg(HNApplication.PUSH_URL, jsonObject);
                    return;
                }
                catch (JSONException localJSONException)
                {
                    localJSONException.printStackTrace();
                    return;
                }
                catch (Exception localException)
                {
                    localException.printStackTrace();
                }
            }
        }).start();
//        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString);
    }

    /**
     * 接收透传消息的函数。
     *
     * @param context
     *            上下文
     * @param message
     *            推送的消息
     * @param customContentString
     *            自定义内容,为空或者json字符串
     */
    @Override
    public void onMessage(Context context, String message, String customContentString) {
        try {
            String messageString = "透传消息 onMessage=\"" + message
                    + "\" customContentString=" + customContentString;

            LogUtil.d(TAG, " (onMessage) : 2");
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
            sendNotification(context, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收通知到达的函数。
     *
     * @param context
     *            上下文
     * @param title
     *            推送的通知的标题
     * @param description
     *            推送的通知的描述
     * @param customContentString
     *            自定义内容，为空或者json字符串
     */

    @Override
    public void onNotificationArrived(Context context, String title,
                                      String description, String customContentString) {

        String notifyString = "通知到达 onNotificationArrived  title=\"" + title
                + "\" description=\"" + description + "\" customContent="
                + customContentString;

        LogUtil.d(TAG, " (onNotificationArrived) : 3");
        LogUtil.e(TAG, " (onNotificationArrived) : " + notifyString);
        // 自定义内容获取方式，mykey和myvalue对应通知推送时自定义内容中设置的键和值
        if (!TextUtils.isEmpty(customContentString)) {
            JSONObject customJson = null;
            try {
                customJson = new JSONObject(customContentString);
                String myvalue = null;
                if (!customJson.isNull("mykey")) {
                    myvalue = customJson.getString("mykey");
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        // 你可以參考 onNotificationClicked中的提示从自定义内容获取具体值
        updateContent(context, notifyString);
    }

    /**
     * 接收通知点击的函数。
     *
     * @param context
     *            上下文
     * @param title
     *            推送的通知的标题
     * @param description
     *            推送的通知的描述
     * @param customContentString
     *            自定义内容，为空或者json字符串
     */
    @Override
    public void onNotificationClicked(Context context, String title,
                                      String description, String customContentString) {
        String notifyString = "通知点击 onNotificationClicked title=\"" + title + "\" description=\""
                + description + "\" customContent=" + customContentString;

        LogUtil.d(TAG, " (onNotificationClicked) : 4");
        LogUtil.e(TAG, " (onNotificationClicked) : " + notifyString);

        String landingUrl = "";
        // 自定义内容获取方式，mykey和myvalue对应通知推送时自定义内容中设置的键和值
        if (!TextUtils.isEmpty(customContentString)) {
            JSONObject customJson = null;
            try {
                customJson = new JSONObject(customContentString);
//                String myvalue = null;
                if (!customJson.isNull("url")) {
                    landingUrl = customJson.getString("url");
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
//        updateContent(context, notifyString);
//        customContentString = "https://www.google.com";
        Intent intent = new Intent();
        intent.setClass(context.getApplicationContext(), MainActivity.class);
        intent.putExtra("url", landingUrl);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(intent);
    }

    /**
     * setTags() 的回调函数。
     *
     * @param context
     *            上下文
     * @param errorCode
     *            错误码。0表示某些tag已经设置成功；非0表示所有tag的设置均失败。
     * @param successTags
     *            设置成功的tag
     * @param failTags
     *            设置失败的tag
     * @param requestId
     *            分配给对云推送的请求的id
     */
    @Override
    public void onSetTags(Context context, int errorCode,
                          List<String> successTags, List<String> failTags, String requestId) {
        String responseString = "onSetTags errorCode=" + errorCode
                + " sucessTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId;

        LogUtil.d(TAG, " (onSetTags) : 5");
        LogUtil.d(TAG, " (onSetTags) : " + responseString);

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString);
    }

    /**
     * delTags() 的回调函数。
     *
     * @param context
     *            上下文
     * @param errorCode
     *            错误码。0表示某些tag已经删除成功；非0表示所有tag均删除失败。
     * @param successTags
     *            成功删除的tag
     * @param failTags
     *            删除失败的tag
     * @param requestId
     *            分配给对云推送的请求的id
     */
    @Override
    public void onDelTags(Context context, int errorCode, List<String> successTags, List<String> failTags, String requestId) {
        String responseString = "onDelTags errorCode=" + errorCode
                + " sucessTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId;
        LogUtil.d(TAG, " (onDelTags) : " + responseString);

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString);
    }

    /**
     * listTags() 的回调函数。
     *
     * @param context
     *            上下文
     * @param errorCode
     *            错误码。0表示列举tag成功；非0表示失败。
     * @param tags
     *            当前应用设置的所有tag。
     * @param requestId
     *            分配给对云推送的请求的id
     */
    @Override
    public void onListTags(Context context, int errorCode, List<String> tags,
                           String requestId) {
        String responseString = "onListTags errorCode=" + errorCode + " tags="
                + tags;

        LogUtil.d(TAG, " (onListTags) : 6");
        LogUtil.d(TAG, " (onListTags) : " + responseString);

        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString);
    }

    /**
     * PushManager.stopWork() 的回调函数。
     *
     * @param context
     *            上下文
     * @param errorCode
     *            错误码。0表示从云推送解绑定成功；非0表示失败。
     * @param requestId
     *            分配给对云推送的请求的id
     */
    @Override
    public void onUnbind(Context context, int errorCode, String requestId) {
        String responseString = "onUnbind errorCode=" + errorCode
                + " requestId = " + requestId;

        LogUtil.d(TAG, " (onUnbind) : 7");
        LogUtil.d(TAG, " (onUnbind) : " + responseString);

        if (errorCode == 0) {
            // 解绑定成功
            Log.d(TAG, "解绑成功");
        }
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        updateContent(context, responseString);
    }

    private void updateContent(Context context, String content) {
        LogUtil.d(TAG, "updateContent content : " + content);
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

    private void sendNotification(Context context, String message)
    {
        try {
            JSONObject jsonMessage = new JSONObject(message);
            LogUtil.d(TAG, " (onMessage) : pushUid : " + jsonMessage.getString("pushUid"));
            LogUtil.d(TAG, " (onMessage) : title : " + jsonMessage.getString("title"));
            LogUtil.d(TAG, " (onMessage) : description : " + jsonMessage.getString("description"));
            LogUtil.d(TAG, " (onMessage) : url : " + jsonMessage.getString("url"));

            boolean foregroud = new ForegroundCheckTask().execute(context.getApplicationContext()).get();

            PendingIntent pendingIntent = null;
//            if(foregroud)
            pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
//            else
//                pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, SplashActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(context);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setTicker(jsonMessage.getString("title"));
            builder.setWhen(System.currentTimeMillis());
            builder.setContentTitle(jsonMessage.getString("title"));
            builder.setContentText(jsonMessage.getString("description"));
            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
            builder.setContentIntent(pendingIntent);
            builder.setAutoCancel(true);
            builder.setPriority(Notification.PRIORITY_MAX);

            // 고유ID로 알림을 생성.
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Integer.parseInt(jsonMessage.getString("pushUid")), builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Context... params) {
            final Context context = params[0].getApplicationContext();
            return isAppOnForeground(context);
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }
}