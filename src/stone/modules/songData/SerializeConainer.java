package stone.modules.songData;

import java.io.IOException;
import java.util.Map;

import stone.util.Path;

/**
 * Encoder to write {@link SongDataEntry}
 * 
 * @author Nelphindal
 * 
 */
public interface SerializeConainer {

	/**
	 * 
	 * @param song
	 * @param mod
	 * @param voices
	 * @throws IOException
	 */
	void write(final Path song, long mod, final Map<Integer, String> voices)
			throws IOException;

	/**
	 * 
	 * @param value
	 * @throws IOException
	 */
	void write(String value) throws IOException;

	/**
	 * 
	 * @param size
	 * @throws IOException
	 */
	void writeSize(int size) throws IOException;

}
