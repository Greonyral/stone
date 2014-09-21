package stone.util;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Represents paths relative to a certain base or absolute, specified by
 * FileSystem
 */
public final class Path implements Comparable<Path> {

	private static int nextHash;

	private static final Map<String, Path> rootMap = buildRootMap();
	private static final ArrayDeque<Integer> reusableHashes =
			new ArrayDeque<>();

	/**
	 * Parses the given string <i>name</i> and returns a path representing the
	 * corresponding path.
	 * 
	 * @param name
	 * @return the parsed path
	 */
	public final static Path getPath(final String... name) {
		Path p;
		if (name.length == 0) {
			throw new IllegalArgumentException();
		}
		int idx = 0;
		if (name[idx].isEmpty()) {
			name[idx] = FileSystem.getFileSeparator();
		}
		final String base = name[idx];
		if (Path.rootMap == null) {
			p = new Path(base);
		} else {
			p = Path.rootMap.get(base);
		}
		if (p == null) {
			throw new RuntimeException("Invalid path: invalid root: "
					+ base);
		}
		if (name[idx].length() <= p.str.length()) {
			++idx;
		} else {
			name[idx] = name[idx].substring(p.str.length());
		}
		return p.resolve(name, idx);
	}

	/**
	 * Parses the given url <i>url</i> and returns a path representing the
	 * corresponding path.
	 * 
	 * @param url
	 * @return the parsed path
	 */
	public final static Path getPath(final URL url) {
		final StringBuilder sb = new StringBuilder(url.getFile());
		Path path = null;
		int pos = 0;
		if (url.getPath().startsWith("file:/")) {
			sb.setHead(5);
		}
		if (FileSystem.type == FileSystem.OSType.WINDOWS) {
			sb.setHead(1);
		}
		while (pos < sb.length()) {
			switch (sb.charAt(pos)) {
				case '!':
					if (path == null) {
						return Path.rootMap.get(sb.toString().substring(0,
								pos));
					}
					return path.resolve(sb.toString().substring(0, pos));
				case '%':
					final byte b0 = sb.getByte(pos + 1);
					assert b0 >= 0xc0;
					final int byteCount =
							b0 < 0x80 ? 1 : (b0 < 0xe ? 3 : 4);
					final ByteBuffer buffer =
							ByteBuffer.allocate(byteCount);
					buffer.put(b0);
					for (int i = pos + 2; buffer.remaining() > 0; ++i) {
						buffer.put(sb.getByte(i));
					}
					buffer.rewind();
					final String replacement =
							FileSystem.UTF8.decode(buffer).toString();
					sb.replace(pos, 1 + (2 * byteCount), replacement);
					continue;
				case '/':
					final String s = sb.toString().substring(0, pos);
					if (path == null) {
						if (s.isEmpty()) {
							path = Path.rootMap.get("/");
						} else {
							path = Path.rootMap.get(s);
						}
					} else {
						path = path.getPathFunc(s);
					}
					sb.setHead(pos + 1);
					pos = 0;
					continue;
				default:
					++pos;
			}

		}
		if (path == null) {
			return Path.rootMap.get(sb.toString());
		}
		return path.resolve(sb.toString());
	}

	/**
	 * Creates an unused path, and marks the associated file for deletion on
	 * exit
	 * 
	 * @param prefix
	 *            prefix of created temporary directory
	 * @return a path usable for a temporary directory
	 */
	public final static Path getTmpDir(final String prefix) {
		while (true) {
			final int rand = (int) (Math.random() * Integer.MAX_VALUE);
			final Path tmp = Path.tmpRoot.resolve(prefix + "_temp" + rand);
			if (!tmp.exists()) {
				return tmp;
			}

		}
	}

	private final static Map<String, Path> buildRootMap() {
		final String[] bases = FileSystem.getBases();
		final Map<String, Path> map = new HashMap<>();
		final String baseName;
		final Path home = FileSystem.getBase();
		Path base = home;
		if (base != null) {
			while (base.parent != null) {
				base = base.parent;
			}
			if (FileSystem.type == FileSystem.OSType.WINDOWS) {
				baseName = base.str.substring(0, 2);
			} else {
				baseName = base.str;
			}
		} else {
			baseName = null;
		}
		if (baseName != null) {
			for (final String p : bases) {
				final Path root;
				if (baseName.equals(p)) {
					root = base;
					map.put("~", home);
				} else {
					root = new Path(p);
				}
				if (!root.exists()) {
					continue;
				}
				map.put(root.str, root);
				map.put(root.pathStr, root);
				map.put(p, root);
			}
		} else {
			for (final String p : bases) {
				final Path root = new Path(p);
				if (!root.exists()) {
					continue;
				}
				map.put(root.str, root);
				map.put(root.pathStr, root);
				map.put(p, root);
			}
		}
		return map;

	}

	private final static boolean delete(final File file) {
		if (!file.exists()) {
			return false;
		}
		if (file.isDirectory()) {
			for (final File f : file.listFiles()) {
				Path.delete(f);
			}
		}
		return file.delete();
	}

	private final static boolean renameFilePath(final Path path,
			final Path target) {
		if (path.toFile().renameTo(target.toFile())) {
			return true;
		}
		try {
			Files.move(path.toAbsolutePath(), target.toAbsolutePath());
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private final String[] dirs;

	private final int hash;

	private final Map<String, WeakReference<Path>> successors =
			new TreeMap<>();
	private File file = null;
	private final String filename;

	private final Path parent;

	private final String str;

	private final String pathStr;

	private final static Object finalizerMonitor = new Object();

	private final static StringBuilder relativizer = new StringBuilder();

	private final static Path tmpRoot = getPath(System.getProperty(
			"java.io.tmpdir").split("\\" + FileSystem.getFileSeparator()));

	/**
	 * creates new path with parent != root
	 */
	private Path(final Path parent, final String name) {
		filename = name;
		this.parent = parent;
		if (parent.parent == null) {
			str = parent.str + name;
			pathStr = parent.pathStr + name;
		} else {
			str = parent.str + "/" + name;
			pathStr =
					parent.pathStr + FileSystem.getFileSeparator() + name;
		}
		parent.successors.put(name, new WeakReference<>(this));
		if (parent.dirs == null) {
			dirs = new String[0];
		} else {
			dirs = new String[parent.dirs.length + 1];
			dirs[parent.dirs.length] = parent.filename;
			System.arraycopy(parent.dirs, 0, dirs, 0, parent.dirs.length);
		}
		if (Path.rootMap == null) {
			hash = ++Path.nextHash;
		} else
		// synchronized (finalizerMonitor) { object owned already by calling
		// getPathFunc(String)
		if (!Path.reusableHashes.isEmpty()) {
			hash = Path.reusableHashes.remove().intValue();
		} else {
			hash = ++Path.nextHash;
		}
		// }
	}

	/**
	 * creates a new base for absolute paths
	 * 
	 * @param name
	 */
	private Path(final String name) {
		dirs = null;
		filename = name;
		parent = null;
		if (name.endsWith(FileSystem.getFileSeparator())) {
			str = name.substring(0, name.length() - 1) + "/";
			pathStr = name;
		} else {
			str = name + "/";
			pathStr = name + FileSystem.getFileSeparator();
		}

		hash = ++Path.nextHash;
	}

	/** */
	@Override
	public int compareTo(final Path o) {
		if (this == o) {
			return 0;
		}
		final int m = getNameCount();
		final int n = o.getNameCount();
		final int min = m > n ? n : m;
		for (int i = 0; i < min; i++) {
			final String mS = getComponentAt(i);
			final String nS = o.getComponentAt(i);
			final int c = mS.compareTo(nS);
			if (c != 0) {
				return c;
			}
		}
		throw new IllegalStateException();
	}

	/**
	 * Tries to insert a suffix to indicate this file is a backup
	 * 
	 * @param suffix
	 *            Suffix to add
	 * @return the name of renamed file
	 */
	public final String createBackup(final String suffix) {
		if (parent == null) {
			throw new RuntimeException("Error renaming root");
		}
		final StringBuilder string = new StringBuilder();
		final String base, end;
		final int dot = filename.lastIndexOf('.');
		if (dot < 0) {
			base = filename;
			end = "";
		} else {
			base = filename.substring(0, dot);
			end = filename.substring(dot);
		}
		string.appendLast(base);
		string.appendLast(suffix);
		while (true) {
			final Path newPath = parent.resolve(string.toString() + end);
			if (!newPath.exists()) {
				if (renameTo(newPath)) {
					return string.appendLast(end).toString();
				}
				return null;
			}
			string.appendLast(suffix);
		}
	}

	/**
	 * Deletes the file matching to <i>this</i> path relative to the base. The
	 * wholte directory will be delelted if <i>this</i> path is pointing to a
	 * directory.
	 * 
	 * @return <i>true</i> if the file was deleted
	 * @see File#delete()
	 */
	public final boolean delete() {
		if (Path.delete(toFile())) {
			return true;
		}
		if (file.exists()) {
			System.err.println("Failed to delete " + pathStr
					+ " - It will be deleted after terminating");
		}
		return false;
	}

	/** */
	@Override
	public final boolean equals(final Object other) {
		if (Path.class.isAssignableFrom(other.getClass())) {
			return this == other;
		}
		return false;
	}

	/**
	 * Checks if the the file where <i>this</i> path points to exists.
	 * 
	 * @return <i>true</i> if the the file where <i>this</i> path points to
	 *         exists
	 */
	public final boolean exists() {
		return toFile().exists();
	}

	/**
	 * Returns the name of the first directory specified by <i>this</i> path.
	 * "/foo/bar" would be return "foo" for example.
	 * 
	 * @return <i>null</i> if <i>this</i> is a base.
	 */
	public final String getBaseName() {
		if (dirs == null) {
			return null;
		}
		assert parent != null;
		if (dirs.length == 0) {
			return parent.str;
		}
		return dirs[0];
	}

	/**
	 * @param layer
	 * @return the part of this path
	 */
	public final String getComponentAt(int layer) {
		if (layer < dirs.length) {
			return dirs[layer];
		}
		return filename;
	}

	/**
	 * Returns the last part of <i>this</i> path. "/foo/bar" would be return
	 * "bar" for example.
	 * 
	 * @return the last part of <i>this</i> path.
	 */
	public final String getFileName() {
		return filename;
	}

	/**
	 * Returns the number of components of <i>this</i> path. "/" will return 0
	 * and "/foo/bar" will return 2.
	 * 
	 * @return the number of components of i>this</i> path
	 */
	public final int getNameCount() {
		return filename == null ? 0 : dirs.length + 1;
	}

	/**
	 * Returns the parent of <i>this</i> path. "/" will return <i>null</i> and
	 * "/foo/bar" will return "/foo".
	 * 
	 * @return the parent
	 */
	public final Path getParent() {
		return parent;
	}

	/**
					 */
	@Override
	public final int hashCode() {
		return hash;
	}

	/**
	 * @param base
	 * @return the string for which base.resolve() would return this
	 */
	public final String relativize(final Path base) {
		Path p = this;
		synchronized (Path.relativizer) {
			Path.relativizer.clear();
			while (p.getNameCount() > base.getNameCount()) {
				p = p.getParent();
			}
			int same = p.getNameCount();
			if (p != base) {
				Path q = base;
				if (q.getNameCount() > p.getNameCount()) {
					while (q.getNameCount() > p.getNameCount()) {
						q = q.getParent();
						Path.relativizer.appendLast("../");
					}
				}
				while (p != q) {
					if (--same == 0) {
						return str;
					}
					p = p.getParent();
					q = q.getParent();
					Path.relativizer.appendLast("../");
				}
			}
			while (same < getNameCount()) {
				Path.relativizer.appendLast(getComponentAt(same++));
				Path.relativizer.appendLast("/");
			}
			Path.relativizer.removeLast();
			return Path.relativizer.toString();
		}
	}

	/**
	 * Renames the file <i>this</i> path is pointing to to where pathNew points
	 * to. If <i>this</i> path is a directory all contained files will be
	 * renamed recursively.
	 * 
	 * @param pathNew
	 *            path pointing to the new location
	 * @return <i>true</i> if renaming was successful
	 * @see File#renameTo(File)
	 */
	public final boolean renameTo(final Path pathNew) {
		if (!pathNew.getParent().exists()) {
			if (!pathNew.getParent().toFile().mkdirs()) {
				return false;
			}
		} else if (pathNew.exists()) {
			if (!pathNew.toFile().delete()) {
				System.err.println("Failed to delte existing file");
				if (toFile().isFile() && pathNew.toFile().isFile()) {
					try {
						System.err.print("Fall back: overwrite ");
						final java.io.OutputStream out =
								new java.io.FileOutputStream(pathNew
										.toFile());
						final java.io.InputStream in =
								new java.io.FileInputStream(toFile());
						final byte[] buffer = new byte[0x2000];
						for (int read = 0; read >= 0; read =
								in.read(buffer)) {
							out.write(buffer, 0, read);
						}
						in.close();
						out.flush();
						out.close();
					} catch (final IOException e) {
						e.printStackTrace();
						System.err.println("failed");
						return false;
					}
				}
				System.err.println("succeeded");
				return true;
			}
		}
		if (toFile().isDirectory()) {
			final String[] files = toFile().list();

			if (files.length == 0) {
				return toFile().renameTo(pathNew.toFile());
			}
			final boolean ret =
					pathNew.exists() || pathNew.toFile().mkdir();
			for (int i = 0; i < files.length; i++) {
				if (!getPathFunc(files[i]).renameTo(
						pathNew.getPathFunc(files[i]))) {
					// undo
					while (i >= 0) {
						pathNew.getPathFunc(files[i]).renameTo(
								getPathFunc(files[i]));
						--i;
					}
					pathNew.delete();
					return false;
				}
			}
			return ret & renameFilePath(this, pathNew);
		}
		return renameFilePath(this, pathNew);
	}

	/**
	 * Concatenates two paths. name will be parsed and appended to <i>this</i>
	 * path. For example "/foo".resolve("/bar") will return "/foo/bar".
	 * 
	 * @param name
	 *            path to append
	 * @return the concatenated path
	 */
	public final Path resolve(final String... name) {
		if (name.length == 0) {
			throw new IllegalArgumentException();
		}
		if (Path.rootMap.containsKey(name[0]) || name[0].isEmpty()) {
			return Path.getPath(name);
		}
		Path p = this;
		for (final String s : name) {
			p = p.getPathFunc(s);
		}
		return p;
	}

	/**
	 * Concatenates two paths. other will be appended to <i>this</i> path. For
	 * example "/foo".resolve("/bar") will return "/foo/bar".
	 * 
	 * @param other
	 *            path to append
	 * @return the concatenated path
	 */
	public final Path resolve(final String other) {
		if (other.equals("..")) {
			return parent;
		}
		if (other.startsWith(FileSystem.getFileSeparator())) {
			return resolve(other.substring(1));
		}
		return getPathFunc(other);
	}

	/**
	 * Converts <i>this</i> path to an absolute path
	 * 
	 * @return the absolute path
	 */
	public final java.nio.file.Path toAbsolutePath() {
		return Paths.get(pathStr);
	}

	/**
	 * Returns the file <i>this</i> path is pointing to.
	 * 
	 * @return the referred file
	 */
	public final File toFile() {
		if (file == null) {
			file = new File(toAbsolutePath().toUri());
		}
		return file;
	}

	/**
	 * Returns a textual representation of <i>this</i> path
	 * 
	 * @return a textual representation of <i>this</i> path
	 */
	@Override
	public final String toString() {
		assert str != null;
		return str;
	}

	private final Path getPathFunc(final String name) {
		final WeakReference<Path> p;
		if (name.startsWith(".")) {
			if (name.equals(".")) {
				return this;
			} else if (name.equals("..")) {
				if (parent == null) {
					return this;
				}
				return parent;
			}
		}
		if (Path.rootMap == null) {
			return new Path(this, name);
		}
		synchronized (Path.finalizerMonitor) {
			p = successors.get(name);
			if ((p == null) || p.isEnqueued()) {
				return new Path(this, name);
			}
			if (p.get() == null) {
				return new Path(this, name);
			}
			return p.get();
		}
	}

	private final Path resolve(final String[] name, int offset) {
		Path p = this;
		for (int i = offset; i < name.length; i++) {
			p = p.getPathFunc(name[i]);
		}
		return p;
	}

	/** */
	@Override
	protected final void finalize() throws Throwable {
		synchronized (Path.finalizerMonitor) {
			if (parent != null) {
				parent.successors.remove(filename);
			}
			Path.reusableHashes.push(Integer.valueOf(hash));
		}
	}
}
