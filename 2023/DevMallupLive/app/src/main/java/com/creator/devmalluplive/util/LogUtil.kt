package com.creator.devmalluplive.util

import android.util.Log
import com.creator.devmalluplive.common.HNApplication

/**
 * Created by skcrackers on 5/27/16.
 */
object LogUtil {
    private const val TAG = "HN_LOG"
    private var mLogLevel = Log.VERBOSE
    private var IS_PRINT_LOG = true
    private const val IS_PRINT_FILENAME = true
    private const val IS_PRINT_METHOD = true
    private const val IS_PRINT_LINE = true

    /**
     *
     * set log level. log level is static. after set, every log request with under set log level will be ignored.
     *
     * @param logLevel
     * log level to set.
     */
    fun setLogLevel(logLevel: Int) {
        mLogLevel = logLevel
    }

    private fun printLog(logLevel: Int, tag: String, msg: Any?) {
        IS_PRINT_LOG = !HNApplication.Companion.mSigned
        if (!IS_PRINT_LOG) {
            return
        }
        if (logLevel < mLogLevel) {
            return
        }
        val sb = StringBuffer()
        if (IS_PRINT_FILENAME || IS_PRINT_METHOD || IS_PRINT_LINE) {
            val e = Exception()
            val arrStackTrace = e.stackTrace
            var fileName: String? = null
            for (element in arrStackTrace) {
                if (fileName == null) {
                    fileName = element.fileName
                    continue
                }
                if (element.fileName == fileName) {
                    continue
                }
                sb.append("[")
                if (IS_PRINT_FILENAME) {
                    sb.append(element.fileName)
                    sb.append("  ")
                }
                if (IS_PRINT_METHOD) {
                    sb.append(element.methodName)
                    sb.append("  ")
                }
                if (IS_PRINT_LINE) {
                    sb.append(element.lineNumber)
                }
                sb.append("] ")
                break
            }
        }
        val strMsg = msg?.toString() ?: "null"
        sb.append(strMsg)
        Log.println(logLevel, tag, sb.toString())
    }

    @JvmOverloads
    fun line(tag: String = TAG) {
        printLog(Log.VERBOSE, tag, "===========================================")
    }

    fun v(msg: Any) {
        printLog(Log.VERBOSE, TAG, msg.toString())
    }

    fun v(tag: String, msg: Any) {
        printLog(Log.VERBOSE, tag, msg.toString())
    }

    fun d(msg: Any) {
        printLog(Log.DEBUG, TAG, msg.toString())
    }

    fun d(tag: String, msg: Any) {
        printLog(Log.DEBUG, tag, msg.toString())
    }

    fun i(msg: Any) {
        printLog(Log.INFO, TAG, msg.toString())
    }

    fun i(tag: String, msg: Any) {
        printLog(Log.INFO, tag, msg.toString())
    }

    fun w(msg: Any) {
        printLog(Log.WARN, TAG, msg.toString())
    }

    fun w(tag: String, msg: Any) {
        printLog(Log.WARN, tag, msg.toString())
    }

    fun e(msg: Any) {
        printLog(Log.ERROR, TAG, msg.toString())
    }

    fun e(tag: String, msg: Any) {
        printLog(Log.ERROR, tag, msg.toString())
    }
}