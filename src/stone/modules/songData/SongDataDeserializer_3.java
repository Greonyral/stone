package stone.modules.songData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import stone.io.InputStream;
import stone.util.Path;


final class SongDataDeserializer_3 {

	private final static byte SEPERATOR_3 = 0;
	private final static byte SEPERATOR_EXT_3 = 1;

	private final static Map<Thread, ByteBuffer> bbMap = new HashMap<>();

	private static final boolean readUntilSep(final ByteBuffer nameBuffer,
			final InputStream in) throws IOException {
		nameBuffer.position(0);
		do {
			final int read = in.read();
			if (read == SongDataDeserializer_3.SEPERATOR_3) {
				return false;
			}
			if (read == SongDataDeserializer_3.SEPERATOR_EXT_3) {
				return true;
			}
			nameBuffer.put((byte) read);
		} while (!in.EOFreached());
		throw new IOException("EOF");
	}

	final static void deserialize(final InputStream in,
			final SongDataContainer sdc, final Path base)
			throws IOException {
		final byte[] modField = new byte[Long.SIZE / 8];
		final byte[] intField = new byte[Integer.SIZE / 8];
		final byte[] idxField = new byte[Short.SIZE / 8];
		final ByteBuffer modBuffer = ByteBuffer.wrap(modField);
		final ByteBuffer intBuffer = ByteBuffer.wrap(intField);
		final ByteBuffer idxBuffer = ByteBuffer.wrap(idxField);
		final ByteBuffer nameBuffer = ByteBuffer.allocate(600);
		final DirTree tree = sdc.getDirTree();
		while (!in.EOFreached()) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			final boolean ext =
					SongDataDeserializer_3.readUntilSep(nameBuffer, in);
			final String name =
					new String(nameBuffer.array(), 0, nameBuffer
							.position());

			in.read(modField);
			modBuffer.position(0);
			final long mod;
			final Map<Integer, String> voices;
			if (ext) {
				mod = modBuffer.getLong();
				in.read(intField);
				intBuffer.position(0);
				voices = new HashMap<>(intBuffer.getInt());

				boolean end = false;
				do {
					in.read(intField);
					intBuffer.position(0);
					final int idx = intBuffer.getInt();
					end =
							SongDataDeserializer_3.readUntilSep(
									nameBuffer, in);
					final String desc =
							new String(nameBuffer.array(), 0, nameBuffer
									.position());
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
					in.read(idxField);
					idxBuffer.position(0);
					final int idx = idxBuffer.getShort();
					final String desc =
							new String(
									in.readTo(SongDataDeserializer_3.SEPERATOR_3));
					voices.put(idx, desc);
				}
			}
			final Path p = base.resolve(name.split("/"));
			if (p.exists()) {
				tree.put(new SongData(p, voices, mod));
			}
		}
	}

	final static byte[] getHeader() {
		final byte[] header = new byte[1];
		header[0] = 3;
		return header;
	}

	final static ByteBuffer
			serialize(final SongData song, final Path base) {
		ByteBuffer bb;
		final Thread t = Thread.currentThread();
		if (!SongDataDeserializer_3.bbMap.containsKey(t)) {
			bb = ByteBuffer.allocate(512);
			SongDataDeserializer_3.bbMap.put(t, bb);
		} else {
			bb = SongDataDeserializer_3.bbMap.get(t);
			bb.rewind();
		}

		final Map<Integer, String> voices = song.voices();
		bb.put(song.getPath().relativize(base).getBytes());
		final int pos = bb.position();
		bb.put(SongDataDeserializer_3.SEPERATOR_3);
		bb.putLong(song.getLastModification());
		if (voices.isEmpty()) {
			return bb;
		}
		final int voicesSize = voices.size();
		final int voiceExt = voicesSize >> Short.SIZE;
		boolean extend = voiceExt != 0;
		if (extend) {
			bb.position(pos);
			bb.put(SongDataDeserializer_3.SEPERATOR_EXT_3);
			bb.putInt(voicesSize);
		} else {
			final int posTmp = bb.position();
			bb.position(pos + 1);
			bb.putShort((short) voices.size());
			bb.position(posTmp);
		}

		final ArrayList<byte[]> voicesBytes = new ArrayList<>(voicesSize);
		for (final Map.Entry<Integer, String> voice : voices.entrySet()) {
			final int idx = voice.getKey().intValue();
			final byte[] name = voice.getValue().getBytes();
			final ByteBuffer bbVoice;

			if (!extend) {
				final int extend_idx = idx >> Short.SIZE;
				if (extend_idx != 0) {
					extend = true;
					final ArrayList<byte[]> voicesBytesTmp =
							new ArrayList<>(voicesSize);
					for (final byte[] voiceDone : voicesBytes) {
						final byte[] newByte =
								new byte[(voiceDone.length - (Short.SIZE / 8))
										+ (Integer.SIZE / 8)];
						final ByteBuffer bbDone =
								ByteBuffer.wrap(voiceDone);
						final ByteBuffer bbNew = ByteBuffer.wrap(newByte);
						bbNew.putInt(bbDone.getShort());
						bbNew.put(voiceDone, bbDone.position(), bbDone
								.remaining());
						voicesBytesTmp.add(newByte);
					}
					voicesBytes.clear();
					voicesBytes.addAll(voicesBytesTmp);
					bb.position(pos);
					bb.put(SongDataDeserializer_3.SEPERATOR_EXT_3);
					bb.putShort((short) 0);
					bb.position((bb.position() + (Long.SIZE / 8))
							- (Short.SIZE / 8));
					bb.putInt(voices.size());
				}
			}

			if (extend) {
				bbVoice =
						ByteBuffer.allocate(name.length + 1
								+ (Integer.SIZE / 8));
				bbVoice.putInt(idx);
				bbVoice.put(name);
				bbVoice.put(SongDataDeserializer_3.SEPERATOR_3);
				voicesBytes.add(bbVoice.array());
			} else {
				bbVoice =
						ByteBuffer.allocate(name.length + 1
								+ (Short.SIZE / 8));
				bbVoice.putShort((short) idx);
				bbVoice.put(name);
				bbVoice.put(SongDataDeserializer_3.SEPERATOR_3);
				voicesBytes.add(bbVoice.array());
			}
		}
		for (final byte[] voice : voicesBytes) {
			while (bb.remaining() < voice.length) {
				final byte[] old = bb.array();
				final int lim = bb.position();
				bb = ByteBuffer.allocate(2 * bb.capacity());
				SongDataDeserializer_3.bbMap.put(t, bb);
				bb.put(old, 0, lim);
			}
			bb.put(voice);
		}
		if (extend) {
			bb.position(bb.position() - 1);
			bb.put(SongDataDeserializer_3.SEPERATOR_EXT_3);
		}
		return bb;
	}
}
