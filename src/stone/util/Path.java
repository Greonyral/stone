package stone.util;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents paths relative to a certain base or absolute, specified by
 * FileSystem.
 * 
 * @author Nelphindal
 */
public final class Path implements Comparable<Path>, Externalizable {

	/**
	 * Interface needed by {@link Path#writeExternals(PathAwareObjectOutput)}
	 * 
	 * @author Nelphindal
	 * 
	 */
	public interface PathAwareObjectOutput {

		/**
		 * @param path
		 * @return id created by {@link #registerId(Path, String)}
		 * @throws InterruptedException
		 */
		int getId(final Path path) throws InterruptedException;

		/**
		 * Invokes actual write process of
		 * {@link Path#writeExternals(PathAwareObjectOutput)}
		 * 
		 * @param parent
		 *            directory
		 * @param filename
		 *            filename contained by <i>parent</i>
		 */
		void registerId(final Path parent, final String filename);

	}

	private static int nextHash;
	private static final Map<String, Path> rootMap = buildRootMap();

	private static final ArrayDeque<Integer> reusableHashes = new ArrayDeque<>();

	private final static Object finalizerMonitor = new Object();

	private final static StringBuilder relativizer = new StringBuilder();

	private final static Path tmpRoot = getPath(System.getProperty(
			"java.io.tmpdir").split("\\" + FileSystem.getFileSeparator()));

	/**
	 * @return parsed Path of {@link FileSystem#getDataDirectory()}
	 */
	public final static Path getDataDirectory() {
		return Path.getPath(FileSystem.getDataDirectory().split(
				"\\" + FileSystem.getFileSeparator()));
	}

	/**
	 * Parses the given string <i>name</i> and returns a path representing the
	 * corresponding path. The resulting path will be separated resulting to
	 * given <i>name</i> on its array components.
	 * 
	 * @param name
	 *            components of path to create
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
			while (++idx < name.length) {
				p = p.getRootMapPathFunc(name[idx]);
			}
			return p;
		} else if ((FileSystem.type == FileSystem.OSType.WINDOWS)
				&& base.startsWith("%") && base.endsWith("%")) {
			final Map<String, String> env = System.getenv();
			final String path = env.get(base.substring(1, base.length() - 1)
					.toUpperCase());
			return getPath(path.split("\\" + FileSystem.getFileSeparator()));
		} else {
			p = Path.rootMap.get(base);
		}
		if (p == null) {
			throw new RuntimeException("Invalid path: invalid root: " + base);
		}
		if (name[idx].length() <= p.str.length()) {
			++idx;
		} else {
			name[idx] = name[idx].substring(p.str.length());
		}
		return p.getPathFunc(name, idx);
	}

	/**
	 * Parses the given url <i>url</i> and returns a path representing the
	 * corresponding path.
	 * 
	 * @param url
	 *            {@link URL} to parse. leading jar: and file: will be
	 *            truncated.
	 * @return the parsed path
	 */
	public final static Path getPath(final URL url) {
		final StringBuilder sb = new StringBuilder(url.toString());
		final List<String> names = new ArrayList<>();
		Path path = null;
		int pos = 0;
		if (sb.startsWith("jar:")) {
			sb.setHead(4);
		}
		if (sb.startsWith("file:/")) {
			sb.setHead(5);
		}
		if (FileSystem.type == FileSystem.OSType.WINDOWS) {
			sb.setHead(1);
		}
		while (pos < sb.length()) {
			switch (sb.charAt(pos)) {
			case '!':
				names.add(sb.toString().substring(0, pos));
				if (path == null) {
					return Path
							.getPath(names.toArray(new String[names.size()]));
				}
				return path.resolve(names.toArray(new String[names.size()]));
			case '%':
				final byte b0 = sb.getByte(pos + 1);
				assert b0 >= 0xc0;
				final int byteCount = b0 < 0x80 ? 1 : (b0 < 0xe ? 3 : 4);
				final ByteBuffer buffer = ByteBuffer.allocate(byteCount);
				buffer.put(b0);
				for (int i = pos + 2; buffer.remaining() > 0; ++i) {
					buffer.put(sb.getByte(i));
				}
				buffer.rewind();
				final String replacement = FileSystem.UTF8.decode(buffer)
						.toString();
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
					names.add(s);
				}
				sb.setHead(pos + 1);
				pos = 0;
				continue;
			default:
				++pos;
				continue;
			}
		}
		if (path == null) {
			return Path.rootMap.get(sb.toString());
		}
		names.add(sb.toString());
		return path.resolve(names.toArray(new String[names.size()]));
	}

	/**
	 * Creates an unused path, and marks the associated file for deletion on
	 * exit
	 * 
	 * @param prefix
	 *            prefix of created temporary directory
	 * @return a path usable for a temporary directory
	 */
	public final static Path getTmpDirOrFile(final String prefix) {
		while (true) {
			final int rand = (int) (Math.random() * Integer.MAX_VALUE);
			final Path tmp = Path.tmpRoot.resolve(prefix + "_temp" + rand);
			if (!tmp.exists()) {
				tmp.toFile().deleteOnExit();
				return tmp;
			}

		}
	}

	/**
	 * Reads from given {@link InputStream} and generates the resulting
	 * {@link Path} object.
	 * 
	 * @param in
	 *            {@link InputStream} to read from. The stream my contain only
	 *            one encoded {@link Path} object.
	 * @return parsed Path
	 * @throws IOException
	 *             exception thrown by <i>in</i>
	 */
	public final static Path readExternal(final InputStream in)
			throws IOException {
		int length = 0;
		while (true) {
			length <<= 7;
			final int read = in.read();
			length += read & 0x7f;
			if (read <= 0x80) {
				break;
			}
		}
		final byte[] bytes = new byte[length];
		in.read(bytes);
		return Path.getPath(new String(bytes, FileSystem.UTF8).split("/"));
	}

	/**
	 * Reads from given {@link InputStream} and generates the resulting
	 * {@link Path} object.
	 * 
	 * @param in
	 *            {@link InputStream} to read from. The stream my contain only
	 *            encoded {@link Path} objects with leading bytes for encoded
	 *            {@link Integer} to use for the map key.
	 * @return parsed {@link Map} of {@link Integer} and {@link Path} objects
	 * @throws IOException
	 *             exception thrown by <i>in</i>
	 */
	public final static Map<Integer, Path> readExternals(final InputStream in)
			throws IOException {
		final Map<Integer, Path> map = new HashMap<>();
		if (in != null) {
			while (true) {
				final IntegerPointer available = new IntegerPointer(
						in.available());
				if (available.isZero()) {
					break;
				}
				while (available.greaterZero()) {
					final Integer idPrev = readInt(in, available);
					final Integer id = readInt(in, available);
					final String name = readName(in, available);
					final Path p;
					if (idPrev == 0) {
						p = getPath(name);
					} else {
						p = map.get(idPrev).resolve(name);
					}
					map.put(id, p);
				}
			}
		}
		if (map.isEmpty()) {
			map.put(-1, null);
		}
		return map;
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

	private final static Integer readInt(final InputStream in,
			final IntegerPointer iPtr) throws IOException {
		int value = 0;
		while (true) {
			final int byteRead = in.read();
			iPtr.decrement();
			if (byteRead < 0) {
				throw new IOException("Unexpected end of file");
			}
			value <<= 7;
			value += 0x7f & byteRead;
			if ((byteRead & 0x80) == 0) {
				break;
			}
		}
		return Integer.valueOf(value);
	}

	private final static String readName(final InputStream in,
			final IntegerPointer iPtr) throws IOException {
		int length = 0;
		while (true) {
			final int byteRead = in.read();
			iPtr.decrement();
			length = (length << 7) | (byteRead & 0x7f);
			if (byteRead < 0x80) {
				break;
			}
		}
		final byte[] buffer = new byte[length];
		in.read(buffer);
		iPtr.decrement(length);
		return new String(buffer, FileSystem.UTF8);
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

	private final Map<String, PathReference> successors = new TreeMap<>();

	private final String filename;

	private final Path parent;

	private final String str;

	private final String pathStr;

	private File file = null;

	/**
	 * creates new path with parent != root
	 */
	private Path(@SuppressWarnings("hiding") final Path parent,
			final String name) {
		this.filename = name;
		this.parent = parent;
		if (parent.parent == null) {
			this.str = parent.str + name;
			this.pathStr = parent.pathStr + name;
		} else {
			this.str = parent.str + "/" + name;
			this.pathStr = parent.pathStr + FileSystem.getFileSeparator()
					+ name;
		}
		final PathReference pWeak = new PathReference(this);
		parent.successors.put(name, pWeak);
		if (parent.dirs == null) {
			this.dirs = new String[0];
		} else {
			this.dirs = new String[parent.dirs.length + 1];
			this.dirs[parent.dirs.length] = parent.filename;
			System.arraycopy(parent.dirs, 0, this.dirs, 0, parent.dirs.length);
		}
		if (Path.rootMap == null) {
			this.hash = ++Path.nextHash;
		} else if (!Path.reusableHashes.isEmpty()) {
			this.hash = Path.reusableHashes.remove().intValue();
		} else {
			this.hash = ++Path.nextHash;
		}
	}

	private Path(final Path p, final String[] names, int offset) {
		Path p0 = p;
		for (int i = offset; i < (names.length - 1); ++i) {
			p0 = new Path(p0, names[i]);
		}
		if (p0.dirs == null) {
			this.dirs = new String[0];
		} else {
			this.dirs = new String[p0.dirs.length + 1];
			System.arraycopy(p0.dirs, 0, this.dirs, 0, p0.dirs.length);
			this.dirs[this.dirs.length - 1] = p0.filename;
		}
		this.filename = names[names.length - 1];
		this.parent = p0;
		if (this.parent.parent == null) {
			this.str = this.parent.str + this.filename;
			this.pathStr = this.parent.pathStr + this.filename;
		} else {
			this.str = this.parent.str + "/" + this.filename;
			this.pathStr = this.parent.pathStr + FileSystem.getFileSeparator()
					+ this.filename;
		}
		if (!Path.reusableHashes.isEmpty()) {
			this.hash = Path.reusableHashes.remove().intValue();
		} else {
			this.hash = ++Path.nextHash;
		}
		p0.successors.put(this.filename, new PathReference(this));
	}

	/**
	 * creates a new base for absolute paths
	 * 
	 * @param name
	 */
	private Path(final String name) {
		this.dirs = null;
		this.filename = name;
		this.parent = null;
		if (name.endsWith(FileSystem.getFileSeparator())) {
			this.str = name.substring(0, name.length() - 1) + "/";
			this.pathStr = name;
		} else {
			this.str = name + "/";
			this.pathStr = name + FileSystem.getFileSeparator();
		}

		this.hash = ++Path.nextHash;
	}

	/** */
	@Override
	public final int compareTo(final Path o) {
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
		if (this.parent == null) {
			throw new RuntimeException("Error renaming root");
		}
		final StringBuilder string = new StringBuilder();
		final String base, end;
		final int dot = this.filename.lastIndexOf('.');
		if (dot < 0) {
			base = this.filename;
			end = "";
		} else {
			base = this.filename.substring(0, dot);
			end = this.filename.substring(dot);
		}
		string.appendLast(base);
		string.appendLast(suffix);
		while (true) {
			final Path newPath = this.parent.resolve(string.toString() + end);
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
		if (this.dirs == null) {
			return null;
		}
		assert this.parent != null;
		if (this.dirs.length == 0) {
			return this.parent.str;
		}
		return this.dirs[0];
	}

	/**
	 * @param layer
	 *            the n-th component of <i>this</i> {@link Path} object
	 * @return the part of this path
	 */
	public final String getComponentAt(int layer) {
		if (layer < this.dirs.length) {
			return this.dirs[layer];
		}
		return this.filename;
	}

	/**
	 * Returns the last part of <i>this</i> path. "/foo/bar" would be return
	 * "bar" for example.
	 * 
	 * @return the last part of <i>this</i> path.
	 */
	public final String getFilename() {
		return this.filename;
	}

	/**
	 * Returns the number of components of <i>this</i> path. "/" will return 0
	 * and "/foo/bar" will return 2.
	 * 
	 * @return the number of components of <i>this</i> path
	 */
	public final int getNameCount() {
		return this.filename == null ? 0 : this.dirs == null ? 1
				: this.dirs.length + 1;
	}

	/**
	 * Returns the parent of <i>this</i> path. "/" will return <i>null</i> and
	 * "/foo/bar" will return "/foo".
	 * 
	 * @return the parent
	 */
	public final Path getParent() {
		return this.parent;
	}

	/**
           */
	@Override
	public final int hashCode() {
		return this.hash;
	}

	@Override
	public final void readExternal(final ObjectInput in)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param base
	 *            {@link Path} to which <i>this</i> object shall be relative
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
						return this.str;
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
		if (pathNew == this) {
			return false;
		}
		if (!pathNew.getParent().exists()) {
			if (!pathNew.getParent().toFile().mkdirs()) {
				return false;
			}
		} else if (pathNew.exists()) {
			if (!pathNew.toFile().delete()) {
				Debug.print("Failed to delte existing file %s\n",
						pathNew.toString());
				if (toFile().isFile() && pathNew.toFile().isFile()) {
					try {
						System.err.print("Fall back: overwrite ");
						final java.io.OutputStream out = new java.io.FileOutputStream(
								pathNew.toFile());
						final java.io.InputStream in = new java.io.FileInputStream(
								toFile());
						final byte[] buffer = new byte[0x2000];
						for (int read = 0; read >= 0; read = in.read(buffer)) {
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
				Debug.print("succeeded\n");
				return true;
			}
		}
		if (toFile().isDirectory()) {
			final String[] files = toFile().list();

			if (files.length == 0) {
				return toFile().renameTo(pathNew.toFile());
			}
			final boolean ret = pathNew.exists() || pathNew.toFile().mkdir();
			for (int i = 0; i < files.length; i++) {
				if (!resolve(files[i]).renameTo(pathNew.resolve(files[i]))) {
					// undo
					while (i >= 0) {
						pathNew.resolve(files[i]).renameTo(resolve(files[i]));
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
	public final Path resolve(final String name) {
		return resolve(new String[] { name });
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
		return getPathFunc(name, 0);
	}

	/**
	 * Converts <i>this</i> path to an absolute path
	 * 
	 * @return the absolute path
	 */
	public final java.nio.file.Path toAbsolutePath() {
		return Paths.get(this.pathStr);
	}

	/**
	 * Returns the file <i>this</i> path is pointing to.
	 * 
	 * @return the referred file
	 */
	public final File toFile() {
		if (this.file == null) {
			this.file = new File(toAbsolutePath().toUri());
		}
		return this.file;
	}

	/**
	 * Returns a textual representation of <i>this</i> path
	 * 
	 * @return a textual representation of <i>this</i> path
	 */
	@Override
	public final String toString() {
		assert this.str != null;
		return this.str;
	}

	@Override
	public final void writeExternal(final ObjectOutput out) throws IOException {
		final byte[] bytes = this.str.getBytes(FileSystem.UTF8);
		encode(bytes.length, out);
		out.write(bytes);
	}

	/**
	 * Writes <i>this</i> object embedded into a stream mapping a file
	 * hierarchy. The resulting output can be parsed back by
	 * {@link #readExternals(InputStream)}.
	 * 
	 * @param out
	 *            instance of {@link PathAwareObjectOutput} taking care of
	 *            creating the output
	 */
	public final void writeExternals(final PathAwareObjectOutput out) {
		if (this.parent != null) {
			int parentId;
			try {
				parentId = out.getId(this.parent);
			} catch (final InterruptedException e) {
				return;
			}
			if (this.parent == null) {
				out.registerId(null, this.filename);
			} else if (parentId == -1) {
				this.parent.writeExternals(out);
			}
		}
		out.registerId(this.parent, this.filename);
	}

	private final void encode(int length, final ObjectOutput out)
			throws IOException {
		int lowByte = length & 0x7f;
		final int highBytes = length & ~0x7f;
		if (highBytes != 0) {
			lowByte |= 0x80;
			encode(length >> 7, out);
		}
		out.write(lowByte);
	}

	private final Path getPathFunc(final String[] names, int offset) {
		Path p = this;
		for (int i = offset; i < names.length; ++i) {
			final String name = names[i];
			final PathReference pWeak;
			if (name.startsWith(".")) {
				if (name.equals(".")) {
					continue;
				} else if (name.equals("..")) {
					if (p.parent == null) {
						continue;
					}
					p = p.parent;
					continue;
				}
			} else {
				final Path pTmp = rootMap.get(name);
				if (pTmp != null) {
					p = pTmp;
					continue;
				}
			}
			final Path pTmp;
			synchronized (Path.finalizerMonitor) {
				pWeak = p.successors.get(name);
				if (pWeak == null) {
					return new Path(p, names, i);
				}
				pTmp = pWeak.get();
				if (pTmp == null) {
					return new Path(p, names, i);
				}
			}
			p = pTmp;
		}
		return p;
	}

	private final Path getRootMapPathFunc(final String name) {
		assert rootMap == null;
		return new Path(this, name);
	}

	/** */
	@Override
	protected final void finalize() throws Throwable {
		synchronized (Path.finalizerMonitor) {
			final Set<PathReference> ss = new HashSet<>(
					this.successors.values());
			for (final WeakReference<Path> s : ss) {
				if (!s.isEnqueued()) {
					s.get().finalize();
				}
			}
			if (this.parent != null) {
				if (this.parent.successors.remove(this.filename) != null) {
					Path.reusableHashes.push(Integer.valueOf(this.hash));
				}
			} else {
				Path.reusableHashes.push(Integer.valueOf(this.hash));
			}
		}
	}
}
