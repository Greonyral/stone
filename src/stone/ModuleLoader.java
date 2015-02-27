package stone;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import stone.modules.Main;
import stone.util.FileSystem;
import stone.util.Path;

/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class ModuleLoader extends ClassLoader {

	class Lookup {
		final java.io.InputStream in;
		final JarFile jarFile;
		final Path path;
		final int size;
		final ZipEntry entry;

		Lookup(int size, final java.io.InputStream in, final Path path) {
			this.in = in;
			this.size = size;

			this.path = path;

			this.jarFile = null;
			entry = null;
		}

		Lookup(int size, final java.io.InputStream in, final Path path,
				final JarFile jarFile, final ZipEntry entry) {
			this.in = in;
			this.size = size;

			this.path = path;

			this.jarFile = jarFile;
			this.entry = entry;
		}
	}

	static final ModuleLoader createLoader() {
		ModuleLoader.instance = new ModuleLoader();
		return ModuleLoader.instance;
	}

	private static ModuleLoader instance;

	private final boolean jar;
	private final Path workingDirectory;

	private final Map<String, Class<?>> map = new HashMap<>();

	private final Path[] cp;

	private byte[] buffer = new byte[0xc000];

	private ModuleLoader() {
		super(null);
		final String className = this.getClass().getCanonicalName()
				.replace('.', '/')
				+ ".class";
		final URL url = Main.class.getClassLoader().getResource(className);

		if (url.getProtocol().equals("file")) {
			final Path classPath = Path.getPath(url);
			this.jar = false;
			this.workingDirectory = classPath.getParent().getParent();
		} else if (url.getProtocol().equals("jar")) {
			this.jar = true;
			this.workingDirectory = Path.getPath(url);
		} else {
			this.jar = false;
			this.workingDirectory = null;
		}
		final String[] cp = System.getProperty("java.class.path").split(
				FileSystem.type == FileSystem.OSType.WINDOWS ? ";" : ":");
		this.cp = new Path[Math.max(1, cp.length)];
		if (cp.length == 0) {
			this.cp[0] = this.workingDirectory;
		} else {
			final Path path = Path.getPath(System.getProperty("user.dir")
					.split("\\" + FileSystem.getFileSeparator()));
			for (int i = 0; i < cp.length; i++) {
				final String[] cpPath = cp[i].split("\\"
						+ FileSystem.getFileSeparator());
				this.cp[i] = path.resolve(cpPath);
			}
		}
	}

	private final Lookup find(final String[] names,
			final String suffix) {
		final String[] namesSuffix = new String[names.length];
		ZipEntry entry = null;
		java.io.InputStream in = null;
		Path path = null;
		JarFile jarFile = null;
		int size = -1, i = 0;
		String entryName = null;

		if (suffix != null) {
			System.arraycopy(names, 0, namesSuffix, 0, names.length);
			namesSuffix[names.length - 1] += suffix;
		}
		for (final String s : names) {
			if (entryName == null)
				entryName = s;
			else {
				entryName += "/";
				entryName += s;
			}

		}
		if (suffix != null)
			entryName += suffix;

		for (; i <= this.cp.length; i++) {
			if (i == this.cp.length) {
				return null;
			}
			in = null;
			jarFile = null;
			path = this.cp[i];
			if (!path.exists()) {
				continue;
			}
			if (path.getFilename().endsWith(".jar")) {
				try {
					jarFile = new JarFile(path.toFile());
					entry = jarFile.getEntry(entryName);
					if (entry == null) {
						jarFile.close();
						jarFile = null;
						continue;
					}
					size = (int) entry.getSize();
					in = jarFile.getInputStream(entry);

					break;
				} catch (final Exception e) {
					e.printStackTrace();
					return null;
				}
			} else {
				if (suffix == null)
					path = path.resolve(names);
				else {
					path = path.resolve(namesSuffix);
				}
				if (!path.exists()) {
					continue;
				}
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
		return jarFile == null ? new Lookup(size, in, path) : new Lookup(size,
				in, path, jarFile, entry);
	}

	/**
	 * @throws ClassNotFoundException
	 */
	@Override
	protected Class<?> findClass(final String name)
			throws ClassNotFoundException {
		final java.io.InputStream in;
		final int size;
		final JarFile jarFile;

		assert this.cp.length >= 1;

		final Lookup found = find(name.split("\\."), ".class");
		if (found == null) {
			throw new ClassNotFoundException();
		}

		in = found.in;
		size = found.size;
		jarFile = found.jarFile;

		assert in != null;
		if (this.buffer.length < size) {
			this.buffer = new byte[(size & 0xffff_ff00) + 0x100];
		}
		try {
			int offset = in.read(this.buffer);
			while (offset < size) {
				offset += in.read(this.buffer, offset, size - offset);
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
		final Class<?> c = defineClass(name, this.buffer, 0, size);
		this.map.put(name, c);
		return c;

	}

	/** */
	@Override
	public final URL getResource(final String s) {
		final Lookup l = find(s.split("/"), null);
		try {
			if (l.jarFile != null) {

				return URI.create(
						"jar:" + l.path.toFile().toURI() + "!/"
								+ l.entry.getName()).toURL();

			} else {
				return l.path.toFile().toURI().toURL();
			}
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** */
	@Override
	public final InputStream getResourceAsStream(final String s) {
		final Lookup l = find(s.split("/"), null);
		return l == null ? null : l.in;
	}

	/**
	 * @return the dir, where the class files are or the jar-archive containing
	 *         them
	 */
	public final Path getWorkingDir() {
		return this.workingDirectory;
	}

	/**
	 * @return true if {@link #getWorkingDir()} returns the path of an
	 *         jar-archive, false otherwise
	 */
	public final boolean wdIsJarArchive() {
		return this.jar;
	}
}
