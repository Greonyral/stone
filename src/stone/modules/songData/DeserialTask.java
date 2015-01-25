package stone.modules.songData;

import java.io.IOException;
import java.util.Map.Entry;

import stone.io.AbstractInputStream;

class DeserialTask implements Runnable {

	private final AbstractInputStream stream;
	private final DeserializeContainer sc;
	private SongData result;

	public DeserialTask(final Entry<String, AbstractInputStream> stream,
			final DeserializeContainer byteStreamIn) {
		this.stream = stream.getValue();
		this.sc = byteStreamIn;
	}

	public DeserialTask() {
		stream = null;
		sc = null;
	}

	@Override
	public void run() {
		try {
			final byte[] bytes = new byte[stream.available()];
			if (bytes.length == 0)
				return;
			int read = stream.read(bytes);
			while (read < bytes.length) {
				read += stream.read(bytes, read, bytes.length - read);
			}
			
			result = sc.parse(bytes);
		} catch (final IOException e) {
		} finally {
			try {
				stream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public SongData getResult() {
		return result;
	}

}
