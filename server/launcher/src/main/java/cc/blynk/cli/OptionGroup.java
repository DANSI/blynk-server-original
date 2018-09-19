package cc.blynk.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A group of mutually exclusive options.
 *
 * @version $Id: OptionGroup.java 1749596 2016-06-21 20:27:06Z britter $
 */
public class OptionGroup {

    /**
     * hold the options
     */
    private final Map<String, Option> optionMap = new LinkedHashMap<>();

    /**
     * the name of the selected option
     */
    private String selected;

    /**
     * specified whether this group is required
     */
    private boolean required;

    /**
     * Set the selected option of this group to <code>name</code>.
     *
     * @param option the option that is selected
     * @throws AlreadySelectedException if an option from this group has
     *                                  already been selected.
     */
    void setSelected(Option option) throws AlreadySelectedException {
        if (option == null) {
            // reset the option previously selected
            selected = null;
            return;
        }

        // if no option has already been selected or the
        // same option is being reselected then set the
        // selected member variable
        if (selected == null || selected.equals(option.getKey())) {
            selected = option.getKey();
        } else {
            throw new AlreadySelectedException(this, option);
        }
    }

    /**
     * @return the selected option name
     */
    String getSelected() {
        return selected;
    }

    /**
     * Returns whether this option group is required.
     *
     * @return whether this option group is required
     */
    public boolean isRequired() {
        return required;
    }

}
