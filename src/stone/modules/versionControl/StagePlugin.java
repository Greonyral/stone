package stone.modules.versionControl;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;

import stone.MasterThread;
import stone.io.GUI;
import stone.io.GUIPlugin;
import stone.modules.VersionControl;
import stone.util.Debug;


/**
 * 
 * GUI wrapping 'git add'
 * 
 * @author Nelphindal
 * 
 */
public class StagePlugin extends GUIPlugin {

	static final String normalize(final String s) {
		final String a = s.replaceAll("<[^>]*>", "\n");
		final String b1 = a.replaceAll(" *\n +", "");
		final String b2 = b1.replaceAll("[-+M]\n","  ");
		final String c = b2.replaceAll("\n\n*", "\n");
		if (c.startsWith("\n"))
			return c.substring(1);
		return c;
	}

	class ButtonMouseListener implements MouseListener {

		private final boolean ok;

		public ButtonMouseListener(@SuppressWarnings("hiding") boolean ok) {
			this.ok = ok;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			return;
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			return;
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

	class ChangedFile {

		private final char changeTypeC;
		private final String s;
		private final String changeType;
		private boolean encrypt;

		@SuppressWarnings("hiding")
		final boolean removed;
		private int conflict;
		private final Set<String> conflictFiles = new HashSet<>();

		ChangedFile(final char c, @SuppressWarnings("hiding") final String s) {
			this.changeTypeC = c;
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
			case 'C':
				this.changeType = "<font color=red>-</font>";
				this.removed = true;
				break;
			default:
				this.changeType = null;
				this.removed = false;
			}
			this.s = s;
		}

		public void enableEncryption(@SuppressWarnings("hiding") boolean encrypt) {
			if (encrypt && !this.encrypt && (this.changeTypeC != '-')) {
				this.encrypt = true;
			} else if (!encrypt && this.encrypt) {
				this.encrypt = false;
			}
		}

		public Integer length() {
			int length = this.s.length() + 2;
			if (this.encrypt) {
				length += 2;
			}
			if (this.conflict > 0) {
				length += 2;
				for (final String c : this.conflictFiles) {
					length += 6 + c.length();
				}
			}
			return length;
		}

		@Override
		public String toString() {
			if (this.encrypt) {
				return this.changeType
						+ " <font color=gray>E</font> <font bgcolor=yellow>"
						+ this.s + "</font>";
			}
			if (this.conflict > 0) {
				final StringBuilder lines = new StringBuilder();
				lines.append(this.changeType);
				lines.append("<font color=red>C</font> <font bgcolor=red>X");
				lines.append(" ");
				lines.append(this.s);
				lines.append("</font>");
				for (final String c : this.conflictFiles) {
					lines.append("<br/><font color=white>XC </font>");
					lines.append("<font bgcolor=orange>X");
					lines.append(" ");
					lines.append(c);
					lines.append("</font>");
				}
				return lines.toString();
			}
			if (this.changeTypeC == 'C') {
				return this.changeType + " <font color=gray>" + this.s
						+ "</font>";
			}
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
			return;
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			return;
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
			final String[] files = s.split("[M+-]C? ");
			for (int i = 1; i < files.length; i++) {
				final String file;
				{
					final String sTmp = files[i].trim();
					if (sTmp.startsWith("X ")) {
						// conflict lock adding
						continue;
					}
					if (sTmp.endsWith(".enc.abc")) {
						final ChangedFile f = StagePlugin.this.unstagedFiles
								.remove(sTmp);
						assert f != null;
						file = "E " + sTmp.substring(0, sTmp.length() - 8)
								+ ".abc";
						StagePlugin.this.unstagedFiles.put(file, f);
						f.enableEncryption(true);
					} else {
						file = sTmp;
					}
				}
				final ChangedFile f = StagePlugin.this.unstagedFiles
						.remove(file);
				if (f == null) {
					throw new NullPointerException();
				}
				StagePlugin.this.stagedFiles.put(file, f);
			}
			StagePlugin.this.left.setSelectionStart(0);
			StagePlugin.this.left.setSelectionEnd(0);
			updateLeftAndRight();
		}
	}

	class StageActionEncryptListener implements MouseListener {

		private final boolean encrypt;

		StageActionEncryptListener(@SuppressWarnings("hiding") boolean encrypt) {
			this.encrypt = encrypt;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			return;
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			return;
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
			setEncrypt(s, this.encrypt);
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
			final String[] files = setEncrypt(s, false);
			final Set<String> done = new HashSet<>();
			for (int i = 1; i < files.length; i++) {
				final String file = files[i];
				if (done.add(file)) {
					final ChangedFile f = StagePlugin.this.stagedFiles
							.remove(file);
					if (f == null) {
						throw new NullPointerException();
					}
					StagePlugin.this.unstagedFiles.put(file, f);
				}
			}
			StagePlugin.this.right.setSelectionStart(0);
			StagePlugin.this.right.setSelectionEnd(0);
			updateLeftAndRight();
		}
	}

	class StageMouseListener implements MouseListener {

		class Selection {

			private String s;

			public void set(@SuppressWarnings("hiding") final String s,
					int start, int end) {
				this.s = normalize(s.substring(start, end));
			}

			@Override
			public final String toString() {
				return s;
			}

		}

		final int h;
		final JEditorPane c;
		private final Selection selection = new Selection();


		@SuppressWarnings("hiding")
		StageMouseListener(final JEditorPane c) {
			this.h = c.getFontMetrics(c.getFont()).getHeight() + 2;
			this.c = c;
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
			int line = e.getY() / h;
			// normalize to start and end of line
			int start;
			int end;

			if (this.c.getSelectionStart() != this.c.getSelectionEnd()) {
				start = this.c.getSelectionStart();
				while (true) {
					final String s = this.c.getSelectedText().replaceAll(
							"[-+M] ", "\n");
					char startC = s.charAt(0);
					switch (startC) {
					case '\n':
						break;
					default:
						this.c.setSelectionStart(--start);
						continue;
					}
					this.c.setSelectionStart(start);
					break;
				}

				end = this.c.getSelectionEnd();
				while (this.c.getSelectedText().length() == end - start) {
					final String s = this.c.getSelectedText().replaceAll(
							" [-+M]", "\n");
					char endC = s.charAt(s.length() - 1);
					switch (endC) {
					case '\n':
						break;
					default:
						this.c.setSelectionEnd(++end);
						continue;
					}
					this.c.setSelectionEnd(--end);
					break;
				}
			} else {
				final String s = this.c.getText();
				start = s.indexOf("<font");
				while (line-- > 0) {
					start = s.indexOf("<br>", start) + 4;
				}
				end = s.indexOf("<br>", start);
				if (end < 0)
					end = s.length();
				final String d = normalize(s);
				selection.set(s, start, end);
				start = d.indexOf(selection.s) + 1;
				end = start + selection.s.length();
				this.c.setSelectionStart(start);
				this.c.setSelectionEnd(end);
			}
			this.c.revalidate();
		}
	}


	private final TreeMap<String, ChangedFile> unstagedFiles = new TreeMap<>();

	private final TreeMap<String, ChangedFile> stagedFiles = new TreeMap<>();

	private final JEditorPane left = new JEditorPane();

	private final JEditorPane right = new JEditorPane();

	private final MasterThread master;

	private final Status status;
	// unstaged files
	private final Set<String> untracked = new HashSet<>();
	private final Set<String> modified = new HashSet<>();
	private final Set<String> missing = new HashSet<>();

	// staged files
	private final Set<String> added = new HashSet<>();
	private final Set<String> changed = new HashSet<>();
	private final Set<String> removed = new HashSet<>();

	private boolean commit;

	private final DirCache cache;

	private final Map<String, String> checkedInFiles = new HashMap<>();

	/**
	 * Creates a new instance of {@link StagePlugin}.
	 * 
	 * @param status
	 *            status of git repository
	 * @param cache
	 *            dir cache of git repository
	 * @param commit
	 *            <i>true</i> if commit shall be performed
	 * @param master
	 *            the master thread to check on interruption
	 */
	@SuppressWarnings("hiding")
	public StagePlugin(final Status status, final DirCache cache,
			boolean commit, final MasterThread master) {
		super("");
		this.commit = commit;
		this.status = status;
		this.master = master;
		this.cache = cache;
		this.left.setEditable(false);
		this.right.setEditable(false);
	}

	/**
	 * 
	 * @param gitSession
	 *            -
	 * @param vc
	 *            -
	 * @return <i>true</i> if a commit shall be done
	 */
	public boolean doCommit(final Git gitSession, final VersionControl vc) {
		try {
			if (this.master.isInterrupted()) {
				return false;
			}
			if (this.commit) {
				for (final ChangedFile f : this.unstagedFiles.values()) {
					if (!(this.untracked.remove(f.s)
							|| this.missing.remove(f.s) || this.modified
								.remove(f.s))) {
						;
						Debug.print("Rolling back %s\n", f.s);
						gitSession.reset().setRef("HEAD").addPath(f.s).call();
					}
				}
				for (final ChangedFile f : this.stagedFiles.values()) {
					if (this.untracked.remove(f.s) || this.missing.remove(f.s)
							|| this.modified.remove(f.s)) {
						if (f.removed) {
							if (f.changeTypeC == 'C') {
								gitSession.rm().addFilepattern(f.s)
										.setCached(true).call();
							}
							gitSession.rm().addFilepattern(f.s).call();
						} else {
							if (f.encrypt) {
								final String encrypted = vc.encrypt(f.s, null,
										true);
								if (encrypted == null) {
									return false;
								}
								gitSession.add().addFilepattern(encrypted)
										.call();
							} else {
								gitSession.add().addFilepattern(f.s).call();
							}
						}
					}
				}
			}
			if (this.commit && this.stagedFiles.isEmpty()) {
				Debug.print("\nStage clean.\nNo files to commit\n\n");
			}
			return this.commit && !this.stagedFiles.isEmpty();
		} catch (final Exception e) {
			return false;
		}
	}

	private void readCache() {
		final int nEntries = this.cache.getEntryCount();

		for (int i = 0; i < nEntries; ++i) {
			final String pathS = this.cache.getEntry(i).getPathString();
			final int offset = pathS.lastIndexOf('/');
			if (offset < 0) {
				this.checkedInFiles.put(pathS, pathS);
			} else {
				this.checkedInFiles.put(pathS, pathS.substring(offset + 1));
			}
		}
	}

	private String[] setEncrypt(final String s, boolean encrypt) {
		final String[] files = s.split("[M+-]C? ");
		for (int i = 1; i < files.length; i++) {
			final String file0, file1, file2;
			{
				String t = files[i].trim();
				if (t.startsWith("E ")) {
					t = t.substring(2);
				}
				file0 = t;
			}
			file1 = "E " + file0;
			{
				String t = file1;
				if (t.endsWith(".enc.abc")) {
					t = t.substring(0, t.length() - 8);
					t = t + ".abc";
				} else if (t.endsWith(".abc")) {
					t = t.substring(0, t.length() - 4);
					t = t + ".enc.abc";
				}

				file2 = t;
			}
			ChangedFile f = StagePlugin.this.stagedFiles.remove(file0);
			if (f != null) {
				if (f.changeTypeC == 'C') {
					f = null;
				} else if (f.encrypt == encrypt) {
					// already at desired state
					files[i] = file0;
					StagePlugin.this.stagedFiles.put(file0, f);
					continue;
				}
			}
			if (f == null) {
				f = StagePlugin.this.stagedFiles.remove(file1);
				if ((f != null) && (f.encrypt == encrypt)) {
					files[i] = file1;
					StagePlugin.this.stagedFiles.put(file1, f);
				}
			}
			if (f == null) {
				f = StagePlugin.this.stagedFiles.remove(file2);
				if ((f != null) && (f.encrypt == encrypt)) {
					files[i] = file2;
					StagePlugin.this.stagedFiles.put(file2, f);
					continue;
				}
			}
			if (f == null) {
				throw new NullPointerException();
			}
			f.enableEncryption(encrypt);
			if (encrypt && f.encrypt) {
				if (StagePlugin.this.stagedFiles.containsKey(file2)) {
					if (StagePlugin.this.stagedFiles.get(file2).changeTypeC == 'C') {
						files[i] = file0;
						StagePlugin.this.stagedFiles.put(file0, f);
					} else {
						files[i] = file2;
						StagePlugin.this.stagedFiles.put(file2, f);
					}
				} else {
					files[i] = file2;
					StagePlugin.this.stagedFiles.put(file2, f);
					if (file2.endsWith(".enc.abc")) {
						// assume implict encrypted files should not be
						// deleted
						StagePlugin.this.stagedFiles.put(file0,
								new ChangedFile('C', file0));
						StagePlugin.this.missing.add(file0);
					}
				}
			} else if (!encrypt && !f.encrypt) {
				if (StagePlugin.this.stagedFiles.containsKey(file0)) {
					if (StagePlugin.this.stagedFiles.get(file0).changeTypeC == 'C') {
						files[i] = file0;
						StagePlugin.this.stagedFiles.put(file0, f);
					} else {
						files[i] = file2;
						StagePlugin.this.stagedFiles.put(file2, f);
					}
				} else {
					files[i] = file0;
					StagePlugin.this.stagedFiles.put(file0, f);
				}
			} else {
				if (StagePlugin.this.stagedFiles.containsKey(file0)) {
					files[i] = file2;
					StagePlugin.this.stagedFiles.put(file2, f);
				} else {
					files[i] = file0;
					StagePlugin.this.stagedFiles.put(file0, f);
				}
			}
		}
		return files;
	}

	private void testConflict(final ChangedFile c) {
		if (this.checkedInFiles.isEmpty()) {
			readCache();
		}
		final String filename;
		{
			final int offset = c.s.lastIndexOf('/');
			if (offset < 0) {
				filename = c.s;
			} else {
				filename = c.s.substring(offset + 1);
			}
		}
		final String filenameRemove = this.checkedInFiles.remove(c.s);

		final Collection<String> filenames = this.checkedInFiles.values();
		if (filenames.contains(filename)) {
			int files = 0;
			for (final Map.Entry<String, String> e : this.checkedInFiles
					.entrySet()) {
				if (e.getValue().equals(filename)) {
					c.conflictFiles.add(e.getKey());
					++files;
				}

			}
			c.conflict = files;
		}
		if (filenameRemove != null) {
			this.checkedInFiles.put(c.s, filenameRemove);
		}
	}

	private void updateLeftAndRight() {
		final StringBuilder sb = new StringBuilder();

		for (final ChangedFile f : this.unstagedFiles.values()) {
			if (sb.length() == 0) {
				sb.append("<html><font face=\"Courier New\">");
			} else {
				sb.append("<br/>");
			}
			sb.append(f.toString());
		}
		sb.append("</font></html>");
		this.left.setContentType("text/html");
		this.left.setText(sb.toString());
		sb.setLength(0);

		for (final ChangedFile f : this.stagedFiles.values()) {
			if (sb.length() == 0) {
				sb.append("<html><font face=\"Courier New\">");
			} else {
				sb.append("<br/>");
			}
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
		final JButton buttonEncrypt = new JButton(" sec ");
		final JButton buttonPlain = new JButton(" plain ");
		final SpringLayout layout = new SpringLayout();

		final JScrollPane leftScroll = new JScrollPane(this.left);
		final JScrollPane rightScroll = new JScrollPane(this.right);

		final int height = 600;
		final int width = 1200;

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
			Font font = buttonEncrypt.getFont();
			font = font.deriveFont((float) (font.getSize() * 0.8));
			panel.add(buttonRemove);
			panel.add(buttonAdd);
			panel.add(buttonEncrypt);
			panel.add(buttonPlain);
			buttonEncrypt.setFont(font);
			buttonPlain.setFont(font);
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

		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, buttonEncrypt,
				(width / 2), SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, buttonEncrypt, 35,
				SpringLayout.NORTH, panel);

		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, buttonPlain,
				width / 2, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, buttonPlain, 0,
				SpringLayout.SOUTH, buttonEncrypt);


		panel.add(new JLabel(
				"                                                               Changed files           ---          Files to be included in next commit"));
		panel.setMaximumSize(new Dimension(width, height));
		panel.setMinimumSize(panel.getMaximumSize());
		panel.setSize(panel.getMaximumSize());

		// do the logic
		this.left
				.setToolTipText("unstaged files - differences between current state and last commit. "
						+ "Next commit unaffected by these files");
		this.right.setToolTipText("staged files - will be part of next commit");

		// unstaged files
		this.untracked.addAll(this.status.getUntracked());
		this.modified.addAll(this.status.getModified());
		this.missing.addAll(this.status.getMissing());
		// staged files
		this.added.addAll(this.status.getAdded());
		this.changed.addAll(this.status.getChanged());
		this.removed.addAll(this.status.getRemoved());

		final Set<String> filteredOut = new HashSet<>();
		boolean clean = true;

		for (final String s : this.untracked) {
			if (s.endsWith(".abc")) {
				final ChangedFile c = new ChangedFile('+', s);
				clean = false;
				this.unstagedFiles.put(s, c);
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}
		for (final String s : this.modified) {
			if (s.endsWith(".abc")) {
				final ChangedFile c = new ChangedFile('M', s);
				clean = false;
				this.unstagedFiles.put(s, c);
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}
		for (final String s : this.missing) {
			if (s.endsWith(".abc")) {
				final ChangedFile c = new ChangedFile('-', s);
				clean = false;
				this.unstagedFiles.put(s, c);
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}

		for (final String s : this.added) {
			if (s.endsWith(".abc")) {
				final ChangedFile c = new ChangedFile('+', s);
				clean = false;
				if (s.endsWith(".enc.abc")) {
					c.enableEncryption(true);
					this.stagedFiles.put("E " + s.substring(0, s.length() - 8)
							+ ".abc", c);
				} else {
					this.stagedFiles.put(s, c);
				}
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}
		for (final String s : this.changed) {
			if (s.endsWith(".abc")) {
				clean = false;
				final ChangedFile c = new ChangedFile('M', s);
				clean = false;
				if (s.endsWith(".enc.abc")) {
					c.enableEncryption(true);
					this.stagedFiles.put("E " + s.substring(0, s.length() - 8)
							+ ".abc", c);
				} else {
					this.stagedFiles.put(s, c);
				}
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}
		for (final String s : this.removed) {
			if (s.endsWith(".abc")) {
				clean = false;
				final ChangedFile c = new ChangedFile('-', s);
				clean = false;
				if (s.endsWith(".enc.abc")) {
					c.enableEncryption(true);
					this.stagedFiles.put("E " + s, c);

				} else {
					this.stagedFiles.put(s, c);
				}
				testConflict(c);
			} else {
				filteredOut.add(s);
			}
		}

		if (clean) {
			return true;
		}

		this.untracked.removeAll(filteredOut);
		this.modified.removeAll(filteredOut);
		this.missing.removeAll(filteredOut);
		this.added.removeAll(filteredOut);
		this.changed.removeAll(filteredOut);
		this.removed.removeAll(filteredOut);

		updateLeftAndRight();

		if (this.commit) {
			this.left.addMouseListener(new StageMouseListener(this.left));
			this.right.addMouseListener(new StageMouseListener(this.right));
		}

		okButton.addMouseListener(new ButtonMouseListener(true));
		abortButton.addMouseListener(new ButtonMouseListener(false));

		buttonAdd.addMouseListener(new StageActionAddListener());
		buttonRemove.addMouseListener(new StageActionRemoveListener());
		buttonEncrypt.addMouseListener(new StageActionEncryptListener(true));
		buttonPlain.addMouseListener(new StageActionEncryptListener(false));

		lockResize();
		panel.setMinimumSize(new Dimension(width, height));
		panel.setPreferredSize(panel.getMinimumSize());

		repack(panel.getMinimumSize());

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


	@Override
	protected void textmode() {
		// TODO
		System.err.println("Text mode not supported - yet");
	}
}
