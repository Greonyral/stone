package stone;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
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
	 * Asks the ModuleLoader to load given module
	 * 
	 * @param module
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

	private final Map<String, Container> containerMap = new HashMap<>();


	private int wait = 2;

	private static final ClassLoader loader = StartupContainer.class
			.getClassLoader();
	private final static Path dataDirectory = Path.getDataDirectory().resolve(FileSystem.type == FileSystem.OSType.UNIX ? ".SToNe" : "SToNe");

	private StartupContainer() {
		try {
			this.io = new IOHandler(Main.TOOLNAME);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
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
	 * @param io
	 */
	public final void createFinalIO(final IOHandler io) {
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
	 * @param flags
	 */
	public final void finishInit(final Flag flags) {
		this.flags = flags;
	}

	/**
	 * 
	 * @param s
	 *            idenifier of desired Container. It has to be the class-name.
	 * @return a Container or <i>null</i> if the Container failed to load.
	 */
	public final Container getContainer(final String s) {
		final Container container = this.containerMap.get(s);
		if (container == null) {
			try {
				@SuppressWarnings("unchecked")
				final Class<Container> containerClass = (Class<Container>) StartupContainer.loader
						.loadClass(s);
				Container containerNew;
				containerNew = (Container) containerClass.getMethod("create",
						getClass()).invoke(null, this);
				this.containerMap.put(s, containerNew);
				return containerNew;
			} catch (final InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (final IllegalAccessException | IllegalArgumentException
					| NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}

		}
		return container;
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
	 * @param main
	 */
	public final void setMain(final Main main) {
		this.main = main;
	}

	/**
	 * @param master
	 */
	public final void setMaster(final MasterThread master) {
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

	public final static Path getDatadirectory() {
		return dataDirectory;
	}

	public final static void registerDownloadedModule(
			final Path downloadedArchive, final String moduleName) {
		if (moduleName.startsWith("Main"))
			registerDownloadedModule(downloadedArchive, "SToNe");
		else {
			final Path target = dataDirectory.resolve(moduleName + ".jar");
			downloadedArchive.renameTo(target);
		}
	}

	public Object args() {
		return flags.getArgs();
	}
}
