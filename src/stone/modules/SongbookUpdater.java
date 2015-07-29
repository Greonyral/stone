package stone.modules;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import stone.Config;
import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.util.Debug;
import stone.util.Option;
import stone.util.Path;


/**
 * Class allowing to update the data needed for the Songbook-plugin
 * 
 * @author Nelphindal
 */
public final class SongbookUpdater implements Module {

	private final static int VERSION = 8;

	private final IOHandler io;

	private final Main main;

	private final MasterThread master;

	private final static String USER = "UserPreferences.ini";
	/** %HOME%\The Lord of The Rings Online */
	private final Path pluginDataPath;
	private final Path songbookPlugindataPath;

	/**
	 * Constructor for building versionInfo
	 */
	public SongbookUpdater() {
		this.io = null;
		this.master = null;
		this.main = null;
		this.songbookPlugindataPath = null;
		this.pluginDataPath = null;

	}

	/**
	 * Constructor as needed by being a Module
	 * 
	 * @param sc
	 *            -
	 */
	public SongbookUpdater(final StartupContainer sc) {
		this.io = sc.getIO();
		this.main = sc.getMain();
		final String home = sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		final Path basePath = Path.getPath(home.split("/"));
		this.pluginDataPath = basePath.resolve("PluginData");
		this.songbookPlugindataPath = this.pluginDataPath
				.resolve("SongbookUpdateData");
		this.master = sc.getMaster();
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
		return new SongbookUpdater(sc);
	}

	@Override
	public final void repair() {
		final String home = this.main.getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		if (home == null) {
			System.out
					.printf("Unable to determine home - SongbookUpdateData could not been deleted");
			return;
		}
		final Path basePath = Path.getPath(home.split("/"));
		final Path updateDataPath = basePath.resolve("PluginData").resolve(
				"SongbookUpdateData");
		final Path updateDataPathZip = basePath.resolve("PluginData").resolve(
				"SongbookUpdateData.zip");
		if (updateDataPath.exists()) {
			final boolean success = updateDataPath.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					updateDataPath.toString(), success ? "" : " failed");
		}
		if (updateDataPathZip.exists()) {
			final boolean success = updateDataPathZip.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					updateDataPathZip.toString(), success ? "" : " failed");
		}
	}

	/**
	 * Causes the re-writing of Songbook.plugindata for all profiles found in
	 * UserPreferences.ini
	 */
	@Override
	public final void run() {
		if (this.master.isInterrupted()) {
			return;
		}
		master.getModule("SongData").run(); // search songs and create entries
		updateSongbookData();
	}

	private void updateSongbookData() {
		if (this.master.isInterrupted()) {
			return;
		}
		/*
		 * creates the file needed for Songbook-plugin
		 */
		final long start = System.currentTimeMillis();
		final Set<String> profiles = new HashSet<>();
		final stone.modules.SongData data = (stone.modules.SongData) master
				.getModule("SongData");

		if (!this.pluginDataPath.exists()) {
			if (!this.pluginDataPath.toFile().mkdir()) {
				this.io.printMessage(null,
						"Missing PluginData directory could not be created",
						true);
			}
			return;
		}
		final File userIni = this.pluginDataPath.getParent().resolve(USER)
				.toFile();
		if (!userIni.exists()) {
			this.io.printMessage(
					"UserPreferences.ini not found",
					"\""
							+ Main.formatMaxLength(
									this.pluginDataPath.getParent(), USER)
							+ "\"\n"
							+ "not found. Check if the path is correct or start LoTRO and login to create it.\n"
							+ "The tool is using the file to get your account names.\n"
							+ "\nScan aborted.", true);
			return;
		}
		final stone.io.InputStream in = this.io.openIn(userIni);
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
					this.io.handleException(ExceptionHandle.CONTINUE, e);
					break;
				}
			} while (true);
		}
		this.io.close(in);

		if (this.master.isInterrupted()) {
			return;
		}

		final File masterPluginData = this.songbookPlugindataPath.toFile();
		masterPluginData.deleteOnExit();

		// write master plugindata and updateFileNew
		this.io.startProgress("Writing " + masterPluginData.getName(),
				data.size());
		data.writeNewSongbookData(masterPluginData);

		this.io.startProgress("", profiles.size());
		Debug.print("%2d profiles found:\n", profiles.size());
		for (final String profile : profiles) {
			Debug.print("- %s\n", profile);
		}

		// copy from master plugindata to each profile
		final Iterator<String> profilesIter = profiles.iterator();
		while (profilesIter.hasNext()) {
			final String profile = profilesIter.next();
			this.io.setProgressTitle("Writing Songbook.plugindata " + profile);
			final File target = this.pluginDataPath.resolve(profile)
					.resolve("AllServers").resolve("SongbookData.plugindata")
					.toFile();
			if (!target.exists()) {
				try {
					target.getParentFile().mkdirs();
					target.createNewFile();
				} catch (final IOException e) {
					this.io.handleException(ExceptionHandle.SUPPRESS, e);
					this.io.updateProgress();
					continue;
				}
			}
			final OutputStream out = this.io.openOut(target);
			this.io.write(this.io.openIn(masterPluginData), out);
			this.io.close(out);
			this.io.updateProgress();
		}
		masterPluginData.delete();
		this.io.endProgress("");
		final long end = System.currentTimeMillis();
		Debug.print("needed %s for updating songbook with %d song(s)",
				stone.util.Time.delta(end - start), data.size());
		if (Config.getInstance().getValue("mainClass").equals("Main_susa")) {
			this.io.printMessage(null,
					"Update of your songbook is complete.\nAvailable songs: "
							+ data.size(), false);
			synchronized(master) {
				try {
					master.wait(java.util.concurrent.TimeUnit.MINUTES.toMillis(5));
				} catch (final InterruptedException e) {
					// no relevance
				}
			}
		} else {
			this.io.printMessage(null,
					"Update of your songbook is complete.\nAvailable songs: "
							+ data.size(), true);
		}
	}

	@Override
	public final void dependingModules(final Set<String> set) {
		set.add("SongData");
	}
}
