package com.creator.gollala.delegator;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by skcrackers on 6/1/16.
 */
public class HNSharedPreference {
    /**
     * <pre>
     * String 데이터를 저장합니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    public static void putSharedPreference
    (Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(key, value);
        editor.commit();
    }

    /**
     * <pre>
     * Boolean 데이터를 저장합니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    public static void putSharedPreference
    (Context context, String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(key, value);
        editor.commit();
    }

    /**
     * <pre>
     * Integer 데이터를 저장합니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @param value   값
     */
    public static void putSharedPreference
    (Context context, String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * <pre>
     * String 데이터를 읽어옵니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 ""이 반환된다.
     */
    public static String getSharedPreference
    (Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(key, "");
    }

    /**
     * <pre>
     * Boolean 데이터를 읽어옵니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 false가 반환된다.
     */
    public static boolean getBooleanSharedPreference
    (Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(key, false);
    }

    /**
     * <pre>
     * Int 데이터를 읽어옵니다.
     * </pre>
     *
     * @param context 컨텍스트
     * @param key     키
     * @return 읽어온 값, 값이 없을 경우 0이 반환된다.
     */
    public static int getIntSharedPreference
    (Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getInt(key, 0);
    }
}