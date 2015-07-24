package stone.io;

import java.awt.Dimension;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
public abstract class GUIPlugin {

	private GUI gui;
	private final String text;
	
	/**
	 * 
	 * @param text displayed if {@link #display(JPanel)} returns <i>false</i>
	 */
	protected GUIPlugin(@SuppressWarnings("hiding") final String text) {
		if (text == null)
			throw new IllegalArgumentException();
		this.text = text;
	}
	/**
	 * Use the given <i>panel</i> to display any stuff this GUIPlugin is intended to
	 * do.
	 * 
	 * @param panel A panel hosted by calling GUI 
	 * @return <i>true</i> if the display operation is finished on returning
	 */
	protected abstract boolean display(JPanel panel);

	/**
	 * @return the title of this GUIPlugin
	 */
	protected abstract String getTitle();

	/**
	 * Disallows the user to resize.
	 */
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
	 * @param d new desired dimensions
	 */
	protected void repack(final Dimension d) {
		if (this.gui != null) {
			this.gui.revalidate(true, false);
			this.gui.setFrameSize(d);
		}
	}

	final boolean display(final JPanel panel, @SuppressWarnings("hiding") final GUI gui) {
		this.gui = gui;
		return display(panel);
	}

	final void endDisplay() {
		this.gui.setResizable(true);
		this.gui = null;
	}

	/**
	 * 
	 * @return text displayed if {@link #display(JPanel)} returns <i>false</i>
	 */
	public String getText() {
		return text;
	}

}
