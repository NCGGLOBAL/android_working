package com.creator.shortdealnet1.util

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec



/**
 * Created by skcrackers on 10/17/16.
 */
object EtcUtil {
    @Throws(UnsupportedEncodingException::class)
    fun parseParams(options: String, isDecode: Boolean): HashMap<String?, String?> {
        val paramMap = HashMap<String?, String?>()
        val params = options.split("\\&").toTypedArray()
        for (param in params) {
            if (!param.contains("=")) {
                continue
            }
            val paramArr: Array<String?> = param.split("\\=").toTypedArray()
            if (paramArr.size != 2) {
                continue
            }
            if (paramArr[0] == null || paramArr[1] == null) {
                continue
            }
            if (isDecode) {
                paramMap[URLDecoder.decode(paramArr[0], "utf-8")] = URLDecoder.decode(
                    paramArr[1], "utf-8"
                )
            } else {
                paramMap[paramArr[0]] = paramArr[1]
            }
        }
        return paramMap
    }

    fun close(`object`: Any?) {
        try {
            if (`object` is FileInputStream) `object`.close() else if (`object` is FileOutputStream) `object`.close() else if (`object` is InputStream) `object`.close() else if (`object` is OutputStream) `object`.close() else if (`object` is Reader) `object`.close() else if (`object` is Writer) `object`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getDeviceProperties(context: Context): HashMap<String, Any> {
        val deviceInfo = HashMap<String, Any>()
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val userAgent = WebView(context).settings.userAgentString
        val isMobile = userAgent.contains("Mobile")
        val isLargeScreen =
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        val isTablet = !isMobile && isLargeScreen
        deviceInfo["isTablet"] = isTablet

        // TODO how to figure out?
        deviceInfo["isTV"] = false
        deviceInfo["platform"] = "android"
        deviceInfo["osVersion"] = Build.VERSION.RELEASE
        var appVersion = ""
        if (packageInfo != null) {
            appVersion = packageInfo.versionName.toString()
        }
        deviceInfo["appVersion"] = appVersion
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var mobileEquipmentId = ""
        if (telephony != null && telephony.deviceId != null) {
            mobileEquipmentId = telephony.deviceId
        }
        deviceInfo["mobileEquipmentId"] = mobileEquipmentId
        deviceInfo["osName"] = "android"
        deviceInfo["platformName"] = "android"
        deviceInfo["deviceId"] = mobileEquipmentId
        deviceInfo["appId"] = context.packageName
        deviceInfo["deviceManufacturer"] = Build.MANUFACTURER
        deviceInfo["deviceModel"] = Build.MODEL
        return deviceInfo
    }

    fun getApplicationName(context: Context): String {
        var appName = ""
        try {
            val appR = context.resources
            appName = appR.getText(appR.getIdentifier("app_name", "string", context.packageName))
                .toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return appName
    }

    fun getResourseIdByName(packageName: String, className: String, name: String?): Int {
        var id = 0
        try {
            val desireClass = Class.forName("$packageName.R$$className")
            if (desireClass != null) id = desireClass.getField(name).getInt(desireClass)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
        return id
    }

    fun encodeURIComponent(s: String?): String? {
        var returnValue: String? = null
        try {
            returnValue = URLEncoder.encode(s, "UTF-8").replace("\\+".toRegex(), "%20")
                .replace("\\%21".toRegex(), "!")
                .replace("\\%27".toRegex(), "'").replace("\\%28".toRegex(), "(")
                .replace("\\%29".toRegex(), ")")
                .replace("\\%7E".toRegex(), "~")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return returnValue
    }

    fun getVersionName(context: Context): String? {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun EscapeJavaScriptFunctionParameter(input: String): String {
        if (input.length > 0) {
            input.replace("\\", "\\\\").replace("'", "\\'").replace("\b", "\\b")
                .replace("\n", "\\n").replace("\t", "\\t").replace("\u000c", "\\u000c")
                .replace("\r", "\\r")
        }
        return input
    }

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

    object Security {
        fun hexToByteArray(hex: String?): ByteArray? {
            if (hex == null || hex.length == 0) {
                return null
            }
            val ba = ByteArray(hex.length / 2)
            for (i in ba.indices) {
                ba[i] = hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
            }
            return ba
        }

        fun byteArrayToHex(ba: ByteArray?): String? {
            if (ba == null || ba.size == 0) {
                return null
            }
            val sb = StringBuffer(ba.size * 2)
            for (x in ba.indices) {
                val hexNumber = "0" + Integer.toHexString(
                    0xFF and ba[x]
                        .toInt()
                )
                sb.append(hexNumber.substring(hexNumber.length - 2))
            }
            return sb.toString()
        }

        @Throws(Exception::class)
        fun encrypt(message: String, key: ByteArray?): String? {
            val skeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING", "BC")
            cipher.init(1, skeySpec)
            val encrypted = cipher.doFinal(message.toByteArray())
            return byteArrayToHex(encrypted)
        }

        @Throws(Exception::class)
        fun decrypt(encrypted: String?, key: ByteArray?): String {
            val skeySpec =
                SecretKeySpec(key, "AES")
            val cipher =
                Cipher.getInstance("AES/ECB/PKCS5PADDING", "BC")
            cipher.init(2, skeySpec)
            val original = cipher.doFinal(
                hexToByteArray(encrypted)
            )
            return String(original)
        }

        fun getDiagest(targetFile: InputStream): String? {
            try {
                val digester = MessageDigest.getInstance("MD5")
                val bytes = ByteArray(8192)
                var byteCount: Int
                while (targetFile.read(bytes).also { byteCount = it } > 0) {
                    digester.update(bytes, 0, byteCount)
                }
                return byteArrayToHex(digester.digest())
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun encrypt(source: InputStream, dest: OutputStream, key: String) {
            try {
                crypt(1, source, dest, SecretKeySpec(key.toByteArray(), "AES"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun decrypt(source: FileInputStream, dest: OutputStream, key: String) {
            try {
                crypt(2, source, dest, SecretKeySpec(key.toByteArray(), "AES"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun decrypt(source: FileInputStream, key: String): ByteArray? {
            try {
                return crypt(2, source, SecretKeySpec(key.toByteArray(), "AES"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        @Throws(Exception::class)
        private fun crypt(mode: Int, source: InputStream, dest: OutputStream, key: SecretKeySpec) {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(mode, key)
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                input = BufferedInputStream(source)
                output = BufferedOutputStream(dest)
                val buffer = ByteArray(1024)
                var read = -1
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(cipher.update(buffer, 0, read))
                }
                output.write(cipher.doFinal())
            } finally {
                if (output != null) try {
                    output.close()
                } catch (localIOException: IOException) {
                }
                if (input != null) try {
                    input.close()
                } catch (localIOException1: IOException) {
                }
            }
        }

        @Throws(Exception::class)
        private fun crypt(mode: Int, source: FileInputStream, key: SecretKeySpec): ByteArray {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(mode, key)
            var input: InputStream? = null
            var output: ByteArrayOutputStream? = null
            try {
                input = BufferedInputStream(source)
                output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var read = -1
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(cipher.update(buffer, 0, read))
                }
                output.write(cipher.doFinal())
            } finally {
                if (output != null) try {
                    output.close()
                } catch (localIOException: IOException) {
                }
                if (input != null) try {
                    input.close()
                } catch (localIOException1: IOException) {
                }
            }
            return output!!.toByteArray()
        }

        @Throws(IllegalArgumentException::class)
        fun toBytes(digits: String?, radix: Int): ByteArray? {
            if (digits == null) {
                return null
            }
            require(!(radix != 16 && radix != 10 && radix != 8)) { "For input radix: \"$radix\"" }
            val divLen = if (radix == 16) 2 else 3
            var length = digits.length
            require(length % divLen != 1) { "For input string: \"$digits\"" }
            length /= divLen
            val bytes = ByteArray(length)
            for (i in 0 until length) {
                val index = i * divLen
                bytes[i] = digits.substring(index, index + divLen).toShort(radix).toByte()
            }
            return bytes
        }

        class StringOutputStream : OutputStream() {
            protected var buf = StringBuffer()
            override fun close() {}
            override fun flush() {
                buf.delete(0, buf.length)
            }

            override fun write(b: ByteArray) {
                val str = String(b)
                buf.append(str)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                val str = String(b, off, len)
                buf.append(str)
            }

            override fun write(b: Int) {
                val str = Integer.toString(b)
                buf.append(str)
            }

            override fun toString(): String {
                return buf.toString()
            }
        }
    }

    fun checkAppInstall(context: Context, packageName: String): Boolean {
        val pm: PackageManager = context.packageManager
        return try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            // 카카오톡이 설치되어 있음
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // 카카오톡이 설치되어 있지 않음
            false
        }
    }

    fun moveToPlayStoreApp(context: Context, packageName: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        context.startActivity(intent)
    }

    fun downloadFile(url: String?,
                     userAgent: String?,
                     contentDisposition: String?,
                     mimeType: String?,
                     context: Context) {

        Toast.makeText(context, "다운로드를 시작합니다.", Toast.LENGTH_LONG).show()

        val request = DownloadManager.Request(Uri.parse(url))
        request.setMimeType(mimeType)

        //------------------------COOKIE!!------------------------

        //------------------------COOKIE!!------------------------
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        //------------------------COOKIE!!------------------------
        //------------------------COOKIE!!------------------------
        request.addRequestHeader(
            "User-Agent",
            userAgent
        )
        request.setDescription("Downloading file...")
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            URLUtil.guessFileName(url, contentDisposition, mimeType)
        )
        val dm = context.getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }

    fun getMarketVersion(packageName: String): String? {
        try {
            val doc: Document = Jsoup.connect(
                "https://play.google.com/store/apps/details?id="
                        + packageName
            ).get()
            val Version: Elements = doc.select(".content")
            for (mElement: Element in Version) {
                if (mElement.attr("itemprop").equals("softwareVersion")) {
                    return mElement.text().trim()
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return null
    }

    fun getMarketVersionFast(packageName: String): String? {
        var mData = ""
        var mVer: String? = null
        try {
            val mUrl = URL(
                "https://play.google.com/store/apps/details?id="
                        + packageName
            )
            val mConnection: HttpURLConnection? = mUrl
                .openConnection() as HttpURLConnection ?: return null
            mConnection?.setConnectTimeout(5000)
            mConnection?.setUseCaches(false)
            mConnection?.setDoOutput(true)
            if (mConnection?.getResponseCode() === HttpURLConnection.HTTP_OK) {
                val mReader = BufferedReader(
                    InputStreamReader(mConnection.getInputStream())
                )
                while (true) {
                    val line = mReader.readLine() ?: break
                    mData += line
                }
                mReader.close()
            }
            mConnection?.disconnect()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return null
        }
        val startToken = "softwareVersion\">"
        val endToken = "<"
        val index = mData.indexOf(startToken)
        if (index == -1) {
            mVer = null
        } else {
            mVer = mData.substring(
                index + startToken.length, (index
                        + startToken.length + 100)
            )
            mVer = mVer.substring(0, mVer.indexOf(endToken)).trim { it <= ' ' }
        }
        return mVer
    }
}