package stone;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

	private static final Config c = Config.getInstance();

	private static final String downloadPage = c.getValue("url");

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
			final MasterThread master = MasterThread.class.cast(Thread
					.currentThread());
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
			if (master != null) {
				master.state.handleEvent(Event.UNLOCK_INT);
			}
		}
	}

	/**
	 * Returns a path for hosting temporarily files and directories. It will be
	 * deleted <i>this</i> thread has been terminated. If the invoking thread is
	 * not an instance of MasterThread <i>null</i> is returned.
	 * 
	 * @return a path located in (one of the) system's temporarily directory or
	 *         <i>null</i> if the invoking thread is no instance of
	 *         MasterThread.
	 */
	public final static Path tmp() {
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			final MasterThread master = MasterThread.class.cast(Thread
					.currentThread());
			return master.tmp;
		}
		return null;
	}

	final Path tmp = Path.getTmpDirOrFile(Main.TOOLNAME);
	final StartupContainer sc;

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
		final Path dataDirectory = StartupContainer.getDatadirectory();
		if (dataDirectory.exists() && dataDirectory.toFile().isFile()) {
			// old file on unix
			// dataDirectory == ~/.SToNe -> rename indirectly
			final Path tmp = dataDirectory.getParent().resolve(".SToNe.tmp");
			dataDirectory.renameTo(tmp);
			dataDirectory.toFile().mkdir();
			tmp.renameTo(Main.homeSetting);
		} else if (Path.getPath("~", ".SToNe").exists() && dataDirectory.toFile().isFile()) {
			// old file on windows
			// dataDirectory == %appdata% != ~/.SToNe -> rename directly
			dataDirectory.toFile().mkdir();
			Path.getPath("~", ".SToNe").renameTo(Main.homeSetting);
		}
		this.sc = os;
		os.setMaster(this);
		this.taskPool = taskPool;
		this.exceptionHandler = new UncaughtExceptionHandler() {

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
		setUncaughtExceptionHandler(this.exceptionHandler);
	}

	public stone.ModuleInfo getModuleInfo(final String m) {
		return this.modulesLocal.get(m);
	}

	/** */
	@Override
	public synchronized void interrupt() {
		this.event = Event.INT;
		notifyAll();
	}

	/**
	 * Interrupts <i>this</i> thread and blocks to wait for all tasks in the
	 * TaskPool to finish.
	 * 
	 * @throws InterruptedException
	 */
	public void interruptAndWait() throws InterruptedException {
		synchronized (this) {
			this.state.handleEvent(Event.INT);
			notifyAll();
		}
		this.taskPool.close();
	}

	/** */
	@Override
	public synchronized boolean isInterrupted() {
		if (this.event != null) {
			handleEvents();
		}
		return this.state.isInterrupted();
	}

	/**
	 * - Finishes startup - Sets the base if not happened before. - Asks the
	 * user which modules to use, launches selected ones - Destroys this process
	 */
	@Override
	public void run() {
		this.io = this.sc.getIO();
		this.wd = this.sc.workingDirectory;

		final ModuleInfo mainModule = new ModuleInfo(c);

		this.io.startProgress("Checking core for updates", -1);
		if (checkModule(mainModule)) {
			this.io.endProgress();
			downloadModule(mainModule.name);
			try {
				die(repack());
				return;
			} catch (final IOException e) {
				this.io.handleException(ExceptionHandle.CONTINUE, e);
				e.printStackTrace();
			}
		}
		
		this.io.endProgress();
		this.io.startProgress("Checking installed java", -1);
		try {
			this.io.checkJRE();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.io.endProgress();
		this.sc.waitForInit();

		if (this.sc.getMain().getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null) == null) {
			final Path fsBase = FileSystem.getBase();
			final Path base;
			if ((FileSystem.type == FileSystem.OSType.WINDOWS)
					&& (Double.parseDouble(System.getProperty("os.version")) < 6)) {
				// 5.0 Windows 2000
				// 5.1 Windows XP
				// 5.2 Windows XP - 64 bit, Server 2003, Server 2003 R2
				// %UserProfile% = C:\Documents and Settings\<username>
				base = fsBase;
			} else {
				base = fsBase.resolve("Documents");
			}
			this.sc.getMain().setConfigValue(Main.GLOBAL_SECTION,
					Main.PATH_KEY,
					base.resolve("The Lord of the Rings Online").toString());

		}
		try {
			final StringOption NAME_OPTION = Main.createNameOption(this.sc
					.getOptionContainer());
			final List<String> moduleSelection = init();
			if (moduleSelection == null) {
				return;
			}
			if (moduleSelection.contains(Main.REPAIR)) {
				repair();
				return;
			}
			checkAvailibility(moduleSelection);
			if (isInterrupted()) {
				return;
			}
			if (!this.sc.flags.parseWOError()) {
				System.err.println(this.sc.flags.unknownOption() + "\n"
						+ this.sc.flags.printHelp());
				die(null);
				return;
			}
			final ArrayDeque<Option> options = new ArrayDeque<>();
			for (final String module : this.possibleModules) {
				if (moduleSelection.contains(module)) {
					options.addAll(this.modulesLocal.get(module).instance
							.getOptions());
				}
			}
			if (!options.isEmpty()) {
				options.addFirst(NAME_OPTION);
				this.io.getOptions(options);
				if (isInterrupted()) {
					return;
				} else {
					this.sc.getMain().flushConfig();
				}
			}
			for (final String module : this.possibleModules) {
				if (moduleSelection.contains(module)) {
					runModule(module);
				}
			}
		} catch (final Exception e) {
			this.io.handleException(ExceptionHandle.TERMINATE, e);
		} finally {
			die(null);
		}
	}

	/**
	 * checks if all selected modules are available
	 * 
	 * @param moduleSelection
	 */
	private final void checkAvailibility(
			final Collection<String> moduleSelection) {
		try {
			if (isInterrupted()) {
				return;
			}
			final Set<String> changedModules = new HashSet<>();
			for (final String m : moduleSelection) {
				final ModuleInfo info = this.modulesLocal.get(m);
				if ((info == null) || checkModule(info)) {
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
			this.io.handleException(ExceptionHandle.TERMINATE, e);
			handleEvents();
		}
	}

	private final boolean checkModule(final ModuleInfo info) {
		try {
			final URL url = new URL(downloadPage + "moduleInfo/" + info.name);
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
			System.out.printf("%s %2d %2d\n", info.name, info.getVersion(),
					versionNew);
			return versionNew > info.getVersion();
		} catch (final IOException e) {
			if (e.getClass() == java.net.UnknownHostException.class) {
				if (this.suppressUnknownHost) {
					return false;
				}
				System.err.println("connection to " + e.getMessage()
						+ " failed");
				this.suppressUnknownHost = true;
			} else {
				e.printStackTrace();
			}
			this.io.printError("Failed to contact github to check if module\n"
					+ info.name + "\n is up to date", false);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	private final void die(final Path path) {
		if (path != null) {
			this.taskPool.close();

			final boolean isFile = this.wd.toFile().isFile();
			final Thread old = this;

			this.io.printMessage(
					"Update complete",
					"The update completed successfully.\nThe program will restart now.",
					true);
			interrupt(); // force shutdown
			this.io.close();
			if (isFile) {
				path.renameTo(this.wd);
			}
			this.tmp.delete();

			final Thread newMaster = new Thread() {

				@Override
				final public void run() {
					try {
						old.join();
						final Class<?> mainClass = ModuleLoader.createLoader()
								.loadClass(stone.Main.class.getCanonicalName());
						mainClass.getMethod("main", String[].class).invoke(
								null, MasterThread.this.sc.args());
					} catch (final IllegalAccessException
							| IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException
							| SecurityException | ClassNotFoundException
							| InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			newMaster.setName(getName());
			newMaster.start();
		} else {
			this.taskPool.close();
			this.state.handleEvent(Event.LOCK_INT);
			this.io.close();
			this.state.handleEvent(Event.UNLOCK_INT);
			this.tmp.delete();
			interrupt();
		}
	}

	private final void downloadModule(final String module) {
		this.io.startProgress("Donwloading module " + module, -1);
		try {
			final URL url = new URL(downloadPage + "modules/" + module + ".jar");
			final URLConnection connection = url.openConnection();
			final Path target;
			try {
				connection.connect();
			} catch (final IOException e) {
				if (e.getClass() == java.net.UnknownHostException.class) {
					this.io.printError("Connection failed " + e.getMessage(),
							false);
					interrupt();
					return;

				}
				System.err.println(e.getClass());
				throw e;
			}
			if (!this.tmp.exists()) {
				this.tmp.toFile().mkdir();
			}
			target = this.tmp.resolve(module + ".jar");
			final InputStream in = connection.getInputStream();
			final OutputStream out = this.io.openOut(target.toFile());
			this.io.setProgressSize(connection.getContentLength());
			final byte[] buffer = new byte[0x2000];
			try {
				while (true) {
					final int read = in.read(buffer);
					if (read < 0) {
						break;
					}
					out.write(buffer, 0, read);
					this.io.updateProgress(read);
				}
				this.io.close(out);

				// place downloaded module at the location where the module
				// loader will find it
				StartupContainer.registerDownloadedModule(target, module);
			} finally {
				in.close();
				this.io.close(out);
				this.io.endProgress();
			}
		} catch (final IOException e) {
			this.io.handleException(ExceptionHandle.TERMINATE, e);
			return;
		}

	}

	private synchronized final void handleEvents() {
		if (this.state.isInterrupted()) {
			return;
		}
		if (this.event == null) {
			return;
		}
		this.state.handleEvent(this.event);
		this.event = null;
	}

	private final List<String> init() {
		this.possibleModules.addAll(c.getSection("modules"));
		try {
			loadModules();
		} catch (final Exception e) {
			this.io.handleException(ExceptionHandle.TERMINATE, e);
			return null;
		}
		if (isInterrupted()) {
			return null;
		}
		this.sc.flags.parseWOError();
		if (this.sc.flags.isEnabled(stone.Main.HELP_ID)) {
			System.out.println(this.sc.flags.printHelp());
			return null;
		}
		if (this.sc.flags.isEnabled(stone.Main.REPAIR_ID)) {
			repair();
			return null;
		}
		this.sc.getOptionContainer().setValuesByParsedFlags();
		final List<String> modules;
		try {
			modules = this.io.selectModules(this.possibleModules);
		} catch (final InterruptedException e1) {
			// dead code
			return null;
		}
		return modules;
	}

	private final void loadModules() {
		this.io.startProgress("Searching for and loading modules",
				this.possibleModules.size());
		for (final String module : this.possibleModules) {
			if (isInterrupted()) {
				return;
			}
			final Class<Module> clazz = StartupContainer.loadModule(module);
			this.modulesLocal.put(module, new ModuleInfo(c, this.sc, clazz,
					module));
			this.io.updateProgress();
		}
		this.io.endProgress();
	}

	private final Path repack() throws IOException {
		if (isInterrupted()) {
			return null;
		}
		if (StartupContainer.getDatadirectory().resolve("SToNe.jar").exists()) {
			final Path tmpArchive = this.tmp.resolve("SToNe.jar");
			StartupContainer.getDatadirectory().resolve("SToNe.jar")
					.renameTo(tmpArchive);
			if (this.sc.jar) {
				return tmpArchive;
			} else {
				unpack(tmpArchive);
				final Path tmp_ = this.tmp.resolve("stone");
				final Path modulesPath = this.wd.resolve("stone");
				final String[] dirs = tmp_.toFile().list();
				this.io.startProgress("Placing new class files", dirs.length);
				boolean success = true;
				for (final String dir : dirs) {
					success &= tmp_.resolve(dir).renameTo(
							modulesPath.resolve(dir));
					this.io.updateProgress();
				}
				if (!success) {
					this.io.printError("Update failed", false);
					return null;
				}
			}
		}
		return this.wd;
	}

	private final void repair() {
		this.taskPool.addTask(new Runnable() {
			@Override
			public final void run() {
				MasterThread.this.sc.getMain().repair();
			}
		});
		for (final ModuleInfo m : this.modulesLocal.values()) {
			this.taskPool.addTask(new Runnable() {
				@Override
				public final void run() {
					m.instance.repair();
				}
			});
		}
		this.taskPool.waitForTasks();
	}

	private final void runModule(final String module) {
		if (isInterrupted()) {
			return;
		}
		final Module m = this.modulesLocal.get(module).instance.init(this.sc);
		m.run();
	}

	private final void unpack(final Path target) {
		try {
			final JarFile jar = new JarFile(target.toFile());
			this.io.startProgress("Unpacking", jar.size());
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
				final Path p = this.tmp.resolve(e.getName().split("/"));
				if (e.isDirectory()) {
					this.io.updateProgress();
					continue;
				}
				if (!p.getParent().exists()) {
					p.getParent().toFile().mkdirs();
				}
				final InputStream in = jar.getInputStream(e);
				final OutputStream out = this.io.openOut(p.toFile());
				this.io.write(in, out);
				this.io.close(out);
				this.io.updateProgress();
			}
			jar.close();
			target.delete();
		} catch (final IOException e) {
			e.printStackTrace();
			this.io.handleException(ExceptionHandle.TERMINATE, e);
		}
	}
}
