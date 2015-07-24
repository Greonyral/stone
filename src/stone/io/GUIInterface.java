package stone.io;

import java.awt.Component;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;

import stone.util.Option;


/**
 * @author Nelphindal
 */
public interface GUIInterface {

	/**
	 * Basic buttons for the GUI
	 * 
	 * @author Nelphindal
	 */
	public enum Button {
		/**
		 * Button indicating confirmation and the termination of current
		 * dialogue
		 */
		OK,
		/**
		 * Button indicating confirmation
		 */
		YES,
		/**
		 * Button indicating declining
		 */
		NO,
		/**
		 * Button indicating declining and the termination of the current
		 * dialogue
		 */
		ABORT;

		final JButton button;

		Button() {
			button = new JButton(name().charAt(0)
					+ name().substring(1).toUpperCase());
		}

		/**
		 * @return the JButton instance
		 */
		public final JButton getButton() {
			return button;
		}
	}

	/**
	 * Sets this GUI invisible and frees all allocated resources. All further
	 * invocations are undefined
	 */
	void destroy();

	/**
	 * Ends current progress
	 * @param text message to displayed on ending
	 */
	void endProgress(String text);

	/**
	 * Displays the given options and allows to modify them
	 * 
	 * @param options to be displayed options
	 */
	void getOptions(Collection<Option> options);

	/**
	 * @return the last pressed button.
	 */
	Button getPressedButton();

	/** @return a component to display a progress-bar */
	Component getProgressBar();

	/**
	 * Initializes all componets allowing to display a progress
	 */
	void initProgress();

	/**
	 * Like {@link #printMessage(String, String, boolean)} but needs always
	 * confirmation and different colors will be used to indicate an error.
	 * 
	 * @see #printMessage
	 * @param errorMessage
	 */
	void printErrorMessage(String errorMessage);

	/**
	 * Prints a message. It will block if <i>toFront</i> is <i>true</i> as long
	 * the user does not confirm the message.
	 * 
	 * @param title
	 * @param message
	 *            the message
	 * @param bringGUItoFront
	 *            user has have to confirm the message
	 */
	void printMessage(String title, String message, boolean bringGUItoFront);

	/**
	 * Displays a no default GUI operation with given plugin.
	 * 
	 * @param plugin
	 * @param text message to display 
	 */
	void runPlugin(GUIPlugin plugin);

	/**
	 * Basic GUI operation to determine the modules to be run.
	 * 
	 * @param modules
	 * @return a set containing the selected modules.
	 */
	List<String> selectModules(List<String> modules);

	/**
	 * Sets the progress.
	 * 
	 * @param progress
	 *            the new state of the progress
	 */
	void setProgress(int progress);

	/**
	 * Sets the progresses maximum size.
	 * 
	 * @param size
	 *            the new maximum size
	 */
	void setProgressSize(int size);

	/**
	 * Works like combined {@link #setProgressTitle(String)} and
	 * {@link #setProgressSize(int)}. {@link #initProgress()} will be called as
	 * needed.
	 * 
	 * @param size
	 * @param title
	 */
	void setProgressSize(int size, String title);

	/**
	 * Sets the title to be shown during running progress.
	 * 
	 * @param title
	 */
	void setProgressTitle(String title);

}
