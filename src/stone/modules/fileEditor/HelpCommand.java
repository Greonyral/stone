package stone.modules.fileEditor;

import java.util.Map;
import java.util.Set;

/**
 * Base class of help commands allowing to display details on each command.
 * 
 * @author Nelphindal
 * 
 */
public abstract class HelpCommand implements Command {

	private final Console c;
	private String help = null;

	/**
	 * Creates a new HelpCommand for given Console
	 * 
	 * @param console
	 *            -
	 */
	public HelpCommand(final Console console) {
		this.c = console;

	}

	@Override
	public final String getCommandText() {
		return "help";
	}
	
	@Override
	public Set<String> complete(final String line) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getHelpText() {
		if (help == null) {
			final StringBuilder sb = new StringBuilder();
			sb.append("Available commands:");
			sb.append(String.format("\n%s\n    %s\n", "help",
					"Prints this message"));
			sb.append(String.format("\n%s\n    %s\n", "help <command>",
					"Displays details for command"));
			for (final Map.Entry<String, Command> entry : c.getCommandMap()
					.entrySet()) {
				final String command = entry.getKey();
				final String helpText = entry.getValue().getHelpText();
				sb.append(String.format("\n%s\n    %s\n", command, helpText));
			}
			sb.append("\n");
			help = sb.toString();
		}
		return help;
	}

	@Override
	public void call(final String[] params) {
		if (params.length == 0) {
			c.out(getHelpText());
		} else {
			@SuppressWarnings("hiding")
			final Command c = this.c.getCommandMap().get(params[0]);
			if (c == null) {
				this.c.err("No such command: " + params[0]);
			} else {
				c.displayHelp(params[1]);
			}
		}
	}

	@Override
	public final void displayHelp(final String params) {
		call(CommandInterpreter.NO_ARGS);
	}
}
