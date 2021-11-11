package com.mallup.dsmall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mallup.dsmall.common.CODE;
import com.mallup.dsmall.delegator.HNSharedPreference;

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

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                if (HNSharedPreference.getSharedPreference(SplashActivity.this, CODE.PREF_TUTORIAL) == "") {
                    intent =  new Intent(SplashActivity.this, TutorialActivity.class);
                } else {
                    intent.putExtra("pushUid", mPushUid);
                    intent.putExtra("url", mLandingUrl);
                }
                startActivity(intent);
                finish();
            }
        }, welcomeScreenDisplay);
    }
}