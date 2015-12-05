package stone.modules.fileEditor;


/**
 * Base class of exit commands allowing to terminate running process.
 * 
 * @author Nelphindal
 * 
 */
public abstract class ExitCommand implements Command {

	protected final Console c;

	/**
	 * Creates a new ExitCommand for given Console
	 * 
	 * @param console
	 *            -
	 */
	public ExitCommand(final Console console) {
		this.c = console;
	}

	@Override
	public final String getCommandText() {
		return "exit";
	}

	/**
	 * Terminates handling of console
	 */
	protected void exit() {
		c.exit();
	}

}
