package stone.io;

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

	private final static byte[] merge(final Queue<byte[]> parts,
			int stackSize) {
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
	private FileInputStream stream;
	private final Charset cs;
	private final File file;

	/**
	 * Generates an empty InputStream
	 */
	public InputStream() {
		super(null);
		cs = null;
		file = null;
	}

	/**
	 * Generates a new InputStream reading from given file
	 * 
	 * @param file
	 *            file to read from
	 * @param cs
	 *            charset used for encoding
	 */
	public InputStream(final File file, final Charset cs) {
		super(new byte[16000]);
		this.cs = cs;
		this.file = file;
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
		return file.delete();
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
		return marked.pop();
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
		final int len = (int) file.length();
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
			return new String(line, 0, line.length - 1, cs);
		}
		return new String(line, cs);
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
	public final byte[] readTo(byte terminal, byte mark)
			throws IOException {
		return readTo(terminal, 0xff & mark);
	}

	/**
	 * Registers an IO-Handler for managing a ProgressMonitor for
	 * {@link #read()}
	 * 
	 * @param io
	 */
	public final void registerProgressMonitor(final IOHandler io) {
		this.io = io;
		io.startProgress("Reading file", (int) file.length());
	}

	/**
	 * Resets the stream to start
	 */
	@Override
	public final void reset() {
		stream = null;
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

	protected final void fillBuff() throws IOException {
		if (stream == null) {
			if (!file.exists()) {
				_length = 0;
				return;
			}
			stream = new FileInputStream(file);
			fillBuff();
			// remove byte order mark
			if (cs.toString().equals("UTF-16")) {
				// FF FE
				if ((_buffer[0] == -1) && (_buffer[1] == -2)) {
					_offset += 2;
				}
			} else if (cs.toString().equals("UTF-8")) {
				// EF BB BF
				if ((_buffer[0] == -17) && (_buffer[1] == -69)
						&& (_buffer[2] == -65)) {
					_offset += 3;
				}
			}
			return;
		}
		super.fillBuffByStream(stream);
	}

	private final int addToStack(final Queue<byte[]> stack, int start) {
		final int len = _offset - start;
		final byte[] part = new byte[len];
		stack.add(part);
		System.arraycopy(_buffer, start, part, 0, len);
		if (io != null) {
			io.updateProgress(len);
		}
		return len;
	}

	private final byte[] readTo(byte terminal, int mark)
			throws IOException {
		marked.clear();
		if (EOFreached()) {
			return null;
		}
		final Queue<byte[]> stack = new ArrayDeque<>();
		int start = _offset, length = 0;
		while (true) {
			if (_buffer[_offset] == mark) {
				marked.add(_offset);
			} else if (_buffer[_offset] == terminal) {
				length += addToStack(stack, start);
				break;
			}
			if (++_offset == _length) {
				length += addToStack(stack, start);
				fillBuff();
				start = 0;
				if (_length == 0) {
					break;
				}
			}
		}
		++_offset;
		return InputStream.merge(stack, length);
	}
}
