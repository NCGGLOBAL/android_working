package com.wisefashion.trend;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by skcrackers on 5/27/16.
 */
public class SplashActivity extends Activity {
    private String mPushUid = "";
    private String mLandingUrl = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final int welcomeScreenDisplay = 1000;

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("pushUid") && intent.hasExtra("url")) {
                mPushUid = intent.getStringExtra("pushUid");
                mLandingUrl = intent.getStringExtra("url");
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
                    startActivity(intent);
                    finish();
                }
            }
        };
        welcomeThread.start();
    }
}