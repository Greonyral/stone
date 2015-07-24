package stone.modules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.GUIInterface.Button;
import stone.io.IOHandler;
import stone.modules.fileEditor.ChangeNumberingGUI;
import stone.modules.fileEditor.ChangeTitleGUI;
import stone.modules.fileEditor.EditorPlugin;
import stone.modules.fileEditor.FileEditorPlugin;
import stone.modules.fileEditor.InvalidNameSchemeException;
import stone.modules.fileEditor.NameScheme;
import stone.modules.fileEditor.NumberingGUI;
import stone.modules.fileEditor.SongChangeData;
import stone.modules.fileEditor.UniformSongsGUI;
import stone.util.BooleanOption;
import stone.util.Flag;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.StringOption;


/**
 * Class to manipulate modification date using the commit history
 * 
 * @author Nelphindal
 */

public class FileEditor implements Module {

	private final static String SECTION = "[fileEditor]";

	private final static int VERSION = 4;

	private static final String DEFAULT_SCHEME = "%title %index/%total [%instrument]$1{ (%duration)}$2{ %b %d}";

	final static BooleanOption createChangeNumberingOption(
			final OptionContainer oc) {
		return new BooleanOption(oc, "changeNumbering",
				"Changes the numbering of one or more songs.",
				"Change song numbering", Flag.NoShortFlag, "change-numbering",
				FileEditor.SECTION, null, false);
	}

	final static BooleanOption createChangeTitleOption(final OptionContainer oc) {
		return new BooleanOption(oc, "changeTitle",
				"Changes the title of one or more songs.", "Change song title",
				't', "change-title", FileEditor.SECTION, null, false);
	}

	final static BooleanOption createModDateOption(final OptionContainer oc) {
		return new BooleanOption(oc, "modDate",
				"Restores the modification date of files in your repository.",
				"Restore mod-date", 'm', "rst-mod", FileEditor.SECTION, null,
				false);
	}

	final static StringOption createSongSchemeOption(final OptionContainer oc) {
		return new StringOption(oc, "uniformScheme",
				"Changes the scheme for the uniform-songs option. Please have look"
						+ "in tha manual for the syntax", "Name scheme",
				Flag.NoShortFlag, "song-scheme", FileEditor.SECTION, "scheme",
				FileEditor.DEFAULT_SCHEME);
	}

	final static BooleanOption createUniformSongsOption(final OptionContainer oc) {
		return new BooleanOption(oc, "uniform",
				"Changes the titles of songs, matching a name scheme.",
				"Uniform song titles", Flag.NoShortFlag, "uniform-songs",
				FileEditor.SECTION, null, false);
	}

	final StringOption SONG_SCHEME;
	final BooleanOption MOD_DATE;
	final BooleanOption CHANGE_TITLE;
	final BooleanOption CHANGE_NUMBERING;
	final BooleanOption UNIFORM_SONGS;

	private final IOHandler io;
	private final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();

	private final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();

	private final Map<String, Integer> changed = new HashMap<>();

	private final Set<String> visited = new HashSet<>();

	private final MasterThread master;

	private final Map<Path, SongChangeData> changes = new HashMap<>();

	private NameScheme scheme;

	private final Main main;

	private final Module songdata;

	/**
	 * Constructor for building versionInfo
	 */
	public FileEditor() {
		this.io = null;
		this.master = null;
		this.MOD_DATE = null;
		this.CHANGE_TITLE = null;
		this.CHANGE_NUMBERING = null;
		this.UNIFORM_SONGS = null;
		this.SONG_SCHEME = null;
		this.main = null;
		this.songdata = null;
	}

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * 
	 * @param sc
	 *            container providing runtime dependent information
	 */
	public FileEditor(final StartupContainer sc) {
		this.io = sc.getIO();
		this.master = sc.getMaster();
		this.MOD_DATE = FileEditor.createModDateOption(sc.getOptionContainer());
		this.CHANGE_TITLE = FileEditor.createChangeTitleOption(sc
				.getOptionContainer());
		this.CHANGE_NUMBERING = FileEditor.createChangeNumberingOption(sc
				.getOptionContainer());
		this.UNIFORM_SONGS = FileEditor.createUniformSongsOption(sc
				.getOptionContainer());
		this.SONG_SCHEME = FileEditor.createSongSchemeOption(sc
				.getOptionContainer());
		this.main = sc.getMain();
		songdata = master.getModule("SongData");
	}

	/**
	 * @param currentDir currently processed directory
	 * @return directories at given directory
	 */
	public final String[] getDirs(final Path currentDir) {
		return ((stone.modules.SongData) songdata).getDirs(currentDir);
	}

	/**
	 * @param currentDir currently processed directory
	 * @return files at given directory
	 */
	public final String[] getFiles(final Path currentDir) {
		return ((SongData) this.songdata).getSongs(currentDir);
	}

	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>(4);
		list.add(this.UNIFORM_SONGS);
		list.add(this.SONG_SCHEME);
		list.add(this.CHANGE_TITLE);
		list.add(this.CHANGE_NUMBERING);
		list.add(this.MOD_DATE);
		return list;
	}

	@Override
	public final int getVersion() {
		return FileEditor.VERSION;
	}

	@Override
	public final Module init(final StartupContainer sc) {
		return this;
	}

	@Override
	public final void repair() {
		// nothing to do
	}

	@Override
	public final void run() {
		if (getVersion() == 0) {
			this.io.printMessage(
					"Editing of files is not functional",
					"Module FileEditor has not been implemented yet.\nPlease try it again with a later version.",
					true);
			return;
		}
		if (this.master.isInterrupted()) {
			return;
		}
		try {
			if (this.UNIFORM_SONGS.getValue()) {
				((SongData) songdata).fill();
				final FileEditorPlugin plugin = new UniformSongsGUI(this,
						((SongData) songdata).getRoot(), "Applying");
				this.io.handleGUIPlugin(plugin);
				uniformSongs(plugin.getSelection());
			}
			if (this.CHANGE_TITLE.getValue()) {
				((SongData) songdata).fill();
				final FileEditorPlugin plugin = new ChangeTitleGUI(this,
						((SongData) songdata).getRoot(), "Changing title");
				this.io.handleGUIPlugin(plugin);
				changeTitle(plugin.getSelection());
			}
			if (this.master.isInterrupted()) {
				return;
			}
			if (this.CHANGE_NUMBERING.getValue()) {
				((SongData) songdata).fill();
				final FileEditorPlugin plugin = new ChangeNumberingGUI(this,
						((SongData) songdata).getRoot(), "Editing file");
				this.io.handleGUIPlugin(plugin);
				changeNumbering(plugin.getSelection());
			}
			if (this.master.isInterrupted()) {
				return;
			}
			for (final SongChangeData scd : this.changes.values()) {
				scd.revalidate(this.io, getNameScheme());
			}

			if (this.MOD_DATE.getValue()) {
				resetModDate();
			}
		} catch (final InvalidNameSchemeException e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final void changeNumbering(final Set<Path> selection) {
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		for (final Path song : selectionFiles) {
			final SongChangeData data = get(song);
			final NumberingGUI plugin = new NumberingGUI(data, this.io, "Renumbering");
			if (this.master.isInterrupted()) {
				return;
			}
			this.io.handleGUIPlugin(plugin);
			if (this.io.getGUI().getPressedButton() == Button.ABORT) {
				this.master.interrupt();
				return;
			}
			plugin.copyFieldsToMaps();
		}

	}

	private final void changeTitle(final Set<Path> selection) {
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		for (final Path file : selectionFiles) {
			final SongChangeData scd = get(file);
			final EditorPlugin plugin = new EditorPlugin(scd.getTitle(),
					"Chance title of "
							+ file.relativize(((SongData) songdata).getRoot()), "Changing title");
			this.io.handleGUIPlugin(plugin);
			scd.setTitle(plugin.get());
		}
	}

	private final void diff(final Git session, final RevWalk walk,
			final RevCommit commitNew, final ObjectReader reader)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		int i = 0;
		if (commitNew.getParentCount() == 0) {

			final RevTree treeNew = commitNew.getTree();
			this.treeParserNew.reset(reader, treeNew.getId());

			final int time = commitNew.getCommitTime();
			try {
				final List<DiffEntry> diffs = session.diff()
						.setOldTree(new EmptyTreeIterator())
						.setNewTree(this.treeParserNew)
						.setShowNameAndStatusOnly(true).call();
				final Iterator<DiffEntry> diffsIterator = diffs.iterator();
				while (diffsIterator.hasNext()) {
					this.changed.put(diffsIterator.next().getNewPath(), time);
				}
			} catch (final GitAPIException e) {
				this.io.handleException(ExceptionHandle.CONTINUE, e);
			}
		} else {
			while (i < commitNew.getParentCount()) {
				final RevCommit commitOld = commitNew.getParent(i++);
				if (this.visited.add(commitOld.getName())) {
					final RevTree treeOld = walk.parseTree(commitOld);
					final RevTree treeNew = commitNew.getTree();
					diff(session, walk, commitOld, reader);

					this.treeParserNew.reset(reader, treeNew.getId());
					this.treeParserOld.reset(reader, treeOld.getId());

					final int time = commitNew.getCommitTime();
					try {
						final List<DiffEntry> diffs = session.diff()
								.setOldTree(this.treeParserOld)
								.setNewTree(this.treeParserNew)
								.setShowNameAndStatusOnly(true).call();
						final Iterator<DiffEntry> diffsIterator = diffs
								.iterator();
						while (diffsIterator.hasNext()) {
							this.changed.put(diffsIterator.next().getNewPath(),
									time);
						}
					} catch (final GitAPIException e) {
						this.io.handleException(ExceptionHandle.CONTINUE, e);
					}
				}
			}
		}

	}

	private final SongChangeData get(final Path file) {
		final SongChangeData change = this.changes.get(file);
		if (change != null) {
			return change;
		}
		final SongChangeData data = new SongChangeData(
				((SongData) songdata).getVoices(file), this.main);
		this.changes.put(file, data);
		return data;
	}


	private final NameScheme getNameScheme() throws InvalidNameSchemeException {
		if (this.scheme == null) {
			this.scheme = new NameScheme(this.SONG_SCHEME.value());
		}
		return this.scheme;
	}

	private final void resetModDate() {
		final Path repo = ((SongData) songdata).getRoot()
				.resolve(
						this.main.getConfigValue(Main.VC_SECTION,
								Main.REPO_KEY, "band"));
		try {
			final Git session = Git.open(repo.toFile());
			try {
				final ObjectId head = session.getRepository().getRef("HEAD")
						.getObjectId();
				final RevWalk walk = new RevWalk(session.getRepository());

				final RevCommit commit = walk.parseCommit(head);
				final ObjectReader reader = session.getRepository()
						.newObjectReader();

				diff(session, walk, commit, reader);
				reader.release();

				for (final Map.Entry<String, Integer> mod : this.changed
						.entrySet()) {
					final File f = repo.resolve(mod.getKey()).toFile();
					if (f.exists()) {
						f.setLastModified(TimeUnit.SECONDS.toMillis(mod
								.getValue()));
					}
				}

				this.io.printMessage(null,
						"update of modification time completed", true);
			} finally {
				session.close();
			}
		} catch (final IOException e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final TreeSet<Path> selectFilesOnly(Set<Path> selection) {
		final TreeSet<Path> selectionFiles = new TreeSet<>();
		final ArrayDeque<Path> queue = new ArrayDeque<>(selection);
		while (!queue.isEmpty()) {
			final Path p = queue.remove();
			if (p.toFile().isDirectory()) {
				for (final String dir : ((SongData) songdata).getDirs(p)) {
					if (dir.equals("..")) {
						continue;
					}
					queue.add(p.resolve(dir));
				}
				for (final String song : ((SongData) songdata).getSongs(p)) {
					selectionFiles.add(p.resolve(song));
				}
			} else {
				selectionFiles.add(p);
			}
		}
		return selectionFiles;
	}

	private final void uniformSongs(final Set<Path> selection)
			throws InvalidNameSchemeException {
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		this.io.startProgress("Appying name scheme to " + selectionFiles.size()
				+ " files", selectionFiles.size());
		for (final Path file : selectionFiles) {
			if (this.master.isInterrupted()) {
				return;
			}
			final SongChangeData scd = get(file);
			scd.uniform(this.io, getNameScheme());
			this.io.updateProgress();
		}
		this.io.endProgress("name scheme applied");
	}

	@Override
	public final void dependingModules(final Set<String> set) {
		set.add("SongData");
	}
}
