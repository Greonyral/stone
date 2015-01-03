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
import javax.swing.SpringLayout;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import stone.MasterThread;
import stone.io.GUI;
import stone.io.GUIPlugin;
import stone.util.Path;

public class StagePlugin extends GUIPlugin {

	private final List<Integer> offsetLeft = new LinkedList<>();
	private final List<Integer> offsetRight = new LinkedList<>();

	private final TreeSet<ChangedFile> unstagedFiles = new TreeSet<>();
	private final TreeSet<ChangedFile> stagedFiles = new TreeSet<>();

	private final JEditorPane left = new JEditorPane();
	private final JEditorPane right = new JEditorPane();

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
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			if (!ok) {
				master.interrupt();
			}
			synchronized (GUI.Button.class) {
				GUI.Button.class.notifyAll();
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

	}

	class StageMouseListener implements MouseListener {

		final int h;
		final List<Integer> offset;
		final JEditorPane c;

		StageMouseListener(final JEditorPane c, final List<Integer> offset) {
			h = c.getFontMetrics(c.getFont()).getHeight() + 2;
			this.c = c;
			this.offset = offset;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
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
			final int offset = y / h;
			if (offset >= this.offset.size()) {
				c.setSelectionStart(y);
				c.setSelectionEnd(y);
				c.revalidate();
				return;
			}
			int start = getStart(offset);
			int end = start + this.offset.get(offset);
			if (c.getSelectionEnd() != c.getSelectionStart()) {
				// normalize to start and end of line
				start = Math.min(c.getSelectionStart(), start);
				int startL = 0, i = 0;
				while (true) {
					int s = getStart(i);
					if (s > start) {
						--i;
						break;
					}
					startL = s;
					++i;
				}
				start = startL;
				int endL = c.getSelectionEnd();
				if (endL > end) {
					while (i < this.offset.size()) {
						int s = getStart(i) + this.offset.get(i);
						endL = s;
						if (s > c.getSelectionEnd())
							break;
						++i;
					}
					end = endL;
				}
			}
			c.setSelectionStart(start + 1);
			c.setSelectionEnd(end + 1);
			c.revalidate();
		}

		private int getStart(int line) {
			int offset = 0;
			while (--line >= 0) {
				offset += this.offset.get(line) + 1;
			}
			return offset;
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}
	}

	class StageActionAddListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			final String s = left.getSelectedText();
			if (s == null) {
				return;
			}
			final String[] files = s.split("[M+-] ");
			for (int i = 1; i < files.length; i++) {
				final char c = s.charAt(files[0].length());
				if (i < files.length - 1) {
					files[i] = files[i].substring(0, files[i].length() - 1);
					files[0] += " ";
				}
				final ChangedFile f = new ChangedFile(c, files[i]);
				stagedFiles.add(f);
				unstagedFiles.remove(f);
				files[0] += c + " " + files[i];
			}
			left.setSelectionStart(0);
			left.setSelectionEnd(0);
			updateLeftAndRight();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
		}

	}

	class StageActionRemoveListener implements MouseListener {

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			e.consume();
			final String s = right.getSelectedText();
			if (s == null) {
				return;
			}
			final String[] files = s.split("[M+-] ");
			for (int i = 1; i < files.length; i++) {
				final char c = s.charAt(files[0].length());
				final ChangedFile f = new ChangedFile(c, files[i]);
				unstagedFiles.add(f);
				stagedFiles.remove(f);
				files[0] += c + " " + files[i];
			}
			right.setSelectionStart(0);
			right.setSelectionEnd(0);
			updateLeftAndRight();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			e.consume();
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
				removed = false;
				break;
			case 'M':
				this.changeType = "M";
				removed = false;
				break;
			case '-':
				this.changeType = "<font color=red>-</font>";
				removed = true;
				break;
			default:
				this.changeType = null;
				removed = false;
			}
			p = repoRoot.resolve(s.split("/"));
			this.s = s;
		}

		@Override
		public int compareTo(final ChangedFile o) {
			return p.compareTo(o.p);
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
			return p.hashCode();
		}

		public boolean equals(final ChangedFile o) {
			return p.equals(o);
		}

		public String toString() {
			return changeType + " " + s;
		}

		public Integer length() {
			return 2 + s.length();
		}

	}

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
		left.setEditable(false);
		right.setEditable(false);
	}

	@Override
	protected boolean display(final JPanel panel) {
		final JButton abortButton = new JButton("Abort");
		final JButton okButton = new JButton("OK");
		final JButton buttonAdd = new JButton(" -> ");
		final JButton buttonRemove = new JButton(" <- ");
		final SpringLayout layout = new SpringLayout();

		final JScrollPane leftScroll = new JScrollPane(left);
		final JScrollPane rightScroll = new JScrollPane(right);

		final int height = 600;
		final int width = 800;

		// create Layout
		panel.setLayout(layout);

		leftScroll
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		leftScroll
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		rightScroll
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		rightScroll
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

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

		if (commit) {
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
				height / 2 + 70, SpringLayout.NORTH, panel);
		panel.add(okButton);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, abortButton,
				width / 2, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, abortButton,
				height / 2 - 70, SpringLayout.NORTH, panel);
		panel.add(abortButton);

		panel.add(new JLabel(
				"                                                               Changed files           ---          Files to be included in next commit"));
		panel.setMaximumSize(new Dimension(width, height));
		panel.setMinimumSize(panel.getMaximumSize());
		panel.setSize(panel.getMaximumSize());

		// do the logic
		left.setToolTipText("unstaged files - differences between current state and last commit. Next commit unaffected by these files");
		right.setToolTipText("staged files - will be part of next commit");

		// unstaged files
		untracked.addAll(status.getUntracked());
		modified.addAll(status.getModified());
		missing.addAll(status.getMissing());
		// staged files
		added.addAll(status.getAdded());
		changed.addAll(status.getChanged());
		removed.addAll(status.getRemoved());

		for (final String s : untracked) {
			unstagedFiles.add(new ChangedFile('+', s));
		}
		for (final String s : modified) {
			unstagedFiles.add(new ChangedFile('M', s));
		}
		for (final String s : missing) {
			unstagedFiles.add(new ChangedFile('-', s));
		}

		for (final String s : added) {
			stagedFiles.add(new ChangedFile('+', s));
		}
		for (final String s : changed) {
			stagedFiles.add(new ChangedFile('M', s));
		}
		for (final String s : removed) {
			stagedFiles.add(new ChangedFile('-', s));
		}
		updateLeftAndRight();

		if (commit) {
			left.addMouseListener(new StageMouseListener(left, offsetLeft));
			right.addMouseListener(new StageMouseListener(right, offsetRight));

			buttonAdd.addMouseListener(new StageActionAddListener());
		}
		// TODO whenever unstage works enable the button
		// buttonRemove.addMouseListener(new StageActionRemoveListener());

		okButton.addMouseListener(new ButtonMouseListener(true));
		abortButton.addMouseListener(new ButtonMouseListener(false));

		lockResize();
		repack(panel.getSize());

		synchronized (GUI.Button.class) {
			if (master.isInterrupted())
				return true;
			try {
				GUI.Button.class.wait();
			} catch (final InterruptedException e) {
				commit = false;
				e.printStackTrace();
				return true;
			}
		}
		panel.removeAll();
		panel.add(new JLabel("Performing changes - please wait"));
		repack(new Dimension(140, 20));

		return true;
	}

	private void updateLeftAndRight() {
		final StringBuilder sb = new StringBuilder();
		offsetLeft.clear();
		offsetRight.clear();

		for (ChangedFile f : unstagedFiles) {
			if (sb.length() == 0)
				sb.append("<html><font face=\"Courier New\">");
			else
				sb.append("<br/>");
			offsetLeft.add(f.length());
			sb.append(f.toString());
		}
		sb.append("</font></html>");
		left.setContentType("text/html");
		left.setText(sb.toString());
		sb.setLength(0);

		for (ChangedFile f : stagedFiles) {
			if (sb.length() == 0)
				sb.append("<html><font face=\"Courier New\">");
			else
				sb.append("<br/>");
			offsetRight.add(f.length());
			sb.append(f.toString());
		}
		sb.append("</font></html>");
		right.setContentType("text/html");
		right.setText(sb.toString());
		sb.setLength(0);

		left.revalidate();
		right.revalidate();
	}

	@Override
	protected String getTitle() {
		return "Git Stage";
	}

	public boolean doCommit(final Git gitSession) {
		try {
			if (master.isInterrupted())
				return false;
			if (commit) {
				for (final ChangedFile f : unstagedFiles) {
					if (untracked.remove(f.s) || missing.remove(f.s)
							|| modified.remove(f.s)) {
						continue;
					} else {
						// TODO unstage
					}
				}
				for (final ChangedFile f : stagedFiles) {
					if (untracked.remove(f.s) || missing.remove(f.s)
							|| modified.remove(f.s)) {
						if (f.removed) {
							gitSession.rm().addFilepattern(f.s).call();
						} else {
							gitSession.add().addFilepattern(f.s).call();
						}
					}
				}
			}
			return commit && !stagedFiles.isEmpty();
		} catch (final Exception e) {
			return false;
		}
	}

}
