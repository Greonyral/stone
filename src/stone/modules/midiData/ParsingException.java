package stone.modules.midiData;

/**
 * Exception indicating that an error occured while parsing selected file and
 * generating the midi-events
 * 
 * @author Nelphindal
 */
abstract class ParsingException extends Exception {

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@Override
	public abstract String toString();
}