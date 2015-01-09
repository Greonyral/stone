package stone.util;

import java.util.HashMap;
import java.util.Map;

import stone.modules.Main;


/**
 * A global visible container holding all available options.
 * 
 * @author Nelphindal
 */
public class OptionContainer {

	private final Map<String, Option> options = new HashMap<>();


	private final Main main;
	private final Flag flags;

	/**
	 * Creates a new option container. All options will be backed up to given
	 * flags
	 * 
	 * @param flags
	 * @param main
	 */
	public OptionContainer(final Flag flags, final Main main) {
		this.flags = flags;
		this.main = main;
	}

	/**
	 * Registers a option
	 * 
	 * @param id
	 *            a unique id for this option
	 * @param tooltip
	 *            a description to be printed in the help message
	 * @param shortFlag
	 *            a unique printable char to register at flags or
	 *            {@link stone.util.Flag#NoShortFlag} to enable this option
	 * @param longFlag
	 *            a unique printable string to register at flags or
	 *            {@link stone.util.Flag#NoLongFlag} to enable this option
	 * @param argExpected
	 * @param option
	 *            the Option to be registered
	 * @see Flag#registerOption(String, String, char, String, boolean)
	 */
	public final void addOption(final String id, final String tooltip,
			char shortFlag, final String longFlag, boolean argExpected,
			final Option option) {
		flags.registerOption(id, tooltip, shortFlag, longFlag, argExpected);
		options.put(id, option);
		final String valueOfFlag = flags.getValue(id);
		if (valueOfFlag != null) {
			option.setByFlagValue(valueOfFlag);
		}
	}

	/**
	 * Copies given values into the values registered at this container
	 * 
	 * @param values
	 */
	public final void copyValues(final Map<String, String> values) {
		flags.setValue(values);
	}

	/**
	 * Sets the value of registered options matching the value of registered
	 * options
	 */
	public final void setValuesByParsedFlags() {
		for (final Map.Entry<String, Option> entry : options.entrySet()) {
			if (flags.isEnabled(entry.getKey())) {
				entry.getValue().value(flags.getValue(entry.getKey()));
			}
		}

	}

	/**
	 * @param section
	 * @param key
	 * @param defaultValue
	 * @return the value of the setting (file)
	 * @see main.Main#getConfigValue(String, String, String)
	 */
	final String getConfigValue(final String section, final String key,
			final String defaultValue) {
		return main.getConfigValue(section, key, defaultValue);
	}

	/**
	 * Sets an
	 * 
	 * @param section
	 * @param key
	 * @param value
	 * @see main.Main#setConfigValue(String, String, String)
	 */
	final void setConfigValue(final String section, final String key,
			final String value) {
		main.setConfigValue(section, key, value);
	}

}
