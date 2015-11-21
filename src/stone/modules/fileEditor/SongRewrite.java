package stone.modules.fileEditor;

import java.io.IOException;

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

	public SongRewrite(final StartupContainer sc) {
		master = sc.getMaster();
		songdata = (SongData) master.getModule(SongData.class
				.getSimpleName());
		root = songdata.getRoot();
		io = sc.getIO();
	}

	public void run() {
		io.handleGUIPlugin(new SongRewriteOptionPlugin());
	}

	static class SongRewriteOptionPlugin extends stone.io.GUIPlugin {

		protected SongRewriteOptionPlugin() {
			super("GUI paused");
		}

		@Override
		protected boolean display(final JPanel panel) {
			final Console c = Console.createConsoleGUI(panel);
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
		}

		
	}
	
	private void run(final Console c) {
		c.run(root);
	}
}
