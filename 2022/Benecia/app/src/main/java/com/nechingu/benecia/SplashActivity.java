package com.nechingu.benecia;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

/**
 * Created by skcrackers on 5/27/16.
 */
public class SplashActivity extends Activity {
    private String mPushUid = "";
    private String mLandingUrl = "";
    private String mYoutubeLinkUrl = "";
    private String mSchemeUrl = "";

    private final int UPDATE_REQUEST_CODE = 1004;
    private AppUpdateManager appUpdateManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        checkUpdate();

        final int welcomeScreenDisplay = 1000;

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("pushUid") && intent.hasExtra("url")) {
                mPushUid = intent.getStringExtra("pushUid");
                mLandingUrl = intent.getStringExtra("url");
            } else if(intent.getExtras() != null) {
                Bundle extras = intent.getExtras();
                CharSequence receiveStrTxt = extras.getCharSequence(intent.EXTRA_TEXT);
                if(receiveStrTxt != null) {
                    mYoutubeLinkUrl = receiveStrTxt.toString();
                }
            }

            // scheme param
            Uri uri = getIntent().getData();
            if(uri != null)
            {
                mSchemeUrl = uri.getQueryParameter("url");
                Log.d("SeongKwon","mSchemeUrl1 : " + mSchemeUrl);
            }
        }

        Thread welcomeThread = new Thread() {
            int wait = 0;

            @Override
            public void run() {
                try {
                    super.run();
                    /**
                     * use while to get the splash time. Use sleep() to increase
                     * the wait variable for every 100L.
                     */
                    while (wait < welcomeScreenDisplay) {
                        sleep(100);
                        wait += 100;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    /**
                     * Called after splash times up. Do some action after splash
                     * times up. Here we moved to another main activity class
                     */
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.putExtra("pushUid", mPushUid);
                    intent.putExtra("url", mLandingUrl);
                    intent.putExtra("youtubeLinkUrl", mYoutubeLinkUrl);
                    intent.putExtra("schemeUrl", mSchemeUrl);
                    startActivity(intent);
                    finish();
                }
            }
        };
        welcomeThread.start();
    }

    // Checks that the update is not stalled during 'onResume()'.
    // However, you should execute this check at all entry points into the app.
    @Override
    protected void onResume() {
        super.onResume();

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                            appUpdateInfo,
                                            AppUpdateType.IMMEDIATE,
                                            this,
                                            UPDATE_REQUEST_CODE);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
    }

    private void checkUpdate() {
        // Creates instance of the manager.
        appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // For a flexible update, use AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.IMMEDIATE,
                            // The current activity making the update request.
                            this,
                            // Include a request code to later monitor this update request.
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}