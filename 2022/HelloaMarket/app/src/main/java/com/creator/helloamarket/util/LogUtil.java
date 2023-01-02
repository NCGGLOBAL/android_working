package com.creator.helloamarket.util;

import android.util.Log;

import com.creator.helloamarket.common.HNApplication;


/**
 * Created by skcrackers on 5/27/16.
 */
public class LogUtil {
    private static final String TAG = "HN_LOG";

    private static int mLogLevel = Log.VERBOSE;

    private static boolean IS_PRINT_LOG = true;
    private static final boolean IS_PRINT_FILENAME = true;
    private static final boolean IS_PRINT_METHOD = true;
    private static final boolean IS_PRINT_LINE = true;

    /**
     *
     * set log level. log level is static. after set, every log request with under set log level will be ignored.
     *
     * @param logLevel
     *            log level to set.
     *
     */
    public static void setLogLevel(int logLevel) {
        mLogLevel = logLevel;
    }

    private static void printLog(int logLevel, String tag, Object msg) {
        IS_PRINT_LOG = !HNApplication.mSigned;

        if (!IS_PRINT_LOG) {
            return;
        }

        if (logLevel < mLogLevel) {
            return;
        }

        StringBuffer sb = new StringBuffer();

        if (IS_PRINT_FILENAME || IS_PRINT_METHOD || IS_PRINT_LINE) {
            Exception e = new Exception();
            StackTraceElement[] arrStackTrace = e.getStackTrace();

            String fileName = null;
            for (StackTraceElement element : arrStackTrace) {
                if (fileName == null) {
                    fileName = element.getFileName();
                    continue;
                }
                if (element.getFileName().equals(fileName)) {
                    continue;
                }
                sb.append("[");
                if (IS_PRINT_FILENAME) {
                    sb.append(element.getFileName());
                    sb.append("  ");
                }
                if (IS_PRINT_METHOD) {
                    sb.append(element.getMethodName());
                    sb.append("  ");
                }
                if (IS_PRINT_LINE) {
                    sb.append(element.getLineNumber());
                }
                sb.append("] ");
                break;
            }
        }

        String strMsg = msg == null ? "null" : String.valueOf(msg);
        sb.append(strMsg);

        Log.println(logLevel, tag, sb.toString());
    }

    public static void line() {
        line(TAG);
    }

    public static void line(String tag) {
        printLog(Log.VERBOSE, tag, "===========================================");
    }

    public static void v(Object msg) {
        printLog(Log.VERBOSE, TAG, String.valueOf(msg));
    }

    public static void v(String tag, Object msg) {
        printLog(Log.VERBOSE, tag, String.valueOf(msg));
    }

    public static void d(Object msg) {
        printLog(Log.DEBUG, TAG, String.valueOf(msg));
    }

    public static void d(String tag, Object msg) {
        printLog(Log.DEBUG, tag, String.valueOf(msg));
    }

    public static void i(Object msg) {
        printLog(Log.INFO, TAG, String.valueOf(msg));
    }

    public static void i(String tag, Object msg) {
        printLog(Log.INFO, tag, String.valueOf(msg));
    }

    public static void w(Object msg) {
        printLog(Log.WARN, TAG, String.valueOf(msg));
    }

    public static void w(String tag, Object msg) {
        printLog(Log.WARN, tag, String.valueOf(msg));
    }

    public static void e(Object msg) {
        printLog(Log.ERROR, TAG, String.valueOf(msg));
    }

    public static void e(String tag, Object msg) {
        printLog(Log.ERROR, tag, String.valueOf(msg));
    }
}
