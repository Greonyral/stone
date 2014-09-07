package stone;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import stone.io.IOHandler;
import stone.modules.Main;
import stone.modules.Module;
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

	public final static Class<Module> loadModule(final String module) {
		try {
			@SuppressWarnings("unchecked") final Class<Module> clazz =
					(Class<Module>) StartupContainer.loader.loadClass("stone.modules." + module);
			return clazz;
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
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

	private StartupContainer() {
		try {
			io = new IOHandler(Main.TOOLNAME);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		boolean jar_ = false;
		Path workingDirectory_ = null;
		try {
			final Class<?> loaderClass = StartupContainer.loader.getClass();
			jar_ =
					(boolean) loaderClass.getMethod("wdIsJarArchive").invoke(
							StartupContainer.loader);
			workingDirectory_ =
					Path.getPath(loaderClass.getMethod("getWorkingDir")
							.invoke(StartupContainer.loader).toString().split("/"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		workingDirectory = workingDirectory_;
		jar = jar_;
	}

	public final void createFinalIO(@SuppressWarnings("hiding") final IOHandler io) {
		this.io = io;
	}

	/**
	 * Creates and returns the created taskPool
	 * 
	 * @return the created taskPool
	 */
	public final TaskPool createTaskPool() {
		taskPool = new TaskPool(this);
		return taskPool;
	}

	/**
	 * Calling this method will provide the parsed command line arguments to any module.
	 * 
	 * @param flags
	 */
	public final void finishInit(@SuppressWarnings("hiding") final Flag flags) {
		this.flags = flags;
	}

	public final Container getContainer(final String s) {
		final Container container = containerMap.get(s);
		if (container == null) {
			try {
				@SuppressWarnings("unchecked") final Class<Container> containerClass =
						(Class<Container>) StartupContainer.loader.loadClass(s);
				Container containerNew;
				containerNew =
						(Container) containerClass.getMethod("create",
								getClass()).invoke(null, this);
				containerMap.put(s, containerNew);
				return containerNew;
			} catch (final InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (final IllegalAccessException | IllegalArgumentException
					| NoSuchMethodException
					| SecurityException | ClassNotFoundException e) {
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
		return io;
	}

	/**
	 * @return the instance of the main-module
	 */
	public final Main getMain() {
		return main;
	}

	/**
	 * @return the MasterThread
	 */
	public final MasterThread getMaster() {
		return master;
	}

	/**
	 * @return the OptionContainer
	 */
	public final OptionContainer getOptionContainer() {
		if (optionContainer == null) {
			optionContainer = new OptionContainer(flags, main);
		}
		return optionContainer;
	}

	/**
	 * @return the TaskPool
	 */
	public final TaskPool getTaskPool() {
		return taskPool;
	}

	public final Path getWorkingDir() {
		return workingDirectory;
	}

	public final synchronized void parseDone() {
		--wait;
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

	public final void setMaster(final MasterThread master) {
		this.master = master;
	}

	public final synchronized void waitForInit() {
		if (--wait <= 0)
			return;
		while (wait != 0) {
			try {
				wait();
			} catch (final InterruptedException e) {
				master.interrupt();
			}
		}
	}

	public final boolean wdIsJarArchive() {
		return jar;
	}

	final Object flags() {
		final Map<String, String> map = flags.getValues();
		final String[] params = new String[map.size()];
		final Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
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
