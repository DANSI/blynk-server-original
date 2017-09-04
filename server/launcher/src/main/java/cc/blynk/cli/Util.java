package cc.blynk.cli;

/**
 * Contains useful helper methods for classes within this package.
 *
 * @version $Id: Util.java 1443102 2013-02-06 18:12:16Z tn $
 */
final class Util {

    private Util() {
    }

    /**
     * Remove the hyphens from the beginning of <code>str</code> and
     * return the new String.
     *
     * @param str The string from which the hyphens should be removed.
     * @return the new String.
     */
    static String stripLeadingHyphens(String str) {
        if (str == null) {
            return null;
        }
        if (str.startsWith("--")) {
            return str.substring(2, str.length());
        } else if (str.startsWith("-")) {
            return str.substring(1, str.length());
        }

        return str;
    }

    /**
     * Remove the leading and trailing quotes from <code>str</code>.
     * E.g. if str is '"one two"', then 'one two' is returned.
     *
     * @param str The string from which the leading and trailing quotes
     *            should be removed.
     * @return The string without the leading and trailing quotes.
     */
    static String stripLeadingAndTrailingQuotes(String str) {
        int length = str.length();
        if (length > 1 && str.startsWith("\"") && str.endsWith("\"")
                && str.substring(1, length - 1).indexOf('"') == -1) {
            str = str.substring(1, length - 1);
        }

        return str;
    }
}
