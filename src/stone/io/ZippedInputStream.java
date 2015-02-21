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
	public ZippedInputStream(final ZipFile zip, final ZipEntry entry)
			throws IOException {
		super(new byte[0x8000]);
		this._zip = new ZipFile(zip.getName()); // ensure my ZipFile stays open
		this._in = this._zip
				.getInputStream(this._zip.getEntry(entry.getName()));
	}


	@Override
	public final int available() throws IOException {
		return this._in.available();

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


	@Override
	protected final void fillBuff() throws IOException {
		super.fillBuffByStream(this._in);
	}

	@Override
	protected final void finalize() {
		try {
			this._zip.close();
		} catch (final IOException e) {
		}
	}
}
