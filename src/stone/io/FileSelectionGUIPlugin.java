package stone.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import stone.util.FileSystem;
import stone.util.Path;


/**
 * A baisc plugin for the GUI allowing to select a single file.
 * 
 * @author Nelphindal
 */
public class FileSelectionGUIPlugin extends GUIPlugin {

	private final String title;
	private final File startDir;
	private final FileFilter filter;
	private final int mode;
	private File selected;

	/**
	 * @param title
	 *            Title to display
	 * @param startDir
	 *            root
	 * @param filter
	 *            Filter to use for displaying
	 * @param text
	 *            displayed if {@link #display(JPanel)} returns false
	 */
	@SuppressWarnings("hiding")
	public FileSelectionGUIPlugin(final String title, final File startDir,
			final FileFilter filter, final String text) {
		super(text);
		this.title = title;
		this.startDir = startDir;
		this.filter = filter;
		this.mode = JFileChooser.FILES_ONLY;
	}

	/**
	 * @return the path of selected file
	 */
	public final Path getSelection() {
		if (this.selected == null) {
			return null;
		}
		return Path.getPath(this.selected.toString().split(
				"\\" + FileSystem.getFileSeparator()));
	}

	/** */
	@Override
	protected final boolean display(final JPanel panel) {
		final JFileChooser fileChooser = new JFileChooser(this.startDir);
		fileChooser.setDialogTitle(this.title);
		fileChooser.setFileFilter(this.filter);
		fileChooser.setFileSelectionMode(this.mode);
		final int ret = fileChooser.showOpenDialog(panel);
		if (ret == JFileChooser.APPROVE_OPTION) {
			this.selected = fileChooser.getSelectedFile();
		}
		synchronized (GUI.Button.class) {
			GUI.Button.class.notifyAll();
			return true;
		}
	}

	/** */
	@Override
	protected final String getTitle() {
		return this.title;
	}


	@Override
	public void textmode() throws IOException {
		Path currentDir = Path.getPath(startDir.toString().split(
				"\\" + FileSystem.getFileSeparator()));
		final BufferedReader r = new BufferedReader(new InputStreamReader(
				System.in));
		final int COLS = 3;
		while (true) {
			System.out.print(IOHandler.printGreen("%s\n", currentDir));
			final File[] entries = currentDir.toFile().listFiles();
			int index = -1;
			for (; index != entries.length;) {
				String line = "";
				for (int col = 0; col < COLS;) {
					if (index == entries.length)
						break;
					final String s;
					if (index <= -1) {
						if (currentDir.getNameCount() == 0) {
							final String[] bases = FileSystem.getBases();
							int i = -(index--) - 1;
							if (i == bases.length) {
								s = null;
								index = 0;
							} else {
								final String base = bases[i];
								if (Path.getPath(base).exists())
									s = IOHandler.printBlue("%-30.30s",
											base.toString());
								else
									s = null;
							}
						} else {
							s = IOHandler.printBlue("%-30.30s", "..");
							index = 0;
						}
					} else
						s = IOHandler.print(entries[index++], filter);
					if (s != null) {
						line += s;
						++col;
					}
				}
				System.out.println(line);
			}
			System.out.print("Please enter your selection: ");
			final String[] input = r.readLine().split(
					"[, \\" + FileSystem.getFileSeparator() + "]");
			Path newDir;
			if (input.length == 0)
				newDir = currentDir.resolve("/");
			else
				newDir = currentDir.resolve(input);
			while (!newDir.exists()) {
				if (newDir.getParent() == newDir)
					newDir = currentDir;
				else
					newDir = newDir.getParent();
			}
			currentDir = newDir;
			if (currentDir.toFile().isFile()) {
				selected = currentDir.toFile();
				return;
			}

		}
	}
}
