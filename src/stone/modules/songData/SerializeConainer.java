package stone.modules.songData;

import java.io.IOException;
import java.util.Map;

import stone.util.Path;

interface SerializeConainer {

	void write(final Path song, long mod, final Map<Integer, String> voices)
			throws IOException;

	void write(String value) throws IOException;

	void writeSize(int size) throws IOException;

}
