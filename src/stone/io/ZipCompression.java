package stone.io;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Simple class for compressing file into a zip-archive
 * 
 * @author Nelphindal
 */
public class ZipCompression {

	/**
	 * Compresses given files to zippedFile using given IO-Handler for managing
	 * the streams
	 * 
	 * @param zippedFile
	 *            zip-file to create
	 * @param ioHandler
	 *            IO-Handler to use
	 * @param files
	 *            files to compress
	 */
	public static final void compress(final File zippedFile,
			final IOHandler ioHandler, final File... files) {
		try {
			if (!zippedFile.exists()) {
				// create file write zip header
				final OutputStream out = ioHandler.openOut(zippedFile);
				final ZipOutputStream zipOut = new ZipOutputStream(out);
				zipOut.close();
				ioHandler.close(out);
			}
			final ZipFile zip = new ZipFile(zippedFile);
			final ZipOutputStreams outs =
					ZipCompression.openOut(zip, ioHandler);
			final ZipOutputStream out = outs.zipOutputStream;
			final java.nio.file.Path zipPath =
					zippedFile.toPath().getParent();
			for (final File file : files) {
				final String relName =
						zipPath.relativize(file.toPath()).toString();
				final ZipEntry entry = new ZipEntry(relName);
				final InputStream in = ioHandler.openIn(file);
				final byte[] buff = new byte[16000];
				int read;
				out.putNextEntry(entry);
				while ((read = in.read(buff)) > 0) {
					out.write(buff, 0, read);
				}
				ioHandler.close(in);
				out.closeEntry();
			}
			out.close();
			zip.close();
			ioHandler.close(outs.out);

			// test
		} catch (final IOException e) {
			zippedFile.delete();
			ioHandler.handleException(ExceptionHandle.TERMINATE, e);
		}
	}

	private static final ZipOutputStreams openOut(
			final ZipFile zippedUpdateFile, final IOHandler ioHandler) {
		final OutputStream out =
				ioHandler.openOut(new File(zippedUpdateFile.getName()));
		return new ZipOutputStreams(out, new ZipOutputStream(out));
	}
}

class ZipOutputStreams {

	final OutputStream out;
	final ZipOutputStream zipOutputStream;

	public ZipOutputStreams(final OutputStream out,
			final ZipOutputStream zipOutputStream) {
		this.out = out;
		this.zipOutputStream = zipOutputStream;
	}

}
