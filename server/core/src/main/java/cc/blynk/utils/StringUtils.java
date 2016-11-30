package cc.blynk.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StringUtils {

    public static final char BODY_SEPARATOR = '\0';
    public static final String BODY_SEPARATOR_STRING = String.valueOf(BODY_SEPARATOR);
    public static final String DEVICE_SEPARATOR = "-";

    public static final String PIN_PATTERN =  "/pin/";
    public static final String PIN_PATTERN_0 =  "/pin[0]/";
    public static final String PIN_PATTERN_1 =  "/pin[1]/";
    public static final String PIN_PATTERN_2 =  "/pin[2]/";
    public static final String PIN_PATTERN_3 =  "/pin[3]/";
    public static final String PIN_PATTERN_4 =  "/pin[4]/";

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
    public static String[] split3(String body) {
        final int i1 = body.indexOf(BODY_SEPARATOR, 1);
        if (i1 == -1) {
            return new String[] {body};
        }

        final int i2 = body.indexOf(BODY_SEPARATOR, i1 + 1);
        if (i2 == -1) {
            return new String[] {body.substring(0, i1), body.substring(i1 + 1, body.length())};
        }

        return new String[] {body.substring(0, i1), body.substring(i1 + 1, i2), body.substring(i2 + 1, body.length())};
    }

    public static String[] split2(String body) {
        final int i1 = body.indexOf(BODY_SEPARATOR, 1);
        if (i1 == -1) {
            return new String[] {body};
        }

        return new String[] {body.substring(0, i1), body.substring(i1 + 1, body.length())};
    }

    //todo this is back compatibility code. remove in future versions
    public static String makeBody(int dashId, int deviceId, String body) {
        if (deviceId == 0) {
            return dashId + BODY_SEPARATOR_STRING + body;
        }
        return dashId + DEVICE_SEPARATOR + deviceId + BODY_SEPARATOR_STRING + body;
    }

}
