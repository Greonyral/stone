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

	@SuppressWarnings("hiding")
	DirMouseListener(FileEditorPlugin fileEditorPlugin, String[] dirs, Path p,
			String[] songs, JPanel panel, JScrollPane scroll) {
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
		this.panel.removeAll();
		if (this.fileEditorPlugin.currentDir.getParent() == this.p) {
			boolean all = true;
			final Set<Path> paths = new HashSet<>();
			for (final String dir : this.dirs) {
				final Path p_ = this.fileEditorPlugin.currentDir.resolve(dir);
				if (p_ == this.fileEditorPlugin.currentDir.getParent()) {
					continue;
				}
				if (!this.fileEditorPlugin.selection.contains(p_)) {
					all = false;
					break;
				}
				paths.add(p_);

			}
			if (all) {
				for (final String song : this.songs) {
					final Path path = this.fileEditorPlugin.currentDir
							.resolve(song);
					if (!this.fileEditorPlugin.selection.contains(path)) {
						all = false;
						break;
					}
					paths.add(path);
				}
			}
			if (all) {
				this.fileEditorPlugin.selection.removeAll(paths);
				this.fileEditorPlugin.selection
						.add(this.fileEditorPlugin.currentDir);
			}
		} else if (this.fileEditorPlugin.selection.remove(this.p)) {
			this.fileEditorPlugin.selection.remove(this.p);
			for (final String dir : this.fileEditorPlugin.fileEditor
					.getDirs(this.p)) {
				if (dir.equals("..")) {
					continue;
				}
				this.fileEditorPlugin.selection.add(this.p.resolve(dir));
			}
			for (final String song : this.fileEditorPlugin.fileEditor
					.getFiles(this.p)) {
				this.fileEditorPlugin.selection.add(this.p.resolve(song));
			}
		}
		this.fileEditorPlugin.currentDir = this.p;
		this.fileEditorPlugin.displayDir(this.panel, this.scroll);
		this.panel.revalidate();

		this.fileEditorPlugin.repack();
	}
}