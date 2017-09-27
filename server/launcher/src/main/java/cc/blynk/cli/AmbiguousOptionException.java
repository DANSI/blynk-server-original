package cc.blynk.cli;

import java.util.Collection;
import java.util.Iterator;

/**
 * Exception thrown when an option can't be identified from a partial name.
 *
 * @version $Id: AmbiguousOptionException.java 1669814 2015-03-28 18:09:26Z britter $
 * @since 1.3
 */
class AmbiguousOptionException extends UnrecognizedOptionException {
    /**
     * This exception {@code serialVersionUID}.
     */
    private static final long serialVersionUID = 5829816121277947229L;

    /**
     * Constructs a new AmbiguousOptionException.
     *
     * @param option          the partial option name
     * @param matchingOptions the options matching the name
     */
    AmbiguousOptionException(String option, Collection<String> matchingOptions) {
        super(createMessage(option, matchingOptions), option);
    }

    /**
     * Build the exception message from the specified list of options.
     */
    private static String createMessage(String option, Collection<String> matchingOptions) {
        StringBuilder buf = new StringBuilder("Ambiguous option: '");
        buf.append(option);
        buf.append("'  (could be: ");

        Iterator<String> it = matchingOptions.iterator();
        while (it.hasNext()) {
            buf.append("'");
            buf.append(it.next());
            buf.append("'");
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(")");

        return buf.toString();
    }
}
