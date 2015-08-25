package stone.modules.songData;

import java.io.IOException;
import java.util.Map.Entry;

import stone.io.AbstractInputStream;

class DeserialTask implements Runnable {

	private final AbstractInputStream stream;
	private final DeserializeContainer sc;
	private SongDataEntry result;
	private final String entry;
	
	public DeserialTask() {
		this.stream = null;
		this.sc = null;
		this.entry = null;
	}

	public DeserialTask(@SuppressWarnings("hiding") final Entry<String, AbstractInputStream> stream,
			final DeserializeContainer byteStreamIn) {
		this.stream = stream.getValue();
		this.sc = byteStreamIn;
		this.entry = stream.getKey();
	}

	public SongDataEntry getResult() {
		return this.result;
	}

	@Override
	public void run() {
		try {
			final byte[] bytes = new byte[this.stream.available()];
			if (bytes.length == 0) {
				return;
			}
			int read = this.stream.read(bytes);
			while (read < bytes.length) {
				read += this.stream.read(bytes, read, bytes.length - read);
			}

			this.result = this.sc.parse(bytes);
		} catch (final IOException e) {
			// Silently disregard thrown exception
		} finally {
			try {
				this.stream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

}
