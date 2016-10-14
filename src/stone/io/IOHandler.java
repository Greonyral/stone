package stone.io;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import stone.MasterThread;
import stone.StartupContainer;
import stone.util.Debug;
import stone.util.FileSystem;
import stone.util.Option;
import stone.util.Path;

/**
 * Central class handling I/O-Operations
 * 
 * @author Nelphindal
 */
public class IOHandler {

	private static final String oracleJavaDownloadURL = "https://www.java.com/en/download/manual.jsp";
	private static final String oracleJRE = "Java(TM) SE Runtime Environment";
	private static final String openJDK = "OpenJDK Runtime Environment";
	private static final String oracleKey = "Recommended Version";

	private static Method getDestroyMethod() {
		try {
			return GUIInterface.class.getMethod("destroy");
		} catch (final NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	private final HashSet<WeakReference<Closeable>> openStreams;

	private final ProgressMonitor progressMonitor;

	private final ArrayDeque<String> logStack;
	private final GUIInterface gui;

	private boolean closed = false;

	final GUI guiReal;

	final MasterThread master;

	/**
	 * Creates a new IO-Handler
	 * 
	 * @param sc
	 *            the object container containing all necessary startup
	 *            information
	 */
	public IOHandler(final StartupContainer sc) {
		this.master = sc.getMaster();

		class GUIProxy extends Proxy {

			/** */
			private static final long serialVersionUID = 1L;

			protected GUIProxy(
					@SuppressWarnings("hiding") final InvocationHandler h) {
				super(h);
			}

			public GUIInterface getProxyInstance() {
				return (GUIInterface) Proxy.newProxyInstance(getClass()
						.getClassLoader(),
						new Class<?>[] { GUIInterface.class }, this.h);
			}

		}
		@SuppressWarnings("hiding")
		final GUI gui = (GUI) sc.getIO().gui;
		this.guiReal = gui == null ? null : new GUI(gui, this.master);
		class GUIInvocationHandler implements InvocationHandler {

			private final Method interruptMethod = getDestroyMethod();


			@Override
			public Object invoke(final Object proxy, final Method method,
					final Object[] args) throws Throwable {
				if (IOHandler.this.master.isInterrupted()
						&& !method.equals(this.interruptMethod)) {
					if (Thread.currentThread() != IOHandler.this.master) {
						throw new InterruptedException();
					}
					return null;
				}
				if (IOHandler.this.guiReal == null) {
					return null;
				}
				return method.invoke(IOHandler.this.guiReal, args);
			}

		}
		final GUIProxy proxy = new GUIProxy(new GUIInvocationHandler());

		this.gui = proxy.getProxyInstance();

		this.progressMonitor = new ProgressMonitor(this.gui);
		this.openStreams = new HashSet<>();
		this.logStack = new ArrayDeque<>();
	}

	/**
	 * Creates a new temporarily IOHandler to present a GUI as fast as possible
	 * 
	 * @param name
	 *            title to be shown with the GUI
	 */
	public IOHandler(final String name) {
		if (name == null) {
			this.gui = null;
		} else {
			this.gui = new GUI(name);
		}
		this.guiReal = null;
		this.progressMonitor = null;
		this.openStreams = null;
		this.logStack = null;
		this.master = null;
	}

	/**
	 * Opens file fileToAppendTo and appends all content of file content
	 * 
	 * @param fileToAppendTo
	 *            file to write to
	 * @param content
	 *            file to read from
	 * @param bytesToDiscard
	 *            number of bytes of file content to discard
	 */
	public final void append(final File fileToAppendTo, final File content,
			int bytesToDiscard) {
		OutputStream out = null;
		InputStream in = null;
		if (!content.exists()) {
			return;
		}
		try {

			try {
				out = new OutputStream(fileToAppendTo, FileSystem.UTF8, true);
				synchronized (this) {
					this.openStreams.add(new WeakReference<Closeable>(out));
				}
				in = new InputStream(content, FileSystem.UTF8);
				synchronized (this) {
					this.openStreams.add(new WeakReference<Closeable>(in));
				}
			} catch (final FileNotFoundException e) {
				handleException(ExceptionHandle.TERMINATE, e);
				return;
			}


			try {
				in.read(new byte[bytesToDiscard]);
				write(in, out);
			} catch (final IOException e) {
				handleException(ExceptionHandle.TERMINATE, e);
			}
		} finally {
			if (out != null) {
				close(out);
			}
			if (in != null) {
				close(in);
			}
		}
	}

	/**
	 * Checks whether version of running JRE is greater or equal to most recent
	 * JRE version.
	 * 
	 * @throws IOException
	 *             if an error occurs while checking version
	 */
	public final void checkJRE() throws IOException {
		final String versionStringInstalled = System
				.getProperty("java.version");
		final String javaName = System.getProperty("java.runtime.name");
		final URL url;
		final String key;
		if (javaName.equals(IOHandler.oracleJRE)) {
			url = new URL(IOHandler.oracleJavaDownloadURL);
			key = IOHandler.oracleKey;
		} else if (javaName.equals(IOHandler.openJDK)) {
			System.out
					.println("Skipping update check for java - installed is openJDK");
			return;
		} else {
			System.out
					.println("Skipping update check for java - installed is a unknown runtime");
			return;
		}
		final java.io.InputStream in = url.openStream();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				new BufferedInputStream(in)));
		final int[] version = new int[3];
		int versionIdx = 0;
		while (true) {
			final String line = reader.readLine();
			if (line == null) {
				break;
			}
			final int index = line.indexOf(key);
			if (index >= 0) {
				final String[] splits = line.substring(index + key.length())
						.split(" ");
				for (String s : splits) {
					s = s.replaceAll("<.*>", "");
					if (s.isEmpty()) {
						continue;
					}
					final char c = s.charAt(0);
					if ((c >= '0') && (c <= '9')) {
						version[versionIdx++] = Integer.valueOf(s);
					}
				}
				break;
			}
		}
		in.close();
		reader.close();
		checkJRE(version, versionIdx, versionStringInstalled);
	}

	/**
	 * Closes all open streams. The main window of a used GUI is closed too.
	 */
	public final void close() {
		synchronized (this) {
			if (!this.closed) {
				endProgress("terminating");
				if (this.gui != null) {
					this.gui.destroy();
				}
			}
			this.closed = true;
			for (final WeakReference<Closeable> c : this.openStreams) {
				final Closeable cla = c.get();
				if (cla != null) {
					try {
						cla.close();
					} catch (final IOException e) {
						handleException(ExceptionHandle.SUPPRESS, e);
					}
				}
			}
			this.openStreams.clear();
		}
		if (!this.logStack.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (final Iterator<String> s = this.logStack.iterator(); true;) {
				sb.append(s.next());
				if (!s.hasNext()) {
					break;
				}
				sb.append("\n");
			}
			Debug.print("%s\n", sb.toString());
			System.err.println(sb.toString());
			System.err.println("closing down");
		}
	}

	/**
	 * Closes given closeable.
	 * 
	 * @param c
	 *            object to close
	 */
	public final synchronized void close(final Closeable c) {
		final Set<WeakReference<Closeable>> openStreamsDel = new HashSet<>();
		for (final WeakReference<Closeable> w : this.openStreams) {
			if (w.get() == null) {
				openStreamsDel.add(w);
			} else if (w.get() == c) {
				openStreamsDel.add(w);
				try {
					c.close();
				} catch (final IOException e) {
					handleException(ExceptionHandle.SUPPRESS, e);
				}
			}
		}
		this.openStreams.removeAll(openStreamsDel);
	}

	/**
	 * Compresses given files to given <i>zipFile</i>
	 * 
	 * @param zipFile
	 *            {@link ZipFile} to create
	 * @param files
	 *            future entries of created <i>zipFile</i>
	 */
	public final void compress(final File zipFile, final File... files) {
		ZipCompression.compress(zipFile, this, files);
	}

	/**
	 * Sets the progress message and units for 100%.
	 * 
	 * @param message
	 *            short message displayed during the displayed progress
	 * @param size
	 *            units representing 100%
	 * @see ProgressMonitor#beginTaskPreservingProgress(String, int)
	 */
	public final void continueProgress(final String message, int size) {
		this.progressMonitor.beginTaskPreservingProgress(message, size);
	}

	/**
	 * Terminates currently displayed progress
	 * 
	 * @param text
	 *            displayed if {@link GUIPlugin#display(JPanel)} returns
	 *            <i>false</i>
	 */
	public final void endProgress(final String text) {
		if (this.progressMonitor != null) {
			this.progressMonitor.endProgress(text);
		}
	}

	/**
	 * @return the GUI used by this IO-Handler
	 */
	public final GUIInterface getGUI() {
		return this.gui;
	}

	/**
	 * 
	 * @return the icon within the jar-archive or the path relative to the
	 *         workingDirectory used for the GUI.
	 */
	public final Image getIcon() {
		return this.guiReal.getIcon();
	}

	/**
	 * Calls {@link stone.io.GUIInterface#getOptions(Collection)} with given
	 * options
	 * 
	 * @param options
	 *            to be displayed otpions
	 */
	public final void getOptions(final Collection<Option> options) {
		if (guiReal == null) {
			// options have to set on invocation or config file
			return;
		}
		this.gui.getOptions(options);
	}

	/**
	 * Returns the progress monitor
	 * 
	 * @return the progress monitor
	 */
	public final ProgressMonitor getProgressMonitor() {
		return this.progressMonitor;
	}

	/**
	 * Process given exception in specified manner.
	 * 
	 * @param handle
	 *            strategy to use
	 * @param exception
	 *            caught exception
	 */
	public final void handleException(final ExceptionHandle handle,
			final Exception exception) {
		if (!handle.suppress()) {
			if (!this.closed) {
				exception.printStackTrace();
				this.gui.printErrorMessage(exception.toString().replaceAll(
						": ", "\n"));
			}
			if (handle.terminate()) {
				close();
			}
		}
	}

	/**
	 * Passes given <i>plugin</i> to underlying GUI
	 * 
	 * @see GUIInterface#runPlugin(GUIPlugin)
	 * @param plugin
	 *            plugin to perform on the GUI
	 */
	public final void handleGUIPlugin(final GUIPlugin plugin) {
		if (guiReal == null)
			try {
				plugin.textmode();
			} catch (final IOException e) {
				handleException(ExceptionHandle.TERMINATE, e);
			}
		else
			this.gui.runPlugin(plugin);
	}

	/**
	 * Prints trace of given {@link Throwable} and aborts the program
	 * 
	 * @param throwable
	 *            any {@link Throwable} caught during the program
	 */
	public final void handleThrowable(final Throwable throwable) {
		throwable.printStackTrace();
		this.gui.printErrorMessage(throwable.toString().replaceAll(": ", "\n"));
		System.exit(3);
	}

	/**
	 * Opens a new stream associated to given file using UTF-8 as default
	 * encoding for reading
	 * 
	 * @param file
	 *            {@link File} to read from
	 * @return the opened stream or <i>null</i> if an error occured
	 */
	public final InputStream openIn(final File file) {
		return openIn(file, FileSystem.UTF8);
	}

	/**
	 * Opens a new stream associated to given charset as encoding for reading
	 * 
	 * @param file
	 *            {@link File} to read from
	 * @param cs
	 *            charset to use for encoding
	 * @return the opened stream or <i>null</i> if an error occured
	 */
	public final synchronized InputStream openIn(final File file,
			final Charset cs) {
		final InputStream stream = new InputStream(file, cs);
		this.openStreams.add(new WeakReference<Closeable>(stream));
		return stream;
	}

	/**
	 * @param zipFile
	 *            {@link File} to read from
	 * @return map of entry names and their streams
	 */
	public final Map<String, AbstractInputStream> openInZip(
			final stone.util.Path zipFile) {
		if (!zipFile.exists()) {
			return null;
		}
		final ZipFile zip;
		final Map<String, AbstractInputStream> map = new HashMap<>();
		{
			ZipFile zipTmp = null;
			try {
				zipTmp = new ZipFile(zipFile.toFile());
			} catch (final Exception e) {
				Debug.print("%s\n", e.getMessage());
			}
			zip = zipTmp;
			if (zip == null) {
				return null;
			}
		}

		final Enumeration<? extends ZipEntry> entries = zip.entries();
		synchronized (this) {
			while (entries.hasMoreElements()) {
				final ZipEntry e = entries.nextElement();

				try {
					final AbstractInputStream in = new ZippedInputStream(zip, e);
					this.openStreams.add(new WeakReference<Closeable>(in));
					map.put(e.getName(), in);
				} catch (final IOException ioe) {
					Debug.print("%s\n", ioe.getMessage());
				}
			}
		}
		try {
			zip.close();
		} catch (final IOException e) {
			Debug.print("%s\n", e.getMessage());
		}
		return map;
	}

	/**
	 * Opens a new stream associated to given file using UTF-8 as default
	 * encoding for writing
	 * 
	 * @param file
	 *            {@link File} to write into
	 * @return the opened stream or <i>null</i> if an error occurred
	 */
	public final synchronized OutputStream openOut(final File file) {
		try {
			final OutputStream stream = new OutputStream(file, FileSystem.UTF8);
			this.openStreams.add(new WeakReference<Closeable>(stream));
			return stream;
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * Opens a new stream associated to given file using given encoding for
	 * writing
	 * 
	 * @param file
	 *            {@link File} to write into
	 * @param cs
	 *            charset to use for encoding
	 * @return the opened stream or <i>null</i> if an error occurred
	 */
	public final synchronized OutputStream openOut(final File file,
			final Charset cs) {
		try {
			final OutputStream stream = new OutputStream(file, cs);
			this.openStreams.add(new WeakReference<Closeable>(stream));
			return stream;
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * opens the given file, uncompress the entries and returns the uncompressed
	 * entries
	 * 
	 * @param zipFile
	 *            zipArchive to uncompress
	 * @return the uncompressed entries
	 */
	public final Set<String> openZipIn(final stone.util.Path zipFile) {
		if (!zipFile.exists()) {
			return null;
		}
		final Set<String> entriesRet = new HashSet<>();
		try {
			final ZipFile zip = new ZipFile(zipFile.toFile());
			final byte[] content = new byte[16000];

			final Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry zipEntry = entries.nextElement();
				final OutputStream out = openOut(zipFile.getParent()
						.resolve(zipEntry.getName()).toFile());
				final java.io.InputStream in = zip.getInputStream(zipEntry);
				int read;
				while ((read = in.read(content)) > 0) {
					out.write(content, 0, read);
				}
				in.close();
				close(out);
				entriesRet.add(zipEntry.getName());
			}
			zip.close();

			return entriesRet;
		} catch (final ZipException e) {
			return new HashSet<>();
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * Prints an error Message
	 * 
	 * @param errorMsg
	 *            message to display
	 * @param stack
	 *            <i>true</i> if the message shall be writen only to log and
	 *            printed later
	 */
	public final void printError(final String errorMsg, boolean stack) {
		if (stack) {
			this.logStack.add(errorMsg);
		} else {
			this.gui.printErrorMessage(errorMsg);
		}
	}

	/**
	 * Prints a message, either on stdout or stderr or by using a GUI
	 * 
	 * @param message
	 *            the message to be printed
	 * @param title
	 *            a optional header
	 * @param bringGUItoFront
	 *            will print the message on stderr or require confirmation if
	 *            set to <i>true</i>
	 */
	public final void printMessage(final String title, final String message,
			boolean bringGUItoFront) {
		this.gui.printMessage(title, message, bringGUItoFront);
	}

	/**
	 * Shows a dialog to select a single file
	 * 
	 * @param title
	 *            message shown as heading during the dialog
	 * @param startDir
	 *            initial directory to work on
	 * @param filter
	 *            filter to use
	 * @return the selected file or <i>null</i> if canceled or interrupted
	 * @param text
	 *            displayed if {@link GUIPlugin#display(JPanel)} returns
	 *            <i>false</i>
	 */
	public final Path selectFile(final String title, final File startDir,
			final FileFilter filter, final String text) {
		final FileSelectionGUIPlugin selector = new FileSelectionGUIPlugin(
				title, startDir, filter, text);
		handleGUIPlugin(selector);
		return selector.getSelection();
	}

	static String print(final File file, final FileFilter filter) {
		if (filter.accept(file)) {
			if (file.isDirectory()) {
				return printBlue("%-30.30s", file.getName());
			}
			return printRed("%-30.30s", file.getName());
		}
		return null;
	}

	final static String printBlue(final String format, final Object... args) {
		return stone.util.Color.print(stone.util.Color.ColorType.BLUE,
				stone.util.Color.ColorType.BLACK, format, args);
	}

	final static String printRed(final String format, final Object... args) {
		return stone.util.Color.print(stone.util.Color.ColorType.RED,
				stone.util.Color.ColorType.BLACK, format, args);
	}

	final static String printGreen(final String format, final Object... args) {
		return stone.util.Color.print(stone.util.Color.ColorType.GREEN,
				stone.util.Color.ColorType.BLACK, format, args);
	}

	/**
	 * @param modules
	 *            list of available modules
	 * @return a subset of given <i>modules</i> selected to be run. <i>null</i>
	 *         if interrupted
	 */
	public final List<String> selectModules(final List<String> modules) {
		if (this.guiReal == null) {
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(System.in));
			final boolean[] active = new boolean[modules.size()];
			boolean needsConfirmation = true;
			final List<String> ret = new java.util.ArrayList<>();
			while (needsConfirmation) {
				System.out.println("\nSelect modules");
				String paddingLeft = "";
				String paddingRight = "";
				int size = (modules.size()) / 10;
				while (size > 1) {
					paddingLeft += " ";
					paddingRight += " ";
					size /= 100;
				}
				if (size == 1) {
					paddingLeft += " ";
				}
				int nextDecreaseLeft = 9;
				int nextDecreaseRight = 99;
				for (int i = 0; i < modules.size(); ++i) {
					System.out.printf("[ %s%d%s ] %s\n", paddingLeft, i + 1,
							paddingRight, modules.get(i));
					if (i == nextDecreaseLeft) {
						nextDecreaseLeft *= 100;
						nextDecreaseLeft += 99;
						paddingLeft = paddingLeft.length() == 0 ? ""
								: paddingLeft.substring(1);
					} else if (i == nextDecreaseRight) {
						nextDecreaseRight *= 100;
						nextDecreaseRight += 99;
						paddingRight = paddingRight.length() == 0 ? ""
								: paddingRight.substring(1);
					}
				}

				try {
					final String line = reader.readLine();
					if (line == null)
						return null;
					final String[] parts = line.split("[\\t\\- ,]");
					needsConfirmation = false;
					for (final String part : parts) {
						if (part.isEmpty())
							continue;
						boolean valid = true;
						final char[] chars = part.toCharArray();
						for (final char c : chars) {
							if (c < '0' || c > '9') {
								System.err.printf("Invalid number %s\n", part);
								valid = false;
								break;
							}
						}
						if (!valid) {
							needsConfirmation = true;
							break;
						}
						final int id = Integer.parseInt(part);
						if (id < 0 || id > modules.size()) {
							valid = false;
							System.err.printf("Invalid number %s\n", part);
							needsConfirmation = true;
							break;
						}
						active[id - 1] = true;
					}
				} catch (final IOException e) {
					handleException(ExceptionHandle.SUPPRESS, e);
					return null;
				}
			}
			for (int i = 0; i < modules.size(); ++i) {
				if (active[i]) {
					ret.add(modules.get(i));
				}
			}
			return ret;
		}
		return this.gui.selectModules(modules);
	}

	/**
	 * @param length
	 *            new size of displayed progress
	 */
	public final void setProgressSize(int length) {
		this.progressMonitor.setProgressSize(length);
	}

	/**
	 * Sets the message of a running progress
	 * 
	 * @param title
	 *            message to be shown
	 */
	public final void setProgressTitle(final String title) {
		this.progressMonitor.setProgressTitle(title);
	}

	/**
	 * Starts a new progress
	 * 
	 * @param message
	 *            a short message to print
	 * @param size
	 *            units to scale to 100%
	 * @see ProgressMonitor#beginTask(String, int)
	 */
	public final void startProgress(final String message, int size) {
		this.progressMonitor.beginTask(message, size);
	}

	/**
	 * Adds 1 unit to current progress
	 * 
	 * @see ProgressMonitor#update(int)
	 */
	public final void updateProgress() {
		this.progressMonitor.update(1);
	}

	/**
	 * Adds <i>value</i> units to current progress
	 * 
	 * @see ProgressMonitor#update(int)
	 * @param value
	 *            units to add
	 */
	public final void updateProgress(int value) {
		this.progressMonitor.update(value);
	}

	/**
	 * writes all content from in to out, and closes the
	 * {@link java.io.InputStream} <i>in</i> afterwards
	 * 
	 * @param in
	 *            stream to read from
	 * @param out
	 *            stream to write to
	 */
	public final void write(final java.io.InputStream in,
			final java.io.OutputStream out) {
		final int bufSize = 8192;
		final byte[] buffer = new byte[bufSize];
		final BufferedInputStream reader = new BufferedInputStream(in,
				buffer.length);
		final BufferedOutputStream writer = new BufferedOutputStream(out,
				buffer.length);
		try {
			while (true) {
				final int read = reader.read(buffer, 0, bufSize);
				if (read < 0) {
					break;
				}
				writer.write(buffer, 0, read);
			}
			writer.flush();
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		} finally {
			close(in);
		}
	}

	/**
	 * Writes a byte to an OutputStream
	 * 
	 * @param out
	 *            the stream to write to
	 * @param b
	 *            the byte to write
	 */
	public final void write(final OutputStream out, byte b) {
		try {
			out.write(b);
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
	}

	/**
	 * Writes bytes to an OutputStream
	 * 
	 * @param out
	 *            the stream to write to
	 * @param bytes
	 *            the bytes to write
	 */
	public final void write(final OutputStream out, byte[] bytes) {
		try {
			out.write(bytes);
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
	}

	/**
	 * Writes <i>length</i> bytes of buffer <i>bytes</i> starting at
	 * <i>offset</i> to <i>out</i>.
	 * 
	 * @param out
	 *            stream to write to
	 * @param bytes
	 *            buffer to read from
	 * @param offset
	 *            first byte in <i>buffer</i> to write
	 * @param length
	 *            bytes to write
	 */
	public final void write(final OutputStream out, byte[] bytes, int offset,
			int length) {
		try {
			out.write(bytes, offset, length);
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
	}

	/**
	 * Writes bytes encoding <i>string</i> to <i>out</i>.
	 * 
	 * @param string
	 *            output to write
	 * @param out
	 *            stream to write to
	 * @see #write(OutputStream, byte[])
	 */
	public final void write(final OutputStream out, final String string) {
		write(out, string.getBytes());
	}

	/**
	 * Writes bytes encoding <i>string</i> to <i>out</i> and appends CRLF.
	 * 
	 * @param string
	 *            output to write
	 * @param out
	 *            stream to write to
	 * @see #write(OutputStream, byte[])
	 */
	public final void writeln(final OutputStream out, final String string) {
		write(out, string.getBytes());
		write(out, "\r\n");
	}

	private final void checkJRE(int[] version, int versionIdx,
			final String versionStringInstalled) {
		final int[] versionInstalled = new int[4];
		int versionInstalledIdx = 0;
		for (final String s : versionStringInstalled.split("[\\._]")) {
			versionInstalled[versionInstalledIdx++] = Integer.parseInt(s);
		}
		if (versionIdx == 2) {
			version[2] = version[1];
			version[1] = version[0];
			version[0] = 1;
		}
		if (versionInstalledIdx == 4) {
			versionInstalled[2] = versionInstalled[3];
		}
		for (int i = 0; i < 3; i++) {
			if (version[i] < versionInstalled[i]) {
				System.err.printf("Check version of used Java\n"
						+ "Installed   : %2d Update %2d\n"
						+ "Recommended : %2d Update %2d\n",
						versionInstalled[1], versionInstalled[2], version[1],
						version[2]);
				return;
			}
			if (version[i] > versionInstalled[i]) {
				printMessage(
						"Update your Java installation",
						"Your java installation is out dated\n"
								+ String.format(
										"Installed  : Version %2d Update %2d\n"
												+ "Recommended: Version %2d Update %2d",
										versionInstalled[1],
										versionInstalled[2], version[1],
										version[2]), true);
				return;
			}
		}
		System.out.printf("Check version of used Java\n"
				+ "Installed   : %2d Update %2d\n"
				+ "Recommended : %2d Update %2d\n", versionInstalled[1],
				versionInstalled[2], version[1], version[2]);
	}
}
