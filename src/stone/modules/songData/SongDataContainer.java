package stone.modules.songData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import stone.Container;
import stone.MasterThread;
import stone.StartupContainer;
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

	public static Map<Integer, String> readExternal(final InputStream is,
			final DeserializeContainer in) throws IOException {
		int parts = in.readSize(is);
		final Map<Integer, String> voices = new TreeMap<>();
		while (parts-- > 0) {
			final int id = in.readSize(is);
			final String value = in.read(is);
			voices.put(id, value);
		}
		return voices;
	}

	public static void writeExternal(final Map<Integer, String> voices,
			final SerializeConainer out) throws IOException {
		out.writeSize(voices.size());
		for (final Map.Entry<Integer, String> entry : voices.entrySet()) {
			out.writeSize(entry.getKey().intValue());
			out.write(entry.getValue());
		}
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

	private final IOHandler io;

	private final TaskPool taskPool;

	private final MasterThread master;

	/**
	 * @param sc
	 */
	public SongDataContainer(final StartupContainer sc) {
		this.io = sc.getIO();
		this.taskPool = sc.getTaskPool();
		this.master = sc.getMaster();
		final String home = sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		assert home != null;
		final Path basePath = Path.getPath(home.split("/")).resolve("Music");
		if (!basePath.exists()) {
			if (!basePath.getParent().exists() || !basePath.toFile().mkdir()) {
				this.io.printError(
						Main.formatMaxLength(
								basePath,
								null,
								"The default path or the path defined in the config-file does not exist:",
								". Please look into the manual for more information."),
						false);
			}
		}
		this.tree = new DirTree(basePath);
	}

	/**
	 * fills the container
	 */
	public final void fill() {
		if (this.master.isInterrupted()) {
			return;
		}
		Debug.print("Searching for songs at \"" + this.tree.getRoot() + "\".\n");

		final Deserializer sdd = Deserializer.init(this, this.master);
		final Scanner scanner = new Scanner(this.master, sdd, this.tree);
		final Crawler crawler = new Crawler(sdd);
		final Runnable taskDeserial = sdd.getDeserialTask();

		if (taskDeserial == null) {
			this.taskPool.addTaskForAll(crawler, 30);
			this.taskPool.addTaskForAll(scanner);
			try {
				sdd.deserialize();
			} catch (final IOException e) {
				this.master.interrupt();
				e.printStackTrace();
				sdd.abort();
			}
		} else {
			this.taskPool.addTaskForAll(taskDeserial, 75);
			this.taskPool.addTaskForAll(crawler, 25);
			this.taskPool.addTaskForAll(scanner);
		}

		crawler.run();
		scanner.run();
		this.taskPool.waitForTasks();
		if (this.master.isInterrupted()) {
			sdd.abort();
			return;
		} else {
			sdd.finish();
		}

		for (final AbtractEoWInAbc e : AbtractEoWInAbc.messages.values()) {
			this.io.printError(e.printMessage(), true);
		}

		Debug.print("%4d songs found -", sdd.songsFound());
	}

	/**
	 * Returns all directories at given directory
	 * 
	 * @param directory
	 * @return directories at given directory
	 */
	public final String[] getDirs(final Path directory) {
		final Set<String> dirs = this.tree.getDirs(directory);
		if (directory == this.tree.getRoot()) {
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
		return this.io;
	}

	/**
	 * @return the base of relative paths
	 */
	public final Path getRoot() {
		return this.tree.getRoot();
	}

	/**
	 * Returns all songs at given directory
	 * 
	 * @param directory
	 * @return songs at given directory
	 */
	public final String[] getSongs(final Path directory) {
		final Set<String> files = this.tree.getFiles(directory);
		return files.toArray(new String[files.size()]);
	}

	/**
	 * @param song
	 * @return the songs of given song
	 */
	public final SongData getVoices(final Path song) {
		return this.tree.get(song);
	}

	/**
	 * Returns the number of songs
	 * 
	 * @return container size
	 */
	public final int size() {
		return this.tree.getFilesCount();
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

		outMaster = this.io.openOut(masterPluginData);
		try {
			// head
			this.io.write(outMaster, "return\r\n{\r\n");

			// section dirs
			this.io.write(outMaster, "\t[\"Directories\"] =\r\n\t{\r\n");
			final Iterator<Path> dirIterator = this.tree.dirsIterator();
			if (dirIterator.hasNext()) {
				this.io.write(outMaster, "\t\t[1] = \"/\"");
				for (int dirIdx = 2; dirIterator.hasNext(); dirIdx++) {
					this.io.writeln(outMaster, ",");
					this.io.write(outMaster, "\t\t["
							+ dirIdx
							+ "] = \"/"
							+ dirIterator.next()
									.relativize(this.tree.getRoot()) + "/\"");
				}
				this.io.writeln(outMaster, "");
			}
			this.io.writeln(outMaster, "\t},");

			// section songs
			this.io.writeln(outMaster, "\t[\"Songs\"] =");
			int songIdx = 0;

			final Iterator<Path> songsIterator = this.tree.filesIterator();
			while (songsIterator.hasNext()) {
				final Path path = songsIterator.next();
				final SongData song = this.tree.get(path);
				if (songIdx++ == 0) {
					this.io.writeln(outMaster, "\t{");
				} else {
					this.io.writeln(outMaster, "\t\t},");
				}

				this.io.writeln(outMaster, "\t\t[" + songIdx + "] =");
				this.io.writeln(outMaster, "\t\t{");
				final String name;
				name = path.getFilename().substring(0,
						path.getFilename().lastIndexOf("."));

				this.io.write(outMaster, "\t\t\t[\"Filepath\"] = \"/");
				if (path.getParent() != this.tree.getRoot()) {
					this.io.write(outMaster,
							path.getParent().relativize(this.tree.getRoot()));
					this.io.write(outMaster, "/");
				}
				this.io.writeln(outMaster, "\",");
				this.io.writeln(outMaster, "\t\t\t[\"Filename\"] = \"" + name
						+ "\",");
				this.io.writeln(outMaster, "\t\t\t[\"Tracks\"] = ");
				this.io.write(outMaster, song.toPluginData());
				this.io.updateProgress();
			}

			// tail
			this.io.writeln(outMaster, "\t\t}");
			this.io.writeln(outMaster, "\t}");
			this.io.write(outMaster, "}");
		} finally {
			this.io.close(outMaster);
		}
	}

	final DirTree getDirTree() {
		return this.tree;
	}
}
