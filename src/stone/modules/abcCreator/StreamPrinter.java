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
	@SuppressWarnings("hiding")
	public StreamPrinter(final InputStream stream, final StringBuilder builder,
			boolean stdErr) {
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
				read = this.stream.read();
			} catch (final IOException e) {
				e.printStackTrace();
				return;
			}
			if (read < 0) {
				return;
			}
			this.builder.append((char) read);
			if (read == '\n') {
				action();
				this.builder.setLength(0);
			}
		} while (true);
	}

	/** Prints result to related stdout/stderr */
	protected void action() {
		(this.stdErr ? System.err : System.out).print(this.builder.toString());
	}

}
