package stone.modules.songData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import stone.MasterThread;
import stone.io.AbstractInputStream;
import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.util.FileSystem;
import stone.util.Path;

// uses multiple files to profit from multi threading
class Deserializer_0 extends Deserializer implements MTDeserializer {

	private final AtomicInteger id = new AtomicInteger();
	private final static String format = "%05d";
	private final BlockingQueue<DeserialTask> streams = new LinkedBlockingQueue<>();
	private final MasterThread master;
	private final static int MOD_LEN = 6;

	class ByteStreamOut implements SerializeConainer, ObjectOutput {

		private final int id = Deserializer_0.this.id.incrementAndGet();
		private final IOHandler io = Deserializer_0.this.io;
		private final Path idx = Deserializer_0.this.idx;
		private OutputStream out;

		@Override
		public void writeBoolean(boolean v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeByte(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeShort(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeChar(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeInt(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeLong(long v) throws IOException {
			byte[] bytes = new byte[Long.BYTES];
			for (int i = bytes.length - 1; i >= 0; --i) {
				bytes[i] = (byte) v;
				v >>= 8;
			}
			write(bytes);
		}

		@Override
		public void writeFloat(float v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeDouble(double v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeBytes(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeChars(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeUTF(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeObject(final Object obj) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(int b) throws IOException {
			out.write(0xff & b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void flush() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(final Path song, long mod,
				final Map<Integer, String> voices) throws IOException {
			final Path outFile = idx.getParent().resolve(
					String.format(format, id));
			out = io.openOut(outFile.toFile());
			boolean success = false;
			try {
				writeMod(mod);
				song.writeExternal(this);
				SongDataContainer.writeExternal(voices, this);
				success = true;
			} finally {
				io.close(out);
				out = null;
				if (!success) {
					outFile.delete();
				}
			}
		}

		private void writeMod(long mod) throws IOException {
			byte[] bytes = new byte[MOD_LEN];
			for (int i = bytes.length - 1; i >= 0; --i) {
				bytes[i] = (byte) mod;
				mod >>= 8;
			}
			write(bytes);
		}

		@Override
		public void writeSize(int size) throws IOException {
			int lowByte = size & 0x7f;
			int highBytes = size & ~0x7f;
			if (highBytes != 0) {
				lowByte |= 0x80;
				writeSize(size >> 7);
			}
			write(lowByte);
		}

		@Override
		public void write(final String value) throws IOException {
			write(value.getBytes(FileSystem.UTF8));
			write(0);
		}
	}

	protected Deserializer_0(final SongDataContainer sdc, final MasterThread master) {
		super(sdc);
		this.master = master;
	}

	@Override
	protected void deserialize_() throws IOException {
		// nothing to do
	}

	@Override
	protected final void generateStream(final SongData data) throws IOException {
		data.serialize(this);
	}

	@Override
	protected final void abort_() {
		final OutputStream out = io.openOut(idx.getParent().resolve("sdd")
				.toFile());
		io.write(out, (byte) 0);
		io.close(out);
		io.compress(idx.toFile(), getFiles());
		clear();
	}

	private final void clear() {
		final Path idxDir = idx.getParent();
		for (int i = 0; i < id.get(); i++) {
			idxDir.resolve(String.format(format, i)).delete();
		}
	}

	private final File[] getFiles() {
		final Path idxDir = idx.getParent();
		final Set<File> files = new HashSet<>();
		for (int i = 0; i < id.get(); i++) {
			files.add(idxDir.resolve(String.format(format, i)).toFile());
		}
		files.add(idxDir.resolve("sdd").toFile());
		return files.toArray(new File[files.size()]);
	}

	@Override
	protected final void finish_() {
		final OutputStream out = io.openOut(idx.getParent().resolve("sdd")
				.toFile());
		io.write(out, (byte) 0);
		io.close(out);
		io.compress(idx.toFile(), getFiles());
		clear();
	}

	@Override
	protected final void crawlDone_() {

	}


	public Runnable getDeserialTask() {
		io.startProgress("Looking up results from last run", -1);
		return new ByteStreamIn();

	}

	class ByteStreamIn implements DeserializeContainer, Runnable {

		private final AtomicInteger id = new AtomicInteger();
		private final DeserialTask end = new DeserialTask();
		private int serialSongs;

		private final void unpack() throws IOException {
			final Map<String, AbstractInputStream> streams = io.openInZip(idx);
			if (streams == null)
				return;
			streams.remove("sdd");
			serialSongs = streams.size();
			io.setProgressSize(serialSongs);
			for (final Map.Entry<String, AbstractInputStream> stream : streams
					.entrySet()) {
				Deserializer_0.this.streams.add(new DeserialTask(stream, this));
				io.updateProgress();
			}
			Deserializer_0.this.streams.add(end);
		}

		public final void run() {
			if (id.getAndIncrement() == 0) {
				try {
					unpack();
					io.startProgress("Parsing results from last run", serialSongs);
					io.updateProgress(serialSongs - streams.size());
				} catch (final IOException e) {
					e.printStackTrace();
				}
			
			}
			try {
				while (!master.isInterrupted()) {
					final DeserialTask task = Deserializer_0.this.streams
							.take();
					if (task == end) {
						Deserializer_0.this.streams.add(end);
						break;
					}
					task.run();
					Deserializer_0.this.sdc.getDirTree().put(task.getResult());
					io.updateProgress(serialSongs - streams.size());
				}
			} catch (final InterruptedException e) {
				return;
			}
		}

		@Override
		public final SongData parse(final byte[] bytes) throws IOException {
			final InputStream is = new ByteArrayInputStream(bytes);
			final long mod;
			final Path path;
			final Map<Integer, String> voices;

			mod = readMod(is);
			path = Path.read(is);
			voices = SongDataContainer.readExternal(is, this);
			return new SongData(path, voices, mod);
		}

		private long readMod(final InputStream is) throws IOException {
			final byte[] bytes = new byte[MOD_LEN];
			long mod = 0;
			is.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				mod <<= 8;
				mod |= 0xff & bytes[i];
			}
			return mod;
		}

		@Override
		public final int readSize(final InputStream in) throws IOException {
			int size = 0;
			while (true) {
				int read = in.read();
				size <<= 7;
				size |= 0x7f & read;
				if (read < 0x80)
					return size;
			}
		}

		@Override
		public String read(final InputStream is) throws IOException {
			final ByteBuffer bb = ByteBuffer.allocate(is.available());
			final byte[] bytes;
			while (true) {
				int read = is.read();
				if (read == 0)
					break;
				bb.put((byte) (0xff & read));
			}
			bytes = new byte[bb.position()];
			bb.rewind();
			bb.get(bytes);
			return new String(bytes, FileSystem.UTF8);
		}
	}

	@Override
	public final SerializeConainer createSerializeConainer() {
		return new ByteStreamOut();
	}
}
