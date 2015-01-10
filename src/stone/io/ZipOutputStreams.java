package stone.io;

import java.util.zip.ZipOutputStream;

class ZipOutputStreams {

	final OutputStream out;
	final ZipOutputStream zipOutputStream;

	public ZipOutputStreams(final OutputStream out,
			final ZipOutputStream zipOutputStream) {
		this.out = out;
		this.zipOutputStream = zipOutputStream;
	}

}