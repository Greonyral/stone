package stone.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * A class for a BufferedInputStream reading from a file
 * 
 * @author Nelphindal
 */
public class InputStream extends AbstractInputStream {

	private final static byte[] merge(final Queue<byte[]> parts, int stackSize) {
		if (parts.size() == 1) {
			return parts.poll();
		}
		final byte[] ret = new byte[stackSize];
		int offset = 0;
		while (!parts.isEmpty()) {
			final byte[] part = parts.poll();
			final int length = part.length;
			System.arraycopy(part, 0, ret, offset, length);
			offset += length;
		}
		return ret;
	}


	private final ArrayDeque<Integer> marked = new ArrayDeque<>();
	private java.io.InputStream stream;
	private final Charset cs;
	private final File file;

	/**
	 * Generates an empty InputStream
	 */
	public InputStream() {
		super(null);
		this.cs = null;
		this.file = null;
	}

	/**
	 * Generates a new InputStream reading from given file
	 * 
	 * @param file
	 *            file to read from
	 * @param cs
	 *            charset used for encoding
	 */
	@SuppressWarnings("hiding")
	public InputStream(final File file, final Charset cs) {
		super(new byte[16000]);
		this.cs = cs;
		this.file = file;
	}


	@Override
	public final int available() throws IOException {
		if (EOFreached()) {
			return 0;
		}
		return (this.stream.available() + this._length) - this._offset;

	}


	@Override
	public final void close() throws IOException {
		super.close();
		if (this.stream != null) {
			this.stream.close();
		}
	}


	/**
	 * Closes this stream and deletes the associated file
	 * 
	 * @return <i>true</i> if deleting was successful
	 * @see File#delete()
	 * @throws IOException
	 *             if an error occurs closing the stream
	 */
	public final boolean deleteFile() throws IOException {
		close();
		return this.file.delete();
	}

	/**
	 * Returns relative offset from current position in <i>this</i> stream to a
	 * previously marked location.
	 * 
	 * @return relative offset from current position in <i>this</i> stream to
	 *         marked location, -1 if there is no more mark
	 * @throws NoSuchElementException
	 *             if no more marked position is available
	 * @see #readTo(byte, byte)
	 */
	public final int getMarkedLoc() throws NoSuchElementException {
		return this.marked.pop();
	}

	/**
	 * Reads all remaining bytes and returns them in a byte array.
	 * 
	 * @return byte array holding bytes read
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final byte[] readFully() throws IOException {
		reset();
		final int len = (int) this.file.length();
		final byte[] ret = new byte[len];
		read(ret);
		return ret;
	}

	/**
	 * Reads all bytes until next byte indicating new line (i.e. 0x0a) is
	 * reached. The byte 0x0a is removed as well as Windows line (0x0d 0x0a)
	 * 
	 * @return bytes between current position and a 0x0a byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final String readLine() throws IOException {
		final byte[] line;
		if (EOFreached()) {
			return null;
		}
		line = readTo((byte) 10);
		if ((line.length != 0) && (line[line.length - 1] == '\r')) {
			return new String(line, 0, line.length - 1, this.cs);
		}
		return new String(line, this.cs);
	}

	/**
	 * Reads all bytes until next byte matching given terminal is reached.
	 * 
	 * @param terminal
	 *            byte to stop reading at
	 * @return bytes between current position and given terminal byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final byte[] readTo(byte terminal) throws IOException {
		return readTo(terminal, -1);
	}

	/**
	 * Reads all bytes until next byte matching given terminal is reached. All
	 * positions of bytes matching given byte mark are marked
	 * 
	 * @param terminal
	 *            byte to stop reading at
	 * @param mark
	 *            byte to mark the position
	 * @return bytes between current position and given terminal byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 * @see #getMarkedLoc()
	 */
	public final byte[] readTo(byte terminal, byte mark) throws IOException {
		return readTo(terminal, 0xff & mark);
	}

	/**
	 * Registers an IO-Handler for managing a {@link ProgressMonitor} for
	 * {@link #read()}
	 * 
	 * @param io
	 *            {@link IOHandler} providing a instance of
	 *            {@link ProgressMonitor}
	 */
	public final void registerProgressMonitor(
			@SuppressWarnings("hiding") final IOHandler io) {
		this.io = io;
		io.startProgress("Reading file", (int) this.file.length());
	}

	/**
	 * Sets <i>this</i> to reach EOF on next invocation.
	 */
	@Override
	public final synchronized void reset() {
		this.stream = null;
		super.reset();
	}

	/**
	 * Not supported
	 * 
	 * @param n
	 *            ignored
	 * @return nothing, throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 *             whenever called
	 */
	@Override
	public final long skip(long n) {
		throw new UnsupportedOperationException();
	}

	private final int addToStack(final Queue<byte[]> stack, int start) {
		final int len = this._offset - start;
		final byte[] part = new byte[len];
		stack.add(part);
		System.arraycopy(this._buffer, start, part, 0, len);
		if (this.io != null) {
			this.io.updateProgress(len);
		}
		return len;
	}

	private final byte[] readTo(byte terminal, int mark) throws IOException {
		this.marked.clear();
		if (EOFreached()) {
			return null;
		}
		final Queue<byte[]> stack = new ArrayDeque<>();
		int start = this._offset, length = 0;
		while (true) {
			if (this._buffer[this._offset] == mark) {
				this.marked.add(this._offset);
			} else if (this._buffer[this._offset] == terminal) {
				length += addToStack(stack, start);
				break;
			}
			if (++this._offset == this._length) {
				length += addToStack(stack, start);
				fillBuff();
				start = 0;
				if (this._length == 0) {
					break;
				}
			}
		}
		++this._offset;
		return InputStream.merge(stack, length);
	}

	@Override
	protected final void fillBuff() throws IOException {
		if (this.stream == null) {
			if (!this.file.exists()) {
				this._length = 0;
				return;
			}
			this.stream = new BufferedInputStream(
					new FileInputStream(this.file));
			fillBuff();
			// remove byte order mark
			if (this.cs.toString().equals("UTF-16")) {
				// FF FE
				if ((this._buffer[0] == -1) && (this._buffer[1] == -2)) {
					this._offset += 2;
				}
			} else if (this.cs.toString().equals("UTF-8")) {
				// EF BB BF
				if ((this._buffer[0] == -17) && (this._buffer[1] == -69)
						&& (this._buffer[2] == -65)) {
					this._offset += 3;
				}
			}
			return;
		}
		super.fillBuffByStream(this.stream);
	}
}
