package stone.modules.songData;

import java.io.IOException;
import java.io.InputStream;

interface DeserializeContainer {

	SongData parse(byte[] bytes) throws IOException;

	int readSize(InputStream is) throws IOException;
	
	String read(InputStream is) throws IOException;

}
