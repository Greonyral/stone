package stone.modules.fileEditor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class CommandInterpreter {

	static final String[] NO_ARGS = new String[0];

	private final Map<String, Command> commandMap = new HashMap<>();
	private final Map<String, HelpCommand> helpCommands = new HashMap<>();

	private final Object endSignal;

	public CommandInterpreter(@SuppressWarnings("hiding") final Object endSignal) {
		this.endSignal = endSignal;
	}

	public Set<String> handleTab(final String line, boolean doubleTab) {
		final String[] cmd = line.split(" ", 2);
		final Command c = commandMap.get(cmd[0]);
		final Set<String> set;
		if (c != null) {
			if (cmd.length == 1)
				set = c.complete("");
			else
				set = c.complete(cmd[1]);
		} else if (cmd[0].length() != line.length()) {
			if (line.startsWith("help "))
				set = complete(line);
			else
				// no match
				return null;
		} else
			set = complete(cmd[0]);
		if (set != null && !doubleTab && set.size() > 1) {
			final java.util.Iterator<String> iter = set.iterator();
			String lcs = iter.next();
			while (iter.hasNext()) {
				final String next = iter.next();
				int length = Math.min(next.length(), lcs.length());
				while (length > 0) {
					final String subString = next.substring(0, length);
					if (lcs.startsWith(subString)) {
						lcs = subString;
						break;
					}
					--length;
				}
				if (length == 0)
					return null;
				if (lcs.isEmpty())
					return null;
				
			}
			if (lcs.isEmpty())
				return null;
			set.clear();
			set.add(lcs);
			return set;
		}
		if (set == null || set.isEmpty())
			return null;
		return set;
	}

	private Set<String> complete(final String cmd) {
		final Set<String> set = new HashSet<>();
		for (final String s : commandMap.keySet()) {
			if (s.startsWith(cmd)) {
				set.add(s);
			}
		}
		for (final String s : helpCommands.keySet()) {
			if (s == null) {
				if ("help".startsWith(cmd)) {
					set.add("help");
				}
			} else if (s.startsWith(cmd)) {
				set.add(s);
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
					helpCommands.put(c.getCommandText().replaceFirst("help ", ""), (HelpCommand) c);
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
