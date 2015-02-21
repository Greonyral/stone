package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import stone.io.GUI;
import stone.io.GUIInterface;
import stone.io.GUIPlugin;


/**
 * A plugin showing a question and allowing to answer with no or yes.
 * 
 * @author Nelphindal
 */
public class NoYesPlugin extends GUIPlugin {

	private final String title;
	protected final String message;
	protected final GUIInterface gui;
	protected final boolean progress;

	/**
	 * @param title
	 * @param message
	 * @param guiInterface
	 * @param progress
	 */
	public NoYesPlugin(final String title, final String message,
			final GUIInterface guiInterface, boolean progress) {
		this.title = title;
		this.message = message;
		this.gui = guiInterface;
		this.progress = progress;
	}

	/**
	 * @return true if yes was activated
	 */
	public final boolean get() {
		return this.gui.getPressedButton() == GUI.Button.YES;
	}

	/** */
	@Override
	protected boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		final JTextArea text = new JTextArea();
		text.setEditable(false);
		text.setText(this.message);
		panel.setLayout(new BorderLayout());
		panelButton.setLayout(new BorderLayout());
		panel.add(panelButton, BorderLayout.SOUTH);
		panel.add(text);
		panelButton.add(GUI.Button.NO.getButton(), BorderLayout.EAST);
		panelButton.add(GUI.Button.YES.getButton(), BorderLayout.WEST);
		if (this.progress) {
			panel.add(this.gui.getProgressBar(), BorderLayout.NORTH);
		}
		return false;
	}

	/** */
	@Override
	protected final String getTitle() {
		return this.title;
	}

}
