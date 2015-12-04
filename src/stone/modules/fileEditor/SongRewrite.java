package stone.modules.fileEditor;

import java.io.IOException;
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

	private final MasterThread master;
	private final SongData songdata;
	private final Path root;
	private final IOHandler io;

	/**
	 * Creates a new object for rewriting songs
	 * @param sc -
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
			final Object endSignal = new Object();
			final Console c = Console.createConsole(endSignal);
			run(c);
		}


	}

	private void run(final Console c) {
		final Set<Command> commands = new HashSet<>();
		class HelpCommandImpl extends HelpCommand {

			public HelpCommandImpl() {
				super(c);
			}
		}
		final HelpCommand helpCommand = new HelpCommandImpl();
		commands.add(helpCommand);
		commands.add(new ExitCommand(c) {

			@Override
			public String getHelpText() {
				return "finish execution";
			}

			@Override
			public void call(final String[] params) {
				exit();
			}

			@Override
			public void displayHelp(int paramsOffset, String[] params) {
				c.out("exit: end processing of SongRewrite\nAll unsaved actions will be discarded");

			}
		});
		// TODO genereate commands

		c.run(root, commands);
	}
}
