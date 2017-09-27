package cc.blynk.cli;

/**
 * Base for Exceptions thrown during parsing of a command-line.
 *
 * @version $Id: ParseException.java 1443102 2013-02-06 18:12:16Z tn $
 */
public class ParseException extends Exception {

    /**
     * Construct a new <code>ParseException</code>
     * with the specified detail message.
     *
     * @param message the detail message
     */
    ParseException(String message) {
        super(message);
    }
}
