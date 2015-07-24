package stone.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import stone.io.GUI;
import stone.io.KeyEventHandler;


/**
 * Superclass for all Options
 * 
 * @author Nelphindal
 */
public abstract class Option {

	class ColorEntry {
		Color fg, bg;
	}

	private final String desc, toolTip, section;
	private final String defaultValue;
	/**
	 * The key identifier for
	 * {@link stone.modules.Main#setConfigValue(String, String, String)} and
	 * {@link stone.modules.Main#getConfigValue(String, String, String)}
	 */
	protected final String key;
	/**
	 * identifier at flags
	 * 
	 * @see stone.util.OptionContainer
	 */
	protected final String name;
	private JPanel panel;
	/** gui used to display */
	protected stone.io.GUIInterface gui;

	private final Map<JTextArea, ColorEntry> colorsTextArea = new HashMap<>();
	private final OptionContainer optionContainer;
	private String value;

	/**
	 * Creates a new option and registers it at the OptionContainer
	 * 
	 * @param optionContainer
	 *            instance of {@link OptionContainer} to use
	 * @param name
	 *            a unique identifier for this option to register at
	 *            OptionContainer
	 * @param toolTip
	 *            a description for <i>this</i> option to use it for example as
	 *            a tool-tip in any GUIs
	 * @param guiDescription
	 *            a short string usable to label <i>this</i> option
	 * @param shortFlag
	 *            a unique printable char to register at flags or
	 *            {@link stone.util.Flag#NoShortFlag} to enable this option
	 * @param longFlag
	 *            a unique printable string to register at flags or
	 *            {@link stone.util.Flag#NoLongFlag} to enable this option@see
	 *            util.OptionContainer#addOption(String, String, char, String,
	 *            boolean, Option)
	 * @param argExpected
	 *            <i>true</i> if <i>this</i> option consists of a key-value-pair
	 */
	@SuppressWarnings("hiding")
	protected Option(final OptionContainer optionContainer, final String name,
			final String toolTip, final String guiDescription, char shortFlag,
			final String longFlag, boolean argExpected) {
		this(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, argExpected, null, null, null);
	}

	/**
	 * Creates a new option and registers it at the OptionContainer
	 * 
	 * @param optionContainer
	 *            instance of {@link OptionContainer} to use
	 * @param name
	 *            a unique identifier for this option to register at
	 *            OptionContainer
	 * @param toolTip
	 *            a description for <i>this</i> option to use it for example as
	 *            a tool-tip in any GUIs
	 * @param guiDescription
	 *            a short string usable to label <i>this</i> option
	 * @param shortFlag
	 *            a unique printable char to register at flags or
	 *            {@link stone.util.Flag#NoShortFlag} to enable this option
	 * @param longFlag
	 *            a unique printable string to register at flags or
	 *            {@link stone.util.Flag#NoLongFlag} to enable this option
	 * @param argExpected
	 *            <i>true</i> if <i>this</i> option consists of a key-value-pair
	 * @param section
	 *            the section identifier for this option, to access by
	 *            {@link stone.modules.Main#getConfigValue(String, String, String)}
	 *            and
	 *            {@link stone.modules.Main#setConfigValue(String, String, String)}
	 * @param key
	 *            the key identifier for this option, to access by
	 *            {@link stone.modules.Main#getConfigValue(String, String, String)}
	 *            and
	 *            {@link stone.modules.Main#setConfigValue(String, String, String)}
	 * @param defaultValue
	 *            the default value for
	 *            {@link stone.modules.Main#getConfigValue(String, String, String)}
	 *            * @see stone.util.OptionContainer#addOption(String, String,
	 *            char, String, boolean, Option)
	 */
	@SuppressWarnings("hiding")
	protected Option(final OptionContainer optionContainer, final String name,
			final String toolTip, final String guiDescription, char shortFlag,
			final String longFlag, boolean argExpected, final String section,
			final String key, final String defaultValue) {
		this.desc = guiDescription;
		this.name = name;
		this.toolTip = toolTip;
		this.key = key;
		this.section = section;
		this.defaultValue = defaultValue;
		if (optionContainer != null) {
			optionContainer.addOption(name, toolTip, shortFlag, longFlag,
					argExpected, this);
		}
		this.optionContainer = optionContainer;
	}

	/**
	 * Uses given panel to display this option.
	 * 
	 * @param rootPanel the panel for displaying
	 * @param activeGui instance of @{link GUIInterface} displaying <i>rootPanel</i>
	 * @param key {@link KeyEventHandler} to process key strokes
	 */
	public final void displayWithGUI(final JPanel rootPanel,
			final stone.io.GUIInterface activeGui, @SuppressWarnings("hiding") final KeyEventHandler key) {
		this.panel = rootPanel;
		this.gui = activeGui;
		display(rootPanel, key);
	}

	/**
	 * Enables or disables all components belonging to this option.
	 * 
	 * @param active sets <i>this</i> option active if <i>active</i> is <i>true</i>
	 */
	public final void enableOnGUI(boolean active) {
		GUI.setEnabled(this.panel, active);
	}

	/**
	 * Releases all resources allocated by
	 * {@link #displayWithGUI(JPanel, stone.io.GUIInterface, KeyEventHandler)}
	 */
	public void endDisplay() {
		this.panel = null;
		this.colorsTextArea.clear();
	}

	/**
	 * @return a short string usable to label <i>this</i> option
	 */
	public final String getDescription() {
		return this.desc;
	}

	/**
	 * @return a description for <i>this</i> option to use it for example as a
	 *         tool-tip in any GUIs
	 */
	public final String getTooltip() {
		return this.toolTip;
	}

	/**
	 * Checks if <i>this</i> is an instance of BooleanhOption
	 * 
	 * @return <i>true</i> if <i>this</i> is an instance of BooleanOption
	 * @see stone.util.BooleanOption
	 */
	public abstract boolean isBoolean();

	/**
	 * Checks if <i>this</i> is an instance of MaskedStringOption
	 * 
	 * @return <i>true</i> if <i>this</i> is an instance of MaskedStringOption
	 * @see stone.util.MaskedStringOption
	 */
	public abstract boolean isMaskable();

	/**
	 * Checks if <i>this</i> is an instance of PathOption
	 * 
	 * @return <i>true</i> if <i>this</i> is an instance of PathOption
	 * @see stone.util.PathOption
	 */
	public abstract boolean isPath();

	/**
	 * Returns the identifier used to register <i>this</i> option at
	 * OptionContainer
	 * 
	 * @return identifier registered at OptionContainer
	 * @see stone.util.OptionContainer
	 * @see #Option(OptionContainer, String, String, String, char, String,
	 *      boolean, String, String, String)
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * @return a string representing the value of <i>this</i> option
	 */
	public String value() {
		if (this.optionContainer == null) {
			return this.value;
		}
		return this.optionContainer.getConfigValue(this.section, this.key,
				this.defaultValue);
	}

	/**
	 * Sets the value corresponding to given string
	 * 
	 * @param value
	 *            new value
	 */
	public void value(@SuppressWarnings("hiding") final String value) {
		if (this.optionContainer == null) {
			this.value = value;
		} else {
			this.optionContainer.setConfigValue(this.section, this.key, value);
		}
	}

	abstract void display(final JPanel rootPanel, final KeyEventHandler keh);

	void setByFlagValue(@SuppressWarnings("hiding") String value) {
		this.value(value);
	}
}
