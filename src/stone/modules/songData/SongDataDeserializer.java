package stone.modules.songData;

import java.io.IOException;

import stone.io.InputStream;
import stone.util.Path;


final class SongDataDeserializer {

	final static void deserialize(final InputStream in,
			final SongDataContainer sdc, final Path base)
			throws IOException {
		if ((in == null) || in.EOFreached()) {
			return;
		}
		final int version = in.read();
		try {
			if (version == 3) {
				SongDataDeserializer_3.deserialize(in, sdc, base);
			}
		} catch (final NullPointerException e) {
			throw new IOException("unexpected end of file");
		}
	}

}
