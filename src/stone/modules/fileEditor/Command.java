package stone.modules.fileEditor;

/**
 * @author Nelphindal
 *
 */
public interface Command {

	/**
	 * 
	 * @return identifier for command
	 */
	String getCommandText();

	/**
	 * 
	 * @return explanation of what this command does
	 */
	String getHelpText();

	/**
	 * Perform operation for this command using given params
	 * @param params
	 */
	void call(String[] params);

	/**
	 * Print more details for a command. Params up to paramOffset are parsed already
	 * @param paramsOffset
	 * @param params
	 */
	void displayHelp(int paramsOffset, String[] params);

}
