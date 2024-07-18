package com.creator.starrymall.util

import android.app.Activity
import android.app.AlertDialog
import com.creator.starrymall.R

/**
 * Created by skcrackers on 5/27/16.
 */
object AlertUtil {
    fun showConfirmDialog(activity: Activity, title: String?, message: String?) {
        val alertbox = AlertDialog.Builder(activity)
        alertbox.setTitle(title)
        alertbox.setCancelable(false)
        alertbox.setMessage(message)
        alertbox.setNeutralButton(activity.resources.getString(R.string.confirm)) { dialog, which -> activity.finish() }
        alertbox.show()
    }
}