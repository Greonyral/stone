package stone.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;


/**
 * A Option allowing any type of content
 * 
 * @author Nelphindal
 */
public final class StringOption extends Option {

	JTextField textField;

	/**
	 * Creates a new StringOption and registers it at the OptionContainer
	 * 
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
	 */
	public StringOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag,
			final String longFlag, final String section, final String key) {
		this(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, section, key, null);
	}

	/**
	 * Creates a new StringOption and registers it at the OptionContainer
	 * 
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
	 *            the value returned if the key does not exist in given section
	 */
	public StringOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag,
			final String longFlag, final String section, final String key,
			final String defaultValue) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, true, section, key, defaultValue);
	}

	/** */
	@Override
	public final void display(final JPanel panel) {
		textField = new JTextField();
		final JScrollPane scrollPane = new JScrollPane(textField);
		final JPanel mainPanel = new JPanel();
		final String value = value();

		final StringBuilder sb = new StringBuilder(value);
		if (value == null) {
			value("");
			textField.setForeground(Color.GRAY);
			textField.setText(getTooltip());
		} else {
			textField.setForeground(Color.BLACK);
			textField.setText(value);
		}
		textField.addKeyListener(new KeyListener() {

			final int[] cursor = new int[3];

			@Override
			public final void keyPressed(final KeyEvent e) {
				e.consume();
			}

			@Override
			public final void keyReleased(final KeyEvent e) {
				cursor[1] = textField.getSelectionStart();
				cursor[2] = textField.getSelectionEnd();
				if (cursor[1] == cursor[2]) {
					cursor[0] = textField.getCaretPosition();
				}
				sb.handleEvent(e, cursor);
				if (sb.isEmpty()) {
					cursor[0] = 0;
					textField.setText(getTooltip());
					textField.setForeground(Color.GRAY);
				} else {
					textField.setText(sb.toString());
					textField.setForeground(Color.BLACK);
				}
				if (cursor[1] != cursor[2]) {
					textField.setSelectionStart(cursor[1]);
					textField.setSelectionEnd(cursor[2]);
				} else {
					textField.setCaretPosition(cursor[0]);
				}
				e.consume();
			}

			@Override
			public final void keyTyped(final KeyEvent e) {
				e.consume();
			}
		});

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(new JLabel(getDescription()), BorderLayout.SOUTH);
		mainPanel.add(scrollPane);
		mainPanel.setPreferredSize(new Dimension(100, 55));
		panel.add(mainPanel);
	}

	/** */
	@Override
	public final void endDisplay() {
		super.endDisplay();
		if (textField.getForeground() == Color.BLACK) {
			value(textField.getText());
		} else {
			value(null);
		}
		textField = null;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isBoolean() {
		return false;
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

}
