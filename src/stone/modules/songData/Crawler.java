package stone.modules.songData;

import java.util.ArrayDeque;

import stone.util.Debug;
import stone.util.Path;

class Crawler implements Runnable {

	private final ArrayDeque<Path> wl = new ArrayDeque<>();
	private final Deserializer sdd;

	private final boolean terminated = false;

	private final java.util.concurrent.atomic.AtomicInteger threads = new java.util.concurrent.atomic.AtomicInteger();

	public Crawler(final Deserializer sdd) {
		this.wl.add(sdd.getRoot());
		this.sdd = sdd;
	}

	@Override
	public final void run() {
		try {
			while (crawl()) {
				;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			this.threads.set(-1);
			synchronized (this.wl) {
				this.wl.notifyAll();
			}
			return;
		} finally {

		}
	}

	public final synchronized boolean terminated() {
		return this.terminated;
	}

	private final boolean crawl() throws InterruptedException {
		final Path path;
		synchronized (this.wl) {
			if (this.wl.isEmpty()) {
				if (this.threads.get() <= 0) {
					synchronized (this.sdd) {
						this.sdd.crawlDone();
					}
					this.wl.notifyAll();
					return false;
				}
				while (this.wl.isEmpty()) {
					this.wl.wait();
					if (this.threads.get() == 0) {
						return false;
					}
				}
				return true;
			}
			path = this.wl.remove();
			this.threads.incrementAndGet();
		}
		if (!path.exists()) {
			this.threads.decrementAndGet();
			return true;
		}
		if (path.toFile().isDirectory()) {
			for (final String name : path.toFile().list()) {
				if (name.startsWith(".")) {
					continue;
				}
				synchronized (this.wl) {
					this.wl.add(path.resolve(name));
					this.wl.notifyAll();
				}
			}
		} else if (path.toFile().isFile()
				&& path.getFilename().endsWith(".abc")) {
			Debug.print("found %s\n", path);
			synchronized (this.sdd) {
				this.sdd.addToQueue(new ModEntry(path));
				this.sdd.notifyAll();
			}
		}
		this.threads.decrementAndGet();
		return true;
	}
}
