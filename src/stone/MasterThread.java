package stone;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.modules.Main;
import stone.modules.Module;
import stone.util.FileSystem;
import stone.util.Option;
import stone.util.Path;
import stone.util.StringOption;
import stone.util.TaskPool;

/**
 * @author Nelphindal
 */
public class MasterThread extends Thread {

	public final static String MODULE_VC_DSP = "Synchronize abc-files";
	public final static String MODULE_VC_NAME = "VersionControl";
	public final static String MODULE_VC_TOOLTIP = "Enables download of changed abc-files. Upload can be enabled additionally.";
	
	public final static String MODULE_ABC_DSP = "Transcribe midi-files to abc-files";
	public final static String MODULE_ABC_NAME = "AbcCreator";
	public final static String MODULE_ABC_TOOLTIP = "Starts the GUI for BruTE - the midi to abc transriper by Bruzo";
	
	public final static String MODULE_SB_DSP = "Create Songbook data";
	public final static String MODULE_SB_NAME = "SongbookUpdater";
	public final static String MODULE_SB_TOOLTIP = "Creates the file needed for the Songbook-plugin by Chiran";
	
	final class ModuleInfo {

		Module instance;
		final String name;

		public ModuleInfo(final Class<Module> clazz, final String name) {
			try {
				instance = clazz.getConstructor(sc.getClass()).newInstance(sc);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				instance = null;
			}
			this.name = name;

		}

		ModuleInfo() {
			// cut here
			this.name = "Main_band";
			// cut here
			instance = sc.getMain();
		}

		public Module clear() {
			final Module m = instance;
			instance = null;
			return m;
		}

		final int getVersion() {
			return instance == null ? -1 : instance.getVersion();
		}
	}

	final Path tmp = Path.getTmpDir(Main.TOOLNAME);
	
	final StartupContainer sc;
	
	private static final String repo =
	  // old URL
	  // "https://raw.githubusercontent.com/Greonyral/stone/master/";
	  "https://github.com/Greonyral/stone/raw/master/";
	  //"file:/D:/Freigabe/Programmierung/arbeitsplatz/Songbook/"; 

	private final ThreadState state = new ThreadState();

	
	private final Map<String, ModuleInfo> modulesLocal = new HashMap<>();
	private final List<String> possibleModules = new ArrayList<>();

	private final TaskPool taskPool;
	private final UncaughtExceptionHandler exceptionHandler;

	private Event event;

	private IOHandler io;

	private boolean suppressUnknownHost;

	private Path wd;

	/**
	 * @param os
	 * @param taskPool
	 */
	public MasterThread(final StartupContainer os, final TaskPool taskPool) {
		sc = os;
		os.setMaster(this);
		this.taskPool = taskPool;
		exceptionHandler = new UncaughtExceptionHandler() {

			@Override
			public final void uncaughtException(final Thread t,
					final Throwable e) {
				if (t.getName().startsWith("AWT-EventQueue")) {
					final String clazz = e.getStackTrace()[0].getClassName();
					if (clazz.startsWith("javax.") || clazz.startsWith("java.")) {
						System.err.println("suppressed exception in thread "
								+ t);
						// suppress exception caused by java(x) packages
						return;
					}
				}
				e.printStackTrace();
			}


		};
		setUncaughtExceptionHandler(exceptionHandler);
	}

	/**
	 * Checks the interrupt state of current thread. In the case the current
	 * thread is an instance of MasterThread the interrupt-flag will be reset
	 * else it will be untouched.
	 * 
	 * @return <i>True</i> if the underlying MasterThread has been interrupted
	 *         and interrupts have not been disabled.
	 */
	public static boolean interrupted() {
		final boolean interrupted = Thread.interrupted();
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			final MasterThread master =
					MasterThread.class.cast(Thread.currentThread());
			master.state.handleEvent(Event.CLEAR_INT);
		}
		return interrupted;
	}
	
	/**
	 * Suspends current thread for given time.
	 * 
	 * @param millis
	 *            Milliseconds to suspend.
	 */
	public static void sleep(long millis) {
		MasterThread master = null;
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			master = MasterThread.class.cast(Thread.currentThread());
			master.state.handleEvent(Event.LOCK_INT);
		}
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (master != null)
				master.state.handleEvent(Event.UNLOCK_INT);
		}
	}

	/**
	 * Returns a path for hosting temporarily files and directories. It will be deleted <i>this</i> thread has been terminated. If the
	 * invoking thread is not an instance of MasterThread <i>null</i> is returned.
	 * 
	 * @return a path located in (one of the) system's temporarily directory or <i>null</i> if the invoking thread is no instance of
	 *         MasterThread.
	 */
	public final static Path tmp() {
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			final MasterThread master =
					MasterThread.class.cast(Thread.currentThread());
			return master.tmp;
		}
		return null;
	}

	/** */
	@Override
	public synchronized void interrupt() {
		event = Event.INT;
		notifyAll();
	}

	/**
	 * Interrupts <i>this</i> thread and blocks to wait for all tasks in the TaskPool to finish.
	 * @throws InterruptedException
	 */
	public void interruptAndWait() throws InterruptedException {
		synchronized (this) {
			state.handleEvent(Event.INT);
			notifyAll();
		}
		taskPool.waitForTasks();
	}

	/** */
	@Override
	public synchronized boolean isInterrupted() {
		if (event != null) {
			handleEvents();
		}
		return state.isInterrupted();
	}


	/**
	 * - Finishes startup
	 * - Sets the base if not happened before.
	 * - Asks the user which modules to use, launches selected ones 
	 * - Destroys this process
	 */
	@Override
	public void run() {
		io = sc.getIO();
		wd = sc.workingDirectory;
		final ModuleInfo mainModule = new ModuleInfo();
		io.startProgress("Checking core for updates", -1);
		if (checkModule(mainModule)) {
			io.endProgress();
			downloadModule(mainModule.name);
			try {
				die(repack());
				return;
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
				e.printStackTrace();
			}
		}
		io.endProgress();
		sc.waitForInit();

		if (sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null) == null) {
			final Path fsBase = FileSystem.getBase();
			final Path base;
			if (FileSystem.type == FileSystem.OSType.WINDOWS
				// 6.0: Vista, Server 2008
				// 6.1: Server 2008 R2, 7
				// 6.2: 8, Server 2012
				// 6.3: 8.1, Server 2012 R2
				// %UserProfile% = C:\Users\<username>
				
				// 5.0 Windows 2000
				// 5.1 Windows XP
				// 5.2 Windows XP - 64 bit, Server 2003, Server 2003 R2
				// %UserProfile% = C:\Documents and Settings\<username>
					&& Double
							.parseDouble(System.getProperty("os.version")) < 6)
				base = fsBase;
			else
				base = fsBase.resolve("Documents");
			sc.getMain().setConfigValue(
					Main.GLOBAL_SECTION,
					Main.PATH_KEY,
					base.resolve("The Lord of The Rings Online")
							.toString());

		}
		try {
			final StringOption NAME_OPTION = Main.createNameOption(sc.getOptionContainer());
			final Set<String> moduleSelection = init();
			if (moduleSelection == null)
				return;
			if (moduleSelection.contains(Main.REPAIR)) {
				repair();
				return;
			}
			checkAvailibility(moduleSelection);
			if (isInterrupted()) {
				return;
			}
			final ArrayDeque<Option> options = new ArrayDeque<>();
			for (final String module : possibleModules) {
				if (moduleSelection.contains(module)) {
					options.addAll(modulesLocal.get(module).instance
							.getOptions());
				}
			}
			if (!options.isEmpty()) {
				options.addFirst(NAME_OPTION);
				io.getOptions(options);
				if (!isInterrupted())
					sc.getMain().flushConfig();
			}
			for (final String module : possibleModules) {
				if (moduleSelection.contains(module)) {
					runModule(module);
				}
			}
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
		} finally {
			die(null);
		}
	}

	/**
	 * checks if all selected modules are available
	 * 
	 * @param moduleSelection
	 */
	private final void checkAvailibility(final Set<String> moduleSelection) {
		try {
			if (isInterrupted()) {
				return;
			}
			final Set<String> changedModules = new HashSet<>();
			for (final String m : moduleSelection) {
				final ModuleInfo info = modulesLocal.get(m);
				if (info == null || checkModule(info)) {
					changedModules.add(m);
					downloadModule(m);
				}
				if (isInterrupted()) {
					return;
				}
			}
			if (changedModules.isEmpty()) {
				return;
			}
			die(repack());
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			handleEvents();
		}
	}

	private final boolean checkModule(final ModuleInfo info) {
		try {
			final URL url =
					new URL(repo + "moduleInfo/" + info.name);
			final URLConnection connection = url.openConnection();
			connection.connect();
			final InputStream in = connection.getInputStream();
			final byte[] bytes = new byte[4];
			final int versionRead = in.read(bytes);
			in.close();
			if (versionRead < 0) {
				return false;
			}
			final int versionNew = ByteBuffer.wrap(bytes).getInt();
			return versionNew != info.getVersion();
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			if (e.getClass() == java.net.UnknownHostException.class) {
				if (suppressUnknownHost) {
					return false;
				}
				System.err.println("connection to " + e.getMessage()
						+ " failed");
				suppressUnknownHost = true;
			} else {
				e.printStackTrace();
			}
			io.printError("Failed to contact github to check if module\n"
					+ info.name + "\n is up to date", false);
		}
		return false;
	}

	private final void die(final Path path) {
		if (path != null) {
			taskPool.close();
			final boolean isFile = wd.toFile().isFile();

			io.printMessage(
					"Update complete",
					"The update completed successfully.\nThe program will restart now.",
					true);
			interrupt();
			io.close();
			if (isFile) {
				path.renameTo(this.wd);
			}
			tmp.delete();
			
			new Thread() {

				@Override
				final public void run() {
					try {
						final Class<?> mainClass = ModuleLoader.createLoader().loadClass(stone.Main.class.getCanonicalName());
						mainClass.getMethod("main", String[].class).invoke(
								null, sc.flags());
					} catch (final IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException
							| SecurityException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		} else {
			taskPool.close();
			io.close();
			tmp.delete();
			interrupt();
		}
	}

	private final void downloadModule(final String module) {
		io.startProgress("Donwloading module " + module, -1);
		try {
			final URL url =
					new URL(repo  + "modules/" + module + ".jar");
			final URLConnection connection = url.openConnection();
			final Path target;
			try {
				connection.connect();
			} catch (final IOException e) {
				if (e.getClass() == java.net.UnknownHostException.class) {
					io.printError("Connection failed " + e.getMessage(), false);
					interrupt();
					return;

				}
				System.err.println(e.getClass());
				throw e;
			}
			io.setProgressSize(connection.getContentLength());
			if (!tmp.exists()) {
				tmp.toFile().mkdir();
			}
			target = tmp.resolve(module + ".jar");
			final InputStream in = connection.getInputStream();
			final OutputStream out = io.openOut(target.toFile());
			final byte[] buffer = new byte[0x2000];
			try {
				while (true) {
					final int read = in.read(buffer);
					if (read < 0) {
						break;
					}
					out.write(buffer, 0, read);
					io.updateProgress(read);
				}
				io.close(out);
				unpackModule(target);
			} finally {
				in.close();
				io.close(out);
				io.endProgress();
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			return;
		}

	}

	private synchronized final void handleEvents() {
		if (state.isInterrupted()) {
			return;
		}
		if (event == null) {
			return;
		}
		state.handleEvent(event);
		event = null;
	}

	private final Set<String> init() {
		possibleModules.add(MODULE_ABC_NAME);
		possibleModules.add("FileEditor");
		// cut here
		possibleModules.add(MODULE_VC_NAME);
		// cut here
		possibleModules.add(MODULE_SB_NAME);
		try {
			loadModules();
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			return null;
		}
		if (isInterrupted()) {
			return null;
		}
		if (sc.flags.isEnabled(stone.Main.HELP_ID)) {
			System.out.println(sc.flags.printHelp());
			return null;
		}
		if (sc.flags.isEnabled(stone.Main.REPAIR_ID)) {
			repair();
			return null;
		}
		try {
			return io.selectModules(possibleModules);
		} catch (final InterruptedException e1) {
			// dead code
			return null;
		}
	}

private final void loadModules() {
		io.startProgress("Searching for and loading modules", possibleModules.size());
		for (final String module : possibleModules) {
			if (isInterrupted()) {
				return;
			}
			final Class<Module> clazz = StartupContainer.loadModule(module);
			if (clazz != null)
				modulesLocal.put(module, new ModuleInfo(clazz, module));
			io.updateProgress();
		}
		io.endProgress();
	}
	
	private final Path repack() throws IOException {
		if (isInterrupted()) {
			return null;
		}
		if (sc.jar) {
			final JarFile jar = new JarFile(wd.toFile());
			io.startProgress("Unpacking running archive", jar.size());
			final Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				if (isInterrupted()) {
					jar.close();
					return null;
				}
				JarEntry entry = entries.nextElement();
				while (entry.isDirectory()) {
					if (!entries.hasMoreElements()) {
						entry = null;
						io.endProgress();
						break;
					}
					io.updateProgress();
					entry = entries.nextElement();
				}
				if (entry == null) {
					break;
				}
				final Path target = tmp.resolve(entry.getName().split("/"));
				if (target.exists()) {
					io.updateProgress();
					continue;
				}
				if (!target.getParent().exists()) {
					target.getParent().toFile().mkdirs();
				}
				final OutputStream out = io.openOut(target.toFile());
				io.write(jar.getInputStream(entry), out);
				io.close(out);
				io.updateProgress();
			}
			jar.close();
			final String[] f = tmp.toFile().list();
			io.startProgress("Packing new archive", f.length);
			final ArrayDeque<Task> worklist = new ArrayDeque<>();
			for (final String s : f) {
				worklist.add(new Task(s, tmp));
			}
			final Path target = tmp.resolve("new.jar");
			final OutputStream out = io.openOut(target.toFile());
			final JarOutputStream jarout = new JarOutputStream(out);
			int size = f.length;
			while (!worklist.isEmpty()) {
				final Task t = worklist.pop();
				if (t.source.toFile().isDirectory()) {
					final String[] ss = t.source.toFile().list();
					io.setProgressSize(size += ss.length);
					for (final String s : ss) {
						worklist.add(new Task(t, s));
					}
					io.updateProgress();
				} else {
					jarout.putNextEntry(new ZipEntry(t.name));
					io.write(io.openIn(t.source.toFile()), jarout);
					jarout.closeEntry();
					io.updateProgress();
					t.source.delete();
				}
			}
			jarout.close();
			io.close(out);
			return target;
		}
		final Path tmp_ = this.tmp.resolve("stone/modules");
		final Path modulesPath = wd.resolve("stone/modules");
		final String[] dirs = tmp_.toFile().list();
		io.startProgress("Placing new class files", dirs.length);
		boolean success = true;
		for (final String dir : dirs) {
			success &= tmp_.resolve(dir).renameTo(modulesPath.resolve(dir));
			io.updateProgress();
		}
		if (!success) {
			io.printError("Update failed", false);
			return null;
		}
		return wd;
	}
	
	private final void repair() {
		taskPool.addTask(new Runnable() {
			@Override
			public final void run() {
				sc.getMain().repair();
			}
		});
		for (final ModuleInfo m : modulesLocal.values()) {
			taskPool.addTask(new Runnable() {
				@Override
				public final void run() {
					m.instance.repair();
				}
			});
		}
		taskPool.waitForTasks();
	}

	private final void runModule(final String module) {
		if (isInterrupted()) {
			modulesLocal.get(module).clear().run();
			return;
		}
		final Module m = modulesLocal.get(module).clear().init(sc);
		m.run();
	}

	private final void unpackModule(final Path target) {
		try {
			final JarFile jar = new JarFile(target.toFile());
			io.startProgress("Unpacking", jar.size());
			for (final JarEntry e : new Iterable<JarEntry>() {
				final Enumeration<JarEntry> es = jar.entries();

				@Override
				public final Iterator<JarEntry> iterator() {
					return new Iterator<JarEntry>() {

						@Override
						public final boolean hasNext() {
							return es.hasMoreElements();
						}

						@Override
						public final JarEntry next() {
							return es.nextElement();
						}

						@Override
						public final void remove() {
							throw new UnsupportedOperationException();
						}

					};
				}

			}) {
				final Path p = tmp.resolve(e.getName().split("/"));
				if (e.isDirectory()) {
					io.updateProgress();
					continue;
				}
				if (!p.getParent().exists()) {
					p.getParent().toFile().mkdirs();
				}
				final InputStream in = jar.getInputStream(e);
				final OutputStream out = io.openOut(p.toFile());
				io.write(in, out);
				io.close(out);
				io.updateProgress();
			}
			jar.close();
			target.delete();
		} catch (final IOException e) {
			e.printStackTrace();
			io.handleException(ExceptionHandle.TERMINATE, e);
		}
	}
}