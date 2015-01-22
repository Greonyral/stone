package stone.modules.songData;

import java.util.ArrayDeque;

import stone.util.Debug;
import stone.util.Path;

class Crawler implements Runnable {

	private final ArrayDeque<Path> wl = new ArrayDeque<>();
	private final Deserializer sdd;

	private boolean terminated = false;

	private final java.util.concurrent.atomic.AtomicInteger threads = new java.util.concurrent.atomic.AtomicInteger();

	public Crawler(final Deserializer sdd) {
		wl.add(sdd.getRoot());
		this.sdd = sdd;
	}

	@Override
	public final void run() {
		try {
			while (crawl())
				;
		} catch (final Exception e) {
			e.printStackTrace();
			threads.set(-1);
			synchronized (wl) {
				wl.notifyAll();
			}
			return;
		} finally {

		}
	}

	private final boolean crawl() throws InterruptedException {
		final Path path;
		synchronized (wl) {
			if (wl.isEmpty()) {
				if (threads.get() <= 0) {
					synchronized (sdd) {
						sdd.crawlDone();
					}
					wl.notifyAll();
					return false;
				}
				while (wl.isEmpty()) {
					wl.wait();
					if (threads.get() == 0)
						return false;
				}
				return true;
			}
			path = wl.remove();
			threads.incrementAndGet();
		}
		if (!path.exists()) {
			threads.decrementAndGet();
			return true;
		}
		if (path.toFile().isDirectory()) {
			for (final String name : path.toFile().list()) {
				if (name.startsWith(".")) {
					continue;
				}
				synchronized (wl) {
					wl.add(path.resolve(name));
					wl.notifyAll();
				}
			}
		} else if (path.toFile().isFile()
				&& path.getFileName().endsWith(".abc")) {
			Debug.print("found %s\n", path);
			synchronized (sdd) {
				sdd.addToQueue(new ModEntry(path));
				sdd.notifyAll();
			}
		}
		threads.decrementAndGet();
		return true;
	}

	public final synchronized boolean terminated() {
		return terminated;
	}
}
