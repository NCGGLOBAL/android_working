package com.ncggloval.hahakoreashop.util

import java.util.*

object EtcUtil {
    fun getRandomKey(length: Int): String {
        val temp = StringBuffer()
        val rnd = Random()
        for (i in 0 until length) {
            val rIndex = rnd.nextInt(3)
            when (rIndex) {
                0 ->                     // a-z
                    temp.append((rnd.nextInt(26) + 97).toChar())
                1 ->                     // A-Z
                    temp.append((rnd.nextInt(26) + 65).toChar())
                2 ->                     // 0-9
                    temp.append(rnd.nextInt(10))
            }
        }
        return temp.toString()
    }

    fun EscapeJavaScriptFunctionParameter(input: String): String {
        if (input.length > 0) {
            input.replace("\\", "\\\\").replace("'", "\\'").replace("\b", "\\b")
                .replace("\n", "\\n").replace("\t", "\\t").replace("\u000c", "\\u000c")
                .replace("\r", "\\r")
        }
        return input
    }
}