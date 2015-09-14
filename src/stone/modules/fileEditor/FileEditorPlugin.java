package stone.modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.GUIInterface;
import stone.io.GUIPlugin;
import stone.modules.FileEditor;
import stone.util.Path;


/**
 * GUIPlugin allowing to select multiple files intended to edit afterwards
 * 
 * @author Nelphindal
 */
public abstract class FileEditorPlugin extends GUIPlugin {

	final FileEditor fileEditor;
	Path currentDir;
	private final Path base;
	private final JLabel pathLabel;
	final Set<Path> selection;

	/**
	 * @param fileEditor
	 *            -
	 * @param root
	 *            -
	 * @param text
	 *            displayed if {@link GUIPlugin#display(JPanel)} returns false
	 */
	protected FileEditorPlugin(
			@SuppressWarnings("hiding") final FileEditor fileEditor,
			final Path root, final String text) {
		super(text);
		this.fileEditor = fileEditor;
		this.currentDir = this.base = root;
		this.selection = new HashSet<>();
		this.pathLabel = new JLabel();
	}

	/**
	 * @return the selection
	 */
	public final Set<Path> getSelection() {
		return this.selection;
	}

	/** */
	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelSelection = new JPanel();
		final JPanel panelButton = new JPanel();
		final JScrollPane scroll = new JScrollPane(panelSelection);

		scroll.setSize(400, 400);
		scroll.setPreferredSize(scroll.getSize());

		panelButton.setLayout(new BorderLayout());
		panelSelection.setLayout(new GridLayout(0, 1));
		panel.setLayout(new BorderLayout());
		panel.add(scroll);
		panel.add(this.pathLabel, BorderLayout.NORTH);
		panel.add(panelButton, BorderLayout.SOUTH);
		panelButton.add(GUIInterface.Button.OK.getButton(), BorderLayout.EAST);
		panelButton.add(GUIInterface.Button.ABORT.getButton(),
				BorderLayout.WEST);
		displayDir(panelSelection, scroll);
		return false;
	}

	/** */
	@Override
	protected void repack() {
		super.repack();
	}

	final void displayDir(final JPanel panel, final JScrollPane scroll) {
		final String[] dirs = this.fileEditor.getDirs(this.currentDir);
		final String[] songs = this.fileEditor.getFiles(this.currentDir);
		if (this.base == this.currentDir) {
			this.pathLabel.setText("/");
		} else {
			this.pathLabel.setText(this.currentDir.relativize(this.base));
		}
		for (final String dir : dirs) {
			final JPanel contentPanel = new JPanel();
			final Path p = this.currentDir.resolve(dir);
			final JCheckBox box = p == this.currentDir.getParent() ? null
					: new JCheckBox();
			final JLabel label = new JLabel(box == null ? "  ../  ["
					+ p.getFilename() + "]" : dir);

			if (box != null) {
				if (this.selection.contains(p)) {
					box.setSelected(true);
				}
				box.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent arg0) {
						if (box.isSelected()) {
							FileEditorPlugin.this.selection.add(p);
						} else {
							FileEditorPlugin.this.selection.remove(p);
						}
					}

				});
			}
			contentPanel.setLayout(new BorderLayout());
			contentPanel.add(label);
			if (box != null) {
				contentPanel.add(box, BorderLayout.WEST);
			}

			contentPanel.addMouseListener(new DirMouseListener(this, dirs, p,
					songs, panel, scroll));
			label.setForeground(Color.ORANGE);
			panel.add(contentPanel);
		}

		for (final String song : songs) {
			final JLabel label = new JLabel(song);
			final JPanel contentPanel = new JPanel();
			final JCheckBox box = new JCheckBox();
			final Path p = this.currentDir.resolve(song);

			if (this.selection.contains(p)) {
				box.setSelected(true);
			}
			box.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (box.isSelected()) {
						FileEditorPlugin.this.selection.add(p);
					} else {
						FileEditorPlugin.this.selection.remove(p);
					}
				}

			});
			contentPanel.setLayout(new BorderLayout());
			contentPanel.add(label);
			contentPanel.add(box, BorderLayout.WEST);

			label.setForeground(Color.BLUE);

			panel.add(contentPanel);
		}
	}
}
