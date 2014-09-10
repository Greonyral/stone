package stone.io;

import java.awt.Dimension;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
public abstract class GUIPlugin {

	private GUI gui;

	/**
	 * Use the given panel to display any stuff this GUIPlugin is intended to
	 * do.
	 * 
	 * @param panel
	 * @return <i>true</i> if the display operation is finished on returning
	 */
	protected abstract boolean display(JPanel panel);

	/**
	 * @return the title of this GUIPlugin
	 */
	protected abstract String getTitle();

	/**
	 * Requests the gui to repack
	 */
	protected void repack() {
		if (gui != null) {
			final Dimension d = gui.getFrameSize();
			gui.revalidate(true, false);
			gui.setFrameSize(d);
		}
	}

	final boolean display(final JPanel panel, final GUI gui) {
		this.gui = gui;
		return display(panel);
	}

	final void endDisplay() {
		gui = null;
	}

}
