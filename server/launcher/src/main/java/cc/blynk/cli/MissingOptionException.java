package cc.blynk.cli;

import java.util.Iterator;
import java.util.List;

/**
 * Thrown when a required option has not been provided.
 *
 * @version $Id: MissingOptionException.java 1443102 2013-02-06 18:12:16Z tn $
 */
class MissingOptionException extends ParseException {
    /**
     * This exception {@code serialVersionUID}.
     */
    private static final long serialVersionUID = 8161889051578563249L;

    /**
     * Construct a new <code>MissingSelectedException</code>
     * with the specified detail message.
     *
     * @param message the detail message
     */
    private MissingOptionException(String message) {
        super(message);
    }

    /**
     * Constructs a new <code>MissingSelectedException</code> with the
     * specified list of missing options.
     *
     * @param missingOptions the list of missing options and groups
     * @since 1.2
     */
    MissingOptionException(List missingOptions) {
        this(createMessage(missingOptions));

    }

    /**
     * Build the exception message from the specified list of options.
     *
     * @param missingOptions the list of missing options and groups
     * @since 1.2
     */
    private static String createMessage(List<?> missingOptions) {
        StringBuilder buf = new StringBuilder("Missing required option");
        buf.append(missingOptions.size() == 1 ? "" : "s");
        buf.append(": ");

        Iterator<?> it = missingOptions.iterator();
        while (it.hasNext()) {
            buf.append(it.next());
            if (it.hasNext()) {
                buf.append(", ");
            }
        }

        return buf.toString();
    }
}
