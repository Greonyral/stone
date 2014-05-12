package updater;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;

import modules.Module;
import util.Path;


/**
 * @author Nelphindal
 */
public class CreateBuilds {

	/**
	 * Searches the build path for modules and extract the version numbers
	 * 
	 * @param args
	 *            ignored
	 */
	public final static void main(final String[] args) {
		final URL url =
				CreateBuilds.class.getClassLoader().getResource(
						CreateBuilds.class.getCanonicalName().toString()
								.replace('.', '/')
								+ ".class");
		final Path root =
				Path.getPath(url.toString().substring(5).split("/"))
						.getParent().getParent();
		final Path p = root.resolve("modules");
		final Path info = root.getParent().resolve("moduleInfo");
		info.toFile().mkdirs();
		for (final String s : p.toFile().list()) {
			new ClassLoader() {

				@SuppressWarnings("unchecked")
				public Class<Module> loadClass0(final String s) {
					if (s.contains("$")) {
						return null;
					}
					if (!s.startsWith("modules.") || s.endsWith("Module")) {
						return null;
					}
					try {
						return (Class<Module>) loadClass(s);
					} catch (final Exception e) {
						return null;
					}
				}

				void run() {
					if (s.endsWith(".class")) {
						try {
							final Class<Module> clazz =
									loadClass0("modules."
											+ s.substring(0, s.length() - 6));
							if (clazz == null) {
								return;
							}
							final Method m = clazz.getMethod("getVersion");
							final int version =
									((Integer) m.invoke(clazz.newInstance()))
											.intValue();
							final java.io.OutputStream out =
									new java.io.FileOutputStream(info.resolve(
											s.substring(0, s.length() - 6))
											.toFile());
							out.write(ByteBuffer.allocate(4).putInt(version)
									.array());
							out.flush();
							out.close();
							System.out.println(s + " version:" + version);
						} catch (final Exception e) {
							System.err.println(s);
							e.printStackTrace();
						}
					}

				}
			}.run();
		}
	}
}
