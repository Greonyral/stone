package stone.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;


/**
 * A FileOutputStream allowing 2 Threads to work on by buffering write access of
 * one
 * 
 * @author Nelphindal
 */
public class OutputStream extends FileOutputStream {

	private final Charset cs;

	/**
	 * Opens a new OutputStream to a file.
	 * 
	 * @param file
	 * @param cs
	 *            encoding to use
	 * @throws FileNotFoundException
	 *             if <i>file</i> exists and is no regular file
	 */
	public OutputStream(final File file, final Charset cs)
			throws FileNotFoundException {
		this(file, cs, false);
	}

	/**
	 * Opens a new OutputStream to a file.
	 * 
	 * @param file
	 * @param cs
	 *            encoding to use
	 * @param append
	 * @throws FileNotFoundException
	 *             if <i>file</i> exists and is no regular file
	 */
	public OutputStream(final File file, final Charset cs, boolean append)
			throws FileNotFoundException {
		super(file, append);
		this.cs = cs;
	}

	/**
	 * Writes the encoded string
	 * 
	 * @param string
	 * @throws IOException
	 *             if an error occurs
	 */
	public final void write(final String string) throws IOException {
		write(string.getBytes(this.cs));
	}
}
