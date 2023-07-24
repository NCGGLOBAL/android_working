package com.creator.movingtown.croper

import android.util.Log

/**
 * @author GT
 */
object L {
    private const val TAG = "ImageZoomCrop"
    fun e(e: Throwable) {
        Log.e(TAG, e.message, e)
    }

    fun e(msg: String?) {
        Log.e(TAG, msg!!)
    }
}