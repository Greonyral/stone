package stone.modules.songData;

import java.io.IOException;
import java.util.ArrayDeque;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.util.Path;

abstract class SongDataDeserializer {
	protected final Path root, idx;
	protected final IOHandler io;
	protected final SongDataContainer sdc;

	private final ArrayDeque<ModEntry> queue = new ArrayDeque<>();
	private int songsFound = 0, songsParsed = 0;
	private boolean crawlDone = false, deserialDone = false;

	protected SongDataDeserializer(final SongDataContainer sdc) {
		this.sdc = sdc;
		this.root = sdc.getRoot();
		this.io = sdc.getIOHandler();
		this.idx = getIdx(root);
	}

	public final static SongDataDeserializer init(final SongDataContainer sdc) {
		final Path idx = getIdx(sdc.getRoot());
		final IOHandler io = sdc.getIOHandler();
		final InputStream in = io.openIn(idx.toFile());
		final SongDataDeserializer instance;
		try {
			int version = in.read();
			switch (version) {
			case 3:
				instance = new SongDataDeserializer_3(sdc);
				// TODO replace as soon decoder 0 is done
				// {
				// private final java.util.concurrent.atomic.AtomicInteger id =
				// new java.util.concurrent.atomic.AtomicInteger(
				// -1);
				//
				// @Override
				// protected void deserialize_() throws IOException {
				// final int id = this.id.incrementAndGet();
				// if (id == 0)
				// new SongDataDeserializer_0(sdc).deserialize();
				// }
				// };
				break;
			case 0:
				instance = new SongDataDeserializer_0(sdc);
				break;
			case -1: // first run
			default:
				instance = new SongDataDeserializer_3(sdc);
			}
			return instance;
		} catch (final IOException e) {
			return null;
		} finally {
			io.close(in);
		}
	}

	private static final Path getIdx(final Path root) {
		final Path idx = root.resolve("..", "PluginData",
				"SongbookUpdateData.idx");

		final Path idxOld = root.resolve("..", "PluginData",
				"SongbookUpdateData.zip");

		if (idxOld.exists())
			idxOld.renameTo(idx);
		return idx;
	}

	protected abstract void abort_();

	protected abstract void deserialize_() throws IOException;

	protected abstract void crawlDone_();

	protected abstract void finish_();

	protected abstract void generateStream(final SongData data);

	public final void abort() {
		abort_();
		io.endProgress();
	}

	public final void crawlDone() {
		if (crawlDone)
			return;
		crawlDone_();
		crawlDone = true;
		notifyAll();
		if (deserialDone) {
			io.startProgress("Parsing songs", songsFound);
			io.updateProgress(songsParsed);
		}
	}

	public final void deserialize() throws IOException {
		deserialize_();
		synchronized (this) {
			deserialDone = true;
			notifyAll();
//			System.err
//					.printf("\n\n==========\nDeserial completed %f parsed\n\n==========\n\n",
//							songsParsed / 1196.0);
			if (crawlDone) {
				io.startProgress("Parsing songs", songsFound);
				io.updateProgress(songsParsed);
			} else {
				io.startProgress("Searching for songs", -1);
			}
		}
	}

	public final void finish() {
		finish_();
		io.endProgress();
	}

	public final IOHandler getIO() {
		return io;
	}

	public final void addToQueue(final ModEntry song) {
		queue.add(song);
		notifyAll();
		++songsFound;
	}

	public final Path getRoot() {
		return root;
	}

	public final ModEntry pollFromQueue() {
		while (queue.isEmpty()) {
			if (crawlDone)
				return null;
			try {
				wait();
			} catch (final InterruptedException e) {
				return null;
			}
		}
		return queue.remove();
	}

	public final void serialize(final SongData data) {
		generateStream(data);
		synchronized (this) {
			++songsParsed;
			if (crawlDone && deserialDone) {
				io.updateProgress();

			} else if (crawlDone && songsParsed == songsFound) {
				// Deserial can be aborted - Parse completed
				// in test at ~ 7% of deserial with method 3
				// TODO
			}
		}
	}

	public final boolean queueIsEmpty() {
		return queue.isEmpty();
	}

	public final int songsFound() {
		return songsFound;
	}

	public Runnable getDeserialTask() {
		return null;
	}

}
