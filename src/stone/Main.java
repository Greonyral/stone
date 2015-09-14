package stone;

import java.lang.reflect.Method;

/**
 * @author Nelphindal
 */
public class Main {

	/**
	 * help
	 */
	public static final String HELP_ID = "help";
	/**
	 * reset
	 */
	public static final String REPAIR_ID = "reset";
	/**
	 * debug
	 */
	public static final String DEBUG_ID = "debug";
	/**
	 * set if new binaries have been downloaded and this is a restart
	 */
	public static final String UPDATE_ID = "update";

	/**
	 * no-gui
	 */
	public static final String NO_GUI_ID = "no-gui";

	/**
	 * @param args
	 *            parameters for the program to be parsed
	 * @throws ReflectiveOperationException
	 *             if any operation based on reflection fails
	 */
	public final static void main(final String[] args)
			throws ReflectiveOperationException {
		final ModuleLoader loader = ModuleLoader.createLoader();
		final Class<?> scClass = loader.loadClass("stone.StartupContainer");
		final Class<?> mainClass = loader.loadClass("stone.modules.Main");
		final Class<?> flagClass = loader.loadClass("stone.util.Flag");
		final Object flags = flagClass.getMethod("getInstance").invoke(null);
		final String[] params = new String[1];
		final Method registerOption = flagClass.getMethod("registerOption",
				String.class, String.class, char.class, String.class,
				boolean.class);
		registerOption.invoke(flags, Main.HELP_ID, "Prints this help and exit",
				'h', "help", false);
		registerOption.invoke(flags, Main.NO_GUI_ID,
				"Runs the program without a GUI", 'g', "no-gui", false);
		registerOption.invoke(flags, Main.DEBUG_ID,
				"Enables more output for debugging", 'd', "debug", false);
		flagClass.getMethod("parse", String[].class).invoke(flags,
				(Object) args);
		final boolean no_gui = ((Boolean) flagClass.getMethod("isEnabled",
				String.class).invoke(flags, NO_GUI_ID)).booleanValue();
		if (no_gui) {
			params[0] = NO_GUI_ID;
		}
		final Object sc = scClass.getMethod("createInstance", String[].class)
				.invoke(null, (Object) params);
		final Object main = mainClass.newInstance();
		// final String flagId, final String tooltip,
		// char shortFlag, final String longFlag, boolean argExpected
		registerOption.invoke(flags, Main.UPDATE_ID,
				"Flag indicating launch of updated program", '\b', null, true);
		// pass args to the flags
		scClass.getMethod("setMain", mainClass).invoke(sc, main);
		mainClass.getMethod("run", scClass, flagClass).invoke(main, sc, flags);
		// will return after terminating
	}
}
