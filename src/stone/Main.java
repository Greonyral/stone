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
	 * @param args parameters for the program to be parsed
	 * @throws ReflectiveOperationException if any operation based on reflection fails
	 */
	public final static void main(final String[] args)
			throws ReflectiveOperationException {
		final ModuleLoader loader = ModuleLoader.createLoader();
		final Class<?> scClass = loader.loadClass("stone.StartupContainer");
		final Class<?> mainClass = loader.loadClass("stone.modules.Main");
		final Class<?> flagClass = loader.loadClass("stone.util.Flag");
		final Object sc = scClass.getMethod("createInstance").invoke(null);
		final Object main = mainClass.newInstance();
		final Object flags = flagClass.getMethod("getInstance").invoke(null);
		// final String flagId, final String tooltip,
		// char shortFlag, final String longFlag, boolean argExpected
		final Method registerOption = flagClass.getMethod("registerOption",
				String.class, String.class, char.class, String.class,
				boolean.class);
		registerOption.invoke(flags, Main.HELP_ID, "Prints this help and exit",
				'h', "help", false);
		registerOption.invoke(flags, Main.DEBUG_ID,
				"Enables more output for debugging", 'd', "debug", false);

		// pass args to the flags
		flagClass.getMethod("parse", String[].class).invoke(flags,
				(Object) args);

		scClass.getMethod("setMain", mainClass).invoke(sc, main);
		mainClass.getMethod("run", scClass, flagClass).invoke(main, sc, flags);
		// will return after terminating
	}
}
