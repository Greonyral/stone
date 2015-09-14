package stone.modules.songData;

import java.util.Map.Entry;

import stone.util.Path;


/**
 * Class holding the path and date of last modification of a file
 * 
 * @author Nelphindal
 */
public class ModEntry implements Entry<Path, Long> {
	static final ModEntry TERMINATE = new ModEntry();

	private final Path path;
	private final long mod;

	/**
	 * @param path
	 *            -
	 */
	public ModEntry(@SuppressWarnings("hiding") final Path path) {
		this.path = path;
		this.mod = path.toFile().lastModified();
	}

	private ModEntry() {
		this.path = null;
		this.mod = 0;
	}

	/**
	 * @return the path
	 */
	@Override
	public final Path getKey() {
		return this.path;
	}

	/**
	 * @return the modification date in milliseconds since the last epoch
	 */
	@Override
	public final Long getValue() {
		return this.mod;
	}

	/**
	 * Not supported.
	 * 
	 * @return -
	 * @throws UnsupportedOperationException
	 *             whenever called
	 */
	@Override
	public final Long setValue(final Long arg0) {
		throw new UnsupportedOperationException();
	}

}
