package stone.modules.abcCreator;

import java.io.IOException;
import java.io.InputStream;


/**
 * Helper class for AbcCreator.call()
 * 
 * @author Nelphindal
 */
public class StreamPrinter implements Runnable {

	/** Stream to read from */
	protected final InputStream stream;
	/** String builder to fill */
	protected final StringBuilder builder;
	private final boolean stdErr;

	/**
	 * @param stream
	 *            the InputStream to print
	 * @param builder
	 *            the StringBuilder to put the input into
	 * @param stdErr
	 *            <i>true</i> if the output shall be printed to stdout
	 */
	public StreamPrinter(final InputStream stream,
			final StringBuilder builder, boolean stdErr) {
		this.stream = stream;
		this.builder = builder;
		this.stdErr = stdErr;
	}

	/**
	 * Reads all symbols from stream until the stream is closed and prints them
	 * on the screen
	 */
	@Override
	public final void run() {
		do {
			int read;
			try {
				read = stream.read();
			} catch (final IOException e) {
				e.printStackTrace();
				return;
			}
			if (read < 0) {
				return;
			}
			builder.append((char) read);
			if (read == '\n') {
				action();
				builder.setLength(0);
			}
		} while (true);
	}

	protected void action() {
		(stdErr ? System.err : System.out).print(builder.toString());
	}

}
