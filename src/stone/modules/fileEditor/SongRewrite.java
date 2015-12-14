package stone.modules.fileEditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;
import stone.MasterThread;
import stone.StartupContainer;
import stone.io.IOHandler;
import stone.modules.SongData;
import stone.util.Path;

/**
 * 
 * Changes to a song inflicting more than one file at same time and where no
 * deep analysis needed.
 * 
 * @author Nelphindal
 * 
 */
public class SongRewrite {

	private final class ReverseSongOrderCommandImpl implements Command {
		private final Console c;

		private ReverseSongOrderCommandImpl(@SuppressWarnings("hiding") final Console c) {
			this.c = c;
		}

		@Override
		public final String getCommandText() {
			return "reverse";
		}

		@Override
		public final String getHelpText() {
			return "Does necessary changes to play song backwards";
		}

		@Override
		public final void call(final String[] params) {
			c.out(getCommandText() + "\n");
			final Path selected = c.state.selected;
			if (selected == null) {
				c.err("No file selected\n");
				return;
			}
			final InputStream stream = io.openIn(selected.toFile());
			master.addTask(new Runnable() {

				@Override
				public void run() {
					final AbcFile abcFile = AbcFile.readFile(stream, c);
					io.close(stream);
					if (abcFile == null) {
						c.err("Reversing " + selected + " finished with errors\n");
						return;
					}
					abcFile.reverse(root, selected, io);
					c.out("Reversing  " + selected.relativize(root) + " finished\n");
				}
			});
			c.out("Processing " + selected.relativize(root) + " in background\n");
		}

		@Override
		public void displayHelp(final String params) {
			c.out("help ");
			c.out(params);
			c.out("\n");
			if (params.isEmpty()) {
				c.out("Reverses currently selected song and writes result maintaining the hierachy to " + root
						+ "_rewritten\n");
			} else {
				c.err("No additional arguments supported\n");
			}
		}

		@Override
		public Set<String> complete(final String line) {
			return null;
		}
	}

	private final class SelectCommandImpl implements Command {
		private final Console c;

		private SelectCommandImpl(@SuppressWarnings("hiding") final Console c) {
			this.c = c;
		}

		@Override
		public String getCommandText() {
			return "select";
		}

		@Override
		public String getHelpText() {
			return "Selects or show the song to work on";
		}

		@Override
		public void call(final String[] params) {
			c.out("select\n");
			if (params.length == 0) {
				if (c.state.selected == null)
					c.out("No song selected\n");
				else
					c.out(c.state.selected.toString() + "\n");

			} else {
				final StringBuilder sb = new StringBuilder();
				for (final String s : params) {
					sb.append(s);
				}
				c.out("\r ");
				c.out(sb.toString());
				c.out("\n");
				final Path selected = root.resolve(sb.toString().split("[\\\\/]"));
				if (selected.exists()) {
					if (selected.toFile().isFile())
						c.state.selected = selected;
					else
						c.err(selected.toString() + " is no file\n");
				} else {
					c.err(selected.toString() + " does not exist\n");
				}
			}
			c.out("\n");
		}

		@Override
		public void displayHelp(final String params) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<String> complete(final String line) {
			if (line.startsWith("..") || line.startsWith("/"))
				return null;

			final Set<String> completions = new TreeSet<>();
			final String toComplete;
			final int delim = line.lastIndexOf("/");
			final Path p;

			if (delim >= 0) {
				p = root.resolve(line.substring(0, delim).split("/"));
				if (delim == line.length())
					toComplete = "";
				else
					toComplete = line.substring(delim + 1);
			} else {
				p = root;
				toComplete = line;
			}
			if (!p.exists())
				return null;
			if (!line.isEmpty() && (toComplete.isEmpty() ? p.exists() : p.resolve(toComplete).exists())) {
				if (root.resolve(line).toFile().isDirectory()) {
					if (!line.endsWith("/"))
						completions.add(getCommandText() + " " + line + "/");
				} else {
					completions.add(getCommandText() + " " + line);
				}
			}
			final String[] list = p.toFile().list();

			for (final String s : list) {
				if (s.startsWith("."))
					continue;
				if (s.startsWith(toComplete)) {
					if (!s.equals(toComplete)) {
						final Path target = p.resolve(s);
						completions.add(getCommandText() + " " + target.relativize(root));
					}
				}
			}
			return completions;
		}
	}

	private final class HelpCommandImpl extends HelpCommand {

		private HelpCommandImpl(final Console c) {
			super(c);
		}

	}

	private final class ExitCommandImpl extends ExitCommand {

		private ExitCommandImpl(@SuppressWarnings("hiding") final Console c) {
			super(c);
		}

		@Override
		public final String getHelpText() {
			return "Finishes execution";
		}

		@Override
		public final void call(final String[] params) {
			exit();
		}

		@Override
		public final void displayHelp(final String params) {
			c.out("exit: end processing of SongRewrite\nAll unsaved actions will be discarded");

		}

		@Override
		public final Set<String> complete(final String line) {
			return null;
		}
	}

	private final MasterThread master;
	private final SongData songdata;
	private final Path root;
	private final IOHandler io;

	/**
	 * Creates a new object for rewriting songs
	 * 
	 * @param sc
	 *            -
	 */
	public SongRewrite(final StartupContainer sc) {
		master = sc.getMaster();
		songdata = (SongData) master.getModule(SongData.class.getSimpleName());
		root = songdata.getRoot();
		io = sc.getIO();
	}

	/**
	 * Run GUI or console based I/O for controlling operation
	 */
	public void run() {
		io.handleGUIPlugin(new SongRewriteOptionPlugin());
	}

	class SongRewriteOptionPlugin extends stone.io.GUIPlugin {

		protected SongRewriteOptionPlugin() {
			super("Cleaning up SongRewrite ...");
		}

		@Override
		protected boolean display(final JPanel panel) {
			final Console c = Console.createConsoleGUI(panel);
			run(c);
			repack();
			lockResize();
			return false;
		}

		@Override
		protected String getTitle() {
			return "Operation continues in this console";
		}

		@Override
		protected void textmode() throws IOException {
			final Console c = Console.createConsole();
			run(c);
		}

	}

	private void run(final Console c) {
		final Set<Command> commands = new HashSet<>();

		commands.add(new HelpCommandImpl(c));
		commands.add(new ExitCommandImpl(c));
		commands.add(new SelectCommandImpl(c));
		commands.add(new ReverseSongOrderCommandImpl(c));
		// add commands here

		c.run(root, commands);
	}
}
