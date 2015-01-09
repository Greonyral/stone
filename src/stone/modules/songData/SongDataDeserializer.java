package stone.modules.songData;

import java.io.IOException;
import java.util.ArrayDeque;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.util.Path;

abstract class SongDataDeserializer {
	protected final Path root, idx;
	protected final IOHandler io;

	private final ArrayDeque<ModEntry> queue = new ArrayDeque<>();

	protected SongDataDeserializer(final Path root, final IOHandler io) {
		this.root = root;
		this.io = io;
		this.idx = getIdx(root);
	}

	public final static SongDataDeserializer init(final Path root,
			final IOHandler io) {
		final Path idx = getIdx(root);
		final InputStream in = io.openIn(idx.toFile());
		final SongDataDeserializer instance;
		try {
			int version = in.read();
			switch (version) {
			case 3:
				instance = new SongDataDeserializer_3(root, io);
				break;
			case 0:
				instance = new SongDataDeserializer_0(root, io);
				break;
			default:
				return null;
			}
			return instance;
		} catch (final IOException e) {
			return null;
		} finally {
			io.close(in);
		}
	}

	private static final Path getIdx(final Path root) {
		final Path idx = root.resolve("SongbookUpdateData.idx");

		final Path idxOld = root.resolve("SongbookUpdateData.zip");

		if (idxOld.exists())
			idxOld.renameTo(idx);
		return idx;
	}

	public abstract void deserialize(final SongDataContainer sdc)
			throws IOException;

	public final void serialize(final SongData data) {
		generateStream(data);
	}

	protected abstract void generateStream(final SongData data);

	public final Path getRoot() {
		return root;
	}

	public abstract void abort();

	public abstract void finish();
	
	public final IOHandler getIO() {
		return io;
	}

	public final ModEntry pollFromQueue() {
		return queue.remove();
	}

	public final boolean queueIsEmpty() {
		return queue.isEmpty();
	}

	public final void addToQueue(final ModEntry song) {
		queue.add(song);
	}

}
