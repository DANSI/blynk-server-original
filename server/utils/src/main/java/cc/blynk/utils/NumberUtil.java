package cc.blynk.utils;

/**
 * Optimized but less precise double parsing method. It is also doesn't spam objects.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.01.17.
 */
public final class NumberUtil {

    public static final double NO_RESULT = Double.MIN_VALUE;
    private static final NumberFormatException cachedNumberFormatException =
            new NumberFormatException("Not a valid double number.");
    private static final NumberFormatException cachedNumberFormatExceptionForPin =
            new NumberFormatException("Not a valid pin number.");

    private NumberUtil() {
    }

    // Precompute Math.pow(10, n) as table:
    private final static int POW_RANGE = 256;
    private final static double[] POS_EXPS = new double[POW_RANGE];
    private final static double[] NEG_EXPS = new double[POW_RANGE];

    static {
        for (int i = 0; i < POW_RANGE; i++) {
            POS_EXPS[i] = Math.pow(10., i);
            NEG_EXPS[i] = Math.pow(10., -i);
        }
    }

    // Calculate the value of the specified exponent - reuse a precalculated value if possible
    private static double getPow10(final int exp) {
        if (exp > -POW_RANGE) {
            if (exp <= 0) {
                return NEG_EXPS[-exp];
            } else if (exp < POW_RANGE) {
                return POS_EXPS[exp];
            }
        }
        return Math.pow(10., exp);
    }

    public static double parseDoubleOrThrow(final String s) {
        double result = parseDouble(s);
        if (result == NO_RESULT) {
            throw cachedNumberFormatException;
        }
        return result;
    }

    public static double parseDouble(final String s) {

        int off = 0;
        int len = s.length();

        if (len == 0) {
            return NO_RESULT;
        }

        char ch;
        boolean numSign = true;

        ch = s.charAt(off);
        if (ch == '+') {
            off++;
            len--;
        } else if (ch == '-') {
            numSign = false;
            off++;
            len--;
        }

        double number;

        boolean error = true;

        int startOffset = off;
        double dval;

        for (dval = 0d; (len > 0) && ((ch = s.charAt(off)) >= '0') && (ch <= '9');) {
            dval *= 10d;
            dval += ch - '0';
            off++;
            len--;
        }
        int numberLength = off - startOffset;

        number = dval;

        if (numberLength > 0) {
            error = false;
        }

        // Check for fractional values after decimal
        if ((len > 0) && (s.charAt(off) == '.')) {

            off++;
            len--;

            startOffset = off;

            for (dval = 0d; (len > 0) && ((ch = s.charAt(off)) >= '0') && (ch <= '9');) {
                dval *= 10d;
                dval += ch - '0';
                off++;
                len--;
            }
            numberLength = off - startOffset;

            if (numberLength > 0) {
                number += getPow10(-numberLength) * dval;
                error = false;
            }
        }

        if (error) {
            return NO_RESULT;
        }

        // Look for an exponent
        if (len > 0) {
            // note: ignore any non-digit character at end:

            if ((ch = s.charAt(off)) == 'e' || ch == 'E') {

                off++;
                len--;

                if (len > 0) {
                    boolean expSign = true;

                    ch = s.charAt(off);
                    if (ch == '+') {
                        off++;
                        len--;
                    } else if (ch == '-') {
                        expSign = false;
                        off++;
                        len--;
                    }

                    int exponent;

                    // note: ignore any non-digit character at end:
                    for (exponent = 0; (len > 0) && ((ch = s.charAt(off)) >= '0') && (ch <= '9');) {
                        exponent *= 10;
                        exponent += ch - '0';
                        off++;
                        len--;
                    }

                    if (!expSign) {
                        exponent = -exponent;
                    }

                    // For very small numbers we try to miminize
                    // effects of denormalization.
                    if (exponent > -300) {
                        number *= getPow10(exponent);
                    } else {
                        number = 1.0E-300 * (number * getPow10(exponent + 300));
                    }
                }
            }
        }
        // check other characters:
        if (len > 0) {
            return NO_RESULT;
        }


        return (numSign) ? number : -number;
    }

    public static int calcHeartbeatTimeout(int heartbeatInterval) {
        return (int) Math.ceil(heartbeatInterval * 2.3D);
    }

    public static short parsePin(String pinString) throws NumberFormatException {
        int i = Integer.parseInt(pinString, 10);
        if (i < 0 || i > 255) {
            throw cachedNumberFormatExceptionForPin;
        }
        return (short) i;
    }
}
