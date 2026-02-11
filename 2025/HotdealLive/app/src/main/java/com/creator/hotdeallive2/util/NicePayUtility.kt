package com.creator.hotdeallive2.util

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

object NicePayUtility {
    fun getyyyyMMddHHmmss(): String {
        /** yyyyMMddHHmmss Date Format  */
        val yyyyMMddHHmmss = SimpleDateFormat("yyyyMMddHHmmss")
        return yyyyMMddHHmmss.format(Date())
    }

    fun encrypt(strData: String): String { // ��ȣȭ ��ų ������
        var strOUTData = ""
        try {
            val md = MessageDigest.getInstance("MD5") // "MD5 �������� ��ȣȭ"
            md.reset()
            //byte[] bytData = strData.getBytes();  
            //md.update(bytData);
            md.update(strData.toByteArray())
            val digest = md.digest()
            val hashedpasswd = StringBuffer()
            var hx: String
            for (i in digest.indices) {
                hx = Integer.toHexString(0xFF and digest[i].toInt())
                //0x03 is equal to 0x3, but we need 0x03 for our md5sum
                if (hx.length == 1) {
                    hx = "0$hx"
                }
                hashedpasswd.append(hx)
            }
            strOUTData = hashedpasswd.toString()
            val raw = strOUTData.toByteArray()
            val encodedBytes = Base64.encode(raw, 0)
            strOUTData = String(encodedBytes)
            //strOUTData = new String(raw);
        } catch (e: NoSuchAlgorithmException) {
        }
        return strOUTData // ��ȣȭ�� �����͸� ����...
    }

    fun AlertDialog(title: String?, message: String?, context: Context?) {
        var ab: AlertDialog.Builder? = null
        ab = AlertDialog.Builder(context)
        ab.setMessage(message)
        ab.setPositiveButton(android.R.string.ok, null)
        ab.setTitle(title)
        ab.show()
    }

    fun isPackageInstalled(ctx: Context, pkgName: String?): Boolean {
        try {
            ctx.packageManager.getPackageInfo(pkgName!!, PackageManager.GET_ACTIVITIES)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return false
        }
        return true
    }
}