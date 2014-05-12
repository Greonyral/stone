package io;

import gui.GUI;
import gui.GUIPlugin;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import util.Path;


/**
 * A baisc plugin for the GUI allowing to select a single file.
 * 
 * @author Nelphindal
 */
public class FileSelectionGUIPlugin implements GUIPlugin {

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

	/** */
	@Override
	public final boolean display(final JPanel panel) {
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

	/**
	 * @return the path of selected file
	 */
	public final Path getSelection() {
		if (selected == null) {
			return null;
		}
		return Path.getPath(selected.toString());
	}

	/** */
	@Override
	public final String getTitle() {
		return title;
	}

}
