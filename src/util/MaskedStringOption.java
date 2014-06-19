package util;

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


/**
 * An Option with ability to mask the value with asterisks
 * 
 * @author Nelphindal
 */
public final class MaskedStringOption extends Option {

	private boolean save, show, initialValue = true;
	private final StringBuilder content, sb;

	/**
	 * Creates a new MaskedStringOption.
	 * 
	 * @param optionContainer
	 * @param name
	 *            a unique identifier for this option to register at
	 *            OptionContainer
	 * @param toolTip
	 *            a description for <i>this</i> option to use it for example as
	 *            a
	 *            tool-tip in any GUIs
	 * @param guiDescription
	 *            a short string usable to label <i>this</i> option
	 * @param shortFlag
	 *            a unique printable char to register at flags or {@link main.Flag#NoShortFlag} to enable this option
	 * @param longFlag
	 *            a unique printable string to register at flags or {@link main.Flag#NoLongFlag} to enable this option
	 * @param section
	 *            the section identifier for this option, to access by {@link main.Main#getConfigValue(String, String, String)} and
	 *            {@link main.Main#setConfigValue(String, String, String)}
	 * @param key
	 *            the key identifier for this option, to access by {@link main.Main#getConfigValue(String, String, String)} and
	 *            {@link main.Main#setConfigValue(String, String, String)}
	 */
	public MaskedStringOption(final OptionContainer optionContainer, final String name,
			final String toolTip, final String guiDescription, char shortFlag,
			final String longFlag, final String section, final String key) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag, longFlag, true,
				section, key, null);
		content = new StringBuilder(super.value());
		sb = new StringBuilder(value());
	}

	/** */
	@Override
	public final void display(final JPanel panel) {
		final JPanel mainPanel = new JPanel();
		final JPanel buttonPanel = new JPanel();
		final JTextField textField = new JTextField();

		if (content.length() == 0) {
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
				if (initialValue) {
					if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
						content.clear();
						initialValue = false;
						textField.setCaretPosition(0);
					}
				}
				cursor[1] = textField.getSelectionStart();
				cursor[2] = textField.getSelectionEnd();
				if (cursor[1] == cursor[2]) {
					cursor[0] = textField.getCaretPosition();
				}
				content.handleEvent(e, cursor);
				printValue(textField);
				if (cursor[1] != cursor[2]) {
					textField.setSelectionStart(cursor[1]);
					textField.setSelectionEnd(cursor[2]);
				} else if (content.isEmpty()) {
					textField.setCaretPosition(0);
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

		final JCheckBox saveBox = new JCheckBox(), showBox = new JCheckBox();
		saveBox.setText("Save");
		saveBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				save = !save;
			}
		});

		showBox.setText("Show");
		showBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				show = !show;
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
		if (save) {
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
		return content.toString();
	}

	/** */
	@Override
	public final void value(final String s) {
		super.value(s);
		content.set(s);
	}

	private final void printValue(final JTextField textField) {
		if (content.isEmpty()) {
			textField.setText(getTooltip());
			textField.setForeground(Color.GRAY);
		} else {
			textField.setForeground(Color.BLACK);
			if (show) {
				textField.setText(content.toString());
			} else {
				sb.clear();
				final int len = content.length();
				for (int i = 0; i < len; i++) {
					sb.appendLast('\u25cf');
				}
				textField.setText(sb.toString());
			}
		}

	}

}
