package stone.updater;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;

import stone.modules.Module;
import stone.util.Path;


/**
 * @author Nelphindal
 */
public class CreateBuilds {

	/**
	 * Searches the build path for modules and extract the version numbers
	 * 
	 * @param args
	 *            ignored
	 * @throws IOException
	 */
	public final static void main(final String[] args) throws IOException {
		final URL url = CreateBuilds.class.getClassLoader().getResource(
				CreateBuilds.class.getCanonicalName().toString()
						.replace('.', '/')
						+ ".class");
		final Path root = Path.getPath(url).getParent().getParent();
		final Path p = root.resolve("modules");
		final Path info = root.getParent().getParent().resolve("moduleInfo");

		info.toFile().mkdirs();

		for (final String s : p.toFile().list()) {
			new ClassLoader() {

				@SuppressWarnings("unchecked")
				public Class<Module> loadClass0(final String moduleName) {
					if (moduleName.contains("$")) {
						return null;
					}
					if (!moduleName.startsWith("stone.modules.")
							|| moduleName.endsWith("Module")
							|| moduleName.endsWith("EnableModuleListener")
							|| moduleName.endsWith("ConfigWriter")) {
						return null;
					}
					try {
						return (Class<Module>) loadClass(moduleName);
					} catch (final Exception e) {
						return null;
					}
				}

				void run() {
					if (s.endsWith(".class")) {
						try {
							final Class<Module> clazz = loadClass0("stone.modules."
									+ s.substring(0, s.length() - 6));
							if (clazz == null) {
								return;
							}
							final Method m = clazz.getMethod("getVersion");
							final int version = ((Integer) m.invoke(clazz
									.newInstance())).intValue();
							final java.io.OutputStream out = new java.io.FileOutputStream(
									info.resolve(s.substring(0, s.length() - 6))
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
		final InputStream in = new FileInputStream(info.resolve("Main")
				.toFile());
		final OutputStream out = new FileOutputStream(info.resolve("Main_band")
				.toFile());
		for (int i = 0; i < 4; i++) {
			out.write(in.read());
		}
		out.flush();
		out.close();
		in.close();
	}
}
