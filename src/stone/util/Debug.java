package stone.util;

import java.io.File;
import java.io.IOException;

import stone.Main;
import stone.StartupContainer;


/**
 * Contains any methods usable for debugging on stdout/stderr.
 * 
 * @author Nelphindal
 * 
 */
public abstract class Debug {

	private static final Debug instance = Flag.getInstance().isEnabled(
			Main.DEBUG_ID) ? new Debug() {


		@Override
		protected final synchronized void printImpl(final String string,
				final Object[] args) {
			System.out.printf(string, args);

		}

	} : new Debug() {

		private final File log = StartupContainer.getDatadirectory()
				.resolve("log.txt").toFile();
		private boolean first = true;


		@Override
		protected final void printImpl(final String string, final Object[] args) {
			if (!string.endsWith("\n")) {
				printImpl(string + "\n", args);
				return;
			}
			final String format = string.replaceAll("\r*\n", FileSystem.getLineSeparator());
			try {	
				final java.io.OutputStream out = new java.io.FileOutputStream(
						log, !first);
				first = false;
				out.write(String.format(format, args).getBytes());
				out.flush();
				out.close();
			} catch (IOException e) {
				return;
			}
		}

	};

	/**
	 * Prints an ouput to stdout if debuging-mode has been enabled.
	 * 
	 * @param string
	 *            A format string as described in Format string syntax.
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string.
	 */
	public final static void print(final String string, final Object... args) {
		Debug.instance.printImpl(string, args);
	}

	/**
	 * The actual implementation of {@link Debug#print(String, Object...)}
	 * 
	 * @param string
	 *            A format string as described in Format string syntax.
	 * 
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string.
	 */
	protected abstract void printImpl(String string, Object[] args);

}
