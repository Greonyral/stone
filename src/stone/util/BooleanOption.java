package stone.util;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.KeyEventHandler;


/**
 * A class for an option allowing only two values.
 * 
 * @author Nelphindal
 */
public final class BooleanOption extends Option {

	private String value;
	private BooleanChangeListener listener;

	/**
	 * @param optionContainer
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
	 */
	public BooleanOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag, final String longFlag,
			final String section, final String key, boolean defaultValue) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, false, section, key, Boolean.valueOf(defaultValue)
						.toString());
	}

	/**
	 * Toggles the value.
	 */
	public final void changeValue() {
		value(Boolean.valueOf(!Boolean.parseBoolean(value())).toString());
	}

	/**
	 * Returns the value of <i>this</i> option
	 * 
	 * @return the value
	 */
	public final boolean getValue() {
		return Boolean.valueOf(value());
	}

	/**
	 * @return <i>true</i>
	 */
	@Override
	public final boolean isBoolean() {
		return true;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isMaskable() {
		return false;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isPath() {
		return false;
	}

	/**
	 * Sets a listener to notify when value of this option changes.
	 * 
	 * @param listener
	 */
	public final void setListener(final BooleanChangeListener listener) {
		this.listener = listener;
	}

	/**
	 * sets the value represented by <i>this</i> to b
	 * 
	 * @param b
	 */
	public final void setValue(boolean b) {
		value(Boolean.valueOf(b).toString());
	}

	/** */
	@Override
	public final String value() {
		if (this.key == null) {
			return this.value;
		}
		return super.value();
	}

	/** */
	@Override
	public final void value(final String value) {
		if (this.key == null) {
			this.value = value;
		} else {
			super.value(value);
		}
		if (this.listener != null) {
			this.listener.newValue(getValue());
		}
	}

	@Override
	protected final void setByFlagValue(final String value) {
		setValue(true);
	}

	@Override
	final void display(final JPanel panelBoolean, final KeyEventHandler keh) {
		final JCheckBox box = new JCheckBox();
		box.setText(getDescription());
		box.setSelected(getValue());
		box.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				changeValue();
			}
		});
		box.setToolTipText(getTooltip());
		panelBoolean.add(box);
	}
}
