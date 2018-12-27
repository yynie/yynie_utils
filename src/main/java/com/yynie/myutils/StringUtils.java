package com.yynie.myutils;

/**
 * A simplified utility class that provides several frequently-used method
 * in String comparing and empty checking
 *
 * @author yy_nie@hotmail.com
 * */
public class StringUtils {

    public static boolean equals(String cs1, String cs2) {
        return cs1 == cs2 ? true : (cs1 != null && cs2 != null && cs1.equals(cs2));
    }

    public static boolean equalsIgnoreCase(String cs1, String cs2){
        return cs1 == cs2 ? true : (cs1 != null && cs2 != null && cs1.equalsIgnoreCase(cs2));
    }

    public static boolean isNotBlank(String s){
        return s != null && s.trim().length() > 0;
    }

    public static boolean isBlank(String s) { return s == null || s.trim().length() == 0; }

    public static boolean isAnyBlank(String... ss) {
        if(ss == null || ss.length == 0){
            return true;
        } else {
            String [] arr$ = ss;
            int len$ = ss.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String s = arr$[i$];
                if(isBlank(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isNoneBlank(String... ss){
        return !isAnyBlank(ss);
    }

    public static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

}
