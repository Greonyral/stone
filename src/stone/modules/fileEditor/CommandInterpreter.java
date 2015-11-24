package stone.modules.fileEditor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandInterpreter {

	private final Map<String, Command> commandMap = new HashMap<>();
	private final Map<String, Command> helpCommands = new HashMap<>();

	public CommandInterpreter(final Object endSignal) {
		// TODO Auto-generated constructor stub
	}

	public Set<Command> handleTab(final String line, boolean doubleTab) {
		// TODO Auto-generated method stub
		return null;
	}

	public void handleEnter(final String line) {
		// TODO Auto-generated method stub

	}

	/**
	 * Registers given commands to be available for processing
	 * 
	 * @param commands
	 *            a set of commands
	 */
	public void registerCommands(final Set<Command> commands) {
		for (final Command c : commands) {
			if (c.getCommandText().equals("help")) {
				helpCommands.put(null, c);
				if (c.getCommandText().startsWith("help "))
					helpCommands.put(
							c.getCommandText().replaceFirst("help ", ""), c);
			}
		}
		if (helpCommands.get(null) == null) {
			helpCommands.put(null, new Command() {

				@Override
				public String getCommandText() {
					return "help";
				}

				@Override
				public String getHelpText() {
					return "print this message\nhelp [command] prints details on given command";
				}
			});
			for (final Command c : commands) {
				helpCommands.put(c.getCommandText(), new Command() {

					@Override
					public String getCommandText() {
						return "help " + c.getCommandText();
					}

					@Override
					public String getHelpText() {
						return "no help text supplied";
					}
				});
			}
		}

	}
}
