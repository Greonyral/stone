package stone.modules.versionControl;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

import stone.MasterThread;
import stone.io.GUI;
import stone.io.GUIPlugin;
import stone.util.Path;

public class StagePlugin extends GUIPlugin {

	class ButtonMouseListener implements MouseListener {

		private final boolean ok;

		public ButtonMouseListener(boolean ok) {
			this.ok = ok;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			terminate(this.ok);
		}

	}

	class ChangedFile implements Comparable<ChangedFile> {

		private final String s;
		private final Path p;
		private final String changeType;

		final boolean removed;

		ChangedFile(final char c, final String s) {
			switch (c) {
			case '+':
				this.changeType = "<font color=green>+</font>";
				this.removed = false;
				break;
			case 'M':
				this.changeType = "M";
				this.removed = false;
				break;
			case '-':
				this.changeType = "<font color=red>-</font>";
				this.removed = true;
				break;
			default:
				this.changeType = null;
				this.removed = false;
			}
			this.p = StagePlugin.this.repoRoot.resolve(s.split("/"));
			this.s = s;
		}

		@Override
		public int compareTo(final ChangedFile o) {
			return this.p.compareTo(o.p);
		}

		public boolean equals(final ChangedFile o) {
			return this.p.equals(o);
		}

		@Override
		public boolean equals(final Object o) {
			if (ChangedFile.class.isInstance(o)) {
				return equals(ChangedFile.class.cast(o));
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.p.hashCode();
		}

		public Integer length() {
			return 2 + this.s.length();
		}

		@Override
		public String toString() {
			return this.changeType + " " + this.s;
		}

	}

	class StageActionAddListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			final String s = StagePlugin.this.left.getSelectedText();
			if (s == null) {
				return;
			}
			final String[] files = s.split("[M+-] ");
			for (int i = 1; i < files.length; i++) {
				final char c = s.charAt(files[0].length());
				if (i < (files.length - 1)) {
					files[i] = files[i].substring(0, files[i].length() - 1);
					files[0] += " ";
				}
				final ChangedFile f = new ChangedFile(c, files[i]);
				StagePlugin.this.stagedFiles.add(f);
				StagePlugin.this.unstagedFiles.remove(f);
				files[0] += c + " " + files[i];
			}
			StagePlugin.this.left.setSelectionStart(0);
			StagePlugin.this.left.setSelectionEnd(0);
			updateLeftAndRight();
		}

	}

	class StageActionRemoveListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			final String s = StagePlugin.this.right.getSelectedText();
			if (s == null) {
				return;
			}
			final String[] files = s.split("[M+-] ");
			for (int i = 1; i < files.length; i++) {
				final char c = s.charAt(files[0].length());
				final ChangedFile f = new ChangedFile(c, files[i]);
				StagePlugin.this.unstagedFiles.add(f);
				StagePlugin.this.stagedFiles.remove(f);
				files[0] += c + " " + files[i];
			}
			StagePlugin.this.right.setSelectionStart(0);
			StagePlugin.this.right.setSelectionEnd(0);
			updateLeftAndRight();
		}

	}

	class StageMouseListener implements MouseListener {

		final int h;
		final List<Integer> offset;
		final JEditorPane c;

		StageMouseListener(final JEditorPane c, final List<Integer> offset) {
			this.h = c.getFontMetrics(c.getFont()).getHeight() + 2;
			this.c = c;
			this.offset = offset;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			final int y = e.getPoint().y;
			final int offset = y / this.h;
			if (offset >= this.offset.size()) {
				this.c.setSelectionStart(y);
				this.c.setSelectionEnd(y);
				this.c.revalidate();
				return;
			}
			int start = getStart(offset);
			int end = start + this.offset.get(offset);
			if (this.c.getSelectionEnd() != this.c.getSelectionStart()) {
				// normalize to start and end of line
				start = Math.min(this.c.getSelectionStart(), start);
				int startL = 0, i = 0;
				while (true) {
					final int s = getStart(i);
					if (s > start) {
						--i;
						break;
					}
					startL = s;
					++i;
				}
				start = startL;
				int endL = this.c.getSelectionEnd();
				if (endL > end) {
					while (i < this.offset.size()) {
						final int s = getStart(i) + this.offset.get(i);
						endL = s;
						if (s > this.c.getSelectionEnd()) {
							break;
						}
						++i;
					}
					end = endL;
				}
			}
			this.c.setSelectionStart(start + 1);
			this.c.setSelectionEnd(end + 1);
			this.c.revalidate();
		}

		private int getStart(int line) {
			int offset = 0;
			while (--line >= 0) {
				offset += this.offset.get(line) + 1;
			}
			return offset;
		}
	}

	private final List<Integer> offsetLeft = new LinkedList<>();

	private final List<Integer> offsetRight = new LinkedList<>();

	private final TreeSet<ChangedFile> unstagedFiles = new TreeSet<>();

	private final TreeSet<ChangedFile> stagedFiles = new TreeSet<>();

	private final JEditorPane left = new JEditorPane();

	private final JEditorPane right = new JEditorPane();

	private final MasterThread master;
	private final Status status;
	private final Path repoRoot;

	// unstaged files
	private final Set<String> untracked = new HashSet<>();
	private final Set<String> modified = new HashSet<>();
	private final Set<String> missing = new HashSet<>();
	// staged files
	private final Set<String> added = new HashSet<>();
	private final Set<String> changed = new HashSet<>();
	private final Set<String> removed = new HashSet<>();

	private boolean commit;

	public StagePlugin(final Status status, boolean commit, Path repoRoot,
			final MasterThread master) {
		this.commit = commit;
		this.status = status;
		this.repoRoot = repoRoot;
		this.master = master;
		this.left.setEditable(false);
		this.right.setEditable(false);
	}

	public boolean doCommit(final Git gitSession) {
		try {
			if (this.master.isInterrupted()) {
				return false;
			}
			if (this.commit) {
				for (final ChangedFile f : this.unstagedFiles) {
					if (this.untracked.remove(f.s) || this.missing.remove(f.s)
							|| this.modified.remove(f.s)) {
						continue;
					} else {
						// TODO unstage
					}
				}
				for (final ChangedFile f : this.stagedFiles) {
					if (this.untracked.remove(f.s) || this.missing.remove(f.s)
							|| this.modified.remove(f.s)) {
						if (f.removed) {
							gitSession.rm().addFilepattern(f.s).call();
						} else {
							gitSession.add().addFilepattern(f.s).call();
						}
					}
				}
			}
			return this.commit && !this.stagedFiles.isEmpty();
		} catch (final Exception e) {
			return false;
		}
	}

	private void updateLeftAndRight() {
		final StringBuilder sb = new StringBuilder();
		this.offsetLeft.clear();
		this.offsetRight.clear();

		for (final ChangedFile f : this.unstagedFiles) {
			if (sb.length() == 0) {
				sb.append("<html><font face=\"Courier New\">");
			} else {
				sb.append("<br/>");
			}
			this.offsetLeft.add(f.length());
			sb.append(f.toString());
		}
		sb.append("</font></html>");
		this.left.setContentType("text/html");
		this.left.setText(sb.toString());
		sb.setLength(0);

		for (final ChangedFile f : this.stagedFiles) {
			if (sb.length() == 0) {
				sb.append("<html><font face=\"Courier New\">");
			} else {
				sb.append("<br/>");
			}
			this.offsetRight.add(f.length());
			sb.append(f.toString());
		}
		sb.append("</font></html>");
		this.right.setContentType("text/html");
		this.right.setText(sb.toString());
		sb.setLength(0);

		this.left.revalidate();
		this.right.revalidate();
	}

	@Override
	protected boolean display(final JPanel panel) {
		final JButton abortButton = new JButton("Abort");
		final JButton okButton = new JButton("OK");
		final JButton buttonAdd = new JButton(" -> ");
		final JButton buttonRemove = new JButton(" <- ");
		final SpringLayout layout = new SpringLayout();

		final JScrollPane leftScroll = new JScrollPane(this.left);
		final JScrollPane rightScroll = new JScrollPane(this.right);

		final int height = 600;
		final int width = 800;

		// create Layout
		panel.setLayout(layout);

		leftScroll
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		leftScroll
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		rightScroll
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		rightScroll
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, buttonAdd,
				width / 2, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, buttonAdd,
				height / 2, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, buttonAdd, 0,
				SpringLayout.EAST, leftScroll);

		layout.putConstraint(SpringLayout.NORTH, buttonRemove, 0,
				SpringLayout.SOUTH, buttonAdd);
		layout.putConstraint(SpringLayout.WEST, buttonRemove, 0,
				SpringLayout.WEST, buttonAdd);
		layout.putConstraint(SpringLayout.EAST, buttonRemove, 0,
				SpringLayout.EAST, buttonAdd);

		if (this.commit) {
			panel.add(buttonRemove);
			panel.add(buttonAdd);
		}

		layout.putConstraint(SpringLayout.WEST, leftScroll, 0,
				SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, leftScroll, 0,
				SpringLayout.WEST, abortButton);
		layout.putConstraint(SpringLayout.NORTH, leftScroll, 25,
				SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, leftScroll, -5,
				SpringLayout.SOUTH, panel);
		panel.add(leftScroll);

		layout.putConstraint(SpringLayout.EAST, rightScroll, 0,
				SpringLayout.EAST, panel);
		layout.putConstraint(SpringLayout.WEST, rightScroll, 0,
				SpringLayout.EAST, abortButton);
		layout.putConstraint(SpringLayout.NORTH, rightScroll, 25,
				SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, rightScroll, -5,
				SpringLayout.SOUTH, panel);
		layout.putConstraint(SpringLayout.WEST, okButton, 0, SpringLayout.EAST,
				leftScroll);
		panel.add(rightScroll);

		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, okButton,
				width / 2, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, okButton,
				(height / 2) + 70, SpringLayout.NORTH, panel);
		panel.add(okButton);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, abortButton,
				width / 2, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, abortButton,
				(height / 2) - 70, SpringLayout.NORTH, panel);
		panel.add(abortButton);

		panel.add(new JLabel(
				"                                                               Changed files           ---          Files to be included in next commit"));
		panel.setMaximumSize(new Dimension(width, height));
		panel.setMinimumSize(panel.getMaximumSize());
		panel.setSize(panel.getMaximumSize());

		// do the logic
		this.left
				.setToolTipText("unstaged files - differences between current state and last commit. Next commit unaffected by these files");
		this.right.setToolTipText("staged files - will be part of next commit");

		// unstaged files
		this.untracked.addAll(this.status.getUntracked());
		this.modified.addAll(this.status.getModified());
		this.missing.addAll(this.status.getMissing());
		// staged files
		this.added.addAll(this.status.getAdded());
		this.changed.addAll(this.status.getChanged());
		this.removed.addAll(this.status.getRemoved());

		for (final String s : this.untracked) {
			this.unstagedFiles.add(new ChangedFile('+', s));
		}
		for (final String s : this.modified) {
			this.unstagedFiles.add(new ChangedFile('M', s));
		}
		for (final String s : this.missing) {
			this.unstagedFiles.add(new ChangedFile('-', s));
		}

		for (final String s : this.added) {
			this.stagedFiles.add(new ChangedFile('+', s));
		}
		for (final String s : this.changed) {
			this.stagedFiles.add(new ChangedFile('M', s));
		}
		for (final String s : this.removed) {
			this.stagedFiles.add(new ChangedFile('-', s));
		}
		updateLeftAndRight();

		if (this.commit) {
			this.left.addMouseListener(new StageMouseListener(this.left,
					this.offsetLeft));
			this.right.addMouseListener(new StageMouseListener(this.right,
					this.offsetRight));

			buttonAdd.addMouseListener(new StageActionAddListener());
		}
		// TODO whenever unstage works enable the button
		// buttonRemove.addMouseListener(new StageActionRemoveListener());

		okButton.addMouseListener(new ButtonMouseListener(true));
		abortButton.addMouseListener(new ButtonMouseListener(false));

		lockResize();
		repack(panel.getSize());

		synchronized (GUI.Button.class) {
			if (this.master.isInterrupted()) {
				return true;
			}
			try {
				GUI.Button.class.wait();
			} catch (final InterruptedException e) {
				this.commit = false;
				e.printStackTrace();
				return true;
			}
		}
		panel.removeAll();
		panel.add(new JLabel("Performing changes - please wait"));
		repack(new Dimension(140, 20));
		return true;
	}

	@Override
	protected String getTitle() {
		return "Git Stage";
	}

	final void terminate(boolean ok) {
		if (!ok) {
			this.master.interrupt();
		}
		synchronized (GUI.Button.class) {
			GUI.Button.class.notifyAll();
		}
	}

}
