package stone.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Class to parse parameters passed to a program using the command line.
 * Implemented as skeleton.
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

	private static final Flag instance = new Flag();

	/**
	 * @return the instance to use.
	 */
	public final static Flag getInstance() {
		return Flag.instance;
	}

	private String[] args;
	private String unknownOption;
	private boolean dirty = true;

	private final Set<String> enabledFlags = new HashSet<>();
	private final Map<String, String> help = new HashMap<>();
	private final Map<String, String> idToLong = new HashMap<>();
	private final Map<String, Integer> idToShort = new HashMap<>();
	private final Map<String, String> longToId = new HashMap<>();
	private final Collection<String> registeredFlags = new ArrayDeque<>();
	private final Map<Integer, String> shortToId = new HashMap<>();
	private final Map<String, Integer> state = new HashMap<>();

	private final Map<String, String> values = new HashMap<>();


	/**
	 * Creates a new instance
	 */
	private Flag() {
	}

	/**
	 * @return last argument of {@link #parse(String[])}
	 */
	public final String[] getArgs() {
		return this.args;
	}

	/**
	 * Returns the value assigned currently in <i>this</i> instance for given
	 * <i>flagId</i>.
	 * 
	 * @param flagId
	 *            identifying name of a switch
	 * @return assigned value
	 */
	public final String getValue(final String flagId) {
		return this.values.get(flagId);
	}

	/**
	 * Returns a map containing all known flagIds with their values.
	 * 
	 * @return a map containing all known flagIds with their values
	 */
	public final Map<String, String> getValues() {
		return this.values;
	}

	/**
	 * Checks if given <i>flagId</i> was parsed or set to be enabled.
	 * 
	 * @param flagId
	 *            identifying name of a switch
	 * @return <i>true</i> if <i>flagId</i> is enabled
	 */
	public final boolean isEnabled(final String flagId) {
		if (this.dirty) {
			parse();
		}
		return this.enabledFlags.contains(flagId);

	}

	/**
	 * Parses given <i>args</i> and assigns the value to the registered flagIds.
	 * 
	 * @param args
	 *            parameters to parse
	 * @return <i>true</i> if <i>args</i> were valid and parsing successful
	 */
	public final boolean parse(@SuppressWarnings("hiding") final String[] args) {
		this.args = args;
		this.unknownOption = null;
		parse();
		return this.unknownOption == null;
	}

	/**
	 * Processes argument of {@link #parse(String[])} again
	 * 
	 * @return <i>true</i> if <i>args</i> were valid and parsing successful
	 * @see #parse(String[])
	 */
	public final boolean parseWOError() {
		if (this.dirty) {
			return parse();
		}
		return this.unknownOption == null;
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
		for (final String fOption : this.registeredFlags) {
			final char shortF = (char) this.idToShort.get(fOption).intValue();
			final String longF = this.idToLong.get(fOption);
			if ((shortF == Flag.NoShortFlag) && (longF == Flag.NoLongFlag)) {
				continue;
			}
			final int state_ = this.state.get(fOption);
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

		for (final String fOption : this.registeredFlags) {
			final String longF = this.idToLong.get(fOption);
			final char shortF = (char) this.idToShort.get(fOption).intValue();
			if ((shortF == Flag.NoShortFlag) && (longF == Flag.NoLongFlag)) {
				continue;
			}
			final String helpText = this.help.get(fOption);
			outPart2 += "\n"
					+ String.format("%s %-16s : %s",
							shortF == Flag.NoShortFlag ? "  " : "-" + shortF,
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
	public final void registerOption(final String flagId, final String tooltip,
			char shortFlag, final String longFlag, boolean argExpected) {
		this.dirty = true;
		if (shortFlag != Flag.NoShortFlag) {
			this.shortToId.put((int) shortFlag, flagId);
		}
		this.idToShort.put(flagId, (int) shortFlag);
		if (longFlag != Flag.NoLongFlag) {
			this.longToId.put(longFlag, flagId);
		}
		this.idToLong.put(flagId, longFlag);

		this.help.put(flagId, tooltip);
		this.registeredFlags.add(flagId);
		int s = 0;
		if (!argExpected) {
			s |= Flag.PRIMITIVE;
		}
		this.state.put(flagId, s);
		this.unknownOption = null;
	}

	/**
	 * Sets all flagIds given in this map as enabled and copies the values.
	 * 
	 * @param values
	 *            map with set of pairs of flagId, value
	 */
	public final void setValue(
			@SuppressWarnings("hiding") final Map<String, String> values) {
		this.values.putAll(values);
		this.enabledFlags.addAll(values.keySet());
	}

	/**
	 * Sets a specific flagId to hold given value.
	 * 
	 * @param flagId
	 *            an unique id to identify this option
	 * @param value
	 *            new value
	 */
	public final void setValue(final String flagId, final String value) {
		this.values.put(flagId, value);
	}

	/**
	 * @return the part of the given <i>args</i> by {@link #parse(String[])}
	 *         which created the first error
	 */
	public final String unknownOption() {
		return "Unknown Option " + this.unknownOption;
	}

	private final boolean parse() {
		boolean missingPrefix = false;
		final Set<String> pendingFlags = new HashSet<>();
		if (!this.dirty
				|| ((this.args == null) && (this.unknownOption == null))) {
			return this.unknownOption == null;
		}
		this.dirty = false;
		for (int i = 0, ci = -1; i < this.args.length; i++) {
			final String id;
			if ((ci < 0) && this.args[i].startsWith("--")) {
				id = this.longToId.get(this.args[i].substring(2));
			} else if ((ci < 0) && this.args[i].startsWith("-")) {
				id = this.shortToId.get((int) this.args[i].charAt(1));
				ci = 1;
			} else {
				final char c = this.args[i].charAt(++ci);
				id = this.shortToId.get((int) c);
				missingPrefix = true;
			}
			if (id == null) {
				if (missingPrefix) {
					// disable all flags parsed since missingPrefix-flag has
					// been enabled

					pendingFlags.clear();
					missingPrefix = false;
				}
				if (this.unknownOption == null) {
					this.unknownOption = this.args[i];
				}
				ci = -1;
				missingPrefix = false;
				continue;
			}
			final String value;
			@SuppressWarnings("hiding")
			final Integer state = this.state.get(id);
			if (state == null) {
				if (missingPrefix) {
					// disable all flags parsed since missingPrefix-flag has
					// been enabled
					pendingFlags.clear();
					missingPrefix = false;
				}
				if (this.unknownOption == null) {
					this.unknownOption = this.args[i];
				}
				ci = -1;
				missingPrefix = false;
				continue;
			} else if ((state & Flag.PRIMITIVE) == 0) {
				if ((this.args[i].length() < (ci + 1))
						|| ((i + 1) == this.args.length)) {
					if (missingPrefix) {
						// disable all flags parsed since missingPrefix-flag has
						// been enabled
						pendingFlags.clear();
						missingPrefix = false;
					}
					if (this.unknownOption == null) {
						this.unknownOption = this.args[i];
					}
					ci = -1;
					missingPrefix = false;
					continue;
				}
				value = this.args[++i];
				ci = -1;
			} else {
				value = null;
			}
			if (missingPrefix) {
				pendingFlags.add(id);
			} else {
				this.enabledFlags.add(id);
			}
			this.values.put(id, value);
			if (ci >= 0) {
				if ((ci + 1) == this.args[i].length()) {
					ci = -1;
					this.enabledFlags.addAll(pendingFlags);
					pendingFlags.clear();
					missingPrefix = false;
				} else {
					i--;
				}
			}
		}
		if (this.unknownOption == null) {
			this.args = null;
			return true;
		}
		return false;
	}
}
