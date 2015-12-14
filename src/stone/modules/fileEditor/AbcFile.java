package stone.modules.fileEditor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import stone.io.IOHandler;
import stone.io.OutputStream;
import stone.util.Path;

class AbcFile {

	private final String header;
	private final Map<Integer, AbcTrack> tracks;

	private AbcFile(final StringBuilder header, final Map<Integer, AbcTrack> tracks) {
		this.header = header.toString();
		this.tracks = new TreeMap<>(tracks);
	}

	public static AbcFile readFile(final InputStream stream, final Console c) {
		AbcFile abcFile = null;
		try {
			abcFile = parseStream(stream, c);
		} catch (IOException e) {
			c.err(e.getMessage());
			return null;
		}
		if (abcFile == null)
			return null;
		return abcFile;
	}

	private static AbcFile parseStream(final InputStream in, final Console c) throws IOException {
		final BufferedReader r = new BufferedReader(new InputStreamReader(in));
		final Map<Integer, AbcTrack> tracks = new HashMap<>();
		final StringBuilder header = new StringBuilder();

		// parse state
		int lineNumber = 0;
		boolean implicitId = false;
		AbcTrack lastTrack = null;

		while (true) {
			String line = r.readLine();
			if (line == null)
				return new AbcFile(header, tracks);
			line = line.trim();
			lineNumber++;
			if (line.length() == 0) {
				continue;
			}
			switch (line.charAt(0)) {
			case '%':
				if (lastTrack == null) {
					header.append(line);
					header.append(stone.util.FileSystem.getLineSeparator());
				} else {
					lastTrack.appendCommentLine(line);
				}
				break;
			case 'X':
				lastTrack = new AbcTrack(line);
				if (tracks.put(lastTrack.getId(), lastTrack) != null) {
					c.err("X: line with duplicate id " + lastTrack.getId() + " in line " + lineNumber + "\n");
					return null;
				}
				break;
			case 'L':
				if (!line.contains(":")) {
					c.err("L line without : in line" + lineNumber + "\n");
					return null;
				}
				if (lastTrack == null) {
					c.err("L: line without preceeding X: line in line " + lineNumber + "\n");
					return null;
				}
				lastTrack.setBase(line.split(":")[1].trim());
				break;
			case 'Q':
				if (!line.contains(":")) {
					c.err("Q line without : in line" + lineNumber + "\n");
					return null;
				}
				if (lastTrack == null) {
					c.err("Q: line without preceeding X: line in line " + lineNumber + "\n");
					return null;
				}
				lastTrack.setSpeed(line.split(":")[1].trim());
				break;
			case 'T':
				if (tracks.isEmpty()) {
					if (implicitId) {
						c.err("T: line without preceeding X: line in line " + lineNumber + "\n");
						return null;
					}
					lastTrack = new AbcTrack("X:1");
					if (tracks.put(Integer.valueOf(1), lastTrack) != null) {
						c.err("T: line without preceeding X: line in line " + lineNumber + "\n");
						return null;
					}
					implicitId = true;
				}
				if (lastTrack == null) {
					c.err("T: line without preceeding X: line in line " + lineNumber + "\n");
					return null;
				}
				lastTrack.appendTitle(line);
				break;
			default:
				if (line.length() > 1 && line.charAt(1) == ':') {
					if (lastTrack != null)
						lastTrack.addMeta(line);
					else {
						header.append(line);
						header.append(stone.util.FileSystem.getLineSeparator());
					}
				} else {
					if (lastTrack == null) {
						c.err("No header line without preceeding X: line in line " + lineNumber + "\n");
						return null;
					}
					lastTrack.appendNoteLine(line);
				}
			}
		}
	}

	/**
	 * Reverses this file. The result will be placed on relative path of root
	 * with suffix _rewritten
	 * 
	 * @param selected
	 *            file from which input has been generated
	 * @param root
	 *            root directory to which to append "_reversed"
	 * @param io 
	 */
	public void reverse(final Path root, final Path selected, final IOHandler io) {
//		pausePad();
		for (final AbcTrack track : tracks.values()) {
			track.reverseNotes();
		}
		
		final String relative = selected.relativize(root);
		final Path rootReversed = root.getParent().resolve(root.getFilename() + "_reversed");
		final Path target = rootReversed.resolve(relative.split("/"));
		
		target.getParent().toFile().mkdirs();
		final OutputStream out = io.openOut(target.toFile());
		io.write(out, header);
		for (final Entry<Integer, AbcTrack> entry : tracks.entrySet()) {
			io.write(out, stone.util.FileSystem.getLineSeparator());
			io.write(out, "X:" + entry.getKey().toString());
			io.write(out, stone.util.FileSystem.getLineSeparator());
			entry.getValue().write(io, out);
		}
		io.close(out);
	}

	/**
	 * Adds pauses to ensure all tracks end at exactly same time
	 */
	private void pausePad() {
		// TODO still buggy
		final Map<Integer, Double> noteDuration = new HashMap<>();
		double max = 0;
		for (final Entry<Integer, AbcTrack> trackEntry : tracks.entrySet()) {
			final AbcTrack track = trackEntry.getValue();
			final Integer id = trackEntry.getKey();
			final Double duration = track.getNoteDuration();
			noteDuration.put(id, duration);
			if (duration.doubleValue() > max) {
				max = duration.doubleValue();
			}
		}
		for (final Entry<Integer, Double> entry : noteDuration.entrySet()) {
			if (entry.getValue().doubleValue() == max)
				continue;
			double missing = max - entry.getValue().doubleValue();
			tracks.get(entry.getKey()).appendPause(missing);
		}
	}
}
