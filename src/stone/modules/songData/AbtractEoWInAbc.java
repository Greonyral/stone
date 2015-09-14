package stone.modules.songData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import stone.util.Path;


/**
 * Basic class for indicating problem on parsing ABC-files
 * 
 * @author Nelphindal
 * 
 */
public abstract class AbtractEoWInAbc {

	/**
	 * 
	 * @return a set of all errors
	 */
	public static Collection<AbtractEoWInAbc> getMessages() {
		return messages.values();
	}

	final Path song;
	final int line;

	final static Map<Path, AbtractEoWInAbc> messages = new HashMap<>();

	@SuppressWarnings("hiding")
	AbtractEoWInAbc(final Path song, int line) {
		this.song = song;
		this.line = line;
		synchronized (AbtractEoWInAbc.messages) {
			AbtractEoWInAbc.messages.put(song, this);
		}
	}

	/**
	 * 
	 * @return message describing <i>this</i> error
	 */
	public String printMessage() {
		return getType().toString() + " " + this.line + ":\n" + getDetail()
				+ "\n" + this.song + "\n";
	}

	@Override
	public String toString() {
		return printMessage();
	}


	abstract String getDetail();

	abstract WarnOrErrorInAbc getType();
}
