package stone.modules.songData;

import java.util.Map;

import stone.util.Path;

interface MTDeserializer {

	void serialize(SerializeConainer sc, final Path pathToSong);

	void serialize(SerializeConainer sc, long modDate);

	void serialize(SerializeConainer sc, final Map<Integer, String> voices);

	SerializeConainer createSerializeConainer();

}
