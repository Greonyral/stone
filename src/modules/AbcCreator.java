package modules;

import gui.DragObject;
import gui.DropTarget;
import gui.DropTargetContainer;
import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import main.Flag;
import main.Main;
import main.MasterThread;
import main.StartupContainer;
import modules.abcCreator.DragAndDropPlugin;
import modules.abcCreator.DrumMapFileFilter;
import modules.abcCreator.ExecutableFileFilter;
import modules.abcCreator.InstrumentMapFileFilter;
import modules.abcCreator.MidiFileFilter;
import modules.abcCreator.StreamPrinter;
import modules.midiData.MidiInstrument;
import modules.midiData.MidiMap;
import modules.midiData.MidiParser;
import util.FileSystem;
import util.Option;
import util.OptionContainer;
import util.Path;
import util.PathOption;
import util.PathOptionFileFilter;
import util.StringOption;
import util.TaskPool;


/**
 * @author Nelphindal
 */
public class AbcCreator implements Module {

	/**
	 * Enum indicating what type of runnable shall be called
	 * 
	 * @author Nelphindal
	 */
	public enum CallType {
		/**
		 * execute and wait for a runnable jar-archive
		 */
		JAR_WAIT, /**
		 * execute a runnable jar-archive
		 */
		JAR, /**
		 * execute and wait for a runnable exe
		 */
		EXE_WAIT, /**
		 * execute a runnable exe
		 */
		EXE;
	}

	static class InitState {
		static final Object INIT = new Object();
		static final Object READ_JAR = new Object();
		static final Object INSTRUMENT_MAP = new Object();
		static final Object DRUM_MAP = new Object();
		static final Object COPY = new Object();
		static final Object UNPACK_JAR = new Object();

		private IOHandler io;

		int progress, size;
		boolean failed;
		Object state = InitState.INIT;

		private String getMessage() {
			if (state == InitState.DRUM_MAP) {
				return "Copying drum-maps to working dir";
			}
			if (state == InitState.UNPACK_JAR) {
				return "Unpacking BruTE";
			}
			if (state == InitState.COPY) {
				return "Copying BruTE";
			}
			if (state == InitState.INIT) {
				return "Init";
			}
			if (state == "INSTRUMENT_MAP") {
				return "Parsing instrument-map";
			}
			if (state == "DRUM_MAP") {
				return "Parsing drum-maps";
			}
			if (state == InitState.READ_JAR) {
				return "Reading BruTE-archive";
			}
			return "...";
		}

		synchronized final void drawState(final IOHandler io) {
			this.io = io;
			io.startProgress(getMessage(), size);
			io.updateProgress(progress);
		}

		synchronized final boolean failed() {
			return failed;
		}

		synchronized final void incrementSize(int size) {
			this.size += size;
			if (io != null) {
				io.setProgressSize(this.size);
			}
		}

		synchronized final void progress() {
			++progress;
			if (io != null) {
				io.updateProgress();
			}
		}

		synchronized final void setFailed() {
			failed = true;
		}

		synchronized final void setSize(final Object state, int size) {
			if (this.state == state) {
				this.size = size;
				if (io != null) {
					io.setProgressSize(size);
				}
			}
		}

		synchronized final void startPhase(final Object state) {
			this.state = state;
			progress = 0;
			size = -1;
			if (io != null) {
				io.startProgress(getMessage(), size);
			}
		}

		synchronized final void startPhase(final Object state, int size) {
			this.state = state;
			progress = 0;
			this.size = size;
			if (io != null) {
				io.startProgress(getMessage(), size);
			}
		}
	}

	final class MapLock {

		boolean lock = false;

		final synchronized void p() {
			while (lock) {
				try {
					this.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
			lock = true;
		}

		final synchronized void v() {
			notifyAll();
			lock = false;
		}
	}

	/**
	 * The section identfier of global config for all settings related to
	 * the GUI for BruTE
	 */
	public static final String SECTION = "[brute]";

	/**
	 * The key within the section, specifying the directory where to find custom
	 * drum-maps
	 */
	public static final String DRUM_MAP_KEY = "drummaps";
	/**
	 * The maximum id of included drum-map
	 */
	public static final int DRUM_MAPS_COUNT = 4;
	private static final PathOptionFileFilter EXEC_FILTER =
			new ExecutableFileFilter();

	private static final PathOptionFileFilter DRUM_MAP_FILTER =
			new DrumMapFileFilter();

	private static final PathOptionFileFilter INSTR_MAP_FILTER =
			new InstrumentMapFileFilter();

	private final static int VERSION = 1;

	private static final FileFilter midiFilter = new MidiFileFilter();

	private static final Path javaPath = AbcCreator.getJavaPath();

	private final static PathOption createDrumMaps(
			final OptionContainer optionContainer, final TaskPool taskPool) {
		final PathOptionFileFilter ff = AbcCreator.DRUM_MAP_FILTER;
		return new PathOption(optionContainer, taskPool, "drumMapsDir",
				"Select a directory containing default drum maps", "Drum Maps",
				Flag.NoShortFlag, "drums", ff,
				JFileChooser.FILES_AND_DIRECTORIES, AbcCreator.SECTION,
				AbcCreator.DRUM_MAP_KEY, null);
	}

	private final static PathOption createInstrMap(
			final OptionContainer optionContainer, final TaskPool taskPool) {
		final PathOptionFileFilter ff = AbcCreator.INSTR_MAP_FILTER;
		return new PathOption(
				optionContainer,
				taskPool,
				"midi2abcMap",
				"Select a custom map, to map midi-instruments on the isntruments used in LoTRO",
				"Instrument Map", Flag.NoShortFlag, "mapping", ff,
				JFileChooser.FILES_ONLY, AbcCreator.SECTION, "instrumentMap",
				null);
	}

	private final static PathOption createPathToAbcPlayer(
			final OptionContainer optionContainer, final TaskPool taskPool) {
		final PathOptionFileFilter ff = AbcCreator.EXEC_FILTER;
		return new PathOption(
				optionContainer,
				taskPool,
				"abcPlayer",
				"The path to the abc-player. Leave it blank if you dont have an abc-player or you do not want to play songs to test",
				"Abc-Player", 'a', "abc-player", ff, JFileChooser.FILES_ONLY,
				AbcCreator.SECTION, "player", null);
	}

	private final static StringOption createStyle(
			final OptionContainer optionContainer) {
		return new StringOption(
				optionContainer,
				"style",
				"The style to use for generated abc. Possible values are rocks and TSO",
				"Style", Flag.NoShortFlag, "style", AbcCreator.SECTION,
				"style", "Rocks");
	}

	private final static Path getJavaPath() {
		final Path javaBin =
				Path.getPath(System.getProperty("java.home")).resolve("bin");
		final Path javaPath;
		if (FileSystem.type == FileSystem.OSType.WINDOWS) {
			javaPath = javaBin.resolve("java.exe");
		} else {
			javaPath = javaBin.resolve("java");
		}
		return javaPath;
	}

	private final PathOption ABC_PLAYER;

	private final PathOption DRUM_MAPS;

	private final PathOption INSTRUMENT_MAP;

	private final StringOption STYLE;

	private final IOHandler io;

	private final Path bruteDir;

	private final Path brutesMidi;// = bruteDir.resolve("mid.mid");

	private final Path brutesMap;// = bruteDir.resolve("out.config");

	private final Path brutesAbc;// = bruteDir.resolve("new.abc");

	private final MidiParser parser;

	private final DropTargetContainer[] targets;

	private final TaskPool taskPool;

	private final MasterThread master;

	private final Path wdDir;

	private final MapLock mapLock = new MapLock();

	private Path midi, abc;

	private final Set<Integer> maps = new HashSet<>();

	private final static InitState initState = new InitState();

	/**
	 * Constructor for building versionInfo
	 */
	public AbcCreator() {
		ABC_PLAYER = null;
		INSTRUMENT_MAP = null;
		STYLE = null;
		DRUM_MAPS = null;
		wdDir = null;
		io = null;
		master = null;
		targets = null;
		parser = null;
		taskPool = null;
		bruteDir = null;
		brutesMidi = brutesMap = brutesAbc = null;
	}

	/**
	 * @param sc
	 * @throws InterruptedException
	 */
	public AbcCreator(final StartupContainer sc) throws InterruptedException {
		while (sc.getOptionContainer() == null) {
			synchronized (sc) {
				sc.wait();
			}
		}
		ABC_PLAYER =
				AbcCreator.createPathToAbcPlayer(sc.getOptionContainer(),
						sc.getTaskPool());
		INSTRUMENT_MAP =
				AbcCreator.createInstrMap(sc.getOptionContainer(),
						sc.getTaskPool());
		STYLE = AbcCreator.createStyle(sc.getOptionContainer());
		DRUM_MAPS =
				AbcCreator.createDrumMaps(sc.getOptionContainer(),
						sc.getTaskPool());
		wdDir = sc.getWorkingDir();
		io = sc.getIO();
		master = sc.getMaster();
		targets = null;
		parser = null;
		taskPool = sc.getTaskPool();
		bruteDir = Path.getTmpDir("BruTE-GUI");
		brutesMidi = brutesMap = brutesAbc = null;
	}

	private AbcCreator(final AbcCreator abc, final StartupContainer sc) {
		io = abc.io;
		master = abc.master;
		targets = MidiInstrument.createTargets();
		parser = MidiParser.createInstance(sc);
		ABC_PLAYER = abc.ABC_PLAYER;
		INSTRUMENT_MAP = abc.INSTRUMENT_MAP;
		STYLE = abc.STYLE;
		DRUM_MAPS = abc.DRUM_MAPS;
		wdDir = abc.wdDir;
		taskPool = abc.taskPool;
		bruteDir = abc.bruteDir;
		brutesMidi = bruteDir.resolve("mid.mid");
		brutesMap = bruteDir.resolve("out.config");
		brutesAbc = bruteDir.resolve("new.abc");
	}

	/**
	 * Issues the transcription from selected midi.
	 * 
	 * @param name
	 * @param title
	 * @param abcTracks
	 * @return <i>true</i> on success
	 */
	public final boolean call_back(final String name, final String title,
			int abcTracks) {
		io.startProgress("Creating map", abcTracks + 1);
		final Path map =
				generateMap(name == null ? "<insert your name here>" : name,
						title == null ? abc.getFileName() : title);
		io.endProgress();
		mapLock.v();
		if (map == null) {
			// no abc-tracks
			return true;
		}
		System.out.println("generated map " + map);
		try {
			copy(map, brutesMap);
			io.startProgress("Waiting for BruTE to finish", abcTracks + 1);
			final int remap = call("remap.exe", bruteDir);
			if (remap != 0) {
				io.printError("Unable to execute BRuTE", false);
				return false;
			}
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return false;
		}
		abc.delete();
		brutesAbc.renameTo(abc);
		brutesMidi.delete();
		brutesMap.delete();
		if (master.isInterrupted()) {
			return false;
		}
		if (name == null) {
			// test
			try {
				final Path abcPlayer = ABC_PLAYER.getValue();
				if (abcPlayer != null) {
					if (abcPlayer.getFileName().endsWith(".exe")) {
						call(abcPlayer, CallType.EXE, abcPlayer.getParent());
					} else {
						call(abcPlayer, CallType.JAR, abcPlayer.getParent(),
								abc.toString());
					}
				}
			} catch (final IOException | InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * @return a set of useable drum-maps
	 */
	public final Set<Integer> getMaps() {
		return maps;
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>();
		list.add(ABC_PLAYER);
		list.add(DRUM_MAPS);
		list.add(INSTRUMENT_MAP);
		list.add(STYLE);
		taskPool.addTask(new Runnable() {

			@Override
			public final void run() {
				bruteDir.toFile().mkdir();
				try {
					final boolean init = init();
					if (!init) {
						AbcCreator.initState.setFailed();
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		});
		return list;
	}

	/** */
	@Override
	public final int getVersion() {
		return AbcCreator.VERSION;
	}

	/** */
	@SuppressWarnings("unchecked")
	@Override
	public final AbcCreator init(final StartupContainer sc) {
		return new AbcCreator(this, sc);
	}

	/**
	 * 
	 */
	public final void lockMap() {
		mapLock.p();
	}

	/**
	 * Asks the user which midi to transcribe and calls brute offering a GUI.
	 */
	@Override
	public final void run() {
		try {
			AbcCreator.initState.drawState(io);
			taskPool.waitForTasks();
			if (AbcCreator.initState.failed() || master.isInterrupted()) {
				return;
			}
			final Path instrumentMap = INSTRUMENT_MAP.getValue();
			final Path drumMaps = DRUM_MAPS.getValue();
			if (instrumentMap != null) {
				AbcCreator.initState.startPhase(InitState.INSTRUMENT_MAP,
						(int) instrumentMap.toFile().length());
				MidiInstrument.readMap(instrumentMap, io);
			}
			if (drumMaps != null) {
				AbcCreator.initState.startPhase(InitState.DRUM_MAP);
				prepareMaps(drumMaps);
			}
			for (int i = 0; i < AbcCreator.DRUM_MAPS_COUNT; i++) {
				maps.add(i);
			}
			io.endProgress();
			runLoop();
		} finally {
			bruteDir.delete();
		}
	}

	/**
	 * 
	 */
	public final void unlockMap() {
		mapLock.v();
	}

	private final int call(final Path location, final CallType type,
			final Path wd, final String... cmd) throws IOException,
			InterruptedException {
		if (master.isInterrupted()) {
			return -127;
		}
		final Runtime runtime = Runtime.getRuntime();
		if (Thread.currentThread().isInterrupted()) {
			return -1;
		}
		final Process p;
		switch (type) {
			case JAR:
			case JAR_WAIT:
				final ProcessBuilder pb;
				final String player;
				if (location.toString().contains(" ")) {
					if (FileSystem.type == FileSystem.OSType.WINDOWS) {
						player = "\"" + location.toString() + "\"";
					} else {
						player = location.toString().replaceAll(" ", "\\\\ ");
					}
				} else {
					player = location.toString();
				}
				pb =
						new ProcessBuilder(AbcCreator.javaPath.toString(),
								"-jar", player);
				for (final String c : cmd) {
					if (c.contains(" ")) {
						if (FileSystem.type == FileSystem.OSType.WINDOWS) {
							pb.command().add("\"" + c + "\"");
						} else {
							pb.command().add(c.replaceAll(" ", "\\\\ "));
						}
					} else {
						pb.command().add(c);
					}
				}
				pb.directory(location.getParent().toFile());
				p = pb.start();
				break;
			case EXE:
			case EXE_WAIT:
				p = runtime.exec(location.toString(), null, wd.toFile());
				break;
			default:
				return -1;
		}
		final java.io.InputStream is = p.getInputStream();
		final java.io.InputStream es = p.getErrorStream();
		final StringBuilder outErr = new StringBuilder();
		final StringBuilder outStd = new StringBuilder();

		final StreamPrinter pE = new StreamPrinter(es, outErr, true);
		final StreamPrinter pS;
		switch (type) {
			case JAR:
			case EXE:
				pS = new StreamPrinter(is, outStd, false);
				new Thread() {

					@Override
					public void run() {
						pE.run();
					}
				}.start();
				new Thread() {

					@Override
					public void run() {
						pS.run();
					}
				}.start();
				return 0;
			default:

		}
		pS = new StreamPrinter(is, outStd, false) {

			@Override
			public void run() {
				do {
					int read;
					try {
						read = stream.read();
					} catch (final IOException e) {
						e.printStackTrace();
						return;
					}
					if (read < 0) {
						io.updateProgress();
						return;
					}
					builder.append((char) read);
					if (read == '\n') {
						final String line = builder.toString();
						if (line.contains("/")) {
							io.updateProgress();
						}
						System.out.print(line);
						builder.setLength(0);
					}
				} while (true);
			}
		};
		taskPool.addTask(pE);
		taskPool.addTask(pS);
		final int exit = p.waitFor();
		return exit;
	}

	private final int call(final String string, final Path bruteDir)
			throws IOException, InterruptedException {
		final Path exe = bruteDir.resolve(string);
		if (!exe.toFile().canExecute()) {
			exe.toFile().setExecutable(true);
		}

		return call(exe, CallType.EXE_WAIT, bruteDir);
	}

	private final Set<Path> copy(final Path source, final Path destination)
			throws IOException {
		final Set<Path> filesAndDirs = new HashSet<>();
		if (source.toFile().isDirectory()) {
			if (destination.toFile().exists()
					&& !destination.toFile().isDirectory()) {
				throw new IOException("Copying directory to file");
			} else {
				for (final String s : source.toFile().list()) {
					if (!destination.toFile().exists()) {
						if (!destination.toFile().mkdir()) {
							throw new IOException("Unable to create directory "
									+ destination);
						}
					}
					filesAndDirs.add(destination.resolve(s));
					taskPool.addTask(new Runnable() {
						@Override
						public final void run() {
							if (master.isInterrupted()) {
								return;
							}
							try {
								copyRek(source.resolve(s),
										destination.resolve(s));
							} catch (final IOException e) {
								e.printStackTrace();
							}
							AbcCreator.initState.progress();
						}
					});
				}
				return filesAndDirs;
			}
		}
		final InputStream in = io.openIn(source.toFile());
		final OutputStream out = io.openOut(destination.toFile());
		io.write(in, out);
		io.close(out);
		filesAndDirs.add(destination);
		return filesAndDirs;
	}

	private final void copyRek(final Path source, final Path destination)
			throws IOException {
		if (source.toFile().isDirectory()) {
			if (destination.toFile().exists()
					&& !destination.toFile().isDirectory()) {
				AbcCreator.initState.setFailed();
				throw new IOException("Copying directory to file");
			} else {
				final String[] files = source.toFile().list();
				AbcCreator.initState.incrementSize(files.length);
				for (final String s : files) {
					if (!destination.toFile().exists()) {
						if (!destination.toFile().mkdir()) {
							throw new IOException("Unable to create directory "
									+ destination);
						}
					}
					taskPool.addTask(new Runnable() {
						@Override
						public final void run() {
							if (master.isInterrupted()
									|| AbcCreator.initState.failed()) {
								return;
							}
							try {
								copyRek(source.resolve(s),
										destination.resolve(s));
							} catch (final IOException e) {
								e.printStackTrace();
							}
							AbcCreator.initState.progress();
						}
					});
				}
			}
			return;
		}
		final InputStream in = io.openIn(source.toFile());
		final OutputStream out = io.openOut(destination.toFile());
		io.write(in, out);
		io.close(out);
	}

	private final void extract(final JarFile jarFile, final String string)
			throws IOException {
		AbcCreator.initState.startPhase(InitState.READ_JAR);
		final Set<ZipEntry> jarEntries = extract_getEntries(jarFile, string);
		AbcCreator.initState
				.startPhase(InitState.UNPACK_JAR, jarEntries.size());
		unpack(jarFile, jarEntries.toArray(new ZipEntry[jarEntries.size()]));
		io.endProgress();
	}

	private final Set<ZipEntry> extract_getEntries(final JarFile jarFile,
			final String... entriesArray) {
		final Enumeration<JarEntry> e = jarFile.entries();
		final Set<ZipEntry> jarEntries = new HashSet<>();
		final Set<String> entries = new HashSet<>();
		for (final String s : entriesArray) {
			entries.add(s);
		}
		while (e.hasMoreElements()) {
			final JarEntry jarEntry = e.nextElement();
			final String entryName =
					jarEntry.getName().substring(6).split("\\/", 2)[0];
			if (!jarEntry.isDirectory() && entries.contains(entryName)) {
				jarEntries.add(jarEntry);
			}
			io.updateProgress(1);
		}
		return jarEntries;
	}

	private final Path generateMap(final String name, final String title) {
		final Path map = midi.getParent().resolve(midi.getFileName() + ".map");
		final OutputStream out = io.openOut(map.toFile());
		final String style = STYLE.value();

		io.writeln(out, String.format("Name: %s", title));
		io.writeln(out, "Speedup: 0");
		io.writeln(out, "Pitch: 0");
		io.writeln(out, "Style: " + style);
		io.writeln(out, "Volume: 0");
		io.writeln(out, "Compress: 1.0");
		io.writeln(out,
				"%no pitch guessing   %uncomment to switch off guessing of default octaves");
		io.writeln(
				out,
				"%no back folding     %uncomment to switch off folding of tone-pitches inside the playable region");
		io.writeln(out, "fadeout length 0    %unoperational still!!!!");
		io.writeln(out, String.format("Transcriber : %s", name));
		final Map<DragObject, Integer> abcPartMap = new HashMap<>();
		boolean empty = true;
		io.updateProgress();
		for (int i = 0; i < targets.length - 1; i++) {
			for (final DropTarget t : targets[i]) {
				empty = false;
				final StringBuilder params = new StringBuilder();
				for (final Map.Entry<String, Integer> param : t.getParams()
						.entrySet()) {
					params.append(" ");
					params.append(t.printParam(param));
				}
				io.writeln(out, "");
				io.writeln(out, "abctrack begin");
				io.writeln(out, "polyphony 6 top");
				io.writeln(out, "duration 2");
				io.writeln(out, String.format("instrument %s%s",
						targets[i].toString(), params.toString()));
				writeAbcTrack(out, t, abcPartMap);
				io.writeln(out, "abctrack end");
				io.updateProgress();
			}
		}

		io.writeln(out, "");
		io.writeln(out,
				"% Instrument names are the ones from lotro, pibgorn is supported as well");
		io.writeln(
				out,
				"% Polyphony sets the maximal number of simultanious tones for this instrument (6 is max)");
		io.writeln(out,
				"% Pitch is in semitones, to shift an octave up : pitch 12 or down  pitch -12");
		io.writeln(
				out,
				"% Volume will be added /substracted from the normal volume of that track (-127 - 127), everything above/below is truncatedtch is in semitones, to shift an octave up : pitch 12 or down  pitch -12");
		io.close(out);
		return empty ? null : map;
	}

	/*
	 * Copies all BruTE into current directory
	 */
	private final boolean init() throws IOException {
		if (wdDir.toFile().isDirectory()) {
			final Path bruteDir = wdDir.resolve("Brute");
			if (!bruteDir.exists()) {
				System.err.println("Unable to find Brute\n" + bruteDir
						+ " does not exist.");
				io.printError("Unable to find Brute", false);
				master.interrupt();
				return false;
			}
			AbcCreator.initState.startPhase(InitState.COPY);
			AbcCreator.initState.setSize(InitState.COPY,
					copy(bruteDir, this.bruteDir).size());
		} else {
			final JarFile jarFile;
			jarFile = new JarFile(wdDir.toFile());
			try {
				extract(jarFile, "Brute");
			} finally {
				jarFile.close();
				io.endProgress();
			}
		}
		return true;
	}

	private void prepareMaps(final Path drumMaps) {
		final String[] files = drumMaps.toFile().list();
		if (files != null) {
			AbcCreator.initState.setSize(InitState.DRUM_MAP, files.length);
			for (final String f : files) {
				if (f.startsWith("drum") && f.endsWith(".drummap.txt")) {
					final String idString = f.substring(4, f.length() - 12);
					final int id;
					try {
						id = Integer.parseInt(idString);
					} catch (final Exception e) {
						AbcCreator.initState.progress();
						continue;
					}
					taskPool.addTask(new Runnable() {

						@Override
						public final void run() {
							try {
								copy(drumMaps.resolve(f), bruteDir.resolve(f));

								AbcCreator.initState.progress();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					});
					maps.add(id);
				} else {
					AbcCreator.initState.progress();
				}
			}
		}
		for (int i = 0; i < AbcCreator.DRUM_MAPS_COUNT; i++) {
			maps.add(i);
		}
		taskPool.waitForTasks();
	}

	private final void runLoop() {
		if (bruteDir == null) {
			return;
		}
		final DragAndDropPlugin dragAndDropPlugin =
				new DragAndDropPlugin(this, taskPool, parser, targets, io);
		while (true) {
			if (master.isInterrupted()) {
				return;
			}
			for (final DropTargetContainer target : targets) {
				target.clearTargets();
			}
			midi =
					io.selectFile(
							"Which midi do you want to transcribe to abc?",
							midi == null ? Path.getPath(
									Main.getConfigValue(Main.GLOBAL_SECTION,
											Main.PATH_KEY, null).split("/"))
									.toFile() : midi.getParent().toFile(),
							AbcCreator.midiFilter);
			if (midi == null) {
				break;
			}

			String abcName = midi.getFileName();
			{
				final int end = abcName.lastIndexOf('.');
				if (end >= 0) {
					abcName = abcName.substring(0, end);
				}
				abcName += ".abc";
			}
			abc = midi.getParent().resolve(abcName);
//			if (!abc.getFileName().endsWith(".abc")) {
//				abc = abc.getParent().resolve(abc.getFileName() + ".abc");
//			}
			if (!parser.setMidi(midi)) {
				continue;
			}
			final MidiMap events = parser.parse();
			if (events == null) {
				continue;
			}
			try {
				copy(midi, brutesMidi);
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
				continue;
			}

			int val;
			try {
				val = call("midival.exe", bruteDir);
				if (val != 0) {
					io.printError("Unable to execute BRuTE", false);
					Thread.currentThread().interrupt();
					return;
				}
			} catch (final InterruptedException e) {
				master.interrupt();
				return;
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
				continue;
			}
			System.out.printf("%s -> %s\n", midi, abc);
			io.handleGUIPlugin(dragAndDropPlugin);
			if (master.isInterrupted()) {
				return;
			}
			final String defaultTitle = midi.getFileName();
			final StringOption TITLE =
					new StringOption(null, null, "Title displayed in the abc",
							"Title", Flag.NoShortFlag, Flag.NoLongFlag,
							AbcCreator.SECTION, "title", defaultTitle);
			final List<Option> options = new ArrayList<Option>();
			options.add(TITLE);
			io.getOptions(options);
			if (master.isInterrupted()) {
				return;
			}
			final String name =
					Main.getConfigValue(Main.GLOBAL_SECTION, Main.NAME_KEY,
							null);
			if (name == null) {
				return;
			}
			if (call_back(name, TITLE.value(), dragAndDropPlugin.getAbcTracks())) {
				io.printMessage(null, "transcribed\n" + midi + "\nto\n" + abc,
						true);
			}
		}
	}

	private final void unpack(final JarFile jarFile,
			final ZipEntry... jarEntries) throws IOException {
		for (final ZipEntry jarEntry : jarEntries) {
			if (master.isInterrupted()) {
				return;
			}
			taskPool.addTask(new Runnable() {
				@Override
				public void run() {
					final OutputStream out;
					final java.io.File file;
					if (jarEntries.length == 1) {
						file = bruteDir.resolve(jarEntry.getName()).toFile();
					} else {
						file =
								bruteDir.resolve(
										jarEntry.getName().substring(6))
										.toFile();
					}
					if (!file.getParentFile().exists()) {
						if (!bruteDir.exists()) {
							return;
						}
						if (!file.getParentFile().mkdirs()) {
							throw new RuntimeException("Unpacking BruTE failed");
						}
					}
					out = io.openOut(file);
					try {
						io.write(jarFile.getInputStream(jarEntry), out);
					} catch (final IOException e) {
						bruteDir.delete();
						throw new RuntimeException("Unpacking BruTE failed");
					} finally {
						io.close(out);
					}
					AbcCreator.initState.progress();
				}
			});
		}
	}

	private final void
			writeAbcTrack(final OutputStream out, final DropTarget abcTrack,
					final Map<DragObject, Integer> abcPartMap) {

		for (final DragObject midiTrack : abcTrack) {
			final String v = midiTrack.getParam("volume", abcTrack);
			io.write(
					out,
					String.format("miditrack %d pitch 0 volume %s",
							midiTrack.getId(), v));
			final int total = midiTrack.getTargets().length;
			if (total > 1) {
				int part = 0;
				if (abcPartMap.containsKey(midiTrack)) {
					part = abcPartMap.get(midiTrack);
				}
				abcPartMap.put(midiTrack, part + 1);
				io.writeln(out, String.format(" prio 1 delay 0 r 0 0 s %d %d",
						total, part));
			} else {
				io.write(out, "\r\n");
			}
		}
	}
}
