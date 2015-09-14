package stone.modules.songData;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import stone.util.Path;


/**
 * Holding all relevant data describing a song
 */
public class SongDataEntry {

	final static SongDataEntry create(final ModEntry song,
			final Map<String, String> voices) {
		final TreeMap<Integer, String> voicesMap = new TreeMap<>();
		for (final Map.Entry<String, String> v : voices.entrySet()) {
			voicesMap.put(Integer.parseInt(v.getKey()), v.getValue());
		}
		return new SongDataEntry(song.getKey(), voicesMap, song.getValue());
	}


	private final TreeMap<Integer, String> sortedVoices;
	private final Path song;

	private long mod;

	@SuppressWarnings("hiding")
	SongDataEntry(final Path song, final Map<Integer, String> voices, long mod) {
		this(song, new TreeMap<>(voices), mod);
	}

	@SuppressWarnings("hiding")
	SongDataEntry(final Path song, final TreeMap<Integer, String> voices,
			long mod) {
		this.song = song;
		this.sortedVoices = voices;
		this.mod = mod;
	}

	/**
	 * @return the time-stamp of the file representing the date (in
	 *         milliseconds) of last modification
	 */
	public final long getLastModification() {
		return this.mod;
	}

	/**
	 * @return the path denoting the file related to <i>this</i>
	 */
	public final Path getPath() {
		return this.song;
	}

	/**
	 * 
	 * @param sdd
	 *            the encoder to use
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	public void serialize(final MTDeserializer sdd) throws IOException {
		final SerializeConainer sc = sdd.createSerializeConainer();
		sc.write(this.song, this.mod, this.sortedVoices);
	}

	/**
	 * Encodes this entry to a format use for the Songbook plugin by Chiran
	 * 
	 * @return string usable in SongbookData.plugindata for the Songbook plugin
	 *         by Chiran
	 */
	public final String toPluginData() {
		int voiceIdx = 0;
		final StringBuilder sb = new StringBuilder();
		sb.append("\t\t\t{\r\n");
		if (this.sortedVoices.isEmpty()) {
			// no X:-line
			sb.append("\t\t\t\t[");
			sb.append(String.valueOf(++voiceIdx));
			sb.append("] =\r\n\t\t\t\t{\r\n");
			sb.append("\t\t\t\t\t[\"Id\"] = \"");
			sb.append(String.valueOf(1));
			sb.append("\",\r\n\t\t\t\t\t[\"Name\"] = \"");
			sb.append("\"\r\n");
		} else {
			for (final Entry<Integer, String> voice : this.sortedVoices
					.entrySet()) {
				if (voiceIdx > 0) {
					sb.append("\t\t\t\t},\r\n");
				}
				sb.append("\t\t\t\t[");
				sb.append(String.valueOf(++voiceIdx));
				sb.append("] =\r\n\t\t\t\t{\r\n");
				sb.append("\t\t\t\t\t[\"Id\"] = \"");
				sb.append(voice.getKey().toString());
				sb.append("\",\r\n\t\t\t\t\t[\"Name\"] = \"");
				sb.append(voice.getValue());
				sb.append("\"\r\n");
			}
		}
		sb.append("\t\t\t\t}\r\n");
		sb.append("\t\t\t}\r\n");
		return sb.toString();
	}

	/**
	 * @return the voices of this song
	 */
	public final Map<Integer, String> voices() {
		return this.sortedVoices;
	}

	final void setLastModification(final Path file) {
		this.mod = file.toFile().lastModified();
	}
}
