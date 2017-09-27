package cc.blynk.cli;

/**
 * Exception thrown during parsing signalling an unrecognized
 * option was seen.
 *
 * @version $Id: UnrecognizedOptionException.java 1443102 2013-02-06 18:12:16Z tn $
 */
public class UnrecognizedOptionException extends ParseException {
    /**
     * This exception {@code serialVersionUID}.
     */
    private static final long serialVersionUID = -252504690284625623L;

    /**
     * The  unrecognized option
     */
    private String option;

    /**
     * Construct a new <code>UnrecognizedArgumentException</code>
     * with the specified detail message.
     *
     * @param message the detail message
     */
    private UnrecognizedOptionException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>UnrecognizedArgumentException</code>
     * with the specified option and detail message.
     *
     * @param message the detail message
     * @param option  the unrecognized option
     * @since 1.2
     */
    UnrecognizedOptionException(String message, String option) {
        this(message);
        this.option = option;
    }

    /**
     * Returns the unrecognized option.
     *
     * @return the related option
     * @since 1.2
     */
    public String getOption() {
        return option;
    }
}
