package com.mallup.ogagae.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.webkit.WebView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by skcrackers on 10/17/16.
 */
public class EtcUtil {
    public static HashMap<String, String> parseParams(String options, boolean isDecode) throws UnsupportedEncodingException {
        HashMap<String, String> paramMap = new HashMap<String, String>();
        String[] params = options.split("\\&");

        for (String param : params) {
            if (!param.contains("=")) {
                continue;
            }
            String[] paramArr = param.split("\\=");
            if (paramArr.length != 2) {
                continue;
            }
            if (paramArr[0] == null || paramArr[1] == null) {
                continue;
            }
            if(isDecode){
                paramMap.put(URLDecoder.decode(paramArr[0], "utf-8"), URLDecoder.decode(paramArr[1], "utf-8"));
            }else{
                paramMap.put(paramArr[0], paramArr[1]);
            }
        }
        return paramMap;
    }

    public static void close(Object object) {
        try {
            if ((object instanceof FileInputStream))
                ((FileInputStream) object).close();
            else if ((object instanceof FileOutputStream))
                ((FileOutputStream) object).close();
            else if ((object instanceof InputStream))
                ((InputStream) object).close();
            else if ((object instanceof OutputStream))
                ((OutputStream) object).close();
            else if ((object instanceof Reader))
                ((Reader) object).close();
            else if ((object instanceof Writer))
                ((Writer) object).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, Object> getDeviceProperties(Context context) {
        HashMap<String, Object> deviceInfo = new HashMap<String, Object>();

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String userAgent = new WebView(context).getSettings().getUserAgentString();
        boolean isMobile = userAgent.contains("Mobile");
        boolean isLargeScreen = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        boolean isTablet = !isMobile && isLargeScreen;
        deviceInfo.put("isTablet", isTablet);

        // TODO how to figure out?
        deviceInfo.put("isTV", false);

        deviceInfo.put("platform", "android");

        deviceInfo.put("osVersion", Build.VERSION.RELEASE);

        String appVersion = "";
        if (packageInfo != null) {
            appVersion = packageInfo.versionName;
        }
        deviceInfo.put("appVersion", appVersion);

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String mobileEquipmentId = "";
        if (telephony != null && telephony.getDeviceId() != null) {
            mobileEquipmentId = telephony.getDeviceId();
        }
        deviceInfo.put("mobileEquipmentId", mobileEquipmentId);

        deviceInfo.put("osName", "android");

        deviceInfo.put("platformName", "android");

        deviceInfo.put("deviceId", mobileEquipmentId);

        deviceInfo.put("appId", context.getPackageName());

        deviceInfo.put("deviceManufacturer", Build.MANUFACTURER);

        deviceInfo.put("deviceModel", Build.MODEL);

        return deviceInfo;
    }

    public static String getApplicationName(Context context) {
        String appName = "";
        try{
            Resources appR = context.getResources();
            appName = appR.getText(appR.getIdentifier("app_name", "string", context.getPackageName())).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appName;
    }

    public static int getResourseIdByName(String packageName, String className, String name) {
        int id = 0;
        try {
            Class<?> desireClass = Class.forName(packageName + ".R$" + className);

            if (desireClass != null)
                id = desireClass.getField(name).getInt(desireClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return id;
    }

    public static String encodeURIComponent(String s) {
        String returnValue = null;
        try {
            returnValue = URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    public static class Security {
        public static byte[] hexToByteArray(String hex)
        {
            if ((hex == null) || (hex.length() == 0)) {
                return null;
            }
            byte[] ba = new byte[hex.length() / 2];

            for (int i = 0; i < ba.length; i++) {
                ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
            }
            return ba;
        }

        public static String byteArrayToHex(byte[] ba) {
            if ((ba == null) || (ba.length == 0)) {
                return null;
            }
            StringBuffer sb = new StringBuffer(ba.length * 2);

            for (int x = 0; x < ba.length; x++) {
                String hexNumber = "0" + Integer.toHexString(0xFF & ba[x]);

                sb.append(hexNumber.substring(hexNumber.length() - 2));
            }

            return sb.toString();
        }

        public static String encrypt(String message, byte[] key) throws Exception {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING", "BC");
            cipher.init(1, skeySpec);

            byte[] encrypted = cipher.doFinal(message.getBytes());

            return byteArrayToHex(encrypted);
        }

        public static String decrypt(String encrypted, byte[] key) throws Exception {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING", "BC");
            cipher.init(2, skeySpec);
            byte[] original = cipher.doFinal(hexToByteArray(encrypted));
            String originalString = new String(original);

            return originalString;
        }

        public static String getDiagest(InputStream targetFile) {
            try {
                MessageDigest digester = MessageDigest.getInstance("MD5");
                byte[] bytes = new byte[8192];
                int byteCount;
                while ((byteCount = targetFile.read(bytes)) > 0)
                {
                    digester.update(bytes, 0, byteCount);
                }

                return byteArrayToHex(digester.digest());
            }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        public static void encrypt(InputStream source, OutputStream dest, String key) {
            try {
                crypt(1, source, dest, new SecretKeySpec(key.getBytes(), "AES"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void decrypt(FileInputStream source, OutputStream dest, String key) {
            try {
                crypt(2, source, dest, new SecretKeySpec(key.getBytes(), "AES"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static byte[] decrypt(FileInputStream source, String key) {
            try {
                return crypt(2, source, new SecretKeySpec(key.getBytes(), "AES"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private static void crypt(int mode, InputStream source, OutputStream dest, SecretKeySpec key) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(mode, key);
            InputStream input = null;
            OutputStream output = null;
            try
            {
                input = new BufferedInputStream(source);
                output = new BufferedOutputStream(dest);
                byte[] buffer = new byte[1024];
                int read = -1;
                while ((read = input.read(buffer)) != -1) {
                    output.write(cipher.update(buffer, 0, read));
                }
                output.write(cipher.doFinal());
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException localIOException) {
                    }
                if (input != null)
                    try {
                        input.close();
                    } catch (IOException localIOException1) {
                    }
            }
        }

        private static byte[] crypt(int mode, FileInputStream source, SecretKeySpec key) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(mode, key);
            InputStream input = null;
            ByteArrayOutputStream output = null;
            try {
                input = new BufferedInputStream(source);
                output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read = -1;
                while ((read = input.read(buffer)) != -1) {
                    output.write(cipher.update(buffer, 0, read));
                }

                output.write(cipher.doFinal());
            } finally {
                if (output != null)
                    try {
                        output.close();
                    } catch (IOException localIOException) {
                    }
                if (input != null)
                    try {
                        input.close();
                    }
                    catch (IOException localIOException1) {
                    }
            }
            return output.toByteArray();
        }

        public static byte[] toBytes(String digits, int radix) throws IllegalArgumentException {
            if (digits == null) {
                return null;
            }
            if ((radix != 16) && (radix != 10) && (radix != 8)) {
                throw new IllegalArgumentException("For input radix: \"" + radix + "\"");
            }
            int divLen = radix == 16 ? 2 : 3;
            int length = digits.length();
            if (length % divLen == 1) {
                throw new IllegalArgumentException("For input string: \"" + digits + "\"");
            }
            length /= divLen;
            byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) {
                int index = i * divLen;
                bytes[i] = (byte) Short.parseShort(digits.substring(index, index + divLen), radix);
            }
            return bytes;
        }

        public class StringOutputStream extends OutputStream {
            protected StringBuffer buf = new StringBuffer();

            public StringOutputStream() {
            }
            public void close() {
            }
            public void flush() {
                this.buf.delete(0, this.buf.length());
            }

            public void write(byte[] b) {
                String str = new String(b);
                this.buf.append(str);
            }

            public void write(byte[] b, int off, int len) {
                String str = new String(b, off, len);
                this.buf.append(str);
            }

            public void write(int b) {
                String str = Integer.toString(b);
                this.buf.append(str);
            }

            public String toString() {
                return this.buf.toString();
            }
        }
    }

    public static String getVersionName(Context context)
    {
        try {
            PackageInfo pi= context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static String EscapeJavaScriptFunctionParameter(String input) {
        String result = input;
        if(result.length() > 0) {
            result.replace("\\", "\\\\").replace("'", "\\'").replace("\b", "\\b").replace("\n", "\\n").replace("\t", "\\t").replace("\f", "\\f").replace("\r", "\\r");
        }

        return result;
    }

    public static String getRandomKey(int length) {
        StringBuffer temp = new StringBuffer();
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            int rIndex = rnd.nextInt(3);
            switch (rIndex) {
                case 0:
                    // a-z
                    temp.append((char) ((int) (rnd.nextInt(26)) + 97));
                    break;
                case 1:
                    // A-Z
                    temp.append((char) ((int) (rnd.nextInt(26)) + 65));
                    break;
                case 2:
                    // 0-9
                    temp.append((rnd.nextInt(10)));
                    break;
            }
        }
        return temp.toString();
    }
}