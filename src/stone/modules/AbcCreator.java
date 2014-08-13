package stone.modules;

import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;

import java.io.File;
import java.io.IOException;
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
import stone.modules.abcCreator.AbcMapPlugin;
import stone.modules.abcCreator.BruteParams;
import stone.modules.abcCreator.DndPluginCaller;
import stone.modules.abcCreator.DndPluginCallerParams;
import stone.modules.abcCreator.DragObject;
import stone.modules.abcCreator.DropTarget;
import stone.modules.abcCreator.DropTargetContainer;
import stone.modules.abcCreator.DrumMapFileFilter;
import stone.modules.abcCreator.ExecutableFileFilter;
import stone.modules.abcCreator.InstrumentMapFileFilter;
import stone.modules.abcCreator.MidiFileFilter;
import stone.modules.abcCreator.StreamPrinter;
import stone.modules.midiData.MidiInstrument;
import stone.modules.midiData.MidiMap;
import stone.modules.midiData.MidiParser;
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
			if (state == InitState.DRUM_MAP) {
				return "Copying drum-maps to working dir";
			}
			if (state == InitState.UNPACK_JAR) {
				return "Unpacking BruTE";
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

		synchronized final void drawState(@SuppressWarnings("hiding") final IOHandler io) {
			this.io = io;
			if (failed) {
				return;
			}
			io.startProgress(getMessage(), size);
			io.updateProgress(progress);
		}

		synchronized final boolean failed() {
			return failed;
		}

		synchronized final void incrementSize(int value) {
			this.size += value;
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

		synchronized final void startPhase(
				@SuppressWarnings("hiding") final Object state) {
			this.state = state;
			progress = 0;
			size = -1;
			if (io != null) {
				io.startProgress(getMessage(), size);
			}
		}

		synchronized final void startPhase(
				@SuppressWarnings("hiding") final Object state,
				@SuppressWarnings("hiding") int size) {
			this.state = state;
			progress = 0;
			this.size = size;
			if (io != null) {
				io.startProgress(getMessage(), size);
			}
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
	public static final int DRUM_MAPS_COUNT = 6;
	private static final PathOptionFileFilter EXEC_FILTER =
			new ExecutableFileFilter();

	private static final PathOptionFileFilter DRUM_MAP_FILTER =
			new DrumMapFileFilter();

	private static final PathOptionFileFilter INSTR_MAP_FILTER =
			new InstrumentMapFileFilter();

	private final static int VERSION = 1;

	private static final FileFilter midiFilter = new MidiFileFilter();

	private static final Path javaPath = AbcCreator.getJavaPath();

	final Path bruteDir;

	final IOHandler io;

	final InitState initState;

	final MasterThread master;

	private final PathOption ABC_PLAYER;

	private final PathOption DRUM_MAPS;

	private final PathOption INSTRUMENT_MAP;

	private final StringOption STYLE;

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
		initState = null;
		brutesMidi = brutesMap = brutesAbc = null;
		main = null;
	}

	/**
	 * @param sc
	 * @throws InterruptedException
	 */
	public AbcCreator(final StartupContainer sc) throws InterruptedException {
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
		initState = new InitState();
		main = sc.getMain();
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
		dragAndDropPlugin =
				new AbcMapPlugin(this, taskPool, parser, targets, io);
		initState = abc.initState;
		main = abc.main;
	}

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
				"The style to use for generated abc. Possible values are Rocks, Meisterbarden and TSO",
				"Style", Flag.NoShortFlag, "style", AbcCreator.SECTION,
				"style", "Rocks");
	}

	private final static Path getJavaPath() {
		final Path javaBin =
				Path.getPath(
						System.getProperty("java.home").split(
								"\\" + FileSystem.getFileSeparator())).resolve(
						"bin");
		final Path javaPath_;
		if (FileSystem.type == FileSystem.OSType.WINDOWS) {
			javaPath_ = javaBin.resolve("java.exe");
		} else {
			javaPath_ = javaBin.resolve("java");
		}
		return javaPath_;
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
	public final boolean call_back(final Object name, final Object title,
			int abcTracks) {
		io.startProgress("Creating map", abcTracks + 1);
		final Path map =
				generateMap(name == null ? "<insert your name here>" : name,
						title == null ? abc.getFileName() : title);
		io.endProgress();
		if (map == null) {
			// no abc-tracks
			return true;
		}
		System.out.println("generated map " + map);
		try {
			copy(map, brutesMap);
			io.startProgress("Waiting for BruTE to finish", abcTracks + 1);
			final int remap = call("remap.exe", bruteDir);
			io.endProgress();
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
					io.startProgress("Starting AbcPlayer", -1);
					if (abcPlayer.getFileName().endsWith(".exe")) {
						call(abcPlayer, CallType.EXE, abcPlayer.getParent());
					} else {
						call(abcPlayer, CallType.JAR, abcPlayer.getParent(),
								abc.toString());
					}
					io.endProgress();
				}
			} catch (final IOException | InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public final Path getFile() {
		return abc;
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
						initState.setFailed();
						bruteDir.delete();
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
		dragAndDropPlugin.link(object, target);
	}

	@Override
	public final void loadMap(final File mapToLoad,
			final DndPluginCaller.LoadedMapEntry c) {
		@SuppressWarnings("resource") final InputStream in =
				io.openIn(mapToLoad);
		in.registerProgressMonitor(io);
		class ParseState {
			private int state;

			final boolean comment() {
				return state < 0;
			}

			final void parseLine(final String line) {
				switch (state) {
					case 0x7000_0000:
						return;
					case 0:
						if (line.startsWith("Speedup: ")) {
							// TODO implement setting of global speed-up
						} else if (line.startsWith("Pitch: ")) {
							// TODO implement setting of global pitch
						} else if (line.startsWith("Style: ")) {
							// TODO implement setting of style
						} else if (line.startsWith("Volume: ")) {
							// TODO implement setting of global volume
						} else if (line.startsWith("Compress: ")) {
							// TODO implement setting of global volume
						} else if (line.startsWith("abctrack begin")) {
							++state;
//						} else if (line.startsWith("fadeout length")) {
							// TODO add support when needed
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
								state = 2;
								parseLine(line);
							}
							return;
						}
					case 2:
						if (!line.startsWith("instrument ")) {
							state = 0x7000_0000;
							return;
						}
						final String s0 = line.substring(11).trim();

						c.addPart(s0);

						state = 3;
						break;
					case 3:
						if (line.startsWith("miditrack")) {
							c.addEntry(line.substring(10));
						} else if (line.startsWith("abctrack end"))
							state = 7;
						else
							return;
						break;
					case 7:
						if (line.startsWith("abctrack begin")) {
							state = 1;
						} else
							return;
						break;
				}
				System.out.println(". " + line);
			}

			final void toggleComment() {
				state = ~state;
			}
		}

		final ParseState state = new ParseState();
		System.out.println("loading map " + mapToLoad);
		try {
			while (true) {
				final String line = in.readLine();
				if (line == null)
					break;
				if (line.startsWith("%"))
					continue;
				if (line.trim().equals("*")) {
					state.toggleComment();
				}
				if (state.comment())
					continue;
				state.parseLine(line);
			}
		} catch (final IOException e) {
			c.error();
			System.err.println(e);
		} catch (final Exception e) {
			c.error();
			e.printStackTrace();
		} finally {
			io.endProgress();
			io.close(in);
		}
		System.out.println("... completed");
	}

	@Override
	public final void printError(final String string) {
		dragAndDropPlugin.printError(string);
	}

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
			initState.drawState(io);
			taskPool.waitForTasks();
			if (initState.failed() || master.isInterrupted()) {
				return;
			}
			final Path instrumentMap = INSTRUMENT_MAP.getValue();
			final Path drumMaps = DRUM_MAPS.getValue();
			if (instrumentMap != null) {
				initState.startPhase(InitState.INSTRUMENT_MAP,
						(int) instrumentMap.toFile().length());
				MidiInstrument.readMap(instrumentMap, io);
			}
			if (drumMaps != null) {
				initState.startPhase(InitState.DRUM_MAP);
				prepareMaps(drumMaps);
			}
			for (int i = 0; i < AbcCreator.DRUM_MAPS_COUNT; i++) {
				maps.add(i);
			}
			io.endProgress();
			runLoop();
		} finally {
			for (final Process p : processList) {
				p.destroy();
				final boolean interrupted = MasterThread.interrupted();
				try {
					p.waitFor();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				if (interrupted)
					master.interrupt();
			}
			bruteDir.delete();
		}
	}

	@Override
	public final TreeSet<DropTarget<JPanel, JPanel, JPanel>> sortedTargets() {
		return dragAndDropPlugin.targets();
	}

	/** */
	@Override
	public final boolean unlink(
			final DragObject<JPanel, JPanel, JPanel> object,
			final DropTarget<JPanel, JPanel, JPanel> target) {
		return dragAndDropPlugin.unlink(object, target);
	}

	@Override
	public final DndPluginCallerParams[] valuesGlobal() {
		return BruteParams.valuesGlobal();
	}

	@SuppressWarnings("resource")
	private final int call(final Path location, final CallType type,
			final Path wd, final String... cmd) throws IOException,
			InterruptedException {
		if (master.isInterrupted()) {
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
				pb =
						new ProcessBuilder(AbcCreator.javaPath.toString(),
								"-jar", player);
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
				p = Runtime.getRuntime().exec(location.toString(), null, wd.toFile());
				break;
			default:
				return -1;
		}
		final java.io.InputStream is = p.getInputStream();
		final java.io.InputStream es = p.getErrorStream();
		final StringBuilder outErr = new StringBuilder();
		final StringBuilder outStd = new StringBuilder();

		processList.add(p);
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
				boolean first = true;

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
							final String[] s =
									line.replaceFirst("\r\n", "").split("/");
							if (first) {
								first = false;
								io.setProgressSize(Integer.parseInt(s[1]) + 1);
							}
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
		processList.remove(p);
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

	@SuppressWarnings("resource")
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
				taskPool.addTask(new Runnable() {
					@Override
					public final void run() {
						if (master.isInterrupted()) {
							return;
						}
						try {
							copyRek(source.resolve(s), destination.resolve(s));
						} catch (final IOException e) {
							e.printStackTrace();
						}
						initState.progress();
					}
				});
			}
			return filesAndDirs;
		}
		final InputStream in = io.openIn(source.toFile());
		final OutputStream out = io.openOut(destination.toFile());
		io.write(in, out);
		io.close(out);
		filesAndDirs.add(destination);
		return filesAndDirs;
	}

	final void copyRek(final Path source, final Path destination)
			throws IOException {
		if (source.toFile().isDirectory()) {
			if (destination.toFile().exists()
					&& !destination.toFile().isDirectory()) {
				initState.setFailed();
				throw new IOException("Copying directory to file");
			}
			final String[] files = source.toFile().list();
			initState.incrementSize(files.length);
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
						if (master.isInterrupted() || initState.failed()) {
							bruteDir.delete();
							return;
						}
						try {
							copyRek(source.resolve(s), destination.resolve(s));
						} catch (final IOException e) {
							e.printStackTrace();
						}
						initState.progress();
					}
				});
			}
			return;
		}
		@SuppressWarnings("resource") final InputStream in =
				io.openIn(source.toFile());
		@SuppressWarnings("resource") final OutputStream out =
				io.openOut(destination.toFile());
		io.write(in, out);
		io.close(out);
	}

	private final void extract(final JarFile jarFile, final String string)
			throws IOException {
		initState.startPhase(InitState.READ_JAR);
		final ZipEntry jarEntry = jarFile.getEntry(string);
		initState.startPhase(InitState.UNPACK_JAR);
		unpack(jarFile, jarEntry);
		final Path jar = bruteDir.resolve(string);
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
		initState.setSize(InitState.UNPACK_JAR, entries.size());
		unpack(jarFile1, entries.toArray(new JarEntry[entries.size()]));
		try {
			jarFile1.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		jar.delete();

	}

	@SuppressWarnings("resource")
	private final Path generateMap(final Object name, final Object title) {
		final Path map = midi.getParent().resolve(midi.getFileName() + ".map");
		final OutputStream out = io.openOut(map.toFile());
		final String style = STYLE.value();

		io.writeln(out, String.format("Name: %s", title));
		io.writeln(out, "Speedup: " + BruteParams.SPEED.value());
		io.writeln(out, "Pitch: " + BruteParams.PITCH.value());
		io.writeln(out, "Style: " + style);
		io.writeln(out, "Volume: " + BruteParams.VOLUME.value());
		io.writeln(out, "Compress: " + BruteParams.DYNAMIC.value());
		io.writeln(out,
				"%no pitch guessing   %uncomment to switch off guessing of default octaves");
		io.writeln(
				out,
				"%no back folding     %uncomment to switch off folding of tone-pitches inside the playable region");
		io.writeln(out, "fadeout length 0    %unoperational still!!!!");
		io.writeln(out, String.format("Transcriber : %s", name));
		final Map<DragObject<JPanel, JPanel, JPanel>, Integer> abcPartMap =
				new HashMap<>();
		boolean empty = true;
		io.updateProgress();

		for (final Iterator<DropTargetContainer<JPanel, JPanel, JPanel>> targetIter =
				this.targets.iterator();;) {
			final DropTargetContainer<JPanel, JPanel, JPanel> target =
					targetIter.next();
			if (!targetIter.hasNext())
				break;
			for (final DropTarget<JPanel, JPanel, JPanel> t : target) {
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
						target.toString(), params.toString()));
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
	final boolean init() throws IOException {
		if (wdDir.toFile().isDirectory()) {
			final Path bruteArchive = wdDir.resolve("Brute.jar");
			if (!bruteArchive.exists()) {
				System.err.println("Unable to find Brute\n" + bruteArchive
						+ " does not exist.");
				return false;
			}
			initState.startPhase(InitState.UNPACK_JAR);
			extract(bruteArchive);
			io.endProgress();
		} else {
			final JarFile jarFile;
			jarFile = new JarFile(wdDir.toFile());
			try {
				extract(jarFile, "BruTE.jar");
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
			initState.setSize(InitState.DRUM_MAP, files.length);
			for (final String f : files) {
				if (f.startsWith("drum") && f.endsWith(".drummap.txt")) {
					final String idString = f.substring(4, f.length() - 12);
					final int id;
					try {
						id = Integer.parseInt(idString);
					} catch (final Exception e) {
						initState.progress();
						continue;
					}
					taskPool.addTask(new Runnable() {

						@Override
						public final void run() {
							try {
								copy(drumMaps.resolve(f), bruteDir.resolve(f));

								initState.progress();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					});
					maps.add(id);
				} else {
					initState.progress();
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
		final StringOption TITLE =
				new StringOption(null, null, "Title displayed in the abc",
						"Title", Flag.NoShortFlag, Flag.NoLongFlag,
						AbcCreator.SECTION, "title", null);
		while (true) {
			if (master.isInterrupted()) {
				return;
			}
			midi =
					io.selectFile(
							"Which midi do you want to transcribe to abc?",
							midi == null ? Path.getPath(
									main.getConfigValue(Main.GLOBAL_SECTION,
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
			final List<Option> options = new ArrayList<>();
			
			TITLE.value(defaultTitle);
			options.add(TITLE);
			io.getOptions(options);
			if (master.isInterrupted()) {
				return;
			}
			final String name =
					main.getConfigValue(Main.GLOBAL_SECTION, Main.NAME_KEY,
							null);
			if (name == null) {
				return;
			}
			if (call_back(name, TITLE.value(), dragAndDropPlugin.size())) {
				io.printMessage(null, "transcribed\n" + midi + "\nto\n" + abc,
						true);
			}
			dragAndDropPlugin.reset();
		}
	}


	@SuppressWarnings("resource")
	private final void unpack(final JarFile jarFile,
			final ZipEntry... jarEntries) {
		for (final ZipEntry jarEntry : jarEntries) {
			if (master.isInterrupted()) {
				return;
			}

			final OutputStream out;
			final java.io.File file;
			if (jarEntries.length == 1) {
				file = bruteDir.resolve(jarEntry.getName()).toFile();
			} else {
				file =
						bruteDir.resolve(jarEntry.getName().substring(6))
								.toFile();
			}
			if (!file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) {
					initState.setFailed();
					bruteDir.delete();
					return;
				}
			}
			try {
				out = io.openOut(file);
				try {
					io.write(jarFile.getInputStream(jarEntry), out);
				} finally {
					io.close(out);
				}
			} catch (final IOException e) {
				initState.setFailed();
				bruteDir.delete();
				return;
			}
			initState.progress();
		}
	}

	private final void writeAbcTrack(final OutputStream out,
			final DropTarget<JPanel, JPanel, JPanel> abcTrack,
			final Map<DragObject<JPanel, JPanel, JPanel>, Integer> abcPartMap) {

		for (final DragObject<JPanel, JPanel, JPanel> midiTrack : abcTrack) {
			final int pitch = midiTrack.getParam(BruteParams.PITCH, abcTrack);
			final int volume = midiTrack.getParam(BruteParams.VOLUME, abcTrack);
			final int delay = midiTrack.getParam(BruteParams.DELAY, abcTrack);
			io.write(out, String.format(
					"miditrack %d pitch %d volume %d delay %d",
					midiTrack.getId(), pitch, volume, delay));
			final int total = midiTrack.getTargets();
			if (total > 1) {
				int part = 0;
				if (abcPartMap.containsKey(midiTrack)) {
					part = abcPartMap.get(midiTrack);
				}
				abcPartMap.put(midiTrack, part + 1);
				io.writeln(out, String.format(" prio 100 " + "split %d %d",
						total, part));
			} else {
				io.write(out, "\r\n");
			}
		}
	}


}
