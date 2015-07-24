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

import stone.io.KeyEventHandler;


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
	  * @param optionContainer instance of {@link OptionContainer} to use
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
	@SuppressWarnings("hiding")
	public StringOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag, final String longFlag,
			final String section, final String key) {
		this(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, section, key, null);
	}

	/**
	 * Creates a new StringOption and registers it at the OptionContainer
	 * 
	 * @param optionContainer instance of {@link OptionContainer} to use
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
	@SuppressWarnings("hiding")
	public StringOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag, final String longFlag,
			final String section, final String key, final String defaultValue) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, true, section, key, defaultValue);
	}

	/** */
	@Override
	public final void display(final JPanel panel, @SuppressWarnings("hiding") final KeyEventHandler key) {
		this.textField = new JTextField();
		final JScrollPane scrollPane = new JScrollPane(this.textField);
		final JPanel mainPanel = new JPanel();
		final String value = value();

		final StringBuilder sb = new StringBuilder(value);
		if (value == null) {
			value("");
			this.textField.setForeground(Color.GRAY);
			this.textField.setText(getTooltip());
		} else {
			this.textField.setForeground(Color.BLACK);
			this.textField.setText(value);
		}
		this.textField.addKeyListener(new KeyListener() {

			final int[] cursor = new int[3];

			@Override
			public final void keyPressed(final KeyEvent e) {
				e.consume();
			}

			@Override
			public final void keyReleased(final KeyEvent e) {
				this.cursor[1] = StringOption.this.textField
						.getSelectionStart();
				this.cursor[2] = StringOption.this.textField.getSelectionEnd();
				if (this.cursor[1] == this.cursor[2]) {
					this.cursor[0] = StringOption.this.textField
							.getCaretPosition();
				}
				final int keyEvent = sb.handleEvent(e, this.cursor);
				if (keyEvent != 0) {
					key.handleKeyEvent(keyEvent);
				} else {
					if (sb.isEmpty()) {
						this.cursor[0] = 0;
						StringOption.this.textField.setText(getTooltip());
						StringOption.this.textField.setForeground(Color.GRAY);
					} else {
						StringOption.this.textField.setText(sb.toString());
						StringOption.this.textField.setForeground(Color.BLACK);
					}
					if (this.cursor[1] != this.cursor[2]) {
						StringOption.this.textField
								.setSelectionStart(this.cursor[1]);
						StringOption.this.textField
								.setSelectionEnd(this.cursor[2]);
					} else {
						StringOption.this.textField
								.setCaretPosition(this.cursor[0]);
					}
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
		if (this.textField.getForeground() == Color.BLACK) {
			value(this.textField.getText());
		} else {
			value(null);
		}
		this.textField = null;
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
