package com.creator.unnitv.util

import android.app.Dialog
import android.content.Context
import android.view.Window
import com.creator.unnitv.R

/**
 * Created by skcrackers on 5/27/16.
 */
class ProgressUtil(context: Context?) : Dialog(context!!) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.view_progress_util)
    }
}