package stone.modules.fileEditor;

import java.util.Set;

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
	 * Print more details for a command.
	 * @param params
	 */
	void displayHelp(String params);

	/**
	 * Handles TAB-completion
	 * @param line complete input line
	 * @return possible completions
	 */
	Set<String> complete(final String line);

}
