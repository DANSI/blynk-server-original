package cc.blynk.cli;

/**
 * Thrown when more than one option in an option group
 * has been provided.
 *
 * @version $Id: AlreadySelectedException.java 1443102 2013-02-06 18:12:16Z tn $
 */
public class AlreadySelectedException extends ParseException {
    /**
     * This exception {@code serialVersionUID}.
     */
    private static final long serialVersionUID = 3674381532418544760L;

    /**
     * The option that triggered the exception.
     */
    private Option option;

    /**
     * Construct a new <code>AlreadySelectedException</code>
     * with the specified detail message.
     *
     * @param message the detail message
     */
    private AlreadySelectedException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>AlreadySelectedException</code>
     * for the specified option group.
     *
     * @param group  the option group already selected
     * @param option the option that triggered the exception
     * @since 1.2
     */
    AlreadySelectedException(OptionGroup group, Option option) {
        this("The option '" + option.getKey() + "' was specified but an option from this group "
                + "has already been selected: '" + group.getSelected() + "'");
        this.option = option;
    }

    /**
     * Returns the option that was added to the group and triggered the exception.
     *
     * @return the related option
     * @since 1.2
     */
    public Option getOption() {
        return option;
    }
}
