package stone.modules;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;
import stone.modules.abcCreator.AbcMapPlugin;
import stone.modules.abcCreator.BruteParams;
import stone.modules.abcCreator.DndPluginCaller;
import stone.modules.abcCreator.DndPluginCallerParams;
import stone.modules.abcCreator.DragObject;
import stone.modules.abcCreator.DropTarget;
import stone.modules.abcCreator.DropTargetContainer;
import stone.modules.abcCreator.DrumMapFileFilter;
import stone.modules.abcCreator.ExecutableJarFileFilter;
import stone.modules.abcCreator.InstrumentMapFileFilter;
import stone.modules.abcCreator.MidiFileFilter;
import stone.modules.abcCreator.StreamPrinter;
import stone.modules.midiData.MidiInstrument;
import stone.modules.midiData.MidiMap;
import stone.modules.midiData.MidiParser;
import stone.util.Debug;
import stone.util.FileSystem;
import stone.util.Flag;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.PathOption;
import stone.util.PathOptionFileFilter;
import stone.util.StringOption;
import stone.util.TaskPool;

/**
 * @author Nelphindal
 */
public class AbcCreator implements Module,
		DndPluginCaller<JPanel, JPanel, JPanel> {

	/**
	 * Enum indicating what type of runnable shall be called
	 * 
	 * @author Nelphindal
	 */
	enum CallType {
		/**
		 * execute and wait for a runnable jar-archive
		 */
		JAR_WAIT,
		/**
		 * execute a runnable jar-archive
		 */
		JAR,
		/**
		 * execute and wait for a runnable exe
		 */
		EXE_WAIT,
		/**
		 * execute a runnable exe
		 */
		EXE;
	}

	static class InitState {
		static final Object INIT = new Object();
		static final Object READ_JAR = new Object();
		static final Object INSTRUMENT_MAP = new Object();
		static final Object DRUM_MAP = new Object();
		static final Object UNPACK_JAR = new Object();

		private IOHandler io;

		int progress, size;
		boolean failed;
		Object state = InitState.INIT;

		private String getMessage() {
			if (this.state == InitState.DRUM_MAP) {
				return "Copying drum-maps to working dir";
			}
			if (this.state == InitState.UNPACK_JAR) {
				return "Unpacking BruTE";
			}
			if (this.state == InitState.INIT) {
				return "Init";
			}
			if (this.state == "INSTRUMENT_MAP") {
				return "Parsing instrument-map";
			}
			if (this.state == "DRUM_MAP") {
				return "Parsing drum-maps";
			}
			if (this.state == InitState.READ_JAR) {
				return "Reading BruTE-archive";
			}
			return "...";
		}

		synchronized final void drawState(final IOHandler io) {
			this.io = io;
			if (this.failed) {
				return;
			}
			io.startProgress(getMessage(), this.size);
			io.updateProgress(this.progress);
		}

		synchronized final boolean failed() {
			return this.failed;
		}

		synchronized final void incrementSize(int value) {
			this.size += value;
			if (this.io != null) {
				this.io.setProgressSize(this.size);
			}
		}

		synchronized final void progress() {
			++this.progress;
			if (this.io != null) {
				this.io.updateProgress();
			}
		}

		synchronized final void setFailed() {
			this.failed = true;
		}

		synchronized final void setSize(final Object state, int size) {
			if (this.state == state) {
				this.size = size;
				if (this.io != null) {
					this.io.setProgressSize(size);
				}
			}
		}

		synchronized final void startPhase(final Object state) {
			this.state = state;
			this.progress = 0;
			this.size = -1;
			if (this.io != null) {
				this.io.startProgress(getMessage(), this.size);
			}
		}

		synchronized final void startPhase(final Object state, int size) {
			this.state = state;
			this.progress = 0;
			this.size = size;
			if (this.io != null) {
				this.io.startProgress(getMessage(), size);
			}
		}
	}

	/**
	 * The section identfier of global config for all settings related to the
	 * GUI for BruTE
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
	public static final int DRUM_MAPS_COUNT = 6;

	private static final PathOptionFileFilter EXEC_JAR_FILTER = new ExecutableJarFileFilter();

	private static final PathOptionFileFilter DRUM_MAP_FILTER = new DrumMapFileFilter();

	private static final PathOptionFileFilter INSTR_MAP_FILTER = new InstrumentMapFileFilter();

	private final static int VERSION = 12;

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
		final PathOptionFileFilter ff = AbcCreator.EXEC_JAR_FILTER;
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
				"The style to use for generated abc. Possible values are Rocks, Meisterbarden and TSO",
				"Style", Flag.NoShortFlag, "style", AbcCreator.SECTION,
				"style", "Rocks");
	}

	private final static Path getJavaPath() {
		final Path javaBin = Path.getPath(
				System.getProperty("java.home").split(
						"\\" + FileSystem.getFileSeparator())).resolve("bin");
		final Path javaPath_;
		if (FileSystem.type == FileSystem.OSType.WINDOWS) {
			javaPath_ = javaBin.resolve("java.exe");
		} else {
			javaPath_ = javaBin.resolve("java");
		}
		return javaPath_;
	}

	final Path bruteDir;

	final IOHandler io;

	final InitState initState;

	final MasterThread master;

	private final PathOption ABC_PLAYER;

	private final PathOption DRUM_MAPS;

	private final PathOption INSTRUMENT_MAP;

	private final StringOption STYLE;

	final StringOption TITLE;

	private final Path brutesMidi;// = bruteDir.resolve("mid.mid");

	private final Path brutesMap;// = bruteDir.resolve("out.config");
	private final Path brutesAbc;// = bruteDir.resolve("new.abc");

	private final MidiParser parser;

	private final List<DropTargetContainer<JPanel, JPanel, JPanel>> targets;

	private final TaskPool taskPool;

	private final Main main;

	private final Path wdDir;

	private Path midi, abc;

	private final Set<Integer> maps = new HashSet<>();

	private AbcMapPlugin dragAndDropPlugin;

	private final ArrayDeque<Process> processList = new ArrayDeque<>();

	private StreamPrinter callResultOut;

	/**
	 * Constructor for building versionInfo
	 */
	public AbcCreator() {
		this.ABC_PLAYER = null;
		this.INSTRUMENT_MAP = null;
		this.STYLE = null;
		this.DRUM_MAPS = null;
		this.TITLE = null;
		this.wdDir = null;
		this.io = null;
		this.master = null;
		this.targets = null;
		this.parser = null;
		this.taskPool = null;
		this.bruteDir = null;
		this.initState = null;
		this.brutesMidi = this.brutesMap = this.brutesAbc = null;
		this.main = null;
	}

	/**
	 * @param sc
	 * @throws InterruptedException
	 */
	public AbcCreator(final StartupContainer sc) throws InterruptedException {
		this.ABC_PLAYER = AbcCreator.createPathToAbcPlayer(
				sc.getOptionContainer(), sc.getTaskPool());
		this.INSTRUMENT_MAP = AbcCreator.createInstrMap(
				sc.getOptionContainer(), sc.getTaskPool());
		this.STYLE = AbcCreator.createStyle(sc.getOptionContainer());
		this.DRUM_MAPS = AbcCreator.createDrumMaps(sc.getOptionContainer(),
				sc.getTaskPool());
		this.TITLE = null;
		this.wdDir = sc.getWorkingDir();
		this.io = sc.getIO();
		this.master = sc.getMaster();
		this.targets = null;
		this.parser = null;
		this.taskPool = sc.getTaskPool();
		this.bruteDir = Path.getTmpDirOrFile("BruTE-GUI");
		this.brutesMidi = this.brutesMap = this.brutesAbc = null;
		this.initState = new InitState();
		this.main = sc.getMain();
	}

	private AbcCreator(final AbcCreator abc, final StartupContainer sc) {
		this.io = abc.io;
		this.master = abc.master;
		this.targets = MidiInstrument.createTargets();
		this.parser = MidiParser.createInstance(sc);

		this.ABC_PLAYER = abc.ABC_PLAYER;
		this.INSTRUMENT_MAP = abc.INSTRUMENT_MAP;
		this.STYLE = abc.STYLE;
		this.DRUM_MAPS = abc.DRUM_MAPS;
		this.TITLE = new StringOption(null, null, "Title displayed in the abc",
				"Title", Flag.NoShortFlag, Flag.NoLongFlag, AbcCreator.SECTION,
				"title", null);
		this.wdDir = abc.wdDir;
		this.taskPool = abc.taskPool;
		this.bruteDir = abc.bruteDir;
		this.brutesMidi = this.bruteDir.resolve("mid.mid");
		this.brutesMap = this.bruteDir.resolve("out.config");
		this.brutesAbc = this.bruteDir.resolve("new.abc");
		this.dragAndDropPlugin = new AbcMapPlugin(this, this.taskPool,
				this.parser, this.targets, this.io);
		this.initState = abc.initState;
		this.main = abc.main;
	}

	/**
	 * Issues the transcription from selected midi.
	 * 
	 * @param name
	 * @param title
	 * @param abcTracks
	 * @return <i>true</i> on success
	 */
	@Override
	public final Object call_back(final Object name, final Object title,
			int abcTracks) {
		this.io.startProgress("Creating map", abcTracks + 1);
		final Path map = generateMap(name == null ? "<insert your name here>"
				: name, title == null ? this.abc.getFilename() : title);
		this.io.endProgress();
		if (map == null) {
			// no abc-tracks
			return new Object() {
				@Override
				public final String toString() {
					return "empty file";
				}
			};
		}
		System.out.println("generated map " + map);
		try {
			copy(map, this.brutesMap);
			this.io.startProgress("Waiting for BruTE to finish", abcTracks + 1);
			final int remap = call("remap.exe", this.bruteDir);
			this.io.endProgress();
			if (remap != 0) {
				this.io.printError("Unable to execute BRuTE", false);
				// will interrupt the process
				return null;
			}
		} catch (final Exception e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
			return null;
		}
		this.abc.delete();
		this.brutesAbc.renameTo(this.abc);
		this.brutesMidi.delete();
		this.brutesMap.delete();
		if (this.master.isInterrupted()) {
			return null;
		}
		if (name == null) {
			// test
			try {
				final Path abcPlayer = this.ABC_PLAYER.getValue();
				if ((abcPlayer != null) && abcPlayer.exists()) {
					this.io.startProgress("Starting AbcPlayer", -1);
					call(abcPlayer, CallType.JAR, abcPlayer.getParent(),
							this.abc.toString());
					this.io.endProgress();
				}
			} catch (final IOException | InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		return this.callResultOut;
	}

	@Override
	public void exec(final Runnable task) {
		this.taskPool.addTask(task);
	}

	/** @return the path to the abc-file to create */
	@Override
	public final Path getFile() {
		return this.abc;
	}

	/**
	 * @return a set of useable drum-maps
	 */
	public final Set<Integer> getMaps() {
		return this.maps;
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>();
		list.add(this.ABC_PLAYER);
		list.add(this.DRUM_MAPS);
		list.add(this.INSTRUMENT_MAP);
		list.add(this.STYLE);
		this.taskPool.addTask(new Runnable() {

			@Override
			public final void run() {
				AbcCreator.this.bruteDir.toFile().mkdir();
				try {
					final boolean init = init();
					if (!init) {
						AbcCreator.this.initState.setFailed();
						AbcCreator.this.bruteDir.delete();
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
	@Override
	public final Module init(final StartupContainer sc) {
		return new AbcCreator(this, sc);
	}

	/** */
	@Override
	public final void link(final DragObject<JPanel, JPanel, JPanel> object,
			final DropTarget<JPanel, JPanel, JPanel> target) {
		this.dragAndDropPlugin.link(object, target);
	}

	/**
	 * Parses given map and registers the result at LoadedMapEntry.
	 */
	@Override
	public final void loadMap(final File mapToLoad,
			final DndPluginCaller.LoadedMapEntry c) {
		final InputStream in = this.io.openIn(mapToLoad);
		in.registerProgressMonitor(this.io);
		class ParseState {
			private int state;

			final boolean comment() {
				return this.state < 0;
			}

			final void parseLine(final String line) {
				switch (this.state) {
				case 0x7000_0000:
					return;
				case 0:
					if (line.startsWith("Name: ")) {
						AbcCreator.this.TITLE.value(line.substring(6).trim());
					} else if (line.startsWith("Speedup: ")) {
						BruteParams.SPEED.value(line.substring(8).trim(),
								AbcCreator.this.io);
					} else if (line.startsWith("Pitch: ")) {
						BruteParams.PITCH.value(line.substring(6).trim(),
								AbcCreator.this.io);
					} else if (line.startsWith("Style: ")) {
						AbcCreator.this.STYLE.value(line.substring(7));
					} else if (line.startsWith("Volume: ")) {
						BruteParams.VOLUME.value(line.substring(7).trim(),
								AbcCreator.this.io);
					} else if (line.startsWith("Compress: ")) {
						BruteParams.DYNAMIC.value(line.substring(10).trim(),
								AbcCreator.this.io);
					} else if (line.startsWith("abctrack begin")) {
						++this.state;
					} else if (line.startsWith("fadeout length")) {
						BruteParams.FADEOUT.value(line.substring(14).trim(),
								AbcCreator.this.io);
					} else {
						return;
					}
					break;
				case 1:
					if (line.startsWith("duration ")) {
						// TODO support of duration
						break;
					} else if (line.startsWith("polyphony ")) {
						// TODO support of polyphony
						break;
					} else {
						if (line.startsWith("instrument ")) {
							this.state = 2;
							parseLine(line);
						}
						return;
					}
				case 2:
					if (!line.startsWith("instrument ")) {
						this.state = 0x7000_0000;
						return;
					}
					final String s0 = line.substring(11).trim();

					c.addPart(s0);

					this.state = 3;
					break;
				case 3:
					if (line.startsWith("miditrack")) {
						c.addEntry(line.substring(10));
					} else if (line.startsWith("abctrack end")) {
						this.state = 7;
					} else {
						return;
					}
					break;
				case 7:
					if (line.startsWith("abctrack begin")) {
						this.state = 1;
					} else {
						return;
					}
					break;
				}
				if (c.error()) {
					System.out.println(". " + line);
					return;
				}
				Debug.print(". %s", line);
			}

			final void toggleComment() {
				this.state = ~this.state;
			}
		}

		final ParseState state = new ParseState();
		Debug.print("loading map %s\n", mapToLoad);
		try {
			while (true) {
				if (c.error()) {
					break;
				}
				final String line = in.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith("%")) {
					continue;
				}
				if (line.trim().equals("*")) {
					state.toggleComment();
				}
				if (state.comment()) {
					continue;
				}
				state.parseLine(line);
			}
		} catch (final IOException e) {
			c.setError();
			System.err.println(e);
		} catch (final Exception e) {
			c.setError();
			e.printStackTrace();
		} finally {
			this.io.endProgress();
			this.io.close(in);
		}
		System.out.println("... completed");
	}

	/**
	 * Helper method to display an error on the GUI.
	 */
	@Override
	public final void printError(final String string) {
		this.dragAndDropPlugin.printError(string);
	}

	/** No effect */
	@Override
	public final void repair() {
		// nothing to do
	}

	/**
	 * Asks the user which midi to transcribe and calls brute offering a GUI.
	 */
	@Override
	public final void run() {
		try {
			this.initState.drawState(this.io);
			this.taskPool.waitForTasks();
			this.io.startProgress("Preparing launch", -1);
			if (this.initState.failed() || this.master.isInterrupted()) {
				return;
			}
			final Path instrumentMap = this.INSTRUMENT_MAP.getValue();
			final Path drumMaps = this.DRUM_MAPS.getValue();
			if (instrumentMap != null) {
				this.initState.startPhase(InitState.INSTRUMENT_MAP,
						(int) instrumentMap.toFile().length());
				MidiInstrument.readMap(instrumentMap, this.io);
			}
			if (drumMaps != null) {
				this.initState.startPhase(InitState.DRUM_MAP);
				prepareMaps(drumMaps);
			}
			for (int i = 0; i < AbcCreator.DRUM_MAPS_COUNT; i++) {
				this.maps.add(i);
			}
			this.io.endProgress();
			runLoop();
		} finally {
			for (final Process p : this.processList) {
				p.destroy();
				final boolean interrupted = MasterThread.interrupted();
				try {
					p.waitFor();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				if (interrupted) {
					this.master.interrupt();
				}
			}
			this.bruteDir.delete();
		}
	}

	/** */
	@Override
	public final TreeSet<DropTarget<JPanel, JPanel, JPanel>> sortedTargets() {
		return this.dragAndDropPlugin.targets();
	}

	/** */
	@Override
	public final boolean unlink(
			final DragObject<JPanel, JPanel, JPanel> object,
			final DropTarget<JPanel, JPanel, JPanel> target) {
		return this.dragAndDropPlugin.unlink(object, target);
	}

	/**
	 * 
	 * @return an array containing all global params, the array may contain
	 *         <i>null</i> entries.
	 */
	@Override
	public final DndPluginCallerParams<?>[] valuesGlobal() {
		return BruteParams.valuesGlobal();
	}

	private final int call(final Path location, final CallType type,
			final Path wd, final String... cmd) throws IOException,
			InterruptedException {
		if (this.master.isInterrupted()) {
			return -127;
		}
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
			pb = new ProcessBuilder(AbcCreator.javaPath.toString(), "-jar",
					player);
			for (final String c : cmd) {
				if (c.contains(" ")) {
					if (FileSystem.type == FileSystem.OSType.WINDOWS) {
						pb.command().add("\"" + c + "\"");
					} else {
						pb.command().add(c.replaceAll(" ", "\\ "));
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
			if (FileSystem.type == FileSystem.OSType.UNIX) {
				Debug.print("Unix System\n... checking for wine\n");
				if (Path.getPath("~", ".wine").exists()) {
					Debug.print("found ~/.wine\n");
				} else {
					System.err.println("unable to run \""
							+ location.getFilename() + "\"");
					return -8;
				}
			}
			p = Runtime.getRuntime().exec(location.toString(), null,
					wd.toFile());
			break;
		default:
			return -1;
		}
		final java.io.InputStream is = p.getInputStream();
		final java.io.InputStream es = p.getErrorStream();
		final StringBuilder outErr = new StringBuilder();
		final StringBuilder outStd = new StringBuilder();

		this.processList.add(p);

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

			private boolean first = true;
			private final String[] lines = new String[2];

			@Override
			public final String toString() {
				return this.lines[0];
			}

			@Override
			protected final void action() {
				final String line = this.builder.toString();
				if (!line.isEmpty()) {
					for (int i = this.lines.length - 2; i >= 0; i--) {
						this.lines[i + 1] = this.lines[i];
					}
					this.lines[0] = line.substring(0, line.length() - 2);
				}
				if (line.contains("/")) {
					final String[] s = line.replaceFirst("\r\n", "").split("/");
					final char start = s[1].charAt(0);
					if ((start < '0') || (start >= '9')) {
						;
					} else if (this.first) {
						this.first = false;
						AbcCreator.this.io.setProgressSize(Integer
								.parseInt(s[1]) + 1);
					}
					AbcCreator.this.io.updateProgress();
				}
				System.out.print(line);
			}
		};
		this.callResultOut = pS;
		this.taskPool.addTask(pE);
		this.taskPool.addTask(pS);
		final int exit = p.waitFor();
		this.processList.remove(p);
		return exit;
	}

	private final int call(final String string, final Path bruteDirectory)
			throws IOException, InterruptedException {
		final Path exe = bruteDirectory.resolve(string);
		if (!exe.toFile().canExecute()) {
			exe.toFile().setExecutable(true);
		}

		return call(exe, CallType.EXE_WAIT, bruteDirectory);
	}

	private final void extract(final URL url) {
		final String s = url.toString();
		if (!s.startsWith("jar:")) {
			this.initState.setFailed();
			return;
		}
		try {
			final JarFile jarFile = new JarFile(Path.getPath(url).toFile());
			extract(jarFile, s.substring(s.indexOf("!") + 2));
		} catch (final IOException e) {
			e.printStackTrace();
			this.initState.setFailed();
		}

	}

	private final void extract(final JarFile jarFile, final String string)
			throws IOException {
		this.initState.startPhase(InitState.READ_JAR);
		final ZipEntry jarEntry = jarFile.getEntry(string);
		this.initState.startPhase(InitState.UNPACK_JAR);
		unpack(jarFile, jarEntry);
		final Path jar = this.bruteDir.resolve(string);
		extract(jar);
	}

	private final void extract(final Path jar) throws IOException {
		final Set<JarEntry> entries = new HashSet<>();
		final JarFile jarFile1 = new JarFile(jar.toFile());
		final Enumeration<JarEntry> ee = jarFile1.entries();
		while (ee.hasMoreElements()) {
			final JarEntry je = ee.nextElement();
			if (!je.isDirectory()) {
				entries.add(je);
			}
		}
		this.initState.setSize(InitState.UNPACK_JAR, entries.size());
		unpack(jarFile1, entries.toArray(new JarEntry[entries.size()]));
		try {
			jarFile1.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private final Path generateMap(final Object name, final Object title) {
		final Path map = this.midi.getParent().resolve(
				this.midi.getFilename() + ".map");
		final OutputStream out = this.io.openOut(map.toFile());
		final String style = this.STYLE.value();

		this.io.writeln(out, String.format("Name: %s", title));
		this.io.writeln(out, "Speedup: " + BruteParams.SPEED.value());
		this.io.writeln(out, "Pitch: " + BruteParams.PITCH.value());
		this.io.writeln(out, "Style: " + style);
		this.io.writeln(out, "Volume: " + BruteParams.VOLUME.value());
		this.io.writeln(out, "Compress: " + BruteParams.DYNAMIC.value());
		this.io.writeln(out,
				"%no pitch guessing   %uncomment to switch off guessing of default octaves");
		this.io.writeln(
				out,
				"%no back folding     %uncomment to switch off folding of tone-pitches inside the playable region");
		this.io.writeln(out, "fadeout length " + BruteParams.FADEOUT.value());
		this.io.writeln(out, String.format("Transcriber : %s", name));
		final Map<DragObject<JPanel, JPanel, JPanel>, Integer> abcPartMap = new HashMap<>();
		boolean empty = true;
		this.io.updateProgress();

		for (final Iterator<DropTargetContainer<JPanel, JPanel, JPanel>> targetIter = this.targets
				.iterator();;) {
			final DropTargetContainer<JPanel, JPanel, JPanel> target = targetIter
					.next();
			if (!targetIter.hasNext()) {
				break;
			}
			for (final DropTarget<JPanel, JPanel, JPanel> t : target) {
				empty = false;
				final StringBuilder params = new StringBuilder();
				for (final Map.Entry<String, Integer> param : t.getParams()
						.entrySet()) {
					params.append(" ");
					params.append(t.printParam(param));
				}
				this.io.writeln(out, "");
				this.io.writeln(out, "abctrack begin");
				this.io.writeln(out, "polyphony 6 top");
				this.io.writeln(out, "duration 2");
				this.io.writeln(out, String.format("instrument %s%s",
						target.toString(), params.toString()));
				writeAbcTrack(out, t, abcPartMap);
				this.io.writeln(out, "abctrack end");
				this.io.updateProgress();
			}
		}

		this.io.writeln(out, "");
		this.io.writeln(out,
				"% Instrument names are the ones from lotro, pibgorn is supported as well");
		this.io.writeln(
				out,
				"% Polyphony sets the maximal number of simultanious tones for this instrument (6 is max)");
		this.io.writeln(out,
				"% Pitch is in semitones, to shift an octave up : pitch 12 or down  pitch -12");
		this.io.writeln(
				out,
				"% Volume will be added /substracted from the normal volume of that track (-127 - 127), everything above/below is truncatedtch is in semitones, to shift an octave up : pitch 12 or down  pitch -12");
		this.io.close(out);
		return empty ? null : map;
	}

	private void prepareMaps(final Path drumMaps) {
		final String[] files = drumMaps.toFile().list();
		if (files != null) {
			this.initState.setSize(InitState.DRUM_MAP, files.length);
			for (final String f : files) {
				if (f.startsWith("drum") && f.endsWith(".drummap.txt")) {
					final String idString = f.substring(4, f.length() - 12);
					final int id;
					try {
						id = Integer.parseInt(idString);
					} catch (final Exception e) {
						this.initState.progress();
						continue;
					}
					this.taskPool.addTask(new Runnable() {

						@Override
						public final void run() {
							try {
								copy(drumMaps.resolve(f),
										AbcCreator.this.bruteDir.resolve(f));

								AbcCreator.this.initState.progress();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					});
					this.maps.add(id);
				} else {
					this.initState.progress();
				}
			}
		}
		for (int i = 0; i < AbcCreator.DRUM_MAPS_COUNT; i++) {
			this.maps.add(i);
		}
		this.taskPool.waitForTasks();
	}

	private final void runLoop() {
		if (this.bruteDir == null) {
			return;
		}
		while (true) {
			if (this.master.isInterrupted()) {
				return;
			}
			this.midi = this.io.selectFile(
					"Which midi do you want to transcribe to abc?",
					this.midi == null ? Path.getPath(
							this.main.getConfigValue(Main.GLOBAL_SECTION,
									Main.PATH_KEY, null).split("/")).toFile()
							: this.midi.getParent().toFile(),
					AbcCreator.midiFilter);
			if (this.midi == null) {
				break;
			}

			String abcName = this.midi.getFilename();
			{
				final int end = abcName.lastIndexOf('.');
				if (end >= 0) {
					abcName = abcName.substring(0, end);
				}
				abcName += ".abc";
			}
			this.abc = this.midi.getParent().resolve(abcName);
			// if (!abc.getFileName().endsWith(".abc")) {
			// abc = abc.getParent().resolve(abc.getFileName() + ".abc");
			// }
			if (!this.parser.setMidi(this.midi)) {
				continue;
			}
			final MidiMap events = this.parser.parse();
			if (events == null) {
				continue;
			}
			try {
				copy(this.midi, this.brutesMidi);
			} catch (final IOException e) {
				this.io.handleException(ExceptionHandle.CONTINUE, e);
				continue;
			}

			int val;
			try {
				val = call("midival.exe", this.bruteDir);
				if (val != 0) {
					this.io.printError("Unable to execute BRuTE", false);
					Thread.currentThread().interrupt();
					return;
				}
			} catch (final InterruptedException e) {
				this.master.interrupt();
				return;
			} catch (final IOException e) {
				this.io.handleException(ExceptionHandle.CONTINUE, e);
				continue;
			}
			System.out.printf("%s -> %s\n", this.midi, this.abc);
			this.io.handleGUIPlugin(this.dragAndDropPlugin);
			if (this.master.isInterrupted()) {
				return;
			}
			final String defaultTitle = this.midi.getFilename();
			final List<Option> options = new ArrayList<>();

			if (this.TITLE.value() == null) {
				this.TITLE.value(defaultTitle);
			}
			options.add(this.TITLE);
			this.io.getOptions(options);
			if (this.master.isInterrupted()) {
				return;
			}
			final String name = this.main.getConfigValue(Main.GLOBAL_SECTION,
					Main.NAME_KEY, null);
			if (name == null) {
				return;
			}
			final Object result = call_back(name, this.TITLE.value(),
					this.dragAndDropPlugin.size());
			if (result != null) {
				this.io.printMessage(null, "transcribed\n" + this.midi
						+ "\nto\n" + this.abc + "\n\n" + result.toString(),
						true);
			}
			this.dragAndDropPlugin.reset();
		}
	}

	private final void unpack(final JarFile jarFile,
			final ZipEntry... jarEntries) {
		for (final ZipEntry jarEntry : jarEntries) {
			if (this.master.isInterrupted()) {
				return;
			}

			final OutputStream out;
			final java.io.File file;

			file = this.bruteDir.resolve(jarEntry.getName()).toFile();

			if (!file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) {
					this.initState.setFailed();
					this.bruteDir.delete();
					return;
				}
			}
			try {
				out = this.io.openOut(file);
				try {
					this.io.write(jarFile.getInputStream(jarEntry), out);
				} finally {
					this.io.close(out);
				}
			} catch (final IOException e) {
				this.initState.setFailed();
				this.bruteDir.delete();
				return;
			}
			this.initState.progress();
		}
	}

	private final void writeAbcTrack(final OutputStream out,
			final DropTarget<JPanel, JPanel, JPanel> abcTrack,
			final Map<DragObject<JPanel, JPanel, JPanel>, Integer> abcPartMap) {

		for (final DragObject<JPanel, JPanel, JPanel> midiTrack : abcTrack) {
			final Integer pitch = BruteParams.PITCH.getLocalValue(midiTrack,
					abcTrack);
			final Integer volume = BruteParams.VOLUME.getLocalValue(midiTrack,
					abcTrack);
			final Integer delay = BruteParams.DELAY.getLocalValue(midiTrack,
					abcTrack);
			this.io.write(out, String.format(
					"miditrack %d pitch %d volume %d delay %d",
					Integer.valueOf(midiTrack.getId()), pitch, volume, delay));
			final int total = midiTrack.getTargets();
			if (total > 1) {
				int part = 0;
				if (abcPartMap.containsKey(midiTrack)) {
					part = abcPartMap.get(midiTrack);
				}
				abcPartMap.put(midiTrack, part + 1);
				this.io.writeln(out, String.format(
						" prio 100 " + "split %d %d", total, part));
			} else {
				this.io.write(out, "\r\n");
			}
		}
	}

	final Set<Path> copy(final Path source, final Path destination)
			throws IOException {
		final Set<Path> filesAndDirs = new HashSet<>();
		if (source.toFile().isDirectory()) {
			if (destination.toFile().exists()
					&& !destination.toFile().isDirectory()) {
				throw new IOException("Copying directory to file");
			}
			for (final String s : source.toFile().list()) {
				if (!destination.toFile().exists()) {
					if (!destination.toFile().mkdir()) {
						throw new IOException("Unable to create directory "
								+ destination);
					}
				}
				filesAndDirs.add(destination.resolve(s));
				this.taskPool.addTask(new Runnable() {
					@Override
					public final void run() {
						if (AbcCreator.this.master.isInterrupted()) {
							return;
						}
						try {
							copyRek(source.resolve(s), destination.resolve(s));
						} catch (final IOException e) {
							e.printStackTrace();
						}
						AbcCreator.this.initState.progress();
					}
				});
			}
			return filesAndDirs;
		}
		final InputStream in = this.io.openIn(source.toFile());
		final OutputStream out = this.io.openOut(destination.toFile());
		this.io.write(in, out);
		this.io.close(out);
		filesAndDirs.add(destination);
		return filesAndDirs;
	}

	final void copyRek(final Path source, final Path destination)
			throws IOException {
		if (source.toFile().isDirectory()) {
			if (destination.toFile().exists()
					&& !destination.toFile().isDirectory()) {
				this.initState.setFailed();
				throw new IOException("Copying directory to file");
			}
			final String[] files = source.toFile().list();
			this.initState.incrementSize(files.length);
			for (final String s : files) {
				if (!destination.toFile().exists()) {
					if (!destination.toFile().mkdir()) {
						throw new IOException("Unable to create directory "
								+ destination);
					}
				}
				this.taskPool.addTask(new Runnable() {
					@Override
					public final void run() {
						if (AbcCreator.this.master.isInterrupted()
								|| AbcCreator.this.initState.failed()) {
							AbcCreator.this.bruteDir.delete();
							return;
						}
						try {
							copyRek(source.resolve(s), destination.resolve(s));
						} catch (final IOException e) {
							e.printStackTrace();
						}
						AbcCreator.this.initState.progress();
					}
				});
			}
			return;
		}
		final InputStream in = this.io.openIn(source.toFile());
		final OutputStream out = this.io.openOut(destination.toFile());
		this.io.write(in, out);
		this.io.close(out);
	}

	/*
	 * Copies all BruTE into current directory
	 */
	final boolean init() throws IOException {
		if (this.wdDir.toFile().isDirectory()) {
			final Path bruteArchive = this.wdDir.resolve("BruTE.jar");
			this.initState.startPhase(InitState.UNPACK_JAR);
			if (!bruteArchive.exists()) {
				final Path bruteArchive2 = bruteArchive.getParent().resolve(
						"..", "brute", bruteArchive.getFilename());
				if (!bruteArchive2.exists()) {
					final URL bruteArchive3 = getClass().getClassLoader()
							.getResource("BruTE.jar");
					if (bruteArchive3 == null
							&& !Path.getPath(bruteArchive3).exists()) {
						System.err.println("Unable to find Brute\n"
								+ bruteArchive + " does not exist.");
						return false;
					} else
						extract(bruteArchive3);
				} else
					extract(bruteArchive2);
			} else {
				extract(bruteArchive);
			}
		} else {
			final JarFile jarFile;
			jarFile = new JarFile(this.wdDir.toFile());
			try {
				extract(jarFile, "BruTE.jar");
			} finally {
				jarFile.close();
			}
		}
		return true;
	}
}
