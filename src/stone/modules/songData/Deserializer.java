package stone.modules.songData;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import stone.MasterThread;
import stone.io.AbstractInputStream;
import stone.io.IOHandler;
import stone.modules.SongData;
import stone.util.Debug;
import stone.util.Path;

/**
 * The data read from previous runs is stored in an archice. This class helps to
 * retrieve the stored information
 * 
 * @author Nelphindal
 * 
 */
abstract public class Deserializer {
	/**
	 * File to give the hint of used version
	 */
	protected static final String VERSION_ID_FILE = "sdd";

	/**
	 * @param sdc
	 *            container to call back to register read data
	 * @param master
	 *            -
	 * @return created instance
	 */
	public final static Deserializer init(final SongData sdc,
			final MasterThread master) {
		final Path idx = getIdx(sdc.getRoot());
		final IOHandler io = sdc.getIOHandler();
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
				instance = new Deserializer_3(sdc) {
					private final java.util.concurrent.atomic.AtomicInteger id = new java.util.concurrent.atomic.AtomicInteger(
							-1);

					@Override
					public final Runnable getDeserialTask() {
						return null;

					}

					@Override
					protected final void deserialize_() throws IOException {
						final int id = this.id.incrementAndGet();
						if (id == 0) {
							final Deserializer sdd = new Deserializer_3(
									this.sdc);
							sdd.deserialize();
							sdd.abort_();
						}
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

		if (idxOld.exists()) {
			idxOld.renameTo(idx);
		}
		return idx;
	}

	/** root of {@link #sdc} */
	protected final Path root;
	/** file containing the information to read */
	protected final Path idx;

	/** {@link IOHandler} to use to open streams */
	protected final IOHandler io;
	/** container to call back to register read data */
	protected final SongData sdc;

	private final ArrayDeque<ModEntry> queue = new ArrayDeque<>();
	private final AtomicInteger songsFound = new AtomicInteger();

	private final AtomicInteger songsParsed = new AtomicInteger();

	private boolean crawlDone = false, deserialDone = false;

	/**
	 * Creates a new instance
	 * 
	 * @param sdc
	 *            container to call back to register read data
	 */
	protected Deserializer(@SuppressWarnings("hiding") final SongData sdc) {
		this.sdc = sdc;
		this.root = sdc.getRoot();
		this.io = sdc.getIOHandler();
		this.idx = getIdx(this.root);
	}

	/**
	 * Terminates all processes invoked by {@link #deserialize()}
	 */
	public final void abort() {
		abort_();
		this.io.endProgress("aborted");
	}

	/**
	 * Informs that a song with given properties have been found and the related
	 * data of the deserialization process may be kept
	 * 
	 * @param song
	 *            a found song
	 */
	public final void addToQueue(final ModEntry song) {
		this.queue.add(song);
		notifyAll();
		this.songsFound.incrementAndGet();
	}

	/**
	 * Informs that no more calls of {@link #addToQueue(ModEntry)} will happen
	 */
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

	/**
	 * Reads completely compressed files to retrieve saved data from previous
	 * run
	 * 
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	public final void deserialize() throws IOException {
		deserialize_();
		synchronized (this) {
			this.deserialDone = true;
			notifyAll();
		}
		Debug.print(
				"==========\nDeserial completed %d parsed\n\n==========\n\n",
				this.songsParsed.get());
		if (this.crawlDone) {
			this.io.startProgress("Parsing songs", this.songsFound.get());
			this.io.updateProgress(this.songsParsed.get());
		} else {
			this.io.startProgress("Searching for songs", -1);
		}
	}

	/**
	 * Informs that reading part of {@link #deserialize()} is done
	 */
	public final void finish() {
		finish_();
		this.io.endProgress("Reading finished");
	}

	/**
	 * 
	 * Implementing classes have to implement this method. A task shall be
	 * returned which can implement {@link DeserializeContainer}
	 * 
	 * @return a task which {@link Runnable#run()} will deserialize on part of
	 *         outstanding work
	 */
	public abstract Runnable getDeserialTask();

	/**
	 * This method is identical to calling {@link SongData#getIOHandler} upon
	 * {@link #sdc}
	 * 
	 * @return {@link IOHandler} to use
	 */
	public final IOHandler getIO() {
		return this.io;
	}

	/**
	 * This method is identical to calling {@link SongData#getRoot} upon
	 * {@link #sdc}
	 * 
	 * @return the base of relative paths of used {@link SongData}
	 */
	public final Path getRoot() {
		return this.root;
	}

	/**
	 * Gets next element to work on
	 * 
	 * @return element to work on
	 */
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

	/**
	 * Checks if queue filled by {@link #addToQueue(ModEntry)} and depleted by
	 * {@link #pollFromQueue()} is empty. It will stay empty if
	 * {@link #crawlDone()} has been invoked
	 * 
	 * @return <i>true</i> if underlying queue is empty
	 */
	public final boolean queueIsEmpty() {
		return this.queue.isEmpty();
	}

	/**
	 * Converts given data to a stream of bytes to retrieve same information on
	 * next run
	 * 
	 * @param data
	 *            informatin to save
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	public final void serialize(final SongDataEntry data) throws IOException {
		generateStream(data);
		this.songsParsed.incrementAndGet();
		boolean update = false;
		synchronized (this) {
			if (this.crawlDone && this.deserialDone) {
				update = true;

			}
		}
		if (this.crawlDone && (this.songsParsed.get() == this.songsFound.get())) {
			return;
		}
		if (update) {
			this.io.updateProgress();
		}
	}

	/**
	 * 
	 * @return number of entries found
	 */
	public final int songsFound() {
		return this.songsFound.get();
	}

	/**
	 * Implements {@link #abort()}
	 */
	protected abstract void abort_();

	/**
	 * Implements {@link #crawlDone()}
	 */
	protected abstract void crawlDone_();

	/**
	 * Reads completely compressed files to retrieve saved data from previous
	 * run
	 * 
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	protected abstract void deserialize_() throws IOException;

	/**
	 * Informs that reading part of {@link #deserialize()} is done
	 */
	protected abstract void finish_();

	/**
	 * Implements {@link #serialize(SongDataEntry)}
	 * 
	 * @param data
	 *            information to store
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	protected abstract void generateStream(final SongDataEntry data)
			throws IOException;

}
