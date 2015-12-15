package stone.modules.songData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import stone.MasterThread;
import stone.io.AbstractInputStream;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.modules.SongData;
import stone.util.Debug;
import stone.util.FileSystem;
import stone.util.Path;

// uses multiple files to profit from multi threading
class Deserializer_0 extends Deserializer implements MTDeserializer {

	class ByteStreamIn implements DeserializeContainer, Runnable {

		@SuppressWarnings("hiding")
		private final AtomicInteger id = new AtomicInteger();
		private final DeserialTask end = new DeserialTask();
		@SuppressWarnings("hiding")
		private final Path idx;

		private int serialSongs;

		ByteStreamIn(@SuppressWarnings("hiding") final Path idx) {
			this.idx = idx;
		}

		@Override
		public final SongDataEntry parse(final byte[] bytes) throws IOException {
			// TODO read as written to restore data
			final InputStream is = new ByteArrayInputStream(bytes);
			final Map<Integer, String> voices;
			final int idDir = is.read();
			final String name = read(is);

			Path dir = Deserializer_0.this.idMapIn.get(idDir);
			if (dir == null) {
				synchronized (Deserializer_0.this.idMapIn) {
					while ((dir = Deserializer_0.this.idMapIn.get(idDir)) == null) {
						try {
							Deserializer_0.this.idMapIn.wait();
						} catch (final InterruptedException e) {
							return null;
						}
					}

				}
			}
			final Path path = dir.resolve(name);
			if (!path.exists()) // file deleted -> info not needed
			{
				Debug.print(path + " not existing");
				return null;
			}
			final long mod = readMod(is);
			if (path.toFile().lastModified() != mod) // file edited -> info not
														// needed
			{
				Debug.print(path + " changed");
				return null;
			}
			voices = SongData.readExternal(is, this);
			return new SongDataEntry(path, voices, mod);
		}

		@Override
		public final String read(final InputStream is) throws IOException {
			final ByteBuffer bb = ByteBuffer.allocate(is.available());
			final byte[] bytes;
			while (true) {
				final int read = is.read();
				if (read <= 0) {
					break;
				}
				bb.put((byte) (0xff & read));
			}
			bytes = new byte[bb.position()];
			bb.rewind();
			bb.get(bytes);
			return new String(bytes, FileSystem.UTF8);
		}

		@Override
		public final int readSize(final InputStream in) throws IOException {
			int size = 0;
			while (true) {
				final int read = in.read();
				size <<= 7;
				size |= 0x7f & read;
				if (read < 0x80) {
					return size;
				}
			}
		}

		@Override
		public final void run() {
			@SuppressWarnings("hiding")
			final int id = this.id.getAndIncrement();
			if (id == 1) {
				unpack();
				Deserializer_0.this.streams.add(this.end);
				Deserializer_0.this.io.startProgress("Parsing results from last run", this.serialSongs);
				Deserializer_0.this.io.updateProgress(this.serialSongs - Deserializer_0.this.streams.size());
			} else if (id == 0) {
				try {
					unpackPaths();
					run();
				} catch (final IOException e) {
					Debug.print("Failed to unpack: %s\n", e.getMessage());
				}
			}
			synchronized (Deserializer_0.this.idMapIn) {
				if (Deserializer_0.this.idMapIn.isEmpty()) {
					try {
						Deserializer_0.this.idMapIn.wait();
					} catch (final InterruptedException e) {
						return;
					}
				}

			}
			try {
				while (!Deserializer_0.this.master.isInterrupted()) {
					final DeserialTask task = Deserializer_0.this.streams.take();
					if (task == this.end) {
						Deserializer_0.this.streams.add(this.end);
						break;
					}
					task.run();
					final SongDataEntry sd = task.getResult();
					if (sd != null) {
						Deserializer_0.this.sdc.getDirTree().put(sd);
					}
					Deserializer_0.this.io.updateProgress();
				}
			} catch (final InterruptedException e) {
				return;
			}
			if (this.id.decrementAndGet() <= 0) {
				try {
					deserialize();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		private final long readMod(final InputStream is) throws IOException {
			final byte[] bytes = new byte[MOD_LEN];
			long mod = 0;
			is.read(bytes);
			for (int i = 0; i < bytes.length; i++) {
				mod <<= 8;
				mod |= 0xff & bytes[i];
			}
			return mod;
		}

		private final void unpack() {
			@SuppressWarnings("hiding")
			final Map<String, AbstractInputStream> streams = Deserializer_0.this.io.openInZip(this.idx);
			if (streams == null) {
				return;
			}
			streams.remove("sdd");
			this.serialSongs = streams.size();
			Deserializer_0.this.io.setProgressSize(this.serialSongs);
			for (final Map.Entry<String, AbstractInputStream> stream : streams.entrySet()) {
				if (!stream.getKey().equals(Deserializer_0.this.pathIdMap.getFilename())) {
					Deserializer_0.this.streams.add(new DeserialTask(stream, this));
				}
			}
			Deserializer_0.this.streams.add(this.end);
		}

		private final void unpackPaths() throws IOException {
			final Map<String, AbstractInputStream> inMap = Deserializer_0.this.io.openInZip(this.idx);
			final InputStream in = inMap == null ? null : inMap.get(Deserializer_0.this.pathIdMap.getFilename());
			@SuppressWarnings("hiding")
			final Map<Integer, Path> idMapIn;
			try {
				idMapIn = Path.readExternals(in);
				Deserializer_0.this.idMapIn.putAll(idMapIn);
			} finally {
				synchronized (Deserializer_0.this.idMapIn) {
					Deserializer_0.this.idMapIn.notifyAll();
				}
				Deserializer_0.this.io.close(in);
			}
		}
	}

	class ByteStreamOut implements SerializeConainer, ObjectOutput, stone.util.Path.PathAwareObjectOutput {

		@SuppressWarnings("hiding")
		private final IOHandler io = Deserializer_0.this.io;
		private OutputStream out;

		@Override
		public final void close() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void flush() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int getId(final Path path) throws InterruptedException {
			@SuppressWarnings("hiding")
			final Integer id;
			synchronized (Deserializer_0.this.idMapOut) {
				id = Deserializer_0.this.idMapOut.get(path);
				if (id == null) {
					Deserializer_0.this.idMapOut.put(path, -1);
				}
			}
			if (id == null) {
				return -1;
			}
			if (id.intValue() == -1) {
				synchronized (Deserializer_0.this.idMapOut) {
					while (Deserializer_0.this.idMapOut.get(path) == -1) {
						Deserializer_0.this.idMapOut.wait();
					}
					return getId(path);
				}
			}
			return id.intValue();
		}

		@Override
		public final void registerId(final Path parent, final String filename) {
			final Path p = parent == null ? Path.getPath(filename) : parent.resolve(filename);

			final int pathId = Deserializer_0.this.pathIdNextOut.incrementAndGet();
			final byte[] idBytesPrev = writeIntRE(parent == null ? 0 : Deserializer_0.this.idMapOut.get(parent));
			final byte[] idBytes = writeIntRE(pathId);
			final byte[] bytesName = filename.getBytes(FileSystem.UTF8);
			final byte[] nameLength = writeIntRE(bytesName.length);
			final byte[] bytes = new byte[idBytesPrev.length + idBytes.length + nameLength.length + bytesName.length];
			int offset = 0;
			System.arraycopy(idBytesPrev, 0, bytes, offset, idBytesPrev.length);
			offset += idBytesPrev.length;
			System.arraycopy(idBytes, 0, bytes, offset, idBytes.length);
			offset += idBytes.length;
			System.arraycopy(nameLength, 0, bytes, offset, nameLength.length);
			offset += nameLength.length;
			System.arraycopy(bytesName, 0, bytes, offset, bytesName.length);
			try {
				synchronized (Deserializer_0.this.pathIdOut) {
					Deserializer_0.this.pathIdOut.write(bytes);
				}

			} catch (final IOException e) {
				e.printStackTrace();
			}
			synchronized (Deserializer_0.this.idMapOut) {
				Deserializer_0.this.idMapOut.put(p, pathId);
				Deserializer_0.this.idMapOut.notifyAll();
			}
		}

		@Override
		public final void write(byte[] b) throws IOException {
			this.out.write(b);
		}

		@Override
		public final void write(byte[] b, int off, int len) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void write(int b) throws IOException {
			this.out.write(0xff & b);
		}

		@Override
		public final void write(final Path song, long mod, final Map<Integer, String> voices) throws IOException {
			final Path directory = song.getParent();
			final String name = song.getFilename();
			int idDir;
			try {
				idDir = getId(directory);

				if (idDir == -1) {
					directory.writeExternals(this);
					idDir = getId(directory);
				}
				final int idSong = Deserializer_0.this.id.incrementAndGet();
				final Path outFile = Deserializer_0.this.idx.getParent().resolve(String.format(format, idSong));
				this.out = this.io.openOut(outFile.toFile());
				write(writeIntRE(idDir));
				write(name);
				writeMod(mod);
				SongData.writeExternal(voices, this);
				this.out.flush();
				synchronized (Deserializer_0.this.files) {
					Deserializer_0.this.files.add(outFile.toFile());
				}
				this.io.close(this.out);
			} catch (final InterruptedException e) {
				this.io.handleException(ExceptionHandle.SUPPRESS, e);
			}

			// Path outFile;
			// song.writeExternals(this);
			// final int id;
			// try {
			// id = getId(song);
			// } catch (final InterruptedException e) {
			// return;
			// }
			//
			// outFile = Deserializer_0.this.idx.getParent().resolve(
			// String.format(format, id));
			// this.out = this.io.openOut(outFile.toFile());
			// boolean success = false;
			// try {
			// writeMod(mod);
			// writeIntRE(Deserializer_0.this.idMapOut.get(song));
			// SongData.writeExternal(voices, this);
			// success = true;
			// } finally {
			// this.io.close(this.out);
			// this.out = null;
			// if (!success) {
			// outFile.delete();
			// }
			// }
		}

		@Override
		public final void write(final String value) throws IOException {
			write(value.getBytes(FileSystem.UTF8));
			write(0);
		}

		@Override
		public final void writeBoolean(boolean v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeByte(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeBytes(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeChar(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeChars(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeDouble(double v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeFloat(float v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeInt(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeLong(long v) throws IOException {
			final byte[] bytes = new byte[Long.BYTES];
			for (int i = bytes.length - 1; i >= 0; --i) {
				bytes[i] = (byte) v;
				v >>= 8;
			}
			write(bytes);
		}

		@Override
		public final void writeObject(final Object obj) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeShort(int v) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public final void writeSize(int size) throws IOException {
			int lowByte = size & 0x7f;
			final int highBytes = size & ~0x7f;
			if (highBytes != 0) {
				lowByte |= 0x80;
				writeSize(size >> 7);
			}
			write(lowByte);
		}

		@Override
		public final void writeUTF(final String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		private byte[] writeIntRE(int i) {
			final int mask = 0x7f;
			int hByte = 0;
			int bytesEncoded = 1;
			int j = i >> 7;
			while (j != 0) {
				if (++bytesEncoded == 4) {
					break;
				}
				j >>= 7;
			}
			j = i;
			final byte[] bytes = new byte[bytesEncoded];
			while (--bytesEncoded >= 0) {
				bytes[bytesEncoded] = (byte) (hByte | (mask & j));
				j >>= 7;
				hByte = 0x80;
			}
			return bytes;
		}

		private void writeMod(long mod) throws IOException {
			final byte[] bytes = new byte[MOD_LEN];
			for (int i = bytes.length - 1; i >= 0; --i) {
				bytes[i] = (byte) mod;
				mod >>= 8;
			}
			write(bytes);
		}
	}

	private final AtomicInteger id = new AtomicInteger();
	private final static String format = "%05d";
	private final BlockingQueue<DeserialTask> streams = new LinkedBlockingQueue<>();

	private final MasterThread master;

	private final Map<Path, Integer> idMapOut = new HashMap<>();
	private final Map<Integer, Path> idMapIn = new HashMap<>();

	private final Path pathIdMap;
	private final AtomicInteger pathIdNextOut = new AtomicInteger();
	private final OutputStream pathIdOut;
	private final ArrayDeque<File> files = new ArrayDeque<>();

	private final static int MOD_LEN = 6;

	protected Deserializer_0(@SuppressWarnings("hiding") final SongData sdc,
			@SuppressWarnings("hiding") final MasterThread master) {
		super(sdc);
		this.master = master;
		if (!this.idx.getParent().exists())
			this.idx.getParent().toFile().mkdirs();
		this.pathIdMap = this.idx.getParent().resolve("path.map");
		this.pathIdOut = this.io.openOut(this.pathIdMap.toFile());
	}

	@Override
	public final SerializeConainer createSerializeConainer() {
		return new ByteStreamOut();
	}

	@Override
	public Runnable getDeserialTask() {
		this.io.startProgress("Looking up results from last run", -1);
		return new ByteStreamIn(this.idx);

	}

	private final void clear(File[] files) {
		final Path idxDir = this.idx.getParent();
		for (final File file : files) {
			file.delete();
		}
		this.pathIdMap.delete();
		idxDir.resolve(VERSION_ID_FILE).delete();
	}

	@Override
	protected final void abort_() {
		final File versionFile = this.idx.getParent().resolve(VERSION_ID_FILE).toFile();
		final OutputStream out = this.io.openOut(versionFile);
		this.io.write(out, (byte) 0);
		this.io.close(out);
		this.files.add(versionFile);
		this.files.add(this.idx.toFile());
		final File[] files = this.files.toArray(new File[this.files.size()]);
		clear(files);
	}

	@Override
	protected final void crawlDone_() {
		return;
	}

	@Override
	protected void deserialize_() throws IOException {
		// nothing to do
	}

	@Override
	protected final void finish_() {
		final File versionFile = this.idx.getParent().resolve(VERSION_ID_FILE).toFile();
		final OutputStream out = this.io.openOut(versionFile);
		this.io.write(out, (byte) 0);
		this.io.close(out);
		this.files.add(versionFile);
		this.files.add(this.pathIdMap.toFile());
		final File[] files = this.files.toArray(new File[this.files.size()]);
		this.io.compress(this.idx.toFile(), files);
		clear(files);
	}

	@Override
	protected final void generateStream(final SongDataEntry data) throws IOException {
		data.serialize(this);
	}
}
