package stone.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractInputStream extends InputStream {

	protected IOHandler io;
	protected final byte[] _buffer;

	protected int _offset = 0;
	protected int _length = -1;

	public AbstractInputStream(final byte[] buffer) {
		this._buffer = buffer;
	}

	/**
	 * Checks if <i>this</i> stream reached the end of file
	 * 
	 * @return <i>true</i> if <i>this</i> stream reached the end of file
	 * @throws IOException
	 */
	public final boolean EOFreached() throws IOException {
		if (this._length < this._offset) {
			this._offset = this._length;
		}
		if ((this._offset == this._length) || (this._length < 0)) {
			fillBuff();
		}
		if (this._length == 0) {
			if (this.io != null) {
				this.io.endProgress();
				this.io = null;
			}
			return true;
		}
		return false;
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
		if (this.io != null) {
			this.io.updateProgress();
		}
		return 0xff & this._buffer[this._offset++];
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

	/**
	 * Resets the stream to start
	 */
	@Override
	public void reset() {
		this._length = -1;
	}

	protected abstract void fillBuff() throws IOException;

	protected final void fillBuffByStream(final java.io.InputStream stream)
			throws IOException {
		final int buffered;
		buffered = this._length - this._offset;
		if (buffered > 0) {
			System.arraycopy(this._buffer, this._offset, this._buffer, 0,
					buffered);
		}
		this._offset = 0;
		this._length = buffered;
		if (stream.available() > 0) {
			final int read = stream.read(this._buffer, buffered,
					this._buffer.length - buffered);
			this._length += read;
		}
	}

	protected final int fillExternalBuffer(final byte[] buffer, int offset,
			int length) {
		final int remIntBuffer = this._length - this._offset;
		final int lengthRet = Math.min(length, remIntBuffer);
		System.arraycopy(this._buffer, this._offset, buffer, offset, lengthRet);
		this._offset += lengthRet;
		if (this.io != null) {
			this.io.updateProgress(lengthRet);
		}
		return lengthRet;
	}

	@Override
	protected void finalize() {
		if (this.io != null) {
			this.io.close(this);
		}
	}
}
