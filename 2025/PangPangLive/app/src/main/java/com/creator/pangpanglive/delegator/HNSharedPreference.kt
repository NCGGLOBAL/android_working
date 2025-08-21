package com.creator.pangpanglive.delegator

import android.content.Context
import android.preference.PreferenceManager

/**
 * Created by skcrackers on 6/1/16.
 */
object HNSharedPreference {
    /**
     * <pre>
     * String 데이터를 저장합니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    fun putSharedPreference(context: Context?, key: String?, value: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.commit()
    }

    /**
     * <pre>
     * Boolean 데이터를 저장합니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    fun putSharedPreference(context: Context?, key: String?, value: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.commit()
    }

    /**
     * <pre>
     * Integer 데이터를 저장합니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    fun putSharedPreference(context: Context?, key: String?, value: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    /**
     * <pre>
     * String 데이터를 읽어옵니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 ""이 반환된다.
     */
    @kotlin.jvm.JvmStatic
    fun getSharedPreference(context: Context?, key: String?): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, "")
    }

    /**
     * <pre>
     * Boolean 데이터를 읽어옵니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 false가 반환된다.
     */
    fun getBooleanSharedPreference(context: Context?, key: String?): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(key, false)
    }

    /**
     * <pre>
     * Int 데이터를 읽어옵니다.
    </pre> *
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 0이 반환된다.
     */
    fun getIntSharedPreference(context: Context?, key: String?): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(key, 0)
    }
}