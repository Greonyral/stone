package stone.modules.fileEditor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;



public class AbcFile {

	public static AbcFile parseStream(final InputStream in) throws IOException {
		final BufferedReader r = new BufferedReader(new InputStreamReader(in));
		final Map<Integer, AbcTrack> tracks = new HashMap<>();
		final StringBuilder sb = new StringBuilder();
		
		// parse state
		int id;
		AbcTrack lastTrack = null;
		
		while (true) {
			String line = r.readLine();
			if (line == null)
				return new AbcFile();
			line = line.trim();
			if (line.startsWith("%")) {
				if (lastTrack == null) {
					sb.append(line);
					sb.append("\n");
				} else {
					lastTrack.appendCommentLine(line);
				}
				continue;
			}
			switch (line.charAt(0)) {
				case 'X':
					lastTrack = new AbcTrack(line);
				break;
				case 'T':
					lastTrack.setTitle(line);
					break;
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		
	}
}
