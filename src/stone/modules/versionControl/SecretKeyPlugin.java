package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import stone.io.GUIInterface;
import stone.io.GUIPlugin;


/**
 * Plugin to select a key
 * 
 * @author Nelphindal
 */
public class SecretKeyPlugin extends GUIPlugin {

	/**
	 * Decodes <i>text</i> a hexdump to the according byte array
	 * 
	 * @param text
	 * @return decoded text for use as key
	 */
	public final static byte[] decode(final String text) {
		int posChars = 0;
		int posKey = 0;
		final char[] chars = text.toCharArray();
		final byte[] key = new byte[chars.length / 2];
		while (posKey < key.length) {
			final byte hByte = (byte) chars[posChars++];
			final byte lByte = (byte) chars[posChars++];
			key[posKey++] = (byte) ((hByte << 8) | lByte);
		}
		return key;
	}

	private final JTextField textField = new JTextField();

	/**
	 * @return the bytes for the selected key
	 */
	public final byte[] getKey() {
		return decode(textField.getText());
	}


	/**
	 * @return the entered text
	 */
	public final String getValue() {
		return textField.getText();
	}

	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(textField);
		panel.add(panelButton, BorderLayout.SOUTH);
		panelButton.add(GUIInterface.Button.ABORT.getButton(),
				BorderLayout.EAST);
		panelButton.add(GUIInterface.Button.OK.getButton(), BorderLayout.EAST);
		return false;
	}

	@Override
	protected final String getTitle() {
		return "Enter the key for song encryption";
	}

}
