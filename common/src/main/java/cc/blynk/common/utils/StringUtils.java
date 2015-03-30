package cc.blynk.common.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StringUtils {

    public static final char SEPARATOR = '\0';
    /**
     * Parses string similar to this : "xw 1 xxxx"
     * Every hard message has at least 3 starting chars we don't need.
     */
    private static final int START_INDEX = 3;

    /**
     * Efficient split method (instead of String.split).
     *
     * Returns pin from hardware body. For instance
     *
     * "aw 11 32" - is body. Where 11 is pin Number.
     *
     * @throws java.lang.NumberFormatException in case parsed pin not a Number.
     *
     */
    public static String fetchPin(String body) {
        int i = START_INDEX;
        while (i < body.length()) {
            if (body.charAt(i) == SEPARATOR) {
                return body.substring(START_INDEX, i);
            }
            i++;
        }

        return body.substring(START_INDEX, i);
    }

}
