package stone.modules.fileEditor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CommandInterpreter {

	static final String[] NO_ARGS = new String[0];

	private final Map<String, Command> commandMap = new HashMap<>();
	private final Map<String, HelpCommand> helpCommands = new HashMap<>();

	private final Object endSignal;

	public CommandInterpreter(@SuppressWarnings("hiding") final Object endSignal) {
		this.endSignal = endSignal;
	}

	public Set<Command> handleTab(final String line, boolean doubleTab) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean handleEnter(String line) {
		line = line.trim();
		int delim = line.indexOf(' ');
		final String baseCommand;
		final String[] args;
		if (delim < 0) {
			baseCommand = line;
			args = NO_ARGS;
		} else {
			baseCommand = line.substring(0, delim);
			args = line.substring(delim + 1, line.length() - delim - 1).split(
					" ");
		}
		if (baseCommand.equals("help")) {
			helpCommands.get(null).call(args);
			return true;
		}
		final Command c = commandMap.get(baseCommand);
		if (c == null) {
			return false;
		}
		c.call(args);
		return true;
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
				helpCommands.put(null, (HelpCommand) c);
				if (c.getCommandText().startsWith("help "))
					helpCommands.put(
							c.getCommandText().replaceFirst("help ", ""),
							(HelpCommand) c);
			} else {
				commandMap.put(c.getCommandText(), c);
			}
		}
		if (helpCommands.get(null) == null) {
			throw new RuntimeException("Missing help command");
		}

	}

	Map<String, Command> getCommandMap() {
		return commandMap;
	}

	void exit() {
		synchronized(endSignal) {
			endSignal.notifyAll();
		}
	}
}
