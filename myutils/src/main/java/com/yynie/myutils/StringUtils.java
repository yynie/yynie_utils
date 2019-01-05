/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2018, yynie
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.yynie.myutils;

/**
 * A simplified utility class that provides several frequently-used method
 * in String comparing and empty checking
 *
 * @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
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
