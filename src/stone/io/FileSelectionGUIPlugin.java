package stone.io;

import java.io.File;

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
	 * @param startDir
	 * @param filter
	 */
	public FileSelectionGUIPlugin(final String title, final File startDir,
			final FileFilter filter) {
		this.title = title;
		this.startDir = startDir;
		this.filter = filter;
		mode = JFileChooser.FILES_ONLY;
	}

	/**
	 * @return the path of selected file
	 */
	public final Path getSelection() {
		if (selected == null)
			return null;
		return Path.getPath(selected.toString().split("\\" + FileSystem.getFileSeparator()));
	}

	/** */
	@Override
	protected final boolean display(final JPanel panel) {
		final JFileChooser fileChooser = new JFileChooser(startDir);
		fileChooser.setDialogTitle(title);
		fileChooser.setFileFilter(filter);
		fileChooser.setFileSelectionMode(mode);
		final int ret = fileChooser.showOpenDialog(panel);
		if (ret == JFileChooser.APPROVE_OPTION) {
			selected = fileChooser.getSelectedFile();
		}
		synchronized (GUI.Button.class) {
			GUI.Button.class.notifyAll();
			return true;
		}
	}

	/** */
	@Override
	protected final String getTitle() {
		return title;
	}

}
