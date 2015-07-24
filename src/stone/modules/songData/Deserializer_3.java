package stone.modules.songData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import stone.io.InputStream;
import stone.io.OutputStream;
import stone.modules.SongData;
import stone.util.Path;

class Deserializer_3 extends Deserializer {

	private final static byte VERSION = 0x3;
	private final static byte SEPERATOR_3 = 0;
	private final static byte SEPERATOR_EXT_3 = 1;

	private final Path tmp;

	private final OutputStream out;
	private final InputStream in;
	private final Path inFile;

	Deserializer_3(@SuppressWarnings("hiding") final SongData sdc) {
		super(sdc);
		final String entry = this.idx.getFilename().replace(".idx", "")
				.replace(".zip", "");
		this.inFile = this.idx.resolve("..", entry);
		this.tmp = this.idx.getParent().resolve(entry + ".tmp");
		final Set<String> zip = this.io.openZipIn(this.idx);
		if ((zip == null) || !zip.contains(entry)) {
			if (this.tmp.exists()) {
				this.tmp.renameTo(this.inFile);
				this.in = this.io.openIn(this.inFile.toFile());
			} else {
				this.in = null;
			}
		} else {
			this.in = this.io.openIn(this.inFile.toFile());
		}
		if (!this.tmp.getParent().exists()) {
			this.out = null;
		} else {
			this.out = this.io.openOut(this.tmp.toFile());
			this.io.write(this.out, VERSION);
		}
	}

	private final void put(final ByteBuffer bb) {
		final byte[] bArray = new byte[bb.position()];
		System.arraycopy(bb.array(), 0, bArray, 0, bArray.length);
		synchronized (this.out) {
			this.io.write(this.out, bArray);
		}
	}

	private final boolean readUntilSep(final ByteBuffer nameBuffer)
			throws IOException {
		nameBuffer.position(0);
		do {
			final int read = this.in.read();
			if (read == Deserializer_3.SEPERATOR_3) {
				return false;
			}
			if (read == Deserializer_3.SEPERATOR_EXT_3) {
				return true;
			}
			nameBuffer.put((byte) read);
		} while (!this.in.EOFreached());
		throw new IOException("EOF");
	}

	@Override
	protected final void abort_() {
		this.io.close(this.out);
		this.io.close(this.in);
		this.idx.delete();
		this.tmp.delete();
	}

	@Override
	protected final void crawlDone_() {
		return;
	}

	@Override
	protected void deserialize_() throws IOException {
		if (this.in == null) {
			return;
		}
		this.io.startProgress("Reading data base of previous run", -1); // init
		this.in.registerProgressMonitor(this.io);
		this.io.setProgressTitle("Reading data base of previous run"); // set
																		// message
		this.in.read(); // version byte

		final byte[] modField = new byte[Long.SIZE / 8];
		final byte[] intField = new byte[Integer.SIZE / 8];
		final byte[] idxField = new byte[Short.SIZE / 8];
		final ByteBuffer modBuffer = ByteBuffer.wrap(modField);
		final ByteBuffer intBuffer = ByteBuffer.wrap(intField);
		final ByteBuffer idxBuffer = ByteBuffer.wrap(idxField);
		final ByteBuffer nameBuffer = ByteBuffer.allocate(600);
		final DirTree tree = this.sdc.getDirTree();
		while (!this.in.EOFreached()) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			final boolean ext = readUntilSep(nameBuffer);
			final String name = new String(nameBuffer.array(), 0,
					nameBuffer.position());

			this.in.read(modField);
			modBuffer.position(0);
			final long mod;
			final Map<Integer, String> voices;
			if (ext) {
				mod = modBuffer.getLong();
				this.in.read(intField);
				intBuffer.position(0);
				voices = new HashMap<>(intBuffer.getInt());

				boolean end = false;
				do {
					this.in.read(intField);
					intBuffer.position(0);
					@SuppressWarnings("hiding")
					final int idx = intBuffer.getInt();
					end = readUntilSep(nameBuffer);
					final String desc = new String(nameBuffer.array(), 0,
							nameBuffer.position());
					voices.put(idx, desc);
				} while (!end);
			} else {
				final int voicesCount;
				voicesCount = modBuffer.getChar();
				modBuffer.position(0);
				modBuffer.putChar((char) 0);
				modBuffer.position(0);
				mod = modBuffer.getLong();
				voices = new HashMap<>(voicesCount);
				for (int i = 0; i < voicesCount; i++) {
					this.in.read(idxField);
					idxBuffer.position(0);
					@SuppressWarnings("hiding")
					final int idx = idxBuffer.getShort();
					final String desc = new String(
							this.in.readTo(Deserializer_3.SEPERATOR_3));
					voices.put(idx, desc);
				}
			}
			final Path p = this.root.resolve(name.split("/"));
			if (p.exists()) {
				tree.put(new SongDataEntry(p, voices, mod));
			}
		}
		this.io.close(this.in);
		this.inFile.delete();
	}

	@Override
	protected final void finish_() {
		this.io.close(this.out);
		this.idx.delete();
		if (this.tmp.exists()) {
			this.tmp.renameTo(this.inFile);
			this.io.compress(this.idx.toFile(), this.inFile.toFile());
			this.tmp.delete();
		}
	}

	@Override
	protected final void generateStream(final SongDataEntry song) {
		ByteBuffer bb = ByteBuffer.allocate(512);

		final Map<Integer, String> voices = song.voices();
		final byte[] path = song.getPath().relativize(this.root).getBytes();

		bb.put(path);
		final int pos = bb.position();
		bb.put(Deserializer_3.SEPERATOR_3);
		bb.putLong(song.getLastModification());
		if (voices.isEmpty()) {
			put(bb);
		}
		final int voicesSize = voices.size();
		final int voiceExt = voicesSize >> Short.SIZE;
		boolean extend = voiceExt != 0;
		if (extend) {
			bb.position(pos);
			bb.put(Deserializer_3.SEPERATOR_EXT_3);
			bb.putInt(voicesSize);
		} else {
			final int posTmp = bb.position();
			bb.position(pos + 1);
			bb.putShort((short) voices.size());
			bb.position(posTmp);
		}

		final ArrayList<byte[]> voicesBytes = new ArrayList<>(voicesSize);
		for (final Map.Entry<Integer, String> voice : voices.entrySet()) {
			@SuppressWarnings("hiding")
			final int idx = voice.getKey().intValue();
			final byte[] name = voice.getValue().getBytes();
			final ByteBuffer bbVoice;

			if (!extend) {
				final int extend_idx = idx >> Short.SIZE;
				if (extend_idx != 0) {
					extend = true;
					final ArrayList<byte[]> voicesBytesTmp = new ArrayList<>(
							voicesSize);
					for (final byte[] voiceDone : voicesBytes) {
						final byte[] newByte = new byte[(voiceDone.length - (Short.SIZE / 8))
								+ (Integer.SIZE / 8)];
						final ByteBuffer bbDone = ByteBuffer.wrap(voiceDone);
						final ByteBuffer bbNew = ByteBuffer.wrap(newByte);
						bbNew.putInt(bbDone.getShort());
						bbNew.put(voiceDone, bbDone.position(),
								bbDone.remaining());
						voicesBytesTmp.add(newByte);
					}
					voicesBytes.clear();
					voicesBytes.addAll(voicesBytesTmp);
					bb.position(pos);
					bb.put(Deserializer_3.SEPERATOR_EXT_3);
					bb.putShort((short) 0);
					bb.position((bb.position() + (Long.SIZE / 8))
							- (Short.SIZE / 8));
					bb.putInt(voices.size());
				}
			}

			if (extend) {
				bbVoice = ByteBuffer.allocate(name.length + 1
						+ (Integer.SIZE / 8));
				bbVoice.putInt(idx);
				bbVoice.put(name);
				bbVoice.put(Deserializer_3.SEPERATOR_3);
				voicesBytes.add(bbVoice.array());
			} else {
				bbVoice = ByteBuffer.allocate(name.length + 1
						+ (Short.SIZE / 8));
				bbVoice.putShort((short) idx);
				bbVoice.put(name);
				bbVoice.put(Deserializer_3.SEPERATOR_3);
				voicesBytes.add(bbVoice.array());
			}
		}
		for (final byte[] voice : voicesBytes) {
			while (bb.remaining() < voice.length) {
				final byte[] old = bb.array();
				final int lim = bb.position();
				bb = ByteBuffer.allocate(2 * bb.capacity());
				bb.put(old, 0, lim);
			}
			bb.put(voice);
		}
		if (extend) {
			bb.position(bb.position() - 1);
			bb.put(Deserializer_3.SEPERATOR_EXT_3);
		}
		put(bb);
	}

	/**
	 * Not supported
	 */
	@Override
	public Runnable getDeserialTask() {
		return null;
	}
}
