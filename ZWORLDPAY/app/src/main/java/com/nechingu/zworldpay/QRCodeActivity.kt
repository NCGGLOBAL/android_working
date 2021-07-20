package com.nechingu.zworldpay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import java.lang.Exception

class QRCodeActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        IntentIntegrator(this).apply {
            setOrientationLocked(false)
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        try {
            if (result == null) {
                super.onActivityResult(requestCode, resultCode, data);
            } else {
                if (result.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                    finish()
                } else {
                    Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show();
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("webviewUrl", result.contents)
                    startActivity(intent)
                    finish()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}