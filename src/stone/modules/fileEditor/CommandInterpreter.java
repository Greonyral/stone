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
		final String baseCmd = line.split(" ", 2)[0];
		final Command c = commandMap.get(baseCmd);
		final Set<Command> set;
		if (c != null) {
			set = c.complete(line.substring(baseCmd.length()));
		} else if (baseCmd.length() != line.length()) {
			if (line.startsWith("help "))
				set = complete(line);
			else
				// no match
				return null;
		} else
			set = complete(baseCmd);
		if (!doubleTab && set != null && set.size() != 1)
			return null;
		return set;
	}

	private Set<Command> complete(final String cmd) {
		final Set<Command> set = new HashSet<>();
		for (final String s : commandMap.keySet()) {
			if (s.startsWith(cmd)) {
				set.add(commandMap.get(s));
			}
		}
		for (final String s : helpCommands.keySet()) {
			if (s == null) {
				if ("help".startsWith(cmd)) {
					set.add(helpCommands.get(null));
				}
			} else if (s.startsWith(cmd)) {
				set.add(commandMap.get(s));
			}
		}
		return set;
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
			args = line.substring(delim + 1).split(" ", 2);
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
		synchronized (endSignal) {
			endSignal.notifyAll();
		}
	}
}
