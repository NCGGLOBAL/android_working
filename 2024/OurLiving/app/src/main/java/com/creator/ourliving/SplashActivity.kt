package com.creator.ourliving

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Created by skcrackers on 5/27/16.
 */
class SplashActivity : Activity() {
    private var mPushUid: String? = ""
    private var mLandingUrl: String? = ""
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val welcomeScreenDisplay = 1000
        val intent = intent
        if (intent != null) {
            if (intent.hasExtra("pushUid") && intent.hasExtra("url")) {
                mPushUid = intent.getStringExtra("pushUid")
                mLandingUrl = intent.getStringExtra("url")
            }
        }
        val welcomeThread: Thread = object : Thread() {
            var wait = 0
            override fun run() {
                try {
                    super.run()
                    /**
                     * use while to get the splash time. Use sleep() to increase
                     * the wait variable for every 100L.
                     */
                    while (wait < welcomeScreenDisplay) {
                        sleep(100)
                        wait += 100
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    /**
                     * Called after splash times up. Do some action after splash
                     * times up. Here we moved to another main activity class
                     */
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.putExtra("pushUid", mPushUid)
                    intent.putExtra("url", mLandingUrl)
                    startActivity(intent)
                    finish()
                }
            }
        }
        welcomeThread.start()
    }
}