package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import stone.io.GUI;
import stone.io.GUIInterface;


public final class GitNoYesPlugin extends NoYesPlugin {

	private final boolean showBox;
	private final JCheckBox box;

	public GitNoYesPlugin(final String title, final String message,
			final GUIInterface guiInterface, boolean progress,
			boolean showBox) {
		super(title, message, guiInterface, progress);
		this.showBox = showBox;
		if (!showBox) {
			box = null;
		} else {
			box = new JCheckBox("encrypt");
		}
	}

	public final boolean encrypt() {
		return showBox && box.isSelected();
	}

	public final void preSelectEncryption() {
		box.setSelected(true);
	}

	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		final JTextArea text = new JTextArea();
		text.setEditable(false);
		text.setText(message);
		panel.setLayout(new BorderLayout());
		panelButton.setLayout(new BorderLayout());
		panel.add(panelButton, BorderLayout.SOUTH);
		panel.add(text);
		panelButton.add(GUI.Button.NO.getButton(), BorderLayout.EAST);
		panelButton.add(GUI.Button.YES.getButton(), BorderLayout.WEST);
		if (progress) {
			panel.add(gui.getProgressBar(), BorderLayout.NORTH);
		}
		return false;
	}
}
