package stone;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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

	private static ModuleLoader instance;

	static final ModuleLoader createLoader() {
		ModuleLoader.instance = new ModuleLoader();
		return ModuleLoader.instance;
	}

	private final boolean jar;

	private final Path workingDirectory;
	private final Map<String, Class<?>> map = new HashMap<>();

	private byte[] buffer = new byte[0xc000];

	private final Path[] cp;

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

	/** */
	@Override
	public final URL getResource(final String s) {
		URL url;
		try {
			url = new URL((this.jar ? "jar:" : "") + "file:/"
					+ this.workingDirectory.toString()
					+ (this.jar ? "!/" + s : ""));
			return url;
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** */
	@SuppressWarnings("resource")
	@Override
	public final InputStream getResourceAsStream(final String s) {
		final String[] names = s.split("/");
		for (final Path p : this.cp) {
			if (p.toFile().exists()) {
				if (p.toFile().isFile()) {
					try {
						final ZipFile zip = new ZipFile(p.toFile());
						final ZipEntry sEntry = zip.getEntry(s);
						if (sEntry == null) {
							zip.close();
							return null;
						}
						return zip.getInputStream(sEntry);
					} catch (final Exception e) {
					}
				} else {
					final Path file = p.resolve(names);
					if (file.exists() && file.toFile().isFile()) {
						try {
							return new FileInputStream(file.toFile());
						} catch (final FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
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

	/** */
	@Override
	protected Class<?> findClass(final String name) {
		java.io.InputStream in = null;
		JarFile jarFile = null;
		Path path;
		int i = 0;
		int size = 0;
		assert this.cp.length >= 1;
		final String[] names = name.split("\\.");
		names[names.length - 1] += ".class";

		for (; i <= this.cp.length; i++) {
			if (i == this.cp.length) {
				return null;
			}
			path = this.cp[i];
			if (!path.exists()) {
				continue;
			}
			if (path.getFilename().endsWith(".jar")) {
				try {
					jarFile = new JarFile(path.toFile());
					final java.util.zip.ZipEntry e = jarFile.getEntry(name
							.replaceAll("\\.", "/") + ".class");
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

			path = path.resolve(names);
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
		if (in == null) {
			throw new RuntimeException(
					"findClass got an null reference for InputStream");
		}
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
		if (i != 0) {
			path = this.cp[i];
			this.cp[i] = this.cp[0];
			this.cp[0] = path;
		}
		final Class<?> c = defineClass(name, this.buffer, 0, size);
		this.map.put(name, c);
		return c;

	}
}
