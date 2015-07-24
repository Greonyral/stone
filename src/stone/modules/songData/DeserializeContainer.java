package stone.modules.songData;

import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulation of {@link Deserializer}
 * @author Nelphindal
 *
 */
public interface DeserializeContainer {

	/**
	 * Turns read bytes into a {@link SongDataEntry} object 
	 * @param bytes
	 * @return created instance
	 * @throws IOException
	 */
	SongDataEntry parse(byte[] bytes) throws IOException;

	/**
	 * Converts bytes of given stream to the uncompressed string
	 * @param is
	 * @return uncompressed part of <i>is</i>
	 * @throws IOException
	 */
	String read(InputStream is) throws IOException;

	/**
	 * Converts bytes of given stream to the get the size of next component
	 * @param is
	 * @return size of next component of <i>is</i>
	 * @throws IOException
	 */
	int readSize(InputStream is) throws IOException;

}
