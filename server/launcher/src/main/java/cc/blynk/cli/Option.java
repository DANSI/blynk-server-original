package cc.blynk.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a single command-line option.  It maintains
 * information regarding the short-name of the option, the long-name,
 * if any exists, a flag indicating if an argument is required for
 * this option, and a self-documenting description of the option.
 * <p>
 * An Option is not created independently, but is created through
 * an instance of {@link Options}. An Option is required to have
 * at least a short or a long-name.
 * <p>
 * <b>Note:</b> once an {@link Option} has been added to an instance
 * of {@link Options}, it's required flag may not be changed anymore.
 *
 * @version $Id: Option.java 1756753 2016-08-18 10:18:43Z britter $
 */
public class Option implements Cloneable {
    /**
     * constant that specifies the number of argument values has not been specified
     */
    private static final int UNINITIALIZED = -1;

    /**
     * constant that specifies the number of argument values is infinite
     */
    static final int UNLIMITED_VALUES = -2;

    /**
     * the name of the option
     */
    private final String opt;

    /**
     * the long representation of the option
     */
    private String longOpt;

    /**
     * description of the option
     */
    private final String description;

    /**
     * specifies whether this option is required to be present
     */
    private boolean required;

    /**
     * specifies whether the argument value of this Option is optional
     */
    private boolean optionalArg;

    /**
     * the number of argument values this option can have
     */
    private int numberOfArgs = UNINITIALIZED;

    /**
     * the type of this Option
     */
    private Class<?> type = String.class;

    /**
     * the list of argument values
     **/
    private List<String> values = new ArrayList<>();

    /**
     * the character that is the value separator
     */
    private char valuesep;

    /**
     * Creates an Option using the specified parameters.
     *
     * @param opt         short representation of the option
     * @param longOpt     the long representation of the option
     * @param hasArg      specifies whether the Option takes an argument or not
     * @param description describes the function of the option
     * @throws IllegalArgumentException if there are any non valid
     *                                  Option characters in <code>opt</code>.
     */
    Option(String opt, String longOpt, boolean hasArg, String description)
            throws IllegalArgumentException {
        // ensure that the option is valid
        OptionValidator.validateOption(opt);

        this.opt = opt;
        this.longOpt = longOpt;

        // if hasArg is set then the number of arguments is 1
        if (hasArg) {
            this.numberOfArgs = 1;
        }

        this.description = description;
    }

    /**
     * Returns the id of this Option.  This is only set when the
     * Option shortOpt is a single character.  This is used for switch
     * statements.
     *
     * @return the id of this Option
     */
    public int getId() {
        return getKey().charAt(0);
    }

    /**
     * Returns the 'unique' Option identifier.
     *
     * @return the 'unique' Option identifier
     */
    String getKey() {
        // if 'opt' is null, then it is a 'long' option
        return (opt == null) ? longOpt : opt;
    }

    /**
     * Retrieve the name of this Option.
     * <p>
     * It is this String which can be used with
     * {@link CommandLine#hasOption(String opt)} and
     * {@link CommandLine#getOptionValue(String opt)} to check
     * for existence and argument.
     *
     * @return The name of this option
     */
    String getOpt() {
        return opt;
    }

    /**
     * Retrieve the type of this Option.
     *
     * @return The type of this option
     */
    public Object getType() {
        return type;
    }

    /**
     * Sets the type of this Option.
     *
     * @param type the type of this Option
     * @since 1.3
     */
    public void setType(Class<?> type) {
        this.type = type;
    }

    /**
     * Retrieve the long name of this Option.
     *
     * @return Long name of this option, or null, if there is no long name
     */
    String getLongOpt() {
        return longOpt;
    }

    /**
     * @return whether this Option can have an optional argument
     */
    private boolean hasOptionalArg() {
        return optionalArg;
    }

    /**
     * Query to see if this Option has a long name
     *
     * @return boolean flag indicating existence of a long name
     */
    boolean hasLongOpt() {
        return longOpt != null;
    }

    /**
     * Query to see if this Option requires an argument
     *
     * @return boolean flag indicating if an argument is required
     */
    boolean hasArg() {
        return numberOfArgs > 0 || numberOfArgs == UNLIMITED_VALUES;
    }

    /**
     * Retrieve the self-documenting description of this Option
     *
     * @return The string description of this option
     */
    String getDescription() {
        return description;
    }

    /**
     * Query to see if this Option is mandatory
     *
     * @return boolean flag indicating whether this Option is mandatory
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Query to see if this Option can take many values.
     *
     * @return boolean flag indicating if multiple values are allowed
     */
    private boolean hasArgs() {
        return numberOfArgs > 1 || numberOfArgs == UNLIMITED_VALUES;
    }


    /**
     * Returns the value separator character.
     *
     * @return the value separator character.
     */
    private char getValueSeparator() {
        return valuesep;
    }

    /**
     * Return whether this Option has specified a value separator.
     *
     * @return whether this Option has specified a value separator.
     * @since 1.1
     */
    private boolean hasValueSeparator() {
        return valuesep > 0;
    }

    /**
     * Returns the number of argument values this Option can take.
     * <p>
     * <p>
     * A value equal to the constant {@link #UNINITIALIZED} (= -1) indicates
     * the number of arguments has not been specified.
     * A value equal to the constant {@link #UNLIMITED_VALUES} (= -2) indicates
     * that this options takes an unlimited amount of values.
     * </p>
     *
     * @return num the number of argument values
     * @see #UNINITIALIZED
     * @see #UNLIMITED_VALUES
     */
    int getArgs() {
        return numberOfArgs;
    }

    /**
     * Adds the specified value to this Option.
     *
     * @param value is a/the value of this Option
     */
    void addValueForProcessing(String value) {
        if (numberOfArgs == UNINITIALIZED) {
            throw new RuntimeException("NO_ARGS_ALLOWED");
        }
        processValue(value);
    }

    /**
     * Processes the value.  If this Option has a value separator
     * the value will have to be parsed into individual tokens.  When
     * n-1 tokens have been processed and there are more value separators
     * in the value, parsing is ceased and the remaining characters are
     * added as a single token.
     *
     * @param value The String to be processed.
     * @since 1.0.1
     */
    private void processValue(String value) {
        // this Option has a separator character
        if (hasValueSeparator()) {
            // get the separator character
            char sep = getValueSeparator();

            // store the index for the value separator
            int index = value.indexOf(sep);

            // while there are more value separators
            while (index != -1) {
                // next value to be added
                if (values.size() == numberOfArgs - 1) {
                    break;
                }

                // store
                add(value.substring(0, index));

                // parse
                value = value.substring(index + 1);

                // get new index
                index = value.indexOf(sep);
            }
        }

        // store the actual value or the last value that has been parsed
        add(value);
    }

    /**
     * Add the value to this Option.  If the number of arguments
     * is greater than zero and there is enough space in the list then
     * add the value.  Otherwise, throw a runtime exception.
     *
     * @param value The value to be added to this Option
     * @since 1.0.1
     */
    private void add(String value) {
        if (!acceptsArg()) {
            throw new RuntimeException("Cannot add value, list full.");
        }

        // store value
        values.add(value);
    }

    /**
     * Returns the specified value of this Option or
     * <code>null</code> if there is no value.
     *
     * @return the value/first value of this Option or
     * <code>null</code> if there is no value.
     */
    public String getValue() {
        return hasNoValues() ? null : values.get(0);
    }

    /**
     * Return the values of this Option as a String array
     * or null if there are no values
     *
     * @return the values of this Option as a String array
     * or null if there are no values
     */
    public String[] getValues() {
        return hasNoValues() ? null : values.toArray(new String[0]);
    }

    /**
     * @return the values of this Option as a List
     * or null if there are no values
     */
    List<String> getValuesList() {
        return values;
    }

    /**
     * Returns whether this Option has any values.
     *
     * @return whether this Option has any values.
     */
    private boolean hasNoValues() {
        return values.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Option option = (Option) o;


        if (opt != null ? !opt.equals(option.opt) : option.opt != null) {
            return false;
        }
        return longOpt != null ? longOpt.equals(option.longOpt) : option.longOpt == null;
    }

    @Override
    public int hashCode() {
        int result;
        result = opt != null ? opt.hashCode() : 0;
        result = 31 * result + (longOpt != null ? longOpt.hashCode() : 0);
        return result;
    }

    /**
     * A rather odd clone method - due to incorrect code in 1.0 it is public
     * and in 1.1 rather than throwing a CloneNotSupportedException it throws
     * a RuntimeException so as to maintain backwards compat at the API level.
     * <p>
     * After calling this method, it is very likely you will want to call
     * clearValues().
     *
     * @return a clone of this Option instance
     * @throws RuntimeException if a {@link CloneNotSupportedException} has been thrown
     *                          by {@code super.clone()}
     */
    @Override
    public Object clone() {
        try {
            Option option = (Option) super.clone();
            option.values = new ArrayList<>(values);
            return option;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("A CloneNotSupportedException was thrown: " + cnse.getMessage());
        }
    }

    /**
     * Tells if the option can accept more arguments.
     *
     * @return false if the maximum number of arguments is reached
     * @since 1.3
     */
    boolean acceptsArg() {
        return (hasArg() || hasArgs() || hasOptionalArg()) && (numberOfArgs <= 0 || values.size() < numberOfArgs);
    }

    /**
     * Tells if the option requires more arguments to be valid.
     *
     * @return false if the option doesn't require more arguments
     * @since 1.3
     */
    boolean requiresArg() {
        if (optionalArg) {
            return false;
        }
        if (numberOfArgs == UNLIMITED_VALUES) {
            return values.isEmpty();
        }
        return acceptsArg();
    }
}
