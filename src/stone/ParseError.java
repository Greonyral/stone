package stone;

/**
 * 
 * @author Nelphindal
 *
 */
public class ParseError {

	private final int lineN;
	private final String lineS;
	private final String lineSection;
	
	/** 
	 * creates a new instance
	 * @param lineN line number causing the error
	 * @param lineS string being parsed
	 * @param lineSection section while parsing
	 */
	@SuppressWarnings("hiding")
	public
	ParseError(int lineN, String lineS, String lineSection) {
		this.lineN = lineN;
		this.lineS = lineS;
		this.lineSection = lineSection == null ? "" : (" in section "  + lineSection);
	}

	@Override
	public final String toString() {
		return "Error parsing " + stone.modules.Main.homeSetting+ "\nIn line" + lineN +":\n\"" + lineS + "\"" + lineSection; 
	}
}
