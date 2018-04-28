package cc.blynk.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry-point into the library.
 * <p>
 * Options represents a collection of {@link Option} objects, which
 * describe the possible options for a command-line.
 * <p>
 * It may flexibly parse long and short options, with or without
 * values.  Additionally, it may parse only a portion of a commandline,
 * allowing for flexible multi-stage parsing.
 *
 *
 * @version $Id: Options.java 1754332 2016-07-27 18:47:57Z britter $
 */
public class Options {

    /**
     * a map of the options with the character key
     */
    private final Map<String, Option> shortOpts = new LinkedHashMap<>();

    /**
     * a map of the options with the long key
     */
    private final Map<String, Option> longOpts = new LinkedHashMap<>();

    /**
     * a map of the required options
     */
    // N.B. This can contain either a String (addOption) or an OptionGroup (addOptionGroup)
    // TODO this seems wrong
    private final List<Object> requiredOpts = new ArrayList<>();

    /**
     * a map of the option groups
     */
    private final Map<String, OptionGroup> optionGroups = new LinkedHashMap<>();


    /**
     * Lists the OptionGroups that are members of this Options instance.
     *
     * @return a Collection of OptionGroup instances.
     */
    Collection<OptionGroup> getOptionGroups() {
        return new HashSet<>(optionGroups.values());
    }

    /**
     * Add an option that only contains a short-name.
     * <p>
     * <p>
     * It may be specified as requiring an argument.
     * </p>
     *
     * @param opt         Short single-character name of the option.
     * @param hasArg      flag signally if an argument is required after this option
     * @param description Self-documenting description
     * @return the resulting Options instance
     */
    public Options addOption(String opt, boolean hasArg, String description) {
        addOption(opt, null, hasArg, description);
        return this;
    }

    /**
     * Add an option that contains a short-name and a long-name.
     * <p>
     * <p>
     * It may be specified as requiring an argument.
     * </p>
     *
     * @param opt         Short single-character name of the option.
     * @param longOpt     Long multi-character name of the option.
     * @param hasArg      flag signally if an argument is required after this option
     * @param description Self-documenting description
     */
    private void addOption(String opt, String longOpt, boolean hasArg, String description) {
        addOption(new Option(opt, longOpt, hasArg, description));
    }

    /**
     * Adds an option instance
     *
     * @param opt the option that is to be added
     */
    private void addOption(Option opt) {
        String key = opt.getKey();

        // add it to the long option list
        if (opt.hasLongOpt()) {
            longOpts.put(opt.getLongOpt(), opt);
        }

        // if the option is required add it to the required list
        if (opt.isRequired()) {
            requiredOpts.remove(key);
            requiredOpts.add(key);
        }

        shortOpts.put(key, opt);
    }

    /**
     * Returns the required options.
     *
     * @return read-only List of required options
     */
    List getRequiredOptions() {
        return Collections.unmodifiableList(requiredOpts);
    }

    /**
     * Retrieve the {@link Option} matching the long or short name specified.
     * <p>
     * <p>
     * The leading hyphens in the name are ignored (up to 2).
     * </p>
     *
     * @param opt short or long name of the {@link Option}
     * @return the option represented by opt
     */
    Option getOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);

        if (shortOpts.containsKey(opt)) {
            return shortOpts.get(opt);
        }

        return longOpts.get(opt);
    }

    /**
     * Returns the options with a long name starting with the name specified.
     *
     * @param opt the partial name of the option
     * @return the options matching the partial name specified, or an empty list if none matches
     * @since 1.3
     */
    List<String> getMatchingOptions(String opt) {
        opt = Util.stripLeadingHyphens(opt);

        List<String> matchingOpts = new ArrayList<>();

        // for a perfect match return the single option only
        if (longOpts.keySet().contains(opt)) {
            return Collections.singletonList(opt);
        }

        for (String longOpt : longOpts.keySet()) {
            if (longOpt.startsWith(opt)) {
                matchingOpts.add(longOpt);
            }
        }

        return matchingOpts;
    }

    /**
     * Returns whether the named {@link Option} is a member of this {@link Options}.
     *
     * @param opt short or long name of the {@link Option}
     * @return true if the named {@link Option} is a member of this {@link Options}
     */
    boolean hasOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);

        return shortOpts.containsKey(opt) || longOpts.containsKey(opt);
    }

    /**
     * Returns whether the named {@link Option} is a member of this {@link Options}.
     *
     * @param opt long name of the {@link Option}
     * @return true if the named {@link Option} is a member of this {@link Options}
     * @since 1.3
     */
    boolean hasLongOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);

        return longOpts.containsKey(opt);
    }

    /**
     * Returns whether the named {@link Option} is a member of this {@link Options}.
     *
     * @param opt short name of the {@link Option}
     * @return true if the named {@link Option} is a member of this {@link Options}
     * @since 1.3
     */
    boolean hasShortOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);

        return shortOpts.containsKey(opt);
    }

    /**
     * Returns the OptionGroup the <code>opt</code> belongs to.
     *
     * @param opt the option whose OptionGroup is being queried.
     * @return the OptionGroup if <code>opt</code> is part of an OptionGroup, otherwise return null
     */
    OptionGroup getOptionGroup(Option opt) {
        return optionGroups.get(opt.getKey());
    }

}
