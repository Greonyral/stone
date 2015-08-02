package stone;

import java.util.Iterator;
import java.util.Map;

import stone.io.IOHandler;
import stone.modules.Main;
import stone.modules.Module;
import stone.util.FileSystem;
import stone.util.Flag;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.TaskPool;

/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class StartupContainer {

	/**
	 * Only one instance shall exist at one time.
	 * 
	 * @return the new created instance.
	 */
	public final static StartupContainer createInstance() {
		return new StartupContainer();
	}

	/**
	 * 
	 * @see FileSystem
	 * @return the parsed output of {@link FileSystem#getDataDirectory()}
	 */
	public final static Path getDatadirectory() {
		return dataDirectory;
	}

	/**
	 * Asks the ModuleLoader to load given module
	 * 
	 * @param module {@link Module} to load
	 * @return the Class of loaded Module or <i> null</i> if the module could
	 *         not been found.
	 */
	public final static Class<Module> loadModule(final String module) {
		try {
			@SuppressWarnings("unchecked")
			final Class<Module> clazz = (Class<Module>) StartupContainer.loader
					.loadClass("stone.modules." + module);
			return clazz;
		} catch (final ClassNotFoundException e) {
			// Should never been thrown, but to make the compiler happy
			// e.printStackTrace();
		}
		return null;
	}

	private TaskPool taskPool;

	boolean initDone;
	final boolean jar;
	final Path workingDirectory;

	private IOHandler io;

	private MasterThread master;

	private OptionContainer optionContainer;

	Flag flags;

	private Main main;


	private int wait = 2;

	
	private static final ClassLoader loader = StartupContainer.class
			.getClassLoader();

	private final static Path dataDirectory = Path.getDataDirectory().resolve(
			FileSystem.type == FileSystem.OSType.UNIX ? ".SToNe" : "SToNe");

	/**
	 * Informs the module manager about downloaded archive.
	 * @param downloadedArchive {@link Path} of downloaded archive
	 * @param moduleName Name of containing {@link Module}
	 */
	public final static void registerDownloadedModule(
			final Path downloadedArchive, final String moduleName) {
		if (moduleName.startsWith("Main")) {
			registerDownloadedModule(downloadedArchive, "SToNe");
		} else {
			final Path target = dataDirectory.resolve(moduleName + ".jar");
			downloadedArchive.renameTo(target);
		}
	}

	private StartupContainer() {
		this.io = new IOHandler(Main.TOOLNAME);
		boolean jar_ = false;
		Path workingDirectory_ = null;
		try {
			final Class<?> loaderClass = StartupContainer.loader.getClass();
			jar_ = (boolean) loaderClass.getMethod("wdIsJarArchive").invoke(
					StartupContainer.loader);
			workingDirectory_ = Path.getPath(loaderClass
					.getMethod("getWorkingDir").invoke(StartupContainer.loader)
					.toString().split("/"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.workingDirectory = workingDirectory_;
		this.jar = jar_;
	}

	/**
	 * @return the parameters of running program
	 */
	public Object args() {
		return this.flags.getArgs();
	}

	/**
	 * @param io {@link IOHandler} to use
	 */
	public final void createFinalIO(@SuppressWarnings("hiding") final IOHandler io) {
		this.io = io;
	}

	/**
	 * Creates and returns the created taskPool
	 * 
	 * @return the created taskPool
	 */
	public final TaskPool createTaskPool() {
		this.taskPool = new TaskPool(this);
		return this.taskPool;
	}

	/**
	 * Calling this method will provide the parsed command line arguments to any
	 * module.
	 * 
	 * @param flags {@link Flag} to use
	 */
	public final void finishInit(@SuppressWarnings("hiding") final Flag flags) {
		this.flags = flags;
	}

	/**
	 * @return the IO-handler
	 */
	public final IOHandler getIO() {
		return this.io;
	}

	/**
	 * @return the instance of the main-module
	 */
	public final Main getMain() {
		return this.main;
	}

	/**
	 * @return the MasterThread
	 */
	public final MasterThread getMaster() {
		return this.master;
	}

	/**
	 * @return the OptionContainer
	 */
	public final OptionContainer getOptionContainer() {
		if (this.optionContainer == null) {
			this.optionContainer = new OptionContainer(this.flags, this.main);
		}
		return this.optionContainer;
	}

	/**
	 * @return the TaskPool
	 */
	public final TaskPool getTaskPool() {
		return this.taskPool;
	}

	/**
	 * @return the directory or jar-archive from which the tool has been loaded
	 *         from.
	 */
	public final Path getWorkingDir() {
		return this.workingDirectory;
	}

	/**
	 * Called to signal the MasterThread that it can continue from
	 * {@link #waitForInit()}.
	 */
	public final synchronized void parseDone() {
		--this.wait;
		notifyAll();
	}

	/**
	 * Sets the instance of the main-module.
	 * 
	 * @param main {@link Main} to use
	 */
	public final void setMain(@SuppressWarnings("hiding") final Main main) {
		this.main = main;
	}

	/**
	 * @param master {@link MasterThread} to use
	 */
	public final void setMaster(@SuppressWarnings("hiding") final MasterThread master) {
		this.master = master;
	}

	/**
	 * Synchronizes the startup. Pauses the MasterThread to wait for the working
	 * threads to parse the config-file.
	 */
	public final synchronized void waitForInit() {
		if (--this.wait <= 0) {
			return;
		}
		while (this.wait != 0) {
			try {
				wait();
			} catch (final InterruptedException e) {
				this.master.interrupt();
			}
		}
	}

	/**
	 * Checks if the workingDirectory is a jar-archive.
	 * 
	 * @return <i>True</i> if the tool has been loaded from a jar-archive.
	 */
	public final boolean wdIsJarArchive() {
		return this.jar;
	}

	final Object flags() {
		final Map<String, String> map = this.flags.getValues();
		final String[] params = new String[map.size()];
		final Iterator<Map.Entry<String, String>> iter = map.entrySet()
				.iterator();
		int i = -1;
		while (iter.hasNext()) {
			final Map.Entry<String, String> e = iter.next();
			if (e.getValue() == null) {
				params[++i] = "--" + e.getKey();
			} else {
				params[++i] = "--" + e.getKey() + " " + e.getValue();
			}
		}
		return params;
	}

	
}
