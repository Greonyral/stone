package stone.modules.songData;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import stone.MasterThread;
import stone.io.AbstractInputStream;
import stone.io.IOHandler;
import stone.util.Path;

abstract class Deserializer {
	protected static final String VERSION_ID_FILE = "sdd";

	protected final Path root, idx;
	protected final IOHandler io;
	protected final SongDataContainer sdc;

	private final ArrayDeque<ModEntry> queue = new ArrayDeque<>();
	private final AtomicInteger songsFound = new AtomicInteger();
	private final AtomicInteger songsParsed = new AtomicInteger();
	private boolean crawlDone = false, deserialDone = false;

	protected Deserializer(final SongDataContainer sdc) {
		this.sdc = sdc;
		this.root = sdc.getRoot();
		this.io = sdc.getIOHandler();
		this.idx = getIdx(root);
	}

	public final static Deserializer init(final SongDataContainer sdc,
			final MasterThread master) {
		final Path idx = getIdx(sdc.getRoot());
		final IOHandler io = sdc.getIOHandler();
		// TODO dont open all streams
		final Map<String, AbstractInputStream> zipEntriesMap = io
				.openInZip(idx);
		final AbstractInputStream in;
		final Deserializer instance;
		if (zipEntriesMap == null)
			in = null;
		else if (zipEntriesMap.size() == 1)
			in = zipEntriesMap.values().iterator().next();
		else
			in = zipEntriesMap.get(VERSION_ID_FILE);
		try {
			int version = in == null ? -1 : in.read();
			switch (version) {
			case 3:
				instance = new Deserializer_0(sdc, master)
				// TODO replace as soon decoder 0 is done
				{
					private final java.util.concurrent.atomic.AtomicInteger id = new java.util.concurrent.atomic.AtomicInteger(
							-1);

					@Override
					protected final void deserialize_() throws IOException {
						final int id = this.id.incrementAndGet();
						if (id == 0) {
							final Deserializer sdd = new Deserializer_3(sdc);
							sdd.deserialize();
							sdd.abort_();
						}
					}

					@Override
					public final Runnable getDeserialTask() {
						return null;

					}
				};
				break;
			case 0:
				instance = new Deserializer_0(sdc, master);
				break;
			case -1: // first run
			default:
				instance = new Deserializer_0(sdc, master);
			}
			return instance;
		} catch (final IOException e) {
			return null;
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

	protected abstract void generateStream(final SongData data)
			throws IOException;

	public final void abort() {
		abort_();
		io.endProgress();
	}

	public final synchronized void crawlDone() {
		if (crawlDone)
			return;
		crawlDone_();
		crawlDone = true;
		notifyAll();
		if (deserialDone) {
			io.startProgress("Parsing songs", songsFound.get());
			io.updateProgress(songsParsed.get());
		}
	}

	public final void deserialize() throws IOException {
		deserialize_();
		synchronized (this) {
			deserialDone = true;
			notifyAll();
		}
		// System.err
		// .printf("\n\n==========\nDeserial completed %f parsed\n\n==========\n\n",
		// songsParsed / 1196.0);
		if (crawlDone) {
			io.startProgress("Parsing songs", songsFound.get());
			io.updateProgress(songsParsed.get());
		} else {
			io.startProgress("Searching for songs", -1);
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
		songsFound.incrementAndGet();
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

	public final void serialize(final SongData data) throws IOException {
		generateStream(data);
		songsParsed.incrementAndGet();
		boolean update = false;
		synchronized (this) {
			if (crawlDone && deserialDone) {
				update = true;

			}
		}
		// if (crawlDone && songsParsed.get() == songsFound.get()) {
		// Deserial can be aborted - Parse completed
		// in test at ~ 7% of deserial with method 3
		// TODO
		// }
		if (update)
			io.updateProgress();
	}

	public final boolean queueIsEmpty() {
		return queue.isEmpty();
	}

	public final int songsFound() {
		return songsFound.get();
	}

	public Runnable getDeserialTask() {
		return null;
	}

}
