package io;

import gui.GUI;
import gui.GUIInterface;
import gui.GUIPlugin;
import gui.ProgressMonitor;

import java.awt.Image;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

import main.MasterThread;
import main.StartupContainer;
import util.FileSystem;
import util.Option;
import util.Path;


/**
 * Central class handling I/O-Operations
 * 
 * @author Nelphindal
 */
public class IOHandler {

	private final HashSet<Closeable> openStreams;

	private final ProgressMonitor progressMonitor;

	private final ArrayDeque<String> logStack;

	private final GUIInterface gui;
	private final GUI guiReal;

	private boolean closed = false;

	private final MasterThread master;

	private final Image icon;

	/**
	 * Creates a new IO-Handler
	 * 
	 * @param sc
	 *            the object container containing all necessary startup
	 *            information
	 * @param iconFile
	 *            the name of the icon within the jar-archive or the path
	 *            relative to the workingDirectory
	 */
	public IOHandler(final StartupContainer sc, final String iconFile) {
		final Path workingDirectory = sc.getWorkingDir();
		master = sc.getMaster();
		Image icon = null;
		final Image iconFinal;
		System.out.println("wd: " + workingDirectory
				+ (sc.wdIsJarArchive() ? " JAR" : ""));
		System.out.println("icon: " + iconFile);
		if (!sc.wdIsJarArchive()) {
			final File f = workingDirectory.resolve(iconFile).toFile();
			if (f.exists()) {
				try {
					icon = ImageIO.read(f);
				} catch (final IOException ioe) {
					handleException(ExceptionHandle.SUPPRESS, ioe);
				}
			}
		} else {
			try {
				final File f = workingDirectory.toFile();
				final JarFile jar = new JarFile(f);
				try {
					final JarEntry iconEntry = jar.getJarEntry(iconFile);
					if (iconEntry != null) {
						icon = ImageIO.read(jar.getInputStream(iconEntry));
					} else {
						System.out.println("Icon not found");
					}
				} catch (final IOException ioe) {
					ioe.printStackTrace();
					handleException(ExceptionHandle.SUPPRESS, ioe);
				} finally {
					jar.close();
				}
			} catch (final Exception e) {
				handleException(ExceptionHandle.SUPPRESS, e);
			}
		}

		iconFinal = icon;
		this.icon = iconFinal;

		class GUIProxy extends Proxy {

			/** */
			private static final long serialVersionUID = 1L;

			protected GUIProxy(final InvocationHandler h) {
				super(h);
			}

			public GUIInterface getProxyInstance() {
				return (GUIInterface) Proxy.newProxyInstance(getClass()
						.getClassLoader(),
						new Class<?>[] { GUIInterface.class }, h);
			}

		}
		guiReal = new GUI((GUI) sc.getIO().gui, master, iconFinal);
		class GUIInvocationHandler implements InvocationHandler {

			private final GUI guiReal = IOHandler.this.guiReal;

			@Override
			public Object invoke(final Object proxy, final Method method,
					final Object[] args) throws Throwable {
				if (master.isInterrupted()) {
					if (Thread.currentThread() == master) {
						guiReal.destroy();
					}
					return null;
				}
				return method.invoke(guiReal, args);
			}

		}
		final GUIProxy proxy = new GUIProxy(new GUIInvocationHandler());

		gui = proxy.getProxyInstance();

		progressMonitor = new ProgressMonitor(gui);
		openStreams = new HashSet<>();
		logStack = new ArrayDeque<>();
	}

	/**
	 * Creates a new temporarily IOHandler to present a GUI as fast as possible
	 * 
	 * @param name
	 *            title to be shown with the GUI
	 * @throws InterruptedException
	 */
	public IOHandler(final String name) throws InterruptedException {
		gui = new GUI(name);
		guiReal = null;
		progressMonitor = null;
		openStreams = null;
		logStack = null;
		master = null;
		icon = null;
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
				openStreams.add(out);
				in = new InputStream(content, FileSystem.UTF8);
				openStreams.add(in);
			} catch (final FileNotFoundException e) {
				if (out != null) {
					close(out);
				}
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
	 * Closes all open streams. The main window of a used GUI is closed too.
	 */
	public final void close() {
		synchronized (this) {
			if (!closed) {
				endProgress();
				gui.destroy();
			}
			closed = true;
		}

		for (final Closeable c : openStreams) {
			try {
				c.close();
			} catch (final IOException e) {
				handleException(ExceptionHandle.SUPPRESS, e);
			}
		}
		openStreams.clear();
		if (!logStack.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (final Iterator<String> s = logStack.iterator(); true;) {
				sb.append(s.next());
				if (!s.hasNext()) {
					break;
				}
				sb.append("\r\n");
			}
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
	public final void close(final Closeable c) {
		if (openStreams.remove(c)) {
			try {
				c.close();
			} catch (final IOException e) {
				handleException(ExceptionHandle.SUPPRESS, e);
			}
		}
	}

	/**
	 * Compresses given files to given zipFile
	 * 
	 * @param zipFile
	 * @param files
	 */
	public final void compress(final File zipFile, final File... files) {
		ZipCompression.compress(zipFile, this, files);
	}

	/**
	 * Sets the progress message and units for 100%.
	 * 
	 * @param message
	 * @param size
	 *            units representing 100%
	 * @see ProgressMonitor#beginTaskPreservingProgress(String, int)
	 */
	public final void continueProgress(final String message, int size) {
		progressMonitor.beginTaskPreservingProgress(message, size);
	}

	/**
	 * Terminates currently displayed progress
	 */
	public final void endProgress() {
		if (progressMonitor != null) {
			progressMonitor.endProgress();
		}
	}

	/**
	 * @return the GUI used by this IO-Handler
	 */
	public GUIInterface getGUI() {
		return gui;
	}

	/**
	 * @return the icon-image used in the GUI
	 */
	public final Image getIcon() {
		return icon;
	}

	/**
	 * Calls {@link gui.GUIInterface#getOptions(Collection)} with given options
	 * 
	 * @param options
	 */
	public final void getOptions(final Collection<Option> options) {
		gui.getOptions(options);
	}

	/**
	 * Returns the progress monitor
	 * 
	 * @return the progress monitor
	 */
	public final ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	/**
	 * Process given exception in specified manner.
	 * 
	 * @param handle
	 * @param exception
	 */
	public final void handleException(final ExceptionHandle handle,
			final Exception exception) {
		if (!handle.suppress()) {
			if (!closed) {
				exception.printStackTrace();
				gui.printErrorMessage(exception.toString().replaceAll(": ",
						"\n"));
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
	 */
	public final void handleGUIPlugin(final GUIPlugin plugin) {
		gui.runPlugin(plugin);
	}

	/**
	 * Process given throwable and aborts the program
	 * 
	 * @param throwable
	 */
	public final void handleThrowable(final Throwable throwable) {
		throwable.printStackTrace();
		gui.printMessage(throwable.getLocalizedMessage(), throwable.toString()
				.replaceAll(": ", "\n"), true);
		System.exit(3);
	}

	/**
	 * Opens a new stream associated to given file using UTF-8 as default
	 * encoding for reading
	 * 
	 * @param file
	 * @return the opened stream or <i>null</i> if an error occured
	 */
	public final InputStream openIn(final File file) {
		return openIn(file, FileSystem.UTF8);
	}

	/**
	 * Opens a new stream associated to given charset as encoding for reading
	 * 
	 * @param file
	 * @param cs
	 *            charset to use for encoding
	 * @return the opened stream or <i>null</i> if an error occured
	 */
	public final InputStream openIn(final File file, final Charset cs) {
		try {
			final InputStream stream = new InputStream(file, cs);
			openStreams.add(stream);
			return stream;
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * Opens a new stream associated to given file using UTF-8 as default
	 * encoding for writing
	 * 
	 * @param file
	 * @return the opened stream or <i>null</i> if an error occurred
	 */
	public final OutputStream openOut(final File file) {
		try {
			final OutputStream stream = new OutputStream(file, FileSystem.UTF8);
			openStreams.add(stream);
			return stream;
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * Opens a new stream associated to given file using given
	 * encoding for writing
	 * 
	 * @param file
	 * @param cs
	 * @return the opened stream or <i>null</i> if an error occurred
	 */
	public final OutputStream openOut(final File file, final Charset cs) {
		try {
			final OutputStream stream = new OutputStream(file, cs);
			openStreams.add(stream);
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
	public final Set<String> openZipIn(final util.Path zipFile) {
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
				final OutputStream out =
						openOut(zipFile.getParent().resolve(zipEntry.getName())
								.toFile());
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
		} catch (final IOException e) {
			handleException(ExceptionHandle.TERMINATE, e);
		}
		return null;
	}

	/**
	 * Prints an error Message
	 * 
	 * @param errorMsg
	 * @param stack
	 *            <i>true</i> if the message shall be writen only to log and
	 *            printed later
	 */
	public final void printError(final String errorMsg, boolean stack) {
		if (stack) {
			logStack.add(errorMsg);
		} else {
			gui.printErrorMessage(errorMsg);
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
		gui.printMessage(title, message, bringGUItoFront);
	}

	/**
	 * @param title
	 * @param startDir
	 * @param filter
	 * @return the selected file or <i>null</i> if canceled or interrupted
	 */
	public final Path selectFile(final String title, final File startDir,
			final FileFilter filter) {
		final FileSelectionGUIPlugin selector =
				new FileSelectionGUIPlugin(title, startDir, filter);
		handleGUIPlugin(selector);
		return selector.getSelection();
	}

	/**
	 * @param modules
	 * @return a subset of given <i>modules</i> selected to be run. <i>null</i>
	 *         if interrupted
	 * @throws InterruptedException
	 */
	public final Set<String> selectModules(final Collection<String> modules)
			throws InterruptedException {
		return gui.selectModules(modules);
	}

	/**
	 * @param length
	 */
	public final void setProgressSize(int length) {
		progressMonitor.setProgressSize(length);
	}

	/**
	 * Sets the message of a running progress
	 * 
	 * @param title
	 *            message to be shown
	 */
	public final void setProgressTitle(final String title) {
		progressMonitor.setProgressTitle(title);
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
		progressMonitor.beginTask(message, size);
	}

	/**
	 * Adds 1 unit to current progress
	 * 
	 * @see ProgressMonitor#update(int)
	 */
	public final void updateProgress() {
		progressMonitor.update(1);
	}

	/**
	 * Adds <i>value</i> units to current progress
	 * 
	 * @see ProgressMonitor#update(int)
	 * @param value
	 *            units to add
	 */
	public final void updateProgress(int value) {
		progressMonitor.update(value);
	}

	/**
	 * writes all content from in to out, and closes the inputStream afterwards
	 * 
	 * @param in
	 * @param out
	 */
	public final void write(final java.io.InputStream in,
			final java.io.OutputStream out) {
		final byte[] buffer = new byte[16000];
		try {
			while (true) {
				final int len = in.read(buffer);
				if (len < 0) {
					break;
				}
				out.write(buffer, 0, len);
			}
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
	 * @param bytes
	 * @param offset
	 * @param length
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
	 * @param out
	 * @see #write(OutputStream, byte[])
	 */
	public final void write(final OutputStream out, final String string) {
		write(out, string.getBytes());
	}

	/**
	 * Writes bytes encoding <i>string</i> to <i>out</i> and appends CRLF.
	 * 
	 * @param string
	 * @param out
	 * @see #write(OutputStream, byte[])
	 */
	public final void writeln(final OutputStream out, final String string) {
		write(out, string.getBytes());
		write(out, "\r\n");
	}
}
