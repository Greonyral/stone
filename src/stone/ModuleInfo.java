package stone;

import java.lang.reflect.InvocationTargetException;

import stone.modules.Module;

/**
 * Container for {@link Module} holding version, tool tip and name.
 * 
 * @author Nelphindal
 * 
 */
public final class ModuleInfo {

	final Module instance;
	final String name;
	final String tooltip;

	private static Config c;
	private static StartupContainer sc;

	/**
	 * @param c
	 *            initially parsed config {@link Config} for retrieving the tool
	 *            tip used for constructor
	 * @param sc
	 *            instance of {@link StartupContainer} to used for constructor
	 */
	public static void init(@SuppressWarnings("hiding") final Config c,
			@SuppressWarnings("hiding") final StartupContainer sc) {
		ModuleInfo.c = c;
		ModuleInfo.sc = sc;
	}

	/**
	 * 
	 * @param clazz
	 *            the real name of {@link Module}
	 * @param name
	 *            identifying name of {@link Module} used for GUIs
	 */
	public ModuleInfo(final Class<Module> clazz,
			@SuppressWarnings("hiding") final String name) {
		@SuppressWarnings("hiding")
		Module instance = null;
		if (clazz != null) {
			try {
				instance = clazz.getConstructor(sc.getClass()).newInstance(sc);
			} catch (final InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				instance = null;
			}
		}
		this.instance = instance;
		this.name = name;
		this.tooltip = c.getValue(name);
	}

	ModuleInfo() {
		this.name = c.getValue("mainClass");
		this.tooltip = null;
		this.instance = new stone.modules.Main();
	}

	/**
	 * @return <i>name</i> of {@link ModuleInfo#ModuleInfo(Class, String)}
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * @return <i>tooltip</i> generated in
	 *         {@link ModuleInfo#ModuleInfo(Class, String)}
	 */
	public final String tooltip() {
		return this.tooltip;
	}

	final int getVersion() {
		return this.instance == null ? -1 : this.instance.getVersion();
	}
}