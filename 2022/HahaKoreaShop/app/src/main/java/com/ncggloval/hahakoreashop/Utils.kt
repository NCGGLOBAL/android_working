package com.ncggloval.hahakoreashop

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log

object Utils {
    const val TAG = "PushDemoActivity"
    var logStringCache = ""

    // 获取ApiKey
    fun getMetaValue(context: Context?, metaKey: String?): String? {
        var metaData: Bundle? = null
        var apiKey: String? = null
        if (context == null || metaKey == null) {
            return null
        }
        try {
            val ai = context.packageManager
                .getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
            if (null != ai) {
                metaData = ai.metaData
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "error " + e.message)
        }
        return apiKey
    }

    fun getTagsList(originalText: String): List<String>? {
        var originalText = originalText
        if (TextUtils.isEmpty(originalText)) {
            return null
        }
        val tags: MutableList<String> = ArrayList()
        var indexOfComma = originalText.indexOf(',')
        var tag: String
        while (indexOfComma != -1) {
            tag = originalText.substring(0, indexOfComma)
            tags.add(tag)
            originalText = originalText.substring(indexOfComma + 1)
            indexOfComma = originalText.indexOf(',')
        }
        tags.add(originalText)
        return tags
    }

    fun getLogText(context: Context?): String? {
        val sp = PreferenceManager
            .getDefaultSharedPreferences(context)
        return sp.getString("log_text", "")
    }

    fun setLogText(context: Context?, text: String?) {
        val sp = PreferenceManager
            .getDefaultSharedPreferences(context)
        val editor = sp.edit()
        editor.putString("log_text", text)
        editor.commit()
    }
}