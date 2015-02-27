package stone.modules;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stone.StartupContainer;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.util.FileSystem;
import stone.util.Flag;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.StringOption;
import stone.util.TaskPool;


/**
 * Dummy module to support updating the core
 * 
 * @author Nelphindal
 */
public class Main implements Module {

	private static final int MAX_LENGTH_INFO = 80;


	private static final int VERSION = 20;

	/**
	 * The name to be used for naming the config-file and the title.
	 */
	public static final String TOOLNAME = "SToNe";

	/**
	 * Section in the config file for VersionControl
	 */
	public static final String VC_SECTION = "[vc]";

	/**
	 * Key within the VersionControl section for naming the path to local
	 * repository
	 */
	public static final String REPO_KEY = "repo";

	/**
	 * Section in the config file for global setting
	 */
	public static final String GLOBAL_SECTION = "[global]";

	/**
	 * Key within the global section for naming the path, where the relative
	 * paths shall start from
	 */
	public static final String PATH_KEY = "path";

	/**
	 * Key within the global section for the name
	 */
	public static final String NAME_KEY = "name";

	/**
	 * Key for indicating to run the repair routine.
	 */
	public static final String REPAIR = "Repair";

	/**
	 * Should be called only once. Creates an option to adjust the user's name.
	 * 
	 * @param oc
	 * @return the option for the name of user.
	 */
	public final static StringOption createNameOption(final OptionContainer oc) {
		return new StringOption(oc, "Name",
				"Should be your ingame name. Used as part of commit messages and as"
						+ " meta-tag in created files.", "Name for Commits",
				'n', "name", Main.GLOBAL_SECTION, Main.NAME_KEY);
	}

	public final static String formatMaxLength(final Path base,
			final String filename) {
		return formatMaxLength(base, filename, "", "");
	}

	public final static String formatMaxLength(final Path base,
			final String filename, final String a, final String b) {
		if (filename == null) {
			return formatMaxLength(base, "", a == null ? "" : a, b == null ? ""
					: b);
		}
		if ((a == null) || (b == null)) {
			return formatMaxLength(base, filename, a == null ? "" : a,
					b == null ? "" : b);
		}
		final StringBuilder sb = new StringBuilder();
		int lengthSB = format(sb, a, 0);
		int pos = 0;
		final int components = base.getNameCount();
		if (lengthSB > 0) {
			sb.append(" ");
			++lengthSB;
		}
		while (pos < components) {
			final String c = base.getComponentAt(pos);
			if (pos++ == 0) {
				if ((lengthSB + 1 + c.length()) >= Main.MAX_LENGTH_INFO) {
					sb.append("\n");
					lengthSB = 0;
				}
				sb.append("\"");
				++lengthSB;
			} else {
				sb.append(FileSystem.getFileSeparator());
				++lengthSB;
				if ((lengthSB + c.length()) >= Main.MAX_LENGTH_INFO) {
					sb.append("\n");
					lengthSB = 0;
				}
			}
			sb.append(c);
			lengthSB += c.length();
		}
		++lengthSB;
		sb.append("\"");
		if (!filename.isEmpty()) {
			if ((lengthSB > 0)
					&& ((lengthSB + filename.length()) >= Main.MAX_LENGTH_INFO)) {
				sb.append("\n");
				lengthSB = 0;
			}
			sb.append(FileSystem.getFileSeparator());
			sb.append(filename);
			lengthSB += filename.length() + 1;
		}
		format(sb, b, lengthSB);
		return sb.toString();
	}

	private static final void createIO(final StartupContainer os) {
		os.createFinalIO(new IOHandler(os));
	}

	private static int format(final StringBuilder sb, final String s,
			int lineLength) {
		final String[] parts = s.split(" ");
		boolean front = sb.length() != 0;
		int l = lineLength;
		for (final String part : parts) {
			if ((l > 0) && ((l + part.length() + 1) >= Main.MAX_LENGTH_INFO)) {
				sb.append("\n");
				l = 0;
			} else if (!front) {
				++l;
				sb.append(" ");
			}
			front = false;
			sb.append(part);
			l += part.length();
		}
		return l;
	}

	/**
	 * The users homeDir
	 */
	public final Path homeDir = FileSystem.getBase();
	final Path homeSetting = this.homeDir.resolve(".SToNe");

	TaskPool taskPool;

	final Map<String, Map<String, String>> configOld = new HashMap<>();

	final Map<String, Map<String, String>> configNew = new HashMap<>();

	/**
	 * Creates a new instance providing the parsed entries of the config
	 */
	public Main() {
	}

	/**
	 * Flushes the configuration
	 */
	public final void flushConfig() {
		if (this.configNew.isEmpty()) {
			return;
		}

		final Runnable r = new MainConfigWriter(this);
		if (this.taskPool == null) {
			r.run();
		} else {
			this.taskPool.addTask(r);
		}
	}

	/**
	 * Gets a value from the config.
	 * 
	 * @param section
	 * @param key
	 * @param defaultValue
	 * @return the value in the config, or defaultValue if the key in given
	 *         section does not exist
	 */
	public final String getConfigValue(final String section, final String key,
			final String defaultValue) {
		synchronized (this.configOld) {
			synchronized (this.configNew) {
				final Map<String, String> map0 = this.configNew.get(section);
				final Map<String, String> map1 = this.configOld.get(section);
				if (map0 != null) {
					final String value0 = map0.get(key);
					if (value0 != null) {
						return value0;
					}
				}
				if (map1 != null) {
					final String value1 = map1.get(key);
					if (value1 != null) {
						return value1;
					}
				}
				return defaultValue;
			}
		}
	}

	/** Should not been called, returns always <i>null</i> without any effect. */
	@Deprecated
	@Override
	public final List<Option> getOptions() {
		return null;
	}

	/** */
	@Override
	public final int getVersion() {
		return Main.VERSION;
	}

	/** Should not been called, returns always <i>null</i> without any effect. */
	@Deprecated
	@Override
	public final Module init(final StartupContainer sc) {
		return null;
	}

	/**
	 * Deletes the config-file.
	 */
	@Override
	public final void repair() {
		if (this.homeSetting.exists()) {
			final boolean success = this.homeSetting.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					this.homeSetting.toString(), success ? "" : " failed");
		}
	}

	/**
	 * Not supported and will throw an UnsupportedOperationException. Call
	 * {@link #run(StartupContainer, Flag)} instead.
	 */
	@Deprecated
	@Override
	public final void run() {
		throw new UnsupportedOperationException();
	}

	/**
	 * The actual main method executing this main module.
	 * 
	 * @param sc
	 * @param flags
	 */
	public final void run(final StartupContainer sc, final Flag flags) {
		final IOHandler io;
		this.taskPool = sc.createTaskPool();
		createIO(sc);
		io = sc.getIO();
		if (io == null) {
			return;
		}
		final Runnable workerRun = this.taskPool.runMaster();
		this.taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				try {
					sc.finishInit(flags); // sync barrier 1
					final InputStream in = io.openIn(
							Main.this.homeSetting.toFile(), FileSystem.UTF8);
					final StringBuilder sb = new StringBuilder();
					String section = null;
					try {
						while (true) {
							final int read = in.read();
							if (read < 0) {
								parseConfig(sb, section);
								break;
							}
							final char c = (char) read;
							if ((c == '\r') || (c == '\t')) {
								sb.append(' ');
							} else if (c == '\n') {
								section = parseConfig(sb, section);
							} else {
								sb.append(c);
							}
						}
					} catch (final IOException e) {
						e.printStackTrace();
					}
					io.close(in);
					if (sb.length() != 0) {
						System.out.println("error parsing config");
						sc.getMaster().interrupt();
					} else {
						sc.parseDone(); // sync barrier 2
					}
				} catch (final Exception e) {
					Main.this.homeSetting.delete();
					Main.this.taskPool.getMaster().interrupt();
					io.close();
					throw e;
				}
			}

		});
		Thread.currentThread().setName("Worker-0");
		workerRun.run();
	}

	/**
	 * Sets a config entry
	 * 
	 * @param section
	 * @param key
	 * @param value
	 */
	public final void setConfigValue(final String section, final String key,
			final String value) {
		synchronized (this.configOld) {
			synchronized (this.configNew) {
				if (value == null) {
					if (getConfigValue(section, key, null) != null) {
						final Map<String, String> map0 = this.configNew
								.get(section);
						if (map0 == null) {
							final Map<String, String> map1 = new HashMap<>();
							map1.put(key, null);
							this.configNew.put(section, map1);
						} else {
							map0.put(key, null);
						}
					}
				} else {
					final Map<String, String> mapOld = this.configOld
							.get(section);
					final Map<String, String> map0 = this.configNew
							.get(section);
					if (mapOld != null) {
						final String valueOld = mapOld.get(key);
						if ((valueOld != null) && valueOld.equals(value)) {
							if (map0 == null) {
								return;
							}
							map0.remove(key);
							if (map0.isEmpty()) {
								this.configNew.remove(section);
							}
							return;
						}
					}
					if (map0 != null) {
						map0.put(key, value);
					} else {
						final Map<String, String> map1 = new HashMap<>();
						map1.put(key, value);
						this.configNew.put(section, map1);
					}
				}
			}
		}
	}

	final String parseConfig(final StringBuilder line, final String section) {
		int idx = 0;
		if (line.length() == 0) {
			return section;
		}
		while (line.charAt(idx) == ' ') {
			++idx;
			if (idx == line.length()) {
				return section;
			}
		}
		if (line.charAt(idx) == '#') {
			// comment -> ignore
			line.setLength(0);
			return section;
		} else if (line.charAt(idx) == '[') {
			final int end = line.indexOf("]", idx) + 1;
			final String currentSection = line.substring(idx, end)
					.toLowerCase();
			this.configOld.put(currentSection, new HashMap<String, String>());
			idx = end;
			while ((idx < line.length()) && (line.charAt(idx) == ' ')) {
				if (++idx == line.length()) {
					break;
				}
			}
			line.setLength(line.length() - idx);
			return currentSection;
		}
		final int start = idx++;
		while (line.charAt(idx) != '=') {
			++idx;
		}
		final String key = line.substring(start, idx).trim().toLowerCase();
		final String value = line.substring(++idx, line.length()).trim();
		this.configOld.get(section).put(key, value);
		line.setLength(0);
		return section;

	}
}
