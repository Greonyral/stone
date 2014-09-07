package stone;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author Nelphindal
 */
public class Main {

	public static final String HELP_ID = "help";
	public static final String REPAIR_ID = "reset";
	public static final String DEBUG_ID = "debug";

	/**
	 * @param args
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public final static void main(final String[] args)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, InstantiationException,
			ClassNotFoundException {
		final ModuleLoader loader = ModuleLoader.createLoader();
		final Class<?> scClass =
				loader.loadClass("stone.StartupContainer");
		final Class<?> mainClass = loader.loadClass("stone.modules.Main");
		final Class<?> flagClass = loader.loadClass("stone.util.Flag");
		final Object sc = scClass.getMethod("createInstance").invoke(null);
		final Object main = mainClass.newInstance();
		final Object flags = flagClass.getMethod("getInstance").invoke(null);
		// final String flagId, final String tooltip,
		// char shortFlag, final String longFlag, boolean argExpected
		final Method registerOption =
				flagClass.getMethod("registerOption", String.class,
						String.class, char.class, String.class,
						boolean.class);
		registerOption.invoke(flags, Main.HELP_ID, "prints this help", 'h',
				"help", false);
		registerOption.invoke(flags, Main.DEBUG_ID,
				"enables more output for debugging", 'd', "debug", false);
		flagClass.getMethod("parse", String[].class).invoke(flags,
				(Object) args);

		scClass.getMethod("setMain", mainClass).invoke(sc, main);
		mainClass.getMethod("run", scClass, flagClass).invoke(main, sc,
				flags);
	}
}
