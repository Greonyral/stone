package stone.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.KeyEventHandler;


/**
 * An Option with ability to mask the value with asterisks
 * 
 * @author Nelphindal
 */
public final class MaskedStringOption extends Option {

	final StringBuilder content;
	boolean initialValue = true;
	boolean save;
	boolean show;
	private final StringBuilder sb;

	/**
	 * Creates a new MaskedStringOption.
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
	public MaskedStringOption(final OptionContainer optionContainer,
			final String name, final String toolTip,
			final String guiDescription, char shortFlag, final String longFlag,
			final String section, final String key) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag,
				longFlag, true, section, key, null);
		this.content = new StringBuilder(super.value());
		this.sb = new StringBuilder(value());
	}

	/** */
	@Override
	public final void display(final JPanel panel,
			@SuppressWarnings("hiding") final KeyEventHandler key) {
		final JPanel mainPanel = new JPanel();
		final JPanel buttonPanel = new JPanel();
		final JTextField textField = new JTextField();

		if (this.content.length() == 0) {
			textField.setText(getTooltip());
			textField.setForeground(Color.GRAY);
		} else {
			printValue(textField);
			textField.setForeground(Color.BLACK);
		}

		textField.addKeyListener(new KeyListener() {

			private final int[] cursor = new int[3];

			@Override
			public final void keyPressed(final KeyEvent e) {
				e.consume();
			}

			@Override
			public final void keyReleased(final KeyEvent e) {
				if (MaskedStringOption.this.initialValue) {
					if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
						MaskedStringOption.this.content.clear();
						MaskedStringOption.this.initialValue = false;
						textField.setCaretPosition(0);
					}
				}
				this.cursor[1] = textField.getSelectionStart();
				this.cursor[2] = textField.getSelectionEnd();
				if (this.cursor[1] == this.cursor[2]) {
					this.cursor[0] = textField.getCaretPosition();
				}
				final int keyEvent = MaskedStringOption.this.content
						.handleEvent(e, this.cursor);
				if (keyEvent != 0) {
					key.handleKeyEvent(keyEvent);
				}
				printValue(textField);
				if (this.cursor[1] != this.cursor[2]) {
					textField.setSelectionStart(this.cursor[1]);
					textField.setSelectionEnd(this.cursor[2]);
				} else if (MaskedStringOption.this.content.isEmpty()) {
					textField.setCaretPosition(0);
				} else {
					textField.setCaretPosition(this.cursor[0]);
				}
				e.consume();
			}

			@Override
			public final void keyTyped(final KeyEvent e) {
				e.consume();
			}
		});

		final JCheckBox saveBox = new JCheckBox(), showBox = new JCheckBox();
		saveBox.setText("Save");
		saveBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				MaskedStringOption.this.save = !MaskedStringOption.this.save;
			}
		});

		showBox.setText("Show");
		showBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				MaskedStringOption.this.show = !MaskedStringOption.this.show;
				printValue(textField);
			}
		});

		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(saveBox);
		buttonPanel.add(showBox);

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(textField);
		mainPanel.add(new JLabel(getDescription()), BorderLayout.SOUTH);
		mainPanel.setToolTipText(getTooltip());
		mainPanel.add(buttonPanel, BorderLayout.EAST);
		panel.add(mainPanel);

	}

	/** */
	@Override
	public final void endDisplay() {
		super.value(getValueToSave());
	}

	/**
	 * @return <i>null</i> if value represented by <i>this</i> option shall not
	 *         be saved. Else the same value as {@link #value()} would return.
	 */
	public final String getValueToSave() {
		if (this.save) {
			return value();
		}
		return null;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isBoolean() {
		return false;
	}

	/**
	 * @return <i>true</i>
	 */
	@Override
	public final boolean isMaskable() {
		return true;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isPath() {
		return false;
	}

	/** */
	@Override
	public final String value() {
		return this.content.toString();
	}

	/** */
	@Override
	public final void value(final String s) {
		super.value(s);
		this.content.set(s);
	}

	final void printValue(final JTextField textField) {
		if (this.content.isEmpty()) {
			textField.setText(getTooltip());
			textField.setForeground(Color.GRAY);
		} else {
			textField.setForeground(Color.BLACK);
			if (this.show) {
				textField.setText(this.content.toString());
			} else {
				this.sb.clear();
				final int len = this.content.length();
				for (int i = 0; i < len; i++) {
					this.sb.appendLast('\u25cf');
				}
				textField.setText(this.sb.toString());
			}
		}

	}

}
