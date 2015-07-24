package stone.io;

/**
 * A progress monitor either using a GUI or printing on stdout to display a
 * progress.
 * 
 * @author Nelphindal
 */
public class ProgressMonitor {

	private final GUIInterface gui;
	private int progress;
	private boolean init = false;
	private int max;

	/**
	 * Creates a new instance using GUI <i>gui</i>
	 * 
	 * @param gui
	 *            GUI to use or <i>null</i> if output shall be done on stdout
	 */
	public ProgressMonitor(@SuppressWarnings("hiding") final GUIInterface gui) {
		this.gui = gui;
	}

	/**
	 * Starts a new task, sets the message and scales for 100%
	 * 
	 * @param paramString
	 *            message to be shown
	 * @param paramInt
	 *            units representing 100%, -1 if unknown. The scale size can be
	 *            set later by calling
	 *            {@link #beginTaskPreservingProgress(String, int)}
	 */
	public final synchronized void beginTask(final String paramString,
			int paramInt) {
		if (paramInt < 0) {
			this.progress = -1;
		} else {
			this.progress = 0;
		}
		beginTaskPreservingProgress(paramString, paramInt);
	}

	/**
	 * Starts a new task, sets the message and scales for 100%
	 * 
	 * @param paramString
	 *            message to be shown
	 * @param paramInt
	 *            units representing 100%
	 */
	public final synchronized void beginTaskPreservingProgress(
			final String paramString, int paramInt) {
		if (!this.init) {
			startSafe();
		}
		System.out.printf("\r%s\n", paramString);
		this.gui.setProgressSize(paramInt, paramString);
		this.gui.setProgress(this.progress);
		this.max = paramInt;

	}

	/**
	 * Ends the task
	 * @param text passed to {@link GUIInterface#endProgress(String)}
	 */
	public final void endProgress(final String text) {
		if (this.init) {
			this.gui.endProgress(text);
			System.out.println();
		}
		this.init = false;
	}

	/**
	 * @param size
	 *            the new maximum size
	 */
	public final synchronized void setProgressSize(int size) {
		this.gui.setProgressSize(size);
		this.progress = size < 0 ? -1 : this.progress < 0 ? 0 : this.progress;
		this.gui.setProgress(this.progress);
		this.max = size;
	}

	/**
	 * Sets the message of a running progress
	 * 
	 * @param paramString
	 *            message to be shown
	 */
	public final void setProgressTitle(final String paramString) {
		this.gui.setProgressTitle(paramString);
		System.out.printf("\n%s\n", paramString);
	}

	/**
	 * Adds to current progress <i>paramInt</i> units
	 * 
	 * @param paramInt
	 *            units to add
	 */
	public final synchronized void update(int paramInt) {
		if (!this.init || (this.progress < 0)) {
			return;
		}
		if (paramInt < 0) {
			this.max -= paramInt;
			this.gui.setProgressSize(this.max);
			update(-paramInt);
			return;
		}
		this.gui.setProgress(this.progress += paramInt);
		if (this.max > 0) {
			final int digits = (int) Math.log10(this.max) + 1;
			System.out.printf(String.format("\r%%%dd / %%%dd (%%6.2f%%%%)",
					digits, digits), this.progress, this.max,
					(this.progress * 100.0) / this.max);
		}
	}

	private final void startSafe() {
		this.init = true;
		this.progress = 0;
		this.gui.initProgress();
	}

}
