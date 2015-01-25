package stone.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * A class for a BufferedInputStream reading from a file
 * 
 * @author Nelphindal
 */
public final class ZippedInputStream extends AbstractInputStream {

	private final InputStream _in;
	private final ZipFile _zip;


	/**
	 * Generates a new InputStream reading from given file
	 * 
	 * @param file
	 *            file to read from
	 * @param cs
	 *            charset used for encoding
	 * @throws IOException 
	 */
	public ZippedInputStream(final ZipFile zip, final ZipEntry entry) throws IOException {
		super(new byte[0x8000]);
		_zip = new ZipFile(zip.getName()); // ensure my ZipFile stays open 
		_in = _zip.getInputStream(_zip.getEntry(entry.getName()));
	}



	/**
	 * Not supported
	 * 
	 * @param n
	 * @return nothing, throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final long skip(long n) {
		throw new UnsupportedOperationException();
	}
	
	public final int available() throws IOException {
		return _in.available();
		
	}


	@Override
	protected final void fillBuff() throws IOException {
		super.fillBuffByStream(_in);
	}
	
	protected final void finalize() {
		try {
			_zip.close();
		} catch (final IOException e) {
		}
	}
}
