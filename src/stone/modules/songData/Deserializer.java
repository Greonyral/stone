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

	public final static Deserializer init(final SongDataContainer sdc,
			final MasterThread master) {
		final Path idx = getIdx(sdc.getRoot());
		final IOHandler io = sdc.getIOHandler();
		// TODO dont open all streams
		final Map<String, AbstractInputStream> zipEntriesMap = io
				.openInZip(idx);
		final AbstractInputStream in;
		final Deserializer instance;
		if (zipEntriesMap == null) {
			in = null;
		} else if (zipEntriesMap.size() == 1) {
			in = zipEntriesMap.values().iterator().next();
		} else {
			in = zipEntriesMap.get(VERSION_ID_FILE);
		}
		try {
			final int version = in == null ? -1 : in.read();
			switch (version) {
			case 3:
				instance = new Deserializer_3(sdc);
				/*
				 * // TODO replace as soon decoder 0 is done { private final
				 * java.util.concurrent.atomic.AtomicInteger id = new
				 * java.util.concurrent.atomic.AtomicInteger( -1);
				 * 
				 * @Override protected final void deserialize_() throws
				 * IOException { final int id = this.id.incrementAndGet(); if
				 * (id == 0) { final Deserializer sdd = new Deserializer_3(sdc);
				 * sdd.deserialize(); sdd.abort_(); } }
				 * 
				 * @Override public final Runnable getDeserialTask() { return
				 * null;
				 * 
				 * } };
				 */
				break;
			case 0:
				instance = new Deserializer_0(sdc, master);
				break;
			case -1: // first run
			default:
				instance = new Deserializer_3(sdc);
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

		if (idxOld.exists()) {
			idxOld.renameTo(idx);
		}
		return idx;
	}

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
		this.idx = getIdx(this.root);
	}

	public final void abort() {
		abort_();
		this.io.endProgress();
	}

	public final void addToQueue(final ModEntry song) {
		this.queue.add(song);
		notifyAll();
		this.songsFound.incrementAndGet();
	}

	public final synchronized void crawlDone() {
		if (this.crawlDone) {
			return;
		}
		crawlDone_();
		this.crawlDone = true;
		notifyAll();
		if (this.deserialDone) {
			this.io.startProgress("Parsing songs", this.songsFound.get());
			this.io.updateProgress(this.songsParsed.get());
		}
	}

	public final void deserialize() throws IOException {
		deserialize_();
		synchronized (this) {
			this.deserialDone = true;
			notifyAll();
		}
		// System.err
		// .printf("\n\n==========\nDeserial completed %f parsed\n\n==========\n\n",
		// songsParsed / 1196.0);
		if (this.crawlDone) {
			this.io.startProgress("Parsing songs", this.songsFound.get());
			this.io.updateProgress(this.songsParsed.get());
		} else {
			this.io.startProgress("Searching for songs", -1);
		}
	}

	public final void finish() {
		finish_();
		this.io.endProgress();
	}

	public Runnable getDeserialTask() {
		return null;
	}

	public final IOHandler getIO() {
		return this.io;
	}

	public final Path getRoot() {
		return this.root;
	}

	public final ModEntry pollFromQueue() {
		while (this.queue.isEmpty()) {
			if (this.crawlDone) {
				return null;
			}
			try {
				wait();
			} catch (final InterruptedException e) {
				return null;
			}
		}
		return this.queue.remove();
	}

	public final boolean queueIsEmpty() {
		return this.queue.isEmpty();
	}

	public final void serialize(final SongData data) throws IOException {
		generateStream(data);
		this.songsParsed.incrementAndGet();
		boolean update = false;
		synchronized (this) {
			if (this.crawlDone && this.deserialDone) {
				update = true;

			}
		}
		// if (crawlDone && songsParsed.get() == songsFound.get()) {
		// Deserial can be aborted - Parse completed
		// in test at ~ 7% of deserial with method 3
		// TODO
		// }
		if (update) {
			this.io.updateProgress();
		}
	}

	public final int songsFound() {
		return this.songsFound.get();
	}

	protected abstract void abort_();

	protected abstract void crawlDone_();

	protected abstract void deserialize_() throws IOException;

	protected abstract void finish_();

	protected abstract void generateStream(final SongData data)
			throws IOException;

}
