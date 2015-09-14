package stone.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;


/**
 * A FileOutputStream allowing 2 or more Threads to work on by buffering write
 * access of one
 * 
 * @author Nelphindal
 */
public class OutputStream extends FileOutputStream {

	private final Charset cs;
	private IOHandler io;

	/**
	 * Opens a new OutputStream to a file. Pre-existing content of the file is
	 * discarded.
	 * 
	 * @param file
	 *            {@link File} to write into
	 * @param cs
	 *            encoding to use
	 * @throws FileNotFoundException
	 *             if <i>file</i> exists and is no regular file
	 */
	public OutputStream(final File file,
			@SuppressWarnings("hiding") final Charset cs)
			throws FileNotFoundException {
		this(file, cs, false);
	}

	/**
	 * Opens a new OutputStream to a file.
	 * 
	 * @param file
	 *            {@link File} to write into
	 * @param cs
	 *            encoding to use
	 * @param append
	 *            <i>true</i> if written bytes shall be appended and
	 *            pre-existing content preserved
	 * @throws FileNotFoundException
	 *             if <i>file</i> exists and is no regular file
	 */
	public OutputStream(final File file,
			@SuppressWarnings("hiding") final Charset cs, boolean append)
			throws FileNotFoundException {
		super(file, append);
		this.cs = cs;
	}

	/**
	 * 
	 * @param io
	 *            {@link IOHandler} to use for displaying progress, each read
	 *            byte will progress by one
	 */
	public void registerProgress(@SuppressWarnings("hiding") final IOHandler io) {
		this.io = io;
	}

	@Override
	public void write(final byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
		if (this.io != null) {
			this.io.updateProgress(len);
		}
	}

	/**
	 * Writes the encoded string
	 * 
	 * @param string
	 *            {@link String} to write
	 * @throws IOException
	 *             if an error occurs
	 */
	public final void write(final String string) throws IOException {
		write(string.getBytes(this.cs));
	}
}
