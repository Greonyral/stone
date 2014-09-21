package stone.modules.songData;

import java.util.HashMap;
import java.util.Map;

import stone.util.Path;


abstract class AbtractEoWInAbc {

	final Path song;
	final int line;
	final static Map<Path, AbtractEoWInAbc> messages = new HashMap<>();

	AbtractEoWInAbc(final Path song, int line) {
		this.song = song;
		this.line = line;
		synchronized (AbtractEoWInAbc.messages) {
			AbtractEoWInAbc.messages.put(song, this);
		}
	}

	abstract String getDetail();

	abstract WarnOrErrorInAbc getType();

	String printMessage() {
		return getType().toString() + " " + line + ":\n" + getDetail()
				+ "\n" + song + "\n";
	}
}
