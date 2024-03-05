package com.creator.twobepet.common

import android.app.Activity
import android.widget.Toast
import com.creator.twobepet.R

/**
 * Created by skcrackers on 5/27/16.
 */
class BackPressCloseHandler(private val mActivity: Activity) {
    private var mBackKeyPressedTime: Long = 0
    private var mToast: Toast? = null
    fun onBackPressed() {
        if (System.currentTimeMillis() > mBackKeyPressedTime + 2000) {
            mBackKeyPressedTime = System.currentTimeMillis()
            mToast = Toast.makeText(
                mActivity,
                mActivity.resources.getString(R.string.exit),
                Toast.LENGTH_SHORT
            )
            mToast?.show()
            return
        }
        if (System.currentTimeMillis() <= mBackKeyPressedTime + 2000) {
            mActivity.finish()
            mToast!!.cancel()
        }
    }
}