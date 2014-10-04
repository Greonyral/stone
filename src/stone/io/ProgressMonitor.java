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
	public ProgressMonitor(final GUIInterface gui) {
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
			progress = -1;
		} else {
			progress = 0;
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
		if (!init) {
			startSafe();
		}
		gui.setProgressSize(paramInt, paramString);
		gui.setProgress(progress);
		max = paramInt;
	}

	/**
	 * Ends the task
	 */
	public final void endProgress() {
		if (init) {
			gui.endProgress();
			System.out.println();
		}
		init = false;
	}

	/**
	 * @param size
	 *            the new maximum size
	 */
	public final synchronized void setProgressSize(int size) {
		gui.setProgressSize(size);
		progress = size < 0 ? -1 : progress < 0 ? 0 : progress;
		gui.setProgress(progress);
	}

	/**
	 * Sets the message of a running progress
	 * 
	 * @param paramString
	 *            message to be shown
	 */
	public final void setProgressTitle(final String paramString) {
		gui.setProgressTitle(paramString);
	}

	/**
	 * Adds to current progress <i>paramInt</i> units
	 * 
	 * @param paramInt
	 *            units to add
	 */
	public final synchronized void update(int paramInt) {
		if (!init || (progress < 0)) {
			return;
		}

		gui.setProgress(progress += paramInt);
		if (max > 0) {
			final int digits = (int) Math.log10(max) + 1;
			System.out.printf(String.format(
					"\r%%%dd / %%%dd (%%6.2f%%%%)", digits, digits),
					progress, max, (progress * 100.0) / max);
		}
	}

	private final void startSafe() {
		init = true;
		progress = 0;
		gui.initProgress();
	}

}
