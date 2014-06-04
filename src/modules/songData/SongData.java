package modules.songData;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.Path;


/**
 * Holding all relevant data describing a song
 */
public class SongData {

	final static SongData create(final ModEntry song,
			final Map<String, String> voices) {
		final TreeMap<Integer, String> voicesMap = new TreeMap<>();
		for (final Map.Entry<String, String> v : voices.entrySet()) {
			voicesMap.put(Integer.parseInt(v.getKey()), v.getValue());
		}
		return new SongData(song.getKey(), voicesMap, song.getValue());
	}

	private final TreeMap<Integer, String> sortedVoices;
	private final Path song;

	private long mod;

	SongData(final Path song, final Map<Integer, String> voices, long mod) {
		this(song, new TreeMap<Integer, String>(voices), mod);
	}

	SongData(final Path song, final TreeMap<Integer, String> voices, long mod) {
		this.song = song;
		sortedVoices = voices;
		this.mod = mod;
	}

	/**
	 * @return the voices of this song
	 */
	public final Map<Integer, String> voices() {
		return sortedVoices;
	}

	final long getLastModification() {
		return mod;
	}

	final Path getPath() {
		return song;
	}

	final void setLastModification(final Path file) {
		mod = file.toFile().lastModified();
	}

	final String toPluginData() {
		int voiceIdx = 0;
		final StringBuilder sb = new StringBuilder();
		sb.append("\t\t\t{\r\n");
		if (sortedVoices.isEmpty()) {
			// no X:-line
			sb.append("\t\t\t\t[");
			sb.append(String.valueOf(++voiceIdx));
			sb.append("] =\r\n\t\t\t\t{\r\n");
			sb.append("\t\t\t\t\t[\"Id\"] = \"");
			sb.append(String.valueOf(1));
			sb.append("\",\r\n\t\t\t\t\t[\"Name\"] = \"");
			sb.append("\"\r\n");
		} else {
			for (final Entry<Integer, String> voice : sortedVoices.entrySet()) {
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
}
