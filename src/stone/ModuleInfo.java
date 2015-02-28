package stone;

import java.lang.reflect.InvocationTargetException;

import stone.modules.Module;

public final class ModuleInfo {

	final Module instance;
	final String name;
	final String tooltip;

	public ModuleInfo(final Config c, final StartupContainer sc,
			final Class<Module> clazz, final String name) {
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

	ModuleInfo(final Config c) {
		this.name = c.getValue("mainClass");
		this.tooltip = null;
		this.instance = new stone.modules.Main();
	}

	public final String name() {
		return this.name;
	}

	public final String tooltip() {
		return this.tooltip;
	}

	final int getVersion() {
		return this.instance == null ? -1 : this.instance.getVersion();
	}
}