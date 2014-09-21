package stone.modules.abcCreator;

import java.io.File;


/**
 * @author Nelphindal
 */
public class DrumMapFileFilter extends stone.util.PathOptionFileFilter {

	/**
	 * Checks if given file is a directory and contains at least one map.
	 * 
	 * @param f
	 * @return <i>true</i> on positive result.
	 */
	public static final boolean check(final File f) {
		if (!f.isDirectory()) {
			return false;
		}
		final String[] list = f.list();
		if (list == null) {
			return false;
		}
		for (final String s : list) {
			if (s.startsWith("drum") && s.endsWith(".drummap.txt")) {
				for (int i = 4; i < (s.length() - 12); i++) {
					if ((s.charAt(i) < '0') || (s.charAt(i) > '9')) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 */
	public DrumMapFileFilter() {
	}

	/**
	 * Checks if given file is directory and at least containing one drum-map or
	 * another directory
	 */
	@Override
	public boolean accept(final File f) {
		return accept(f, 0);
	}

	/** */
	@Override
	public String getDescription() {
		return "Directory with instrument map drum.[1-9]([0-9])*.drummap.txt)";
	}

	/**
	 * @return the selected directory. If a drum-map was selected the containing
	 *         directory will be returned.
	 */
	@Override
	public final File value(final File file) {
		if (file.isFile()) {
			return file.getParentFile();
		} else if (DrumMapFileFilter.check(file)) {
			return file;
		}
		return null;
	}

	private boolean accept(final File f, int depth) {
		if (depth == 5) {
			return true;
		}
		if (check(f)) {
			return true;
		} else if (f.isDirectory()) {
			for (final File fDir : f.listFiles()) {
				if (accept(fDir, depth + 1)) {
					return true;
				}
			}
		}
		return false;
	}

}
