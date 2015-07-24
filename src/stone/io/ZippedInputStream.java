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
	 * @param zip
	 *            file to read from
	 * @param entry
	 *            name of entry to read from
	 * @throws IOException
	 *             if an I/O error has occurred or if a ZIP format error has
	 *             occurred
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
	 * @param n ignored
	 * @return nothing, throws UnsupportedOperationException
	 * @throws UnsupportedOperationException any time this method is called
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
			// Silently disregard the thrown exception
		}
	}
}
