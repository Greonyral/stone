package stone;

import java.lang.reflect.InvocationTargetException;

import stone.modules.Module;

public final class ModuleInfo {

	final Module instance;
	final String name;
	final String tooltip;

	public ModuleInfo(final Config c, final StartupContainer sc, final Class<Module> clazz,
			final String name) {
		Module instance = null;
		try {
			instance = clazz.getConstructor(sc.getClass()).newInstance(sc);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			instance = null;
		}
		this.instance = instance;
		this.name = name;
		tooltip = c.getValue(name);
	}

	ModuleInfo(final Config c) {
		name = c.getValue("mainClass");
		tooltip = null;
		instance = new stone.modules.Main();
	}

	final int getVersion() {
		return instance == null ? -1 : instance.getVersion();
	}

	public final String name() {
		return name;
	}
	
	public final String tooltip() {
		return tooltip;
	}
}