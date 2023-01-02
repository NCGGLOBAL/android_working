package com.mallup.mungemarket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

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

        final int welcomeScreenDisplay = 3000;

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("pushUid") && intent.hasExtra("url")) {
                mPushUid = intent.getStringExtra("pushUid");
                mLandingUrl = intent.getStringExtra("url");
            }
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
        }, welcomeScreenDisplay);
    }
}