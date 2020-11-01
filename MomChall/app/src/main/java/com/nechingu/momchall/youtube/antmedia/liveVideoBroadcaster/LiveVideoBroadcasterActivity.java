package com.nechingu.momchall.youtube.antmedia.liveVideoBroadcaster;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequest;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import com.nechingu.momchall.MainActivity;
import com.nechingu.momchall.R;
import com.nechingu.momchall.common.HNApplication;
import com.nechingu.momchall.youtube.YoutubeActivity;
import com.nechingu.momchall.youtube.util.YouTubeApi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import at.grabner.circleprogress.AnimationState;
import at.grabner.circleprogress.AnimationStateChangedListener;
import at.grabner.circleprogress.CircleProgressView;
import io.antmedia.android.broadcaster.ILiveVideoBroadcaster;
import io.antmedia.android.broadcaster.LiveVideoBroadcaster;
import io.antmedia.android.broadcaster.utils.Resolution;

import static com.nechingu.momchall.youtube.YoutubeActivity.RTMP_BASE_URL;

public class LiveVideoBroadcasterActivity extends AppCompatActivity {
    private static final int CHECK_EVENT_TASK = 0;
    private static final int SEND_KAKAO_MESSAGE = 1;

    private ViewGroup mRootView;
    boolean mIsRecording = false;
    private EditText mStreamNameEditText;
    private Timer mTimer;
    private long mElapsedTime;
    public TimerHandler mTimerHandler;
    private ImageButton mSettingsButton;
    private CameraResolutionsFragment mCameraResolutionsDialog;
    private Intent mLiveVideoBroadcasterServiceIntent;
    private TextView mStreamLiveStatus;
    private GLSurfaceView mGLView;
    private ILiveVideoBroadcaster mLiveVideoBroadcaster;
    private Button mBroadcastControlButton;
    private CircleProgressView mCircleProgressView;

    private String mBroadcastId;
    private String mWatchUri;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();

    private WindowManager.LayoutParams params;
    private float brightness; // 밝기값은 float형으로 저장되어 있습니다.
    private boolean mIsLive = false;
    private int systemBrightness = 255;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LiveVideoBroadcaster.LocalBinder binder = (LiveVideoBroadcaster.LocalBinder) service;
            if (mLiveVideoBroadcaster == null) {
                mLiveVideoBroadcaster = binder.getService();
                mLiveVideoBroadcaster.init(LiveVideoBroadcasterActivity.this, mGLView);
                mLiveVideoBroadcaster.setAdaptiveStreaming(true);
            }
            mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);

        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLiveVideoBroadcaster = null;
        }
    };

    private TimerTask mTask;
    private TimerTask mTaskThumbnails;
    private Timer mTimerCheckThumbnails;
    private Timer mTimerCheckStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_live_video_broadcaster);

            // Hide title
            //requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            mBroadcastId = getIntent().getStringExtra(YouTubeApi.BROADCAST_ID_KEY);
            mWatchUri = getIntent().getStringExtra("watchUri");

            //binding on resume not to having leaked service connection
            mLiveVideoBroadcasterServiceIntent = new Intent(this, LiveVideoBroadcaster.class);
            //this makes service do its job until done
            startService(mLiveVideoBroadcasterServiceIntent);

            mTimerHandler = new TimerHandler();
            mStreamNameEditText = (EditText) findViewById(R.id.stream_name_edit_text);

            mRootView = (ViewGroup) findViewById(R.id.root_layout);
            mSettingsButton = (ImageButton) findViewById(R.id.settings_button);
            mStreamLiveStatus = (TextView) findViewById(R.id.stream_live_status);

            mBroadcastControlButton = (Button) findViewById(R.id.toggle_broadcasting);
            mCircleProgressView = findViewById(R.id.pb_circle);

            // Configure the GLSurfaceView.  This will start the Renderer thread, with an
            // appropriate EGL activity.
            mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
            if (mGLView != null) {
                mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
            }

            // 투명 웹뷰
            WebView wv_chat = findViewById(R.id.wv_chat);
            wv_chat.setWebViewClient(new WebViewClient());
            wv_chat.setWebChromeClient(new WebChromeClient());
            wv_chat.getSettings().setUserAgentString(wv_chat.getSettings().getUserAgentString() + ";device=app;" + " NINTH");

            // setSavePassword - default false in android 4.4 and above
            // setPluginState - use plugin deprecate
            // setAppCacheMaxSize - deprecate
            wv_chat.getSettings().setJavaScriptEnabled(true);
            wv_chat.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            wv_chat.getSettings().setAllowFileAccess(true);
            wv_chat.getSettings().setAllowContentAccess(true);
            wv_chat.getSettings().setLoadsImagesAutomatically(true);
            wv_chat.getSettings().setLoadWithOverviewMode(true);
            wv_chat.getSettings().setSupportMultipleWindows(false);
            wv_chat.getSettings().setUseWideViewPort(true);
            wv_chat.getSettings().setDatabaseEnabled(true);
            wv_chat.getSettings().setDomStorageEnabled(true);
            wv_chat.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            wv_chat.getSettings().setSupportMultipleWindows(true);
            wv_chat.getSettings().setAppCacheEnabled(true);
            wv_chat.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            wv_chat.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
            wv_chat.setBackgroundColor(0);
            wv_chat.loadUrl(HNApplication.URL + "/addon/live/live_unni.asp?vCode=" + mBroadcastId);

            final TextView tv_status = (TextView)findViewById(R.id.tv_status);
            mTask = new TimerTask() {
                int cnt = 0;

                @Override
                public void run() {
                    if(cnt % 3 == 0) {
//                        new CheckEventTask().execute(mBroadcastId);
                    } else if(cnt % 3 == 1){
                        LiveVideoBroadcasterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mIsRecording && !tv_status.getText().toString().equals("live"))
                                    tv_status.setText("testStarting");
                            }
                        });
//                        new StartEventTask().execute(mBroadcastId, "live");
                    } else {
                        LiveVideoBroadcasterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mIsRecording && tv_status.getText().toString().equals("testStarting")) {
                                    tv_status.setText("live");
                                    mIsLive = true;

                                    Intent BrIntent = new Intent(MainActivity.BROADCAST_MESSAGE);
                                    BrIntent.putExtra("liveUrl", mWatchUri);
                                    BrIntent.putExtra("liveStatus", "1");   // 방송중(시작)
                                    sendBroadcast(BrIntent);
                                }
                            }
                        });
//                        new StartEventTask().execute(mBroadcastId, "testing");
                    }
                    cnt++;
                }
            };
            mTimerCheckStatus = new Timer();
            mTimerCheckStatus.schedule(mTask, 1000, 10000);

            // 화면 정보 불러오기
            params = getWindow().getAttributes();

            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAutoResolution();
                }
            }, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 기존 밝기 저장
        brightness = params.screenBrightness;
        // 최대 밝기로 설정
        params.screenBrightness = 1f;
        // 밝기 설정 적용
        getWindow().setAttributes(params);
    }

    public void changeCamera(View v) {
        if (mLiveVideoBroadcaster != null) {
            mLiveVideoBroadcaster.changeCamera();
        }

        // 최대 밝기로 설정
        params.screenBrightness = 1f;
        // 밝기 설정 적용
        getWindow().setAttributes(params);

        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setAutoResolution();
            }
        }, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //this lets activity bind
        bindService(mLiveVideoBroadcasterServiceIntent, mConnection, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LiveVideoBroadcaster.PERMISSIONS_REQUEST: {
                if (mLiveVideoBroadcaster.isPermissionGranted()) {
                    mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.RECORD_AUDIO) ) {
                        mLiveVideoBroadcaster.requestPermission();
                    }
                    else {
                        new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                                .setTitle(R.string.permission)
                                .setMessage(getString(R.string.app_doesnot_work_without_permissions))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        try {
                                            //Open the specific App Info page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                            startActivity(intent);

                                        } catch ( ActivityNotFoundException e ) {
                                            //e.printStackTrace();

                                            //Open the generic Apps page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                            startActivity(intent);

                                        }
                                    }
                                })
                                .show();
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //hide dialog if visible not to create leaked window exception
        if (mCameraResolutionsDialog != null && mCameraResolutionsDialog.isVisible()) {
            mCameraResolutionsDialog.dismiss();
        }
        mLiveVideoBroadcaster.pause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);

        // 기존 밝기로 변경
        params.screenBrightness = brightness;
        getWindow().setAttributes(params);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLiveVideoBroadcaster.setDisplayOrientation();
        }

    }

    public void showSetResolutionDialog(View v) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragmentDialog = getSupportFragmentManager().findFragmentByTag("dialog");
        if (fragmentDialog != null) {

            ft.remove(fragmentDialog);
        }

        ArrayList<Resolution> sizeList = mLiveVideoBroadcaster.getPreviewSizeList();


        if (sizeList != null && sizeList.size() > 0) {
            mCameraResolutionsDialog = new CameraResolutionsFragment();

            mCameraResolutionsDialog.setCameraResolutions(sizeList, mLiveVideoBroadcaster.getPreviewSize());
            mCameraResolutionsDialog.show(ft, "resolutiton_dialog");
        }
        else {
            Snackbar.make(mRootView, "No resolution available",Snackbar.LENGTH_LONG).show();
        }
    }

    public void toggleBroadcasting(final View v) {
        if (!mIsRecording)
        {
            if (mLiveVideoBroadcaster != null) {
                if (!mLiveVideoBroadcaster.isConnected()) {
                    String streamName = mStreamNameEditText.getText().toString();

                    // start
                    new startBroadcastAsyncTask(v).execute(RTMP_BASE_URL + streamName);
                }
                else {
                    Snackbar.make(mRootView, R.string.streaming_not_finished, Snackbar.LENGTH_LONG).show();
                }
            }
            else {
                Snackbar.make(mRootView, R.string.oopps_shouldnt_happen, Snackbar.LENGTH_LONG).show();
            }
        }
        else
        {
            triggerStopRecording();
        }
    }


    public void triggerStopRecording() {
        if (mIsRecording) {
//            mBroadcastControlButton.setText(R.string.start_broadcasting);
            mBroadcastControlButton.setBackgroundResource(R.drawable.ic_stop_red_60dp);

            mStreamLiveStatus.setVisibility(View.GONE);
            mStreamLiveStatus.setText(R.string.live_indicator);
            mSettingsButton.setVisibility(View.VISIBLE);

            stopTimer();
            mLiveVideoBroadcaster.stopBroadcasting();
        }

        mIsRecording = false;
    }

    //This method starts a mTimer and updates the textview to show elapsed time for recording
    public void startTimer() {

        if(mTimer == null) {
            mTimer = new Timer();
        }

        mElapsedTime = 0;
        mTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                mElapsedTime += 10; //increase every sec
                mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();

                if (mLiveVideoBroadcaster == null || !mLiveVideoBroadcaster.isConnected()) {
                    mTimerHandler.obtainMessage(TimerHandler.CONNECTION_LOST).sendToTarget();
                }
            }
        }, 0, 1000);
    }


    public void stopTimer()
    {
        if (mTimer != null) {
            this.mTimer.cancel();
        }
        this.mTimer = null;
        this.mElapsedTime = 0;

        endEvent();
    }

    public void setResolution(Resolution size) {
        mLiveVideoBroadcaster.setResolution(size);
    }

    public void setAutoResolution() {
        ArrayList<Resolution> sizeList = mLiveVideoBroadcaster.getPreviewSizeList();

        if (sizeList != null && sizeList.size() > 0) {
            for(int idx = 0; idx < sizeList.size(); idx++) {
                Log.e("SeongKwon", sizeList.get(idx).height + "//" + sizeList.get(idx).width);
                if(sizeList.get(idx).height == 720 && sizeList.get(idx).width == 1280) {
                    setResolution(sizeList.get(idx));
                }
            }
        } else {
            Snackbar.make(mRootView, "No resolution available",Snackbar.LENGTH_LONG).show();
        }
    }

    private class TimerHandler extends Handler {
        static final int CONNECTION_LOST = 2;
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    mStreamLiveStatus.setText(getString(R.string.live_indicator) + " - " + getDurationString((int) mElapsedTime));
                    break;
                case CONNECTION_LOST:
                    triggerStopRecording();
                    new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                            .setMessage(R.string.broadcast_connection_lost)
                            .setPositiveButton(android.R.string.yes, null)
                            .show();

                    break;
            }
        }
    }

    public static String getDurationString(int seconds) {

        if(seconds < 0 || seconds > 2000000)//there is an codec problem and duration is not set correctly,so display meaningfull string
            seconds = 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if(hours == 0)
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        else
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    public static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    public void endEvent() {
        if (mTimerCheckStatus != null) {
            this.mTimerCheckStatus.cancel();
        }

        Intent data = new Intent();
        data.putExtra(YouTubeApi.BROADCAST_ID_KEY, mBroadcastId);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, data);
        } else {
            getParent().setResult(Activity.RESULT_OK, data);
        }
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            endEvent();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


//    private class StartEventTask extends AsyncTask<String, Void, String> {
//        private String broadcastId = "";
//        private YouTube youtube;
//        @Override
//        protected void onPreExecute() {}
//
//        @Override
//        protected String doInBackground(String... params) {
//            youtube = new YouTube.Builder(transport, jsonFactory, YoutubeActivity.credential).setApplicationName(YoutubeActivity.APP_NAME).build();
//            try {
//                broadcastId = params[0];
//                String type = params[1];
//                YouTubeApi.startEvent(youtube, broadcastId, type);
//            } catch (UserRecoverableAuthIOException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                return e.getMessage();
//            }
//
//            return "";
//        }
//
//        @Override
//        protected void onPostExecute(String param) {
//            mTaskThumbnails = new TimerTask() {
//                int cnt = 0;
//                @Override
//                public void run() {
//                    try {
//                        File thumbnail = new File(LiveVideoBroadcasterActivity.this.getFilesDir(), "Image_youtube.jpg");
//                        if (thumbnail != null) {
//                            try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(thumbnail))) {
//                                if(cnt < 10) {
//                                    com.google.api.services.youtube.model.ThumbnailSetResponse thumbnailresponse =  youtube.thumbnails().set(mBroadcastId, new InputStreamContent("application/octet-stream", bufferedInputStream)).execute();
//                                    Log.e("SeongKwon", cnt + "//" + "thumbnailresponse = " + thumbnailresponse.getItems().toString());
//                                    cnt++;
//                                } else {
//                                    mTimerCheckThumbnails.cancel();
//                                    mTimerCheckThumbnails.purge();
//                                }
//                            } catch (final IOException e) {
////                                throw new ThumbnailIOException(e);
//                                // TODO 처리
//                                e.printStackTrace();
//                            }
//                        } else {
//                            URL url = new URL("https://img.youtube.com/vi/" + mBroadcastId + "/hqdefault.jpg");
//                            InputStream input = url.openStream();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    cnt++;
//                }
//            };
//            mTimerCheckThumbnails = new Timer();
//            mTimerCheckThumbnails.schedule(mTaskThumbnails, 5000, 10000);
//        }
//    }

//    private class CheckEventTask extends AsyncTask<String, Void, String> {
//        private String broadcastId = "";
//        @Override
//        protected void onPreExecute() {}
//
//        @Override
//        protected String doInBackground(String... params) {
//            YouTube youtube = new YouTube.Builder(transport, jsonFactory, YoutubeActivity.credential).setApplicationName(YoutubeActivity.APP_NAME).build();
//            try {
//                broadcastId = params[0];
//                YouTubeApi.getLiveEvents(youtube, broadcastId);
//            } catch (UserRecoverableAuthIOException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                return e.getMessage();
//            }
//            return "";
//        }
//
//        @Override
//        protected void onPostExecute(String param) {}
//    }

    private class startBroadcastAsyncTask extends AsyncTask<String, String, Boolean> {
        ContentLoadingProgressBar progressBar;
        View view = null;

        startBroadcastAsyncTask(View v) {
            // list all the parameters like in normal class define
            view = v;
        }

        @Override
        protected void onPreExecute() {
            view.setVisibility(View.GONE);
            mCircleProgressView.setValue(0.0f);
            mCircleProgressView.setUnitVisible(true);
            mCircleProgressView.setOnAnimationStateChangedListener(null);

            progressBar = new ContentLoadingProgressBar(LiveVideoBroadcasterActivity.this);
            progressBar.show();
        }

        @Override
        protected Boolean doInBackground(String... url) {
            return mLiveVideoBroadcaster.startBroadcasting(url[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressBar.hide();
            mIsRecording = result;
            if (result) {
                mStreamLiveStatus.setVisibility(View.GONE);
                mSettingsButton.setVisibility(View.GONE);
                startTimer();//start the recording duration

                // 변경
                mCircleProgressView.setOnAnimationStateChangedListener(new AnimationStateChangedListener() {
                    @Override
                    public void onAnimationStateChanged(AnimationState _animationState) {
                        if(_animationState == AnimationState.IDLE) {
                            mCircleProgressView.setOnAnimationStateChangedListener(null);
                            view.setBackgroundResource(R.drawable.ic_stop_red_60dp);
                            view.setVisibility(View.VISIBLE);

                            mCircleProgressView.setValue(0.0f);
                            mCircleProgressView.setUnitVisible(false);
                        }
                    }
                });
                view.setVisibility(View.GONE);
                mCircleProgressView.setMaxValue(100);
                mCircleProgressView.setValueAnimated(0, 100, 40000);
            }
            else {
                mCircleProgressView.setOnAnimationStateChangedListener(null);
                view.setBackgroundResource(R.drawable.ic_play_arrow_red_60dp);
                view.setVisibility(View.VISIBLE);

                mCircleProgressView.setValue(0.0f);
                mCircleProgressView.setUnitVisible(false);

                Snackbar.make(mRootView, R.string.stream_not_started, Snackbar.LENGTH_LONG).show();

                triggerStopRecording();
            }
        }
    }
}