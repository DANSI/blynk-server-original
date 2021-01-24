package cc.blynk.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public final class StringUtils {

    public final static String BLYNK_LANDING = "https://www.blynk.cc";

    public static final char BODY_SEPARATOR = '\0';
    public static final String BODY_SEPARATOR_STRING = String.valueOf(BODY_SEPARATOR);
    public static final char DEVICE_SEPARATOR = '-';
    public static final String DEVICE_SEPARATOR_STRING = "-";

    public static final Pattern PIN_PATTERN =  Pattern.compile("/pin/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_0 =  Pattern.compile("/pin[0]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_1 =  Pattern.compile("/pin[1]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_2 =  Pattern.compile("/pin[2]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_3 =  Pattern.compile("/pin[3]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_4 =  Pattern.compile("/pin[4]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_5 =  Pattern.compile("/pin[5]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_6 =  Pattern.compile("/pin[6]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_7 =  Pattern.compile("/pin[7]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_8 =  Pattern.compile("/pin[8]/", Pattern.LITERAL);
    public static final Pattern PIN_PATTERN_9 =  Pattern.compile("/pin[9]/", Pattern.LITERAL);
    public static final Pattern GENERIC_PLACEHOLDER = Pattern.compile("%s", Pattern.LITERAL);
    private static final Pattern NOT_SUPPORTED_CHARS = Pattern.compile("[\\\\/:*?\"<>| ]");

    public static final Pattern DATETIME_PATTERN =  Pattern.compile("/datetime_iso/", Pattern.LITERAL);
    public static final Pattern DEVICE_OWNER_EMAIL =  Pattern.compile("device_owner_email",
                                                                      Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
    public static final String WEBSOCKET_PATH = "/websocket";
    public static final String WEBSOCKETS_PATH = "/websockets";
    public static final String WEBSOCKET_WEB_PATH = "/dashws";

    private StringUtils() {
    }

    /**
     * Parses string similar to this : "xw 1 xxxx"
     * Every hard message has at least 3 starting chars we don't need.
     */
    private static final int START_INDEX = 3;

    private static final String IN_DATA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    /**
     * Optimized method for splitting. It is uses knowledge of Blynk message structure. So it is 2-3 times faster
     * and produces less garbage.
     *
     * Does same as String.split(BODY_SEPARATOR_STRING, 3);
     *
     * See StringUtilPerfTest
     *
     Benchmark                                        Mode  Cnt    Score    Error  Units
     StringUtilPerfTest.customSplit3_aw_100_900       avgt    5   46.806 ±  6.927  ns/op
     StringUtilPerfTest.customSplit3_aw_10_long_text  avgt    5   52.334 ±  9.231  ns/op
     StringUtilPerfTest.customSplit3_aw_1_2           avgt    5   48.483 ± 14.347  ns/op
     StringUtilPerfTest.customSplit3_vw_1             avgt    5   34.943 ±  9.918  ns/op
     StringUtilPerfTest.customSplit3_vw_99_22222      avgt    5   46.511 ± 15.401  ns/op
     StringUtilPerfTest.customSplit3_vw_99_900        avgt    5   48.025 ± 18.951  ns/op
     StringUtilPerfTest.split3_aw_100_900             avgt    5  113.358 ± 26.606  ns/op
     StringUtilPerfTest.split3_aw_10_long_text        avgt    5  117.831 ± 39.682  ns/op
     StringUtilPerfTest.split3_aw_1_2                 avgt    5  106.119 ±  1.289  ns/op
     StringUtilPerfTest.split3_vw_1                   avgt    5   87.868 ± 11.709  ns/op
     StringUtilPerfTest.split3_vw_99_22222            avgt    5  115.280 ±  8.697  ns/op
     StringUtilPerfTest.split3_vw_99_900              avgt    5  123.085 ± 20.625  ns/op
     *
     *
     */
    public static String[] split3(char separator, String body) {
        final int i1 = body.indexOf(separator, 1);
        if (i1 == -1) {
            return new String[] {body};
        }

        final int i2 = body.indexOf(separator, i1 + 1);
        if (i2 == -1) {
            return new String[] {body.substring(0, i1), body.substring(i1 + 1)};
        }

        return new String[] {body.substring(0, i1), body.substring(i1 + 1, i2), body.substring(i2 + 1)};
    }

    public static String[] split3(String body) {
        return split3(BODY_SEPARATOR, body);
    }

    public static String[] split2Device(String body) {
        return split2(DEVICE_SEPARATOR, body);
    }

    public static String[] split2(char separator, String body) {
        final int i1 = body.indexOf(separator, 1);
        if (i1 == -1) {
            return new String[] {body};
        }

        return new String[] {body.substring(0, i1), body.substring(i1 + 1)};
    }

    public static String[] split2(String body) {
        return split2(BODY_SEPARATOR, body);
    }

    public static String prependDashIdAndDeviceId(int dashId, int deviceId, String body) {
        return "" + dashId + DEVICE_SEPARATOR + deviceId + BODY_SEPARATOR + body;
    }

    public static String randomPassword(int len) {
        return randomString(IN_DATA, len);
    }

    public static String randomString(int len) {
        //using only lowercase chars for app id.
        String dataForId = IN_DATA.substring(0, 26);
        return randomString(dataForId, len);
    }

    private static String randomString(String inData, int len) {
        StringBuilder sb = new StringBuilder(len);
        int inDataLength = inData.length();
        for (int i = 0; i < len; i++) {
            sb.append(inData.charAt(SECURE_RANDOM.nextInt(inDataLength)));
        }
        return sb.toString();
    }

    private static String removeUnsupportedChars(String name) {
        return NOT_SUPPORTED_CHARS.matcher(name).replaceAll("");
    }

    public static String truncate(String name, int size) {
        return name.length() <= size ? name : name.substring(0, size);
    }

    public static String escapeCSV(String name) {
        name = name.replace("\"", "\"\"");
        if (name.contains(",") || name.contains(";") || name.contains("\"")) {
            return "\"" + name + "\"";
        }
        return name;
    }

    public static String truncateFileName(String name) {
        if (name == null) {
            return "";
        }

        String truncated = removeUnsupportedChars(name);
        return truncate(truncated, 16);
    }

    public static boolean isReadOperation(String body) {
        return body.length() > 1 && body.charAt(1) == 'r';
    }

    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

}
