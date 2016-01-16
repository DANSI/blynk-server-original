package cc.blynk.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StringUtils {

    public static final char BODY_SEPARATOR = '\0';
    public static final String BODY_SEPARATOR_STRING = String.valueOf(BODY_SEPARATOR);
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
            if (body.charAt(i) == BODY_SEPARATOR) {
                return body.substring(START_INDEX, i);
            }
            i++;
        }

        return body.substring(START_INDEX, i);
    }

    /*
    private static final int LIMIT3 = 3;
    public static String[] split3(String body) {
        String[] s = new String[LIMIT3];
        int counter = 0;
        int i = 2;
        int prev = -1;
        while (i < body.length() && counter < LIMIT3 - 1) {
            if (body.charAt(i) == BODY_SEPARATOR) {
                s[counter++] = body.substring(prev + 1, i);
                prev = i;
            }
            i++;
        }
        s[counter] = body.substring(prev + 1, body.length());
        return s;
    }
    */

    public static String[] split3(String body) {
        final int i1 = body.indexOf(BODY_SEPARATOR, 2);
        if (i1 == -1) {
            return new String[] {body};
        }

        final int i2 = body.indexOf(BODY_SEPARATOR, i1 + 1);
        if (i2 == -1) {
            return new String[] {body.substring(0, i1), body.substring(i1, body.length())};
        }

        return new String[] {body.substring(0, i1), body.substring(i1 + 1, i2), body.substring(i2 + 1, body.length())};
    }

}
