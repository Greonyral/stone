package stone.util;

import java.nio.charset.Charset;


/**
 * Class for abstraction between different FileSystems. Any returned paths use /
 * for path seperation. For OS dependend representation see
 * {@link stone.util.Path}
 * 
 * @author Nelphindal
 */
public abstract class FileSystem {

	/**
	 * @author Nelphindal
	 */
	public enum OSType {
		/**
		 * Any windows version between Windows XP and Windows 8
		 */
		WINDOWS("\\", "\r\n", "%appdata%"),
		// /**
		// * Any unix indicating itself as linux kernel
		// */
		// LINUX("/", "\n"),
		/**
		 * Any unix system, which is not sub-classified
		 */
		UNIX("/", "\n", "~"),
		/**
		 * Any system not classified
		 */
		UNKNOWN(null, null, null);

		private String subtype;

		private final String sepLine, sepFile, dataDirectory;

		@SuppressWarnings("hiding")
		OSType(final String sepFile, final String sepLine,
				final String dataDirectory) {
			this.sepLine = sepLine;
			this.sepFile = sepFile;
			this.dataDirectory = dataDirectory;
		}

		/**
		 * 
		 * @return the path of the directory containing additional data
		 */
		final String getDataDirectory() {
			return this.dataDirectory;
		}

		final String getFileSeparator() {
			return this.sepFile;
		}

		final String getLineSeparator() {
			return this.sepLine;
		}

		final String getSubtype() {
			return this.subtype;
		}

		final OSType setSubtype(final String substring) {
			this.subtype = substring;
			return this;
		}
	}

	/** The default charset of underlying OS */
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

	/** The default charset UTF-8 */
	public static final Charset UTF8 = Charset.forName("UTF8");

	/** The OS loaded this class */
	public static final OSType type = FileSystem.determineOSType();

	private static final FileSystem instance = FileSystem.createInstance();

	private final static Path home = FileSystem.getHome();

	/**
	 * @return The path equivalent to ~ on linux, %UserProfile% on windows.
	 */
	public final static Path getBase() {
		return FileSystem.home;
	}

	/**
	 * @return all possible starts of absolute addresses on this FileSystem
	 */
	public final static String[] getBases() {
		return FileSystem.instance.getFSBases();
	}

	/**
	 * @return the root directory used for data of running programm
	 */
	public final static String getDataDirectory() {
		return FileSystem.type.getDataDirectory();
	}

	/**
	 * @return the File-separator
	 */
	public final static String getFileSeparator() {
		return FileSystem.type.getFileSeparator();
	}

	/**
	 * @return the File-separator
	 */
	public final static String getLineSeparator() {
		return FileSystem.type.getLineSeparator();
	}

	private final static FileSystem createInstance() {
		switch (FileSystem.type) {
		case UNIX:
			// case LINUX:
			return new UnixFileSystem();
		case WINDOWS:
			return new WindowsFileSystem();
		default:
			return null;
		}
	}

	private final static OSType determineOSType() {
		final String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			return OSType.WINDOWS.setSubtype(osName.substring(8));
		} else if (osName.startsWith("Unix") || osName.startsWith("Linux")) {
			return OSType.UNIX;
		}
		// } else if (osName.startsWith("Linux")) {
		// return OSType.LINUX;
		throw new UnrecognizedOSException();
	}

	private final static Path getHome() {
		final String home_ = System.getProperty("user.home");
		switch (FileSystem.type) {
		case UNIX:
			// case LINUX:
			return Path.getPath(home_.split(FileSystem.OSType.UNIX.sepFile));
		case WINDOWS:
			if (FileSystem.type.subtype.equals("Windows Vista")) {
				// workaround for bug on vista, see
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6519127
				final String user = System.getProperty("user.name");
				return Path.getPath((home_.substring(0, 9) + user).split("\\"
						+ getFileSeparator()));
			}
			return Path.getPath(home_.split("\\" + getFileSeparator()));
		default:
		}
		return null;
	}

	/**
	 * Creates a new FileSystem
	 */
	protected FileSystem() {
	}

	/**
	 * @return all possible starts of absolute addresses on this FileSystem.
	 */
	protected abstract String[] getFSBases();
}

final class UnixFileSystem extends FileSystem {

	UnixFileSystem() {
	}

	@Override
	protected final String[] getFSBases() {
		return new String[] { "/" };
	}
}

final class UnrecognizedOSException extends RuntimeException {

	/** */
	private static final long serialVersionUID = 1L;

}

final class WindowsFileSystem extends FileSystem {

	WindowsFileSystem() {
	}

	@Override
	protected final String[] getFSBases() {
		final String[] bases = new String[26];
		for (char c = 'A', i = 0; c <= 'Z'; c++, i++) {
			bases[i] = c + ":";
		}
		return bases;
	}
}
