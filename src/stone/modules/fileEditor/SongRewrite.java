package stone.modules.fileEditor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

		private ReverseSongOrderCommandImpl(
				@SuppressWarnings("hiding") final Console c) {
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
			// TODO Auto-generated method stub

		}

		@Override
		public void displayHelp(final String params) {
			c.out("help ");
			c.out(params);
			c.out("\n");
			if (params.isEmpty()) {
				c.out("Reverses currently selected song and writes result maintaining the hierachy to "
						+ root + "_rewritten\n");
			} else {
				c.err("No additional arguments supported\n");
			}
		}

		@Override
		public Set<Command> complete(final String line) {
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
				if (state.file == null)
					c.out("No song selected\n");
				else
					c.out(state.file.toString() + "\n");

			} else {
				final StringBuilder sb = new StringBuilder();
				for (final String s : params) {
					sb.append(s);
				}
				c.out("\r ");
				c.out(sb.toString());
				c.out("\n");
				final Path selected = root.resolve(sb.toString().split(
						"[\\\\/]"));
				if (selected.exists()) {
					if (selected.toFile().isFile())
						state.file = selected;
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
		public Set<Command> complete(final String line) {
			// TODO
			return null;
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
		public final Set<Command> complete(final String line) {
			return null;
		}
	}

	static class ABC_RewriteState extends State {

		Path file;

	}

	private final MasterThread master;
	private final SongData songdata;
	private final Path root;
	private final IOHandler io;
	private final ABC_RewriteState state = new ABC_RewriteState();

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
			final Console c = Console.createConsoleGUI(panel, state);
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
			final Console c = Console.createConsole(state);
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
