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

	protected void lockResize() {
		this.gui.setResizable(false);
	}

	/**
	 * Requests the gui to repack
	 */
	protected void repack() {
		if (this.gui != null) {
			this.gui.revalidate(true, false);
		}
	}

	/**
	 * Requests the gui to repack and resize frame to Dimesion d
	 */
	protected void repack(final Dimension d) {
		if (this.gui != null) {
			this.gui.revalidate(true, false);
			this.gui.setFrameSize(d);
		}
	}

	final boolean display(final JPanel panel, final GUI gui) {
		this.gui = gui;
		return display(panel);
	}

	final void endDisplay() {
		this.gui.setResizable(true);
		this.gui = null;
	}

}
