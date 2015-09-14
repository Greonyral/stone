package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import stone.io.GUIInterface;
import stone.io.GUIPlugin;


/**
 * {@link GUIPlugin} to select a key
 * 
 * @author Nelphindal
 */
public class SecretKeyPlugin extends GUIPlugin {

	/**
	 * Decodes <i>text</i> a hexdump to the according byte array
	 * 
	 * @param text
	 *            string containing the key
	 * @return decoded text for use as key
	 */
	public final static byte[] decode(final String text) {
		int posChars = 0;
		int posKey = 0;
		final char[] chars = text.toCharArray();
		final byte[] key = new byte[chars.length / 2];
		while (posKey < key.length) {
			final int[] bytes = new int[] { chars[posChars++],
					chars[posChars++] };
			int byteValue = 0;
			for (final int byteV : bytes) {
				byteValue <<= 4;
				if ((byteV >= '0') && (byteV <= '9')) {
					byteValue += byteV - '0';
				} else if ((byteV >= 'a') && (byteV <= 'f')) {
					byteValue += (byteV - 'a') + 10;
				} else if ((byteV >= 'A') && (byteV <= 'F')) {
					byteValue += (byteV - 'A') + 10;
				} else {
					throw new IllegalArgumentException();
				}
			}
			key[posKey++] = (byte) byteValue;

		}
		return key;
	}


	private final JTextField textField = new JTextField();

	/** */
	public SecretKeyPlugin() {
		super("");
	}

	/**
	 * @return the bytes for the selected key
	 */
	public final byte[] getKey() {
		return decode(this.textField.getText());
	}


	/**
	 * @return the entered text
	 */
	public final String getValue() {
		return this.textField.getText();
	}

	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(this.textField);
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
