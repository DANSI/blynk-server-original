package cc.blynk.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents list of arguments parsed against a {@link Options} descriptor.
 * <p>
 * It allows querying of a boolean {@link #hasOption(String opt)},
 * in addition to retrieving the {@link #getOptionValue(String opt)}
 * for options requiring arguments.
 * <p>
 * Additionally, any left-over or unrecognized arguments,
 * are available for further processing.
 *
 * @version $Id: CommandLine.java 1786144 2017-03-09 11:34:57Z britter $
 */
public class CommandLine {

    /**
     * the processed options
     */
    private final List<Option> options = new ArrayList<>();

    /**
     * Creates a command line.
     */
    CommandLine() {
        // nothing to do
    }

    /**
     * Query to see if an option has been set.
     *
     * @param opt Short name of the option
     * @return true if set, false if not
     */
    public boolean hasOption(String opt) {
        return options.contains(resolveOption(opt));
    }


    /**
     * Retrieve the first argument, if any, of this option.
     *
     * @param opt the name of the option
     * @return Value of the argument if option is set, and has an argument,
     * otherwise null.
     */
    public String getOptionValue(String opt) {
        String[] values = getOptionValues(opt);

        return (values == null) ? null : values[0];
    }

    /**
     * Retrieves the array of values, if any, of an option.
     *
     * @param opt string name of the option
     * @return Values of the argument if option is set, and has an argument,
     * otherwise null.
     */
    private String[] getOptionValues(String opt) {
        List<String> values = new ArrayList<>();

        for (Option option : options) {
            if (opt.equals(option.getOpt()) || opt.equals(option.getLongOpt())) {
                values.addAll(option.getValuesList());
            }
        }

        return values.isEmpty() ? null : values.toArray(new String[0]);
    }

    /**
     * Retrieves the option object given the long or short option as a String
     *
     * @param opt short or long name of the option
     * @return Canonicalized option
     */
    private Option resolveOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);
        for (Option option : options) {
            if (opt.equals(option.getOpt())) {
                return option;
            }

            if (opt.equals(option.getLongOpt())) {
                return option;
            }

        }
        return null;
    }

    /**
     * Add an option to the command line.  The values of the option are stored.
     *
     * @param opt the processed option
     */
    void addOption(Option opt) {
        options.add(opt);
    }

}
