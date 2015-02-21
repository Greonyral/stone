package stone.modules.abcCreator;

import java.io.File;


/**
 * @author Nelphindal
 */
public abstract class FileEndingFilter extends stone.util.PathOptionFileFilter {

	private final int dots;

	/**
	 * @param dots
	 *            parts, separated by '.', necessary for {@link #ending(String)}
	 */
	protected FileEndingFilter(int dots) {
		this.dots = dots;
	}

	/**
	 * Checks if the given file shall be accepted.
	 */
	@Override
	public final boolean accept(final File file) {
		if (file.isDirectory()) {
			return true;
		}
		final String name = file.getName();
		int end = name.length();
		for (int i = 0; i < this.dots; i++) {
			end = name.lastIndexOf('.', end - 1);
			if (end < 0) {
				return false;
			}
		}

		return ending(name.substring(end).toLowerCase());
	}

	/**
	 * @param file
	 * @return file
	 */
	@Override
	public final File value(final File file) {
		return file;
	}

	/**
	 * Checks if the given filename shall be accepted.
	 * 
	 * @param name
	 * @return true if <i>name</i> shall be accepted.
	 */
	protected abstract boolean ending(final String name);
}
