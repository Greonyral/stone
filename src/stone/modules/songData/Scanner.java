package stone.modules.songData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import stone.MasterThread;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
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

	private final IOHandler io;

	private final DirTree tree;

	private final MasterThread master;

	private final Deserializer sdd;

	/**
	 * @param master -
	 * @param sdd -
	 * @param tree -
	 */
	@SuppressWarnings("hiding")
	public Scanner(MasterThread master, final Deserializer sdd,
			final DirTree tree) {
		this.io = sdd.getIO();
		this.tree = tree;
		this.master = master;
		this.sdd = sdd;
	}

	/** */
	@Override
	public final void run() {
		while (!this.master.isInterrupted() && parseSongs()) {
			;
		}
	}

	private final SongDataEntry getVoices(final ModEntry song) {
		final SongDataEntry songdata = this.tree.get(song.getKey());
		if ((songdata == null)
				|| (songdata.getLastModification() != song.getValue())) {
			final Path songFile = song.getKey();

			final Map<String, String> voices = new HashMap<>();
			final InputStream songContent = this.io.openIn(songFile.toFile(),
					FileSystem.DEFAULT_CHARSET);

			try {
				// you can expect T: after X: line, if enforcing abc-syntax
				String line;
				try {
					line = songContent.readLine();
				} catch (final IOException e) {
					this.io.handleException(ExceptionHandle.TERMINATE, e);
					return null;
				}
				boolean error = false;
				int lineNumber = 1;

				while (line != null) {
					// search for important lines
					if (line.startsWith("X:")) {
						final int lineNumberOfX = lineNumber;
						final String voiceId = line.substring(
								line.indexOf(":") + 1).trim();
						try {
							line = songContent.readLine();
							++lineNumber;
						} catch (final IOException e) {
							this.io.handleException(ExceptionHandle.TERMINATE,
									e);
						}
						if ((line == null) || !line.startsWith("T:")) {
							Debug.print("%s\n",
									new MissingTLineInAbc(song.getKey(),
											lineNumber));
							error = true;
							if (line == null) {
								break;
							}
						}
						final StringBuilder desc = new StringBuilder();
						do {
							desc.append(line.substring(line.indexOf(":") + 1)
									.trim());
							try {
								line = songContent.readLine();
								++lineNumber;
							} catch (final IOException e) {
								this.io.handleException(
										ExceptionHandle.TERMINATE, e);
							}
							if (line == null) {
								break;
							}
							if (line.startsWith("T:")) {
								desc.append(" ");
								Debug.print("%s\n", new MultipleTLinesInAbc(
										lineNumber, song.getKey()));
							} else {
								break;
							}
						} while (true);
						if (desc.length() >= 65) {
							Debug.print("%s\n",
									new LongTitleInAbc(song.getKey(),
											lineNumberOfX));
						}
						voices.put(voiceId, Scanner.clean(desc.toString()));
						continue;
					} else if (line.startsWith("T:")) {
						Debug.print("%s\n", new NoXLineInAbc(song.getKey(),
								lineNumber));
						error = true;
					}
					try {
						line = songContent.readLine();
						++lineNumber;
					} catch (final IOException e) {
						this.io.handleException(ExceptionHandle.TERMINATE, e);
					}
				}
				if (error) {
					return null;
				}
				if (voices.isEmpty()) {
					this.io.printError(String.format("Warning: %-50s %s", song
							.getKey().toString(), "has no voices"), true);
				}
				final SongDataEntry sd = SongDataEntry.create(song, voices);
				synchronized (song) {
					this.tree.put(sd);
				}

				return sd;
			} finally {
				this.io.close(songContent);
			}
		}
		return songdata;
	}

	private final boolean parseSongs() {
		final ModEntry song;
		synchronized (this.sdd) {
			song = this.sdd.pollFromQueue();
		}
		if (song == null) {
			return false;
		}
		final SongDataEntry voices = getVoices(song);
		try {
			this.sdd.serialize(voices);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return true;
	}
}
