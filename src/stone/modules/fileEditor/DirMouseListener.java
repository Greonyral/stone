package stone.modules.fileEditor;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import stone.util.Path;


final class DirMouseListener implements MouseListener {
	/**
	 * 
	 */
	private final FileEditorPlugin fileEditorPlugin;
	private final String[] dirs;
	private final Path p;
	private final String[] songs;
	private final JPanel panel;
	private final JScrollPane scroll;

	DirMouseListener(FileEditorPlugin fileEditorPlugin, String[] dirs,
			Path p, String[] songs, JPanel panel, JScrollPane scroll) {
		this.fileEditorPlugin = fileEditorPlugin;
		this.dirs = dirs;
		this.p = p;
		this.songs = songs;
		this.panel = panel;
		this.scroll = scroll;
	}

	@Override
	public final void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mousePressed(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		panel.removeAll();
		if (fileEditorPlugin.currentDir.getParent() == p) {
			boolean all = true;
			final Set<Path> paths = new HashSet<>();
			for (final String dir : dirs) {
				final Path p_ = fileEditorPlugin.currentDir.resolve(dir);
				if (p_ == fileEditorPlugin.currentDir.getParent()) {
					continue;
				}
				if (!fileEditorPlugin.selection.contains(p_)) {
					all = false;
					break;
				}
				paths.add(p_);

			}
			if (all) {
				for (final String song : songs) {
					final Path path =
							fileEditorPlugin.currentDir.resolve(song);
					if (!fileEditorPlugin.selection.contains(path)) {
						all = false;
						break;
					}
					paths.add(path);
				}
			}
			if (all) {
				fileEditorPlugin.selection.removeAll(paths);
				fileEditorPlugin.selection
						.add(fileEditorPlugin.currentDir);
			}
		} else if (fileEditorPlugin.selection.remove(p)) {
			fileEditorPlugin.selection.remove(p);
			for (final String dir : fileEditorPlugin.fileEditor.getDirs(p)) {
				if (dir.equals("..")) {
					continue;
				}
				fileEditorPlugin.selection.add(p.resolve(dir));
			}
			for (final String song : fileEditorPlugin.fileEditor
					.getFiles(p)) {
				fileEditorPlugin.selection.add(p.resolve(song));
			}
		}
		fileEditorPlugin.currentDir = p;
		fileEditorPlugin.displayDir(panel, scroll);
		panel.revalidate();

		fileEditorPlugin.repack();
	}
}