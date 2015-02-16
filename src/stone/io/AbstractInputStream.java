package stone.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractInputStream extends InputStream {

	protected IOHandler io;
	protected final byte[] _buffer;

	protected int _offset = 0;
	protected int _length = -1;

	public AbstractInputStream(final byte[] buffer) {
		_buffer = buffer;
	}

	/**
	 * Returns next byte in <i>this</i> stream.
	 * 
	 * @return next byte in <i>this</i> stream or -1 if there is no more byte
	 *         because end of file has been reached. Further reading will throw
	 *         an appropriate IOException
	 * @throws IOException
	 *             if an error occurs reading in the file
	 */
	@Override
	public final int read() throws IOException {
		if (EOFreached()) {
			return -1;
		}
		if (io != null) {
			io.updateProgress();
		}
		return 0xff & _buffer[_offset++];
	}

	/**
	 * Checks if <i>this</i> stream reached the end of file
	 * 
	 * @return <i>true</i> if <i>this</i> stream reached the end of file
	 * @throws IOException
	 */
	public final boolean EOFreached() throws IOException {
		if (_length < _offset) {
			_offset = _length;
		}
		if ((_offset == _length) || (_length < 0)) {
			fillBuff();
		}
		if (_length == 0) {
			if (io != null) {
				io.endProgress();
				io = null;
			}
			return true;
		}
		return false;
	}

	/**
	 * Resets the stream to start
	 */
	@Override
	public void reset() {
		_length = -1;
	}

	protected abstract void fillBuff() throws IOException;

	protected final void fillBuffByStream(final java.io.InputStream stream)
			throws IOException {
		final int buffered;
		buffered = _length - _offset;
		if (buffered > 0) {
			System.arraycopy(_buffer, _offset, _buffer, 0, buffered);
		}
		_offset = 0;
		_length = buffered;
		if (stream.available() > 0) {
			final int read = stream.read(_buffer, buffered, _buffer.length
					- buffered);
			_length += read;
		}
	}

	protected void finalize() {
		if (io != null)
			io.close(this);
	}

	/**
	 * Tries to read as many bytes as needed to fill given buffer
	 * 
	 * @param buffer
	 *            buffer to fill
	 * @return number of bytes read, -1 on EOF
	 * @throws IOException
	 *             if an error occurs reading in the file
	 */
	@Override
	public final int read(byte[] buffer) throws IOException {
		if (EOFreached()) {
			return -1;
		}
		int read = 0;
		while (true) {
			read += fillExternalBuffer(buffer, read, buffer.length - read);
			// offset += read; done by fillExternal Buffer
			if (EOFreached() || (read == buffer.length)) {
				return read;
			}
		}
	}

	/**
	 * Tries to read as many bytes as needed to fill given buffer.
	 * 
	 * @param buffer
	 *            buffer to fill
	 * @param offset
	 *            position in the buffer to start
	 * @param length
	 *            number of bytes to read
	 * @return number of bytes read
	 * @throws IOException
	 *             if an error occurs reading in the file
	 * @throws IllegalArgumentException
	 *             if offset or length do not fulfill the requirements
	 */
	@Override
	public final int read(byte[] buffer, int offset, int length)
			throws IOException {
		if (EOFreached()) {
			return -1;
		}
		if ((length > buffer.length) || (length < 0)
				|| (offset >= buffer.length) || (offset < 0)
				|| (length > (buffer.length - offset))) {
			throw new IllegalArgumentException();
		}
		int read = 0;
		while (true) {
			read += fillExternalBuffer(buffer, read + offset, length - read);
			if (EOFreached() || (read == length)) {
				return read;
			}
		}
	}

	protected final int fillExternalBuffer(final byte[] buffer, int offset,
			int length) {
		final int remIntBuffer = _length - _offset;
		final int lengthRet = Math.min(length, remIntBuffer);
		System.arraycopy(_buffer, _offset, buffer, offset, lengthRet);
		_offset += lengthRet;
		if (io != null) {
			io.updateProgress(lengthRet);
		}
		return lengthRet;
	}
}
