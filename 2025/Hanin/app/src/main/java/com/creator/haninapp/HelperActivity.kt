package com.creator.haninapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.creator.haninapp.helpers.Constants
import com.google.android.material.snackbar.Snackbar

/**
 * Created by darshan on 26/9/16.
 */
open class HelperActivity : AppCompatActivity() {
    protected var view: View? = null
    private val maxLines = 4
    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    //    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA
    protected fun checkPermission() {
        // Android 13 (API 33) 이상에서는 Photo Picker 사용으로 권한 불필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionGranted()
        } else {
            // Android 12 이하에서만 권한 체크
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                permissionGranted()
            } else {
                ActivityCompat.requestPermissions(this, permissions, Constants.PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun requestPermission() {
        // Android 13 이상에서는 권한 요청 불필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        
        // Android 12 이하에서만 권한 요청
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showRequestPermissionRationale()
        } else {
            showAppPermissionSettings()
        }
    }

    private fun showRequestPermissionRationale() {
        if (view == null) return
        val snackbar = Snackbar.make(
            view!!,
            getString(R.string.permission_info),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(R.string.permission_ok)) {
                ActivityCompat.requestPermissions(
                    this@HelperActivity,
                    permissions,
                    Constants.PERMISSION_REQUEST_CODE
                )
            }

        /*((TextView) snackbar.getView()
                .findViewById(android.support.design.R.id.snackbar_text)).setMaxLines(maxLines);*/snackbar.show()
    }

    private fun showAppPermissionSettings() {
        if (view == null) return
        val snackbar = Snackbar.make(
            view!!,
            getString(R.string.permission_force),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(R.string.permission_settings)) {
                val uri = Uri.fromParts(
                    getString(R.string.permission_package),
                    this@HelperActivity.packageName,
                    null
                )
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                intent.data = uri
                startActivityForResult(intent, Constants.PERMISSION_REQUEST_CODE)
            }

        /*((TextView) snackbar.getView()
                .findViewById(android.support.design.R.id.snackbar_text)).setMaxLines(maxLines);*/snackbar.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != Constants.PERMISSION_REQUEST_CODE || grantResults.size == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            permissionDenied()
        } else {
            permissionGranted()
        }
    }

    protected open fun permissionGranted() {}
    private fun permissionDenied() {
        hideViews()
        requestPermission()
    }

    protected open fun hideViews() {}
}