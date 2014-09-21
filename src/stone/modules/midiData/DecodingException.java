package stone.modules.midiData;

/**
 * Exception indicating that an error occurred while decoding previously parsed
 * midi-events
 * 
 * @author Nelphindal
 */
abstract class DecodingException extends Exception {

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@Override
	public abstract String toString();
}