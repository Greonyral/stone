package stone.modules;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;
import stone.modules.songData.SongDataContainer;
import stone.util.Debug;
import stone.util.Option;
import stone.util.Path;


/**
 * Class allowing to update the data needed for the Songbook-plugin
 * 
 * @author Nelphindal
 */
public final class SongbookUpdater implements Module {

	private final static int VERSION = 6;
	private final static String USER = "UserPreferences.ini";

	private final IOHandler io;
	private final SongDataContainer container;

	/** %HOME%\The Lord of The Rings Online */
	private final Path pluginDataPath;
	private final Path songbookPlugindataPath;
	private final Main main;

	private final Thread master;

	/**
	 * Constructor for building versionInfo
	 */
	public SongbookUpdater() {
		io = null;
		master = null;
		main = null;
		pluginDataPath = null;
		songbookPlugindataPath = null;
		container = null;
	}

	/**
	 * Constructor as needed by being a Module
	 * 
	 * @param sc
	 * @throws InterruptedException
	 */
	public SongbookUpdater(final StartupContainer sc)
			throws InterruptedException {
		io = null;
		master = null;
		pluginDataPath = null;
		songbookPlugindataPath = null;
		main = sc.getMain();
		container = null;
	}

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * @param sc
	 * @param old
	 */
	private SongbookUpdater(final StartupContainer sc,
			final SongbookUpdater old) {
		io = sc.getIO();
		main = old.main;
		final String home =
				sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
						Main.PATH_KEY, null);
		final Path basePath = Path.getPath(home.split("/"));
		pluginDataPath = basePath.resolve("PluginData");
		songbookPlugindataPath =
				pluginDataPath.resolve("SongbookUpdateData");
		master = sc.getMaster();
		container =
				(SongDataContainer) sc
						.getContainer(SongDataContainer.class
								.getCanonicalName());
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		return java.util.Collections.emptyList();
	}

	/** */
	@Override
	public final int getVersion() {
		return SongbookUpdater.VERSION;
	}

	/** */
	@Override
	public final SongbookUpdater init(final StartupContainer sc) {
		return new SongbookUpdater(sc, this);
	}

	@Override
	public final void repair() {
		final String home =
				main.getConfigValue(Main.GLOBAL_SECTION, Main.PATH_KEY,
						null);
		if (home == null) {
			System.out
					.printf("Unable to determine home - SongbookUpdateData could not been deleted");
			return;
		}
		final Path basePath = Path.getPath(home.split("/"));
		final Path updateDataPath =
				basePath.resolve("PluginData").resolve(
						"SongbookUpdateData");
		final Path updateDataPathZip =
				basePath.resolve("PluginData").resolve(
						"SongbookUpdateData.zip");
		if (updateDataPath.exists()) {
			final boolean success = updateDataPath.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					updateDataPath.toString(), success ? "" : " failed");
		}
		if (updateDataPathZip.exists()) {
			final boolean success = updateDataPathZip.delete();
			System.out
					.printf("Delet%s %s%s\n", success ? "ed" : "ing",
							updateDataPathZip.toString(), success ? ""
									: " failed");
		}
	}

	/**
	 * Causes the re-writing of Songbook.plugindata for all profiles found in
	 * UserPreferences.ini
	 */
	@Override
	public final void run() {
		if (master.isInterrupted()) {
			return;
		}
		updateSongbookData();
	}

	/*
	 * creates the file needed for Songbook-plugin
	 */
	private final void updateSongbookData() {
		final long start = System.currentTimeMillis();
		final Set<String> profiles = new HashSet<>();

		if (!pluginDataPath.exists()) {
			if (!pluginDataPath.toFile().mkdir()) {
				io.printMessage(
						null,
						"Missing PluginData directory could not be created",
						true);
			}
			return;
		}
		final File userIni =
				pluginDataPath.getParent().resolve(SongbookUpdater.USER)
						.toFile();
		if (!userIni.exists()) {
			io.printMessage(
					"UserPreferences.ini not found",
					"\""
							+ Main.formatMaxLength(pluginDataPath
									.getParent(), SongbookUpdater.USER)
							+ "\"\n"
							+ "not found. Check if the path is correct or start LoTRO and login to create it.\n"
							+ "The tool is using the file to get your account names.\n"
							+ "\nScan aborted.", true);
			return;
		}
		final InputStream in = io.openIn(userIni);
		if (userIni.length() != 0) {
			do {
				try {
					in.readTo((byte) '[');
					final String line = in.readLine();
					if (line == null) {
						break;
					}
					if (line.startsWith("User_")) {
						do {
							final String userLine = in.readLine();
							if (userLine.startsWith("UserName=")) {
								profiles.add(userLine.substring(9));
								break;
							}
						} while (true);
					}
				} catch (final IOException e) {
					io.handleException(ExceptionHandle.CONTINUE, e);
					break;
				}
			} while (true);
		}
		io.close(in);
		container.fill();

		final File masterPluginData = songbookPlugindataPath.toFile();
		masterPluginData.deleteOnExit();

		// write master plugindata and updateFileNew
		io.startProgress("Writing " + masterPluginData.getName(),
				container.size());
		container.writeNewSongbookData(masterPluginData);

		io.startProgress("", profiles.size());
		Debug.print("%2d profiles found:\n", profiles.size());
		for (final String profile : profiles) {
			Debug.print("- %s\n", profile);
		}

		// copy from master plugindata to each profile
		final Iterator<String> profilesIter = profiles.iterator();
		while (profilesIter.hasNext()) {
			final String profile = profilesIter.next();
			io.setProgressTitle("Writing Songbook.plugindata " + profile);
			final File target =
					pluginDataPath.resolve(profile).resolve("AllServers")
							.resolve("SongbookData.plugindata").toFile();
			if (!target.exists()) {
				try {
					target.getParentFile().mkdirs();
					target.createNewFile();
				} catch (final IOException e) {
					io.handleException(ExceptionHandle.SUPPRESS, e);
					io.updateProgress();
					continue;
				}
			}
			final OutputStream out = io.openOut(target);
			io.write(io.openIn(masterPluginData), out);
			io.close(out);
			io.updateProgress();
		}
		masterPluginData.delete();
		io.endProgress();
		final long end = System.currentTimeMillis();
		Debug.print("needed %s for updating songbook with %d song(s)",
				stone.util.Time.delta(end - start), container.size());
		io.printMessage(null,
				"Update of your songbook is complete.\nAvailable songs: "
						+ container.size(), true);
	}
}
