package avecsafoi.ktx.mts.util;

import org.apache.commons.lang3.StringUtils;

public class StringU {

    public static String leftPad(String s, int n, char p) {
        return StringUtils.leftPad(s, n, p);
    }

    public static String rightPad(String s, int n) {
        return StringUtils.rightPad(s, n, ' ');
    }

}
