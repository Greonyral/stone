package stone.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Class to parse parameters passed to a program using the command line.
 * 
 * @author Nelphindal
 */
public class Flag {

	/**
	 * 
	 */
	public static final String NoLongFlag = new String();
	/**
	 * 
	 */
	public static final char NoShortFlag = 5;
	private static final int PRIMITIVE = 1;

	public final static Flag getInstance() {
		return Flag.instance;
	}

	private final Set<String> enabledFlags = new HashSet<>();
	private final Map<String, String> help = new HashMap<>();
	private final Map<String, String> idToLong = new HashMap<>();
	private final Map<String, Integer> idToShort = new HashMap<>();
	private final Map<String, String> longToId = new HashMap<>();
	private final Collection<String> registeredFlags = new ArrayDeque<>();
	private final Map<Integer, String> shortToId = new HashMap<>();
	private final Map<String, Integer> state = new HashMap<>();

	private final Map<String, String> values = new HashMap<>();

	private static final Flag instance = new Flag();

	/**
	 * Creates a new instance
	 */
	private Flag() {
	}

	/**
	 * Returns the value assigned currently in <i>this</i> instance for given
	 * <i>flagId</i>.
	 * 
	 * @param flagId
	 * @return assigned value
	 */
	public final String getValue(final String flagId) {
		return values.get(flagId);
	}

	/**
	 * Returns a map containing all known flagIds with their values.
	 * 
	 * @return a map containing all known flagIds with their values
	 */
	public final Map<String, String> getValues() {
		return values;
	}

	/**
	 * Checks if given <i>flagId</i> was parsed or set to be enabled.
	 * 
	 * @param flagId
	 * @return <i>true</i> if <i>flagId</i> is enabled
	 */
	public final boolean isEnabled(final String flagId) {
		return enabledFlags.contains(flagId);

	}

	/**
	 * Parses given <i>args</i> and assigns the value to the registered flagIds.
	 * 
	 * @param args
	 *            parameters to parse
	 * @return <i>true</i> if <i>args</i> were valid and parsing successful
	 */
	public final boolean parse(final String[] args) {
		for (int i = 0, ci = -1; i < args.length; i++) {
			final String id;
			if ((ci < 0) && args[i].startsWith("--")) {
				id = longToId.get(args[i].substring(2));
			} else if ((ci < 0) && args[i].startsWith("-")) {
				id = shortToId.get((int) args[i].charAt(1));
				ci = 1;
			} else {
				final char c = args[i].charAt(++ci);
				id = shortToId.get((int) c);
			}
			if (id == null) {
				System.err.println("unknown option " + args[i]);
				System.err.println(printHelp());
				return false;
			}
			final String value;
			if ((state.get(id) & Flag.PRIMITIVE) == 0) {
				if (args[i].length() > (ci + 1)) {
					System.err.println("unknown option " + args[i]);
					System.err.println(printHelp());
					return false;
				}
				value = args[++i];
				ci = -1;
			} else {
				value = null;
			}
			enabledFlags.add(id);
			values.put(id, value);
			if (ci >= 0) {
				if ((ci + 1) == args[i].length()) {
					ci = -1;
				} else {
					i--;
				}
			}
		}
		return true;
	}

	/**
	 * Generates a message containing all registered flags, allowing the user to
	 * choose the parameters to pass to the program.
	 * 
	 * @return a help message
	 */
	public final String printHelp() {
		String outPart1 = "", outPart2 = "";
		final String outPart3 = "";
		for (final String fOption : registeredFlags) {
			final char shortF = (char) idToShort.get(fOption).intValue();
			final String longF = idToLong.get(fOption);
			if ((shortF == Flag.NoShortFlag) && (longF == Flag.NoLongFlag)) {
				continue;
			}
			final int state_ = state.get(fOption);
			final boolean primi = (state_ & Flag.PRIMITIVE) != 0;
			outPart1 += " [";
			if (shortF != Flag.NoShortFlag) {
				outPart1 += " -" + shortF;
				if (longF != Flag.NoLongFlag) {
					outPart1 += " |";
				}
			}
			if (longF != Flag.NoLongFlag) {
				outPart1 += " --" + longF;
			}
			if (!primi) {
				if (!primi) {
					outPart1 += " <value>";
				}
			}
			outPart1 += " ]";
		}

		for (final String fOption : registeredFlags) {
			final String longF = idToLong.get(fOption);
			final char shortF = (char) idToShort.get(fOption).intValue();
			if ((shortF == Flag.NoShortFlag) && (longF == Flag.NoLongFlag)) {
				continue;
			}
			final String helpText = help.get(fOption);
			outPart2 +=
					"\n"
							+ String.format("%s %-16s : %s",
									shortF == Flag.NoShortFlag ? "  "
											: "-" + shortF,
											longF == null ? "" : "--" + longF,
													helpText == null ? "" : helpText);
		}
		return outPart1 + outPart2 + outPart3;
	}

	/**
	 * Registers a new option
	 * 
	 * @param flagId
	 *            an unique id to identify this option
	 * @param tooltip
	 *            a description to be printed in a help message to explain this
	 *            option
	 * @param shortFlag
	 *            a unique printable char to enable this option or
	 *            {@link #NoShortFlag}
	 * @param longFlag
	 *            a unique string literal to enable this option or
	 *            {@link #NoLongFlag}
	 * @param argExpected
	 *            <i>true</i>if this option needs a additional value
	 */
	public final void registerOption(final String flagId,
			final String tooltip, char shortFlag, final String longFlag,
			boolean argExpected) {
		if (shortFlag != Flag.NoShortFlag) {
			shortToId.put((int) shortFlag, flagId);
		}
		idToShort.put(flagId, (int) shortFlag);
		if (longFlag != Flag.NoLongFlag) {
			longToId.put(longFlag, flagId);
		}
		idToLong.put(flagId, longFlag);

		help.put(flagId, tooltip);
		registeredFlags.add(flagId);
		int s = 0;
		if (!argExpected) {
			s |= Flag.PRIMITIVE;
		}
		state.put(flagId, s);
	}

	/**
	 * Sets all flagIds given in this map as enabled and copies the values.
	 * 
	 * @param values
	 *            map with set of pairs of flagId, value
	 */
	public final void setValue(final Map<String, String> values) {
		this.values.putAll(values);
		enabledFlags.addAll(values.keySet());
	}

	/**
	 * Sets a specific flagId to hold given value.
	 * 
	 * @param flagId
	 * @param value
	 *            new value
	 */
	public final void setValue(final String flagId, final String value) {
		values.put(flagId, value);
	}
}
