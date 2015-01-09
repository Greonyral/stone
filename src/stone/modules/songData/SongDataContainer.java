package stone.modules.songData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import stone.Container;
import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.modules.Main;
import stone.util.Debug;
import stone.util.Path;
import stone.util.TaskPool;

/**
 * Central class for holding all data related to the songs
 * 
 * @author Nelphindal
 */
public class SongDataContainer implements Container {

	/**
	 * @param sc
	 * @return the created new instance
	 */
	public final static Container create(final StartupContainer sc) {
		return new SongDataContainer(sc);
	}

	@SuppressWarnings("unused")
	private final static void cleanUp(final StringBuilder title_i) {
		int i = title_i.indexOf("]");
		while (i > 0) {
			title_i.replace(i, i + 1, "] ");
			i = title_i.indexOf("]", i + 1);
		}
	}

	private final DirTree tree;

	private final Set<Path> songsFound = new HashSet<>();

	
	private final IOHandler io;

	private final TaskPool taskPool;

	private final MasterThread master;

	private final boolean crawlNeeded = true;

	private boolean dirty = true;

	/**
	 * @param sc
	 */
	public SongDataContainer(final StartupContainer sc) {
		io = sc.getIO();
		taskPool = sc.getTaskPool();
		master = sc.getMaster();
		final String home = sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		assert home != null;
		final Path basePath = Path.getPath(home.split("/")).resolve("Music");
		if (!basePath.exists()) {
			if (!basePath.getParent().exists() || !basePath.toFile().mkdir()) {
				io.printError(
						Main.formatMaxLength(
								basePath,
								null,
								"The default path or the path defined in the config-file does not exist:",
								". Please look into the manual for more information."),
						false);
			}
		}
		tree = new DirTree(basePath);
	}

	/**
	 * fills the container
	 */
	public final void fill() {
		if (master.isInterrupted()) {
			return;
		}
		if (dirty) {
			Debug.print("Searching for songs at " + tree.getRoot() + ".");
	
			final Scanner scanner;
			final SongDataDeserializer sdd = SongDataDeserializer.init(
					tree.getRoot(), io);
			if (crawlNeeded) {
				final Crawler crawler;
				crawler = new Crawler(sdd, new ArrayDeque<Path>());
				scanner = new Scanner(master, sdd, tree, songsFound);
				taskPool.addTaskForAll(crawler, scanner);

				try {
					sdd.deserialize(this);
					synchronized (io) {
						crawler.historySolved();
						if (crawler.terminated()) {
							io.setProgressSize(crawler.getProgress());
						}
					}
				} catch (final IOException e) {
					sdd.abort();
					io.handleException(ExceptionHandle.SUPPRESS, e);
				}
			} else {
				scanner = new Scanner(master, sdd, tree, songsFound);
				taskPool.addTaskForAll(scanner);
			}
			taskPool.waitForTasks();
			dirty = false;
			io.endProgress();
			
			for (final AbtractEoWInAbc e : AbtractEoWInAbc.messages.values()) {
				io.printError(e.printMessage(), true);
			}

			Debug.print("%4d songs found -", size());
		}
	}

	/**
	 * Returns all directories at given directory
	 * 
	 * @param directory
	 * @return directories at given directory
	 */
	public final String[] getDirs(final Path directory) {
		final Set<String> dirs = tree.getDirs(directory);
		if (directory == tree.getRoot()) {
			return dirs.toArray(new String[dirs.size()]);
		}
		final String[] array = dirs.toArray(new String[dirs.size() + 1]);
		System.arraycopy(array, 0, array, 1, dirs.size());
		array[0] = "..";
		return array;
	}

	/**
	 * Returns the used IO-Handler
	 * 
	 * @return the used IO-Handler
	 */
	public final IOHandler getIOHandler() {
		return io;
	}

	/**
	 * @return the base of relative paths
	 */
	public final Path getRoot() {
		return tree.getRoot();
	}

	/**
	 * Returns all songs at given directory
	 * 
	 * @param directory
	 * @return songs at given directory
	 */
	public final String[] getSongs(final Path directory) {
		final Set<String> files = tree.getFiles(directory);
		return files.toArray(new String[files.size()]);
	}

	/**
	 * @param song
	 * @return the songs of given song
	 */
	public final SongData getVoices(final Path song) {
		return tree.get(song);
	}

	/**
	 * Returns the number of songs
	 * 
	 * @return container size
	 */
	public final int size() {
		fill();
		return tree.getFilesCount();
	}

	/**
	 * Writes the results of all scanned songs to the file useable for the
	 * Songbook-plugin
	 * 
	 * @param masterPluginData
	 *            the file where the Songbook-plugin expects it
	 */
	public final void writeNewSongbookData(final File masterPluginData) {
		final OutputStream outMaster;

		outMaster = io.openOut(masterPluginData);
		try {
			// head
			io.write(outMaster, "return\r\n{\r\n");

			// section dirs
			io.write(outMaster, "\t[\"Directories\"] =\r\n\t{\r\n");
			final Iterator<Path> dirIterator = tree.dirsIterator();
			if (dirIterator.hasNext()) {
				io.write(outMaster, "\t\t[1] = \"/\"");
				for (int dirIdx = 2; dirIterator.hasNext(); dirIdx++) {
					io.writeln(outMaster, ",");
					io.write(outMaster, "\t\t[" + dirIdx + "] = \"/"
							+ dirIterator.next().relativize(tree.getRoot())
							+ "/\"");
				}
				io.writeln(outMaster, "");
			}
			io.writeln(outMaster, "\t},");

			// section songs
			io.writeln(outMaster, "\t[\"Songs\"] =");
			int songIdx = 0;

			final Iterator<Path> songsIterator = tree.filesIterator();
			while (songsIterator.hasNext()) {
				final Path path = songsIterator.next();
				final SongData song = tree.get(path);
				if (songIdx++ == 0) {
					io.writeln(outMaster, "\t{");
				} else {
					io.writeln(outMaster, "\t\t},");
				}

				io.writeln(outMaster, "\t\t[" + songIdx + "] =");
				io.writeln(outMaster, "\t\t{");
				final String name;
				name = path.getFileName().substring(0,
						path.getFileName().lastIndexOf("."));

				io.write(outMaster, "\t\t\t[\"Filepath\"] = \"/");
				if (path.getParent() != tree.getRoot()) {
					io.write(outMaster,
							path.getParent().relativize(tree.getRoot()));
					io.write(outMaster, "/");
				}
				io.writeln(outMaster, "\",");
				io.writeln(outMaster, "\t\t\t[\"Filename\"] = \"" + name
						+ "\",");
				io.writeln(outMaster, "\t\t\t[\"Tracks\"] = ");
				io.write(outMaster, song.toPluginData());
				io.updateProgress();
			}

			// tail
			io.writeln(outMaster, "\t\t}");
			io.writeln(outMaster, "\t}");
			io.write(outMaster, "}");
		} finally {
			io.close(outMaster);
		}
	}

	final DirTree getDirTree() {
		return tree;
	}
}
