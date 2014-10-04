package stone.modules.songData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import stone.MasterThread;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;
import stone.util.Debug;
import stone.util.FileSystem;
import stone.util.Path;


/**
 * Task to extract all needed information for SongbookPlugin for LoTRO
 * 
 * @author Nelphindal
 */
public final class Scanner implements Runnable {

	private static final String clean(final String desc) {
		final StringBuilder result = new StringBuilder();
		int pos = 0;
		for (int i = 0; i < desc.length(); i++) {
			final char c = desc.charAt(i);
			if (c == '\\') {
				result.append(desc.substring(pos, i));
				pos = i += 2;
			} else if (c == '"') {
				result.append(desc.substring(pos, i));
				result.append("\\\"");
				pos = i + 1;
			} else if (((c >= ' ') && (c <= ']' /*
												 * including uppercased chars
												 * and digits
												 */))
					|| ((c >= 'a') && (c <= 'z'))
					|| ((c > (char) 127) && (c < (char) 256))) {
				continue;
			} else {
				result.append(desc.substring(pos, i));
				pos = i + 1;
			}
		}
		result.append(desc.substring(pos));
		return result.toString();
	}

	private final ArrayDeque<ModEntry> queue;

	private final IOHandler io;

	private final DirTree tree;

	private final Set<Path> songsFound;

	private final OutputStream out;

	private final MasterThread master;

	/**
	 * @param io
	 * @param queue
	 * @param master
	 * @param out
	 * @param tree
	 * @param songsFound
	 */
	public Scanner(final IOHandler io, final ArrayDeque<ModEntry> queue,
			final MasterThread master, final OutputStream out,
			final DirTree tree, final Set<Path> songsFound) {
		this.queue = queue;
		this.io = io;
		this.tree = tree;
		this.songsFound = songsFound;
		this.out = out;
		this.master = master;
	}

	/** */
	@Override
	public final void run() {
		while (true) {
			final ModEntry song;
			synchronized (queue) {
				if (queue.isEmpty()) {
					return;
				}
				song = queue.remove();
			}
			if (song == ModEntry.TERMINATE) {
				synchronized (queue) {
					queue.add(song);
					queue.notifyAll();
					return;
				}
			}

			final SongData data = getVoices(song);
			if (data == null) {
				if (master.isInterrupted()) {
					return;
				}
				synchronized (songsFound) {
					songsFound.remove(song.getKey());
				}
			} else {
				final ByteBuffer bytes;
				bytes =
						SongDataDeserializer_3.serialize(data, tree
								.getRoot());
				synchronized (this) {
					io.write(out, bytes.array(), 0, bytes.position());
				}
				tree.put(data);
				if (master.isInterrupted()) {
					return;
				}
			}
			io.updateProgress();
		}
	}

	private final SongData getVoices(final ModEntry song) {
		final SongData songdata = tree.get(song.getKey());
		if ((songdata == null)
				|| (songdata.getLastModification() != song.getValue())) {
			final Path songFile = song.getKey();

			final Map<String, String> voices = new HashMap<>();
			final InputStream songContent =
					io.openIn(songFile.toFile(),
							FileSystem.DEFAULT_CHARSET);

			try {
				// you can expect T: after X: line, if enforcing abc-syntax
				String line;
				try {
					line = songContent.readLine();
				} catch (final IOException e) {
					io.handleException(ExceptionHandle.TERMINATE, e);
					return null;
				}
				boolean error = false;
				int lineNumber = 1;

				while (line != null) {
					// search for important lines
					if (line.startsWith("X:")) {
						final int lineNumberOfX = lineNumber;
						final String voiceId =
								line.substring(line.indexOf(":") + 1)
										.trim();
						try {
							line = songContent.readLine();
							++lineNumber;
						} catch (final IOException e) {
							io.handleException(ExceptionHandle.TERMINATE,
									e);
						}
						if ((line == null) || !line.startsWith("T:")) {
							Debug.print("%s\n", new MissingTLineInAbc(song
									.getKey(), lineNumber));
							error = true;
							if (line == null) {
								break;
							}
						}
						final StringBuilder desc = new StringBuilder();
						do {
							desc.append(line.substring(
									line.indexOf(":") + 1).trim());
							try {
								line = songContent.readLine();
								++lineNumber;
							} catch (final IOException e) {
								io.handleException(
										ExceptionHandle.TERMINATE, e);
							}
							if (line == null) {
								break;
							}
							if (line.startsWith("T:")) {
								desc.append(" ");
								Debug.print("%s\n",
										new MultipleTLinesInAbc(
												lineNumber, song.getKey()));
							} else {
								break;
							}
						} while (true);
						if (desc.length() >= 65) {
							Debug.print("%s\n", new LongTitleInAbc(song
									.getKey(), lineNumberOfX));
						}
						voices.put(voiceId, Scanner.clean(desc.toString()));
						continue;
					} else if (line.startsWith("T:")) {
						Debug.print("%s\n", new NoXLineInAbc(
								song.getKey(), lineNumber));
						error = true;
					}
					try {
						line = songContent.readLine();
						++lineNumber;
					} catch (final IOException e) {
						io.handleException(ExceptionHandle.TERMINATE, e);
					}
				}
				if (error) {
					return null;
				}
				if (voices.isEmpty()) {
					io.printError(String.format("Warning: %-50s %s", song
							.getKey().toString(), "has no voices"), true);
				}
				final SongData sd = SongData.create(song, voices);
				synchronized (song) {
					tree.put(sd);
				}

				return sd;
			} finally {
				io.close(songContent);
			}
		}
		return songdata;
	}
}
