package stone.io;

/**
 * Simple handler of keyboard input
 * 
 * @author Nelphindal
 * 
 */
public interface KeyEventHandler {

	/**
	 * Tries to handle <i>event</i>.
	 * @param event incomming key
	 */
	void handleKeyEvent(int event);
}
