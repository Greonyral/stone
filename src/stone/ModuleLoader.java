package stone;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import stone.modules.Main;
import stone.util.FileSystem;
import stone.util.Path;


/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class ModuleLoader extends ClassLoader {

	private static ModuleLoader instance;

	static final ModuleLoader createLoader() {
		ModuleLoader.instance = new ModuleLoader();
		return ModuleLoader.instance;
	}

	private final boolean jar;

	private final Path workingDirectory;

	private byte[] buffer = new byte[0xc000];

	private final Map<String, Class<?>> loadedClasses = new HashMap<>();

	private final Path[] cp;

	private static final ClassLoader sysCll = getSystemClassLoader();

	private ModuleLoader() {
		super(null);
		final String className =
				this.getClass().getCanonicalName().replace('.', '/')
				+ ".class";
		final URL url = Main.class.getClassLoader().getResource(className);
		if (url.getProtocol().equals("file")) {
			final Path classPath = Path.getPath(url);
			jar = false;
			workingDirectory = classPath.getParent().getParent();
		} else if (url.getProtocol().equals("jar")) {
			jar = true;
			workingDirectory = Path.getPath(url);
		} else {
			jar = false;
			workingDirectory = null;
		}
		final String[] cp =
		System.getProperty("java.class.path").split(
				FileSystem.type == FileSystem.OSType.WINDOWS ? ";"
						: ":");
		this.cp = new Path[Math.max(1, cp.length)];
		if (cp.length == 0) {
			this.cp[0] = workingDirectory;
		} else {
			final Path path =
					Path.getPath(System.getProperty("user.dir").split(
							"\\" + FileSystem.getFileSeparator()));
			for (int i = 0; i < cp.length; i++) {
				final String[] cpPath =
						cp[i].split("\\" + FileSystem.getFileSeparator());
				this.cp[i] = path.resolve(cpPath);
			}
		}
	}

	/** */
	@Override
	public final URL getResource(final String s) {
		URL url;
		try {
			url =
					new URL((jar ? "jar:" : "") + "file:/"
							+ workingDirectory.toString()
							+ (jar ? "!/" + s : ""));
			return url;
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the dir, where the class files are or the jar-archive containing
	 *         them
	 */
	public final Path getWorkingDir() {
		return workingDirectory;
	}

	/** */
	@Override
	public final Class<?> loadClass(final String name)
			throws ClassNotFoundException {
		final Class<?> c;
		if (name.startsWith("java.") || name.startsWith("javax.")
				|| name.startsWith("sun.")) {
			c = ModuleLoader.sysCll.loadClass(name);
		} else {
			c = findClass(name);
		}
		return c;
	}

	/**
	 * @return true if {@link #getWorkingDir()} returns the path of an
	 *         jar-archive, false otherwise
	 */
	public final boolean wdIsJarArchive() {
		return jar;
	}

	/** */
	@SuppressWarnings("resource")
	@Override
	protected Class<?> findClass(final String name) {
		Class<?> c = loadedClasses.get(name);
		if (c == null) {
			java.io.InputStream in = null;
			JarFile jarFile = null;
			Path path;
			int i = 0;
			int size = 0;
			assert cp.length >= 1;
			for (; i <= cp.length; i++) {
				if (i == cp.length)
					return null;
				path = cp[i];
				if (!path.exists()) {
					continue;
				}
				if (path.getFileName().endsWith(".jar")) {
					try {
						jarFile = new JarFile(path.toFile());
						final java.util.zip.ZipEntry e =
								jarFile.getEntry(name.replaceAll("\\.",
										"/")
										+ ".class");
						if (e == null) {
							jarFile.close();
							jarFile = null;
							continue;
						}
						size = (int) e.getSize();
						in = jarFile.getInputStream(e);
						break;
					} catch (final IOException e) {
						e.printStackTrace();
						return null;
					}
				}
				for (final String p : name.split("\\.")) {
					path = path.resolve(p);
				}
				path =
						path.getParent().resolve(
								path.getFileName() + ".class");
				if (!path.exists()) {
					continue;
				}
				size = (int) path.toFile().length();

				try {
					in = new java.io.FileInputStream(path.toFile());
					break;
				} catch (final FileNotFoundException e) {
					e.printStackTrace();
					return null;
				}
			}
			if (in == null)
				throw new RuntimeException(
						"findClass got an null reference for InputStream");
			assert in != null;
			if (buffer.length < size) {
				buffer = new byte[(size & 0xffff_ff00) + 0x100];
			}
			size = 0;
			try {
				while (true) {
					final int read =
							in.read(buffer, size, buffer.length - size);
					if (read < 0) {
						break;
					}
					size += read;
				}
			} catch (final IOException e) {
				e.printStackTrace();
				return null;
			}
			try {
				in.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			c = defineClass(name, buffer, 0, size);
			loadedClasses.put(name, c);
			if (i != 0) {
				path = cp[i];
				cp[i] = cp[0];
				cp[0] = path;
			}

		}
		return c;
	}
}
