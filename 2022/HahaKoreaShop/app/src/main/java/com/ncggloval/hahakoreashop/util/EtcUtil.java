package com.ncggloval.hahakoreashop.util;

import java.util.Random;

public class EtcUtil {
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

    public static String EscapeJavaScriptFunctionParameter(String input) {
        String result = input;
        if(result.length() > 0) {
            result.replace("\\", "\\\\").replace("'", "\\'").replace("\b", "\\b").replace("\n", "\\n").replace("\t", "\\t").replace("\f", "\\f").replace("\r", "\\r");
        }

        return result;
    }
}