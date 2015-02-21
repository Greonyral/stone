package stone.modules.abcCreator;

import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;


final class LoadButtonMouseListener extends ReleaseListener {

	LoadButtonMouseListener(final ReleaseMouseListenerParams params) {
		super(params);
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		e.consume();
		if (this.abcMapPlugin.state.loadingMap) {
			return;
		}
		final JFileChooser fc = new JFileChooser(this.abcMapPlugin.caller
				.getFile().getParent().toFile());
		fc.setFileFilter(new FileFilter() {

			@Override
			public final boolean accept(final File f) {
				return f.isDirectory()
						|| (f.isFile() && f.getName().endsWith(".map"));
			}

			@Override
			public final String getDescription() {
				return "MAP-files (.map)";
			}
		});

		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final int sel = fc.showOpenDialog(this.loadButton);
		if (sel == JFileChooser.APPROVE_OPTION) {
			for (final DragObject<JPanel, JPanel, JPanel> o : this.abcMapPlugin.trackMap
					.values()) {
				for (final DropTarget<JPanel, JPanel, JPanel> t : o
						.getTargetContainer().removeAllLinks(o)) {
					if (t != this.abcMapPlugin.state.emptyTarget) {
						t.getDisplayableComponent().getParent()
								.remove(t.getDisplayableComponent());
					}
				}
				o.clearTargets();
				for (final DragObject<JPanel, JPanel, JPanel> alias : o
						.getAliases()) {
					alias.forgetAlias();
					for (final DropTarget<JPanel, JPanel, JPanel> t : alias
							.getTargetContainer().removeAllLinks(alias)) {
						if (t != this.abcMapPlugin.state.emptyTarget) {
							t.getDisplayableComponent().getParent()
									.remove(t.getDisplayableComponent());
						}
					}
					this.abcMapPlugin.panelLeft.remove(alias
							.getDisplayableComponent());
					this.abcMapPlugin.panelLeft.validate();
				}
				o.addTarget(this.abcMapPlugin.state.emptyTarget);
				this.abcMapPlugin.state.emptyTarget.link(o);
			}
			this.abcMapPlugin.emptyCenter();
			this.abcMapPlugin.instrumentToTrack.clear();
			this.abcMapPlugin.state.label.setText("Parsing loaded map ...");
			this.abcMapPlugin.state.loadingMap = true;
			final File mapToLoad = fc.getSelectedFile();
			ParamMap.setTracks(this.abcMapPlugin.trackMap);
			this.abcMapPlugin.caller.exec(new MapLoadingThread(
					this.abcMapPlugin, mapToLoad));
		}
	}
}