package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import stone.io.GUIInterface;
import stone.io.GUIPlugin;


/**
 * A plugin allowing to edit a message
 * 
 * @author Nelphindal
 */
public final class EditorPlugin extends GUIPlugin {

	private final String title;
	private final String content;

	/**
	 * @param content
	 *            -
	 * @param title
	 *            -
	 * @param text
	 *            displayed if {@link #display(JPanel)} returns false
	 */
	@SuppressWarnings("hiding")
	public EditorPlugin(final String content, final String title,
			final String text) {
		super(text);
		this.content = content;
		this.title = title;
	}

	/**
	 * @return the entered content
	 */
	public final String get() {
		return this.content;
	}

	/** */
	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JScrollPane(new JTextArea(this.content)));
		panel.add(panelButton, BorderLayout.SOUTH);

		panelButton.add(GUIInterface.Button.OK.getButton(), BorderLayout.EAST);
		panelButton.add(GUIInterface.Button.ABORT.getButton(),
				BorderLayout.WEST);
		return false;
	}

	/** */
	@Override
	protected final String getTitle() {
		return this.title;
	}

}
