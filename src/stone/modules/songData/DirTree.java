package stone.modules.songData;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import stone.util.Path;


final class DirTree {

	private final String name;
	private final AtomicInteger size = new AtomicInteger(0);
	final Path base;
	final TreeMap<String, DirTree> directories = new TreeMap<>();
	final TreeMap<String, SongData> files = new TreeMap<>();
	final DirTree parent;

	public DirTree(final Path base) {
		this.parent = null;
		this.name = null;
		this.base = base;
	}

	private DirTree(final DirTree parent, final String name) {
		this.base = parent.base;
		this.parent = parent;
		this.name = name;
	}

	public final Set<String> getDirs(final Path path) {
		return walkTo(path).directories.keySet();
	}

	public final Set<String> getFiles(final Path path) {
		return walkTo(path).files.keySet();
	}

	private final void add(final SongData songdata) {
		final Path path = songdata.getPath();
		final DirTree t = walkTo(path.getParent());
		if (t == null) {
			return;
		}
		synchronized (t) {
			final SongData sd = t.files.get(path.getFilename());
			if (sd == null) {
				t.files.put(path.getFilename(), songdata);
			} else if (sd.getLastModification() < songdata
					.getLastModification()) {
				t.files.put(path.getFilename(), songdata);
			} else {
				synchronized (AbtractEoWInAbc.messages) {
					AbtractEoWInAbc.messages.remove(path);
				}
				return;
			}
		}
		for (DirTree tree = t; tree != null; tree = tree.parent) {
			tree.size.incrementAndGet();
		}
	}

	final Path buildPath() {
		if (this.parent == null) {
			return this.base;
		}
		return this.parent.buildPath().resolve(this.name);
	}

	final Iterator<Path> dirsIterator() {
		return new Iterator<Path>() {
			private DirTree currentTree = walkTo(DirTree.this.base);
			private Iterator<String> iter = this.currentTree.directories
					.keySet().iterator();
			private final ArrayDeque<Iterator<String>> iterStack = new ArrayDeque<>();

			@Override
			public boolean hasNext() {
				while (true) {
					if (this.iter.hasNext()) {
						return true;
					}
					if (this.iterStack.isEmpty()) {
						return false;
					}
					// pop
					this.currentTree = this.currentTree.parent;
					this.iter = this.iterStack.removeLast();
				}
			}

			@Override
			public Path next() {
				final String next = this.iter.next();
				final Path ret = this.currentTree.buildPath().resolve(next);
				if (this.currentTree.directories.get(next) != null) {
					this.iterStack.add(this.iter);
					this.currentTree = this.currentTree.directories.get(next);
					this.iter = this.currentTree.directories.keySet()
							.iterator();
				}
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	final Iterator<Path> filesIterator() {
		return new Iterator<Path>() {

			private DirTree currentTree = walkTo(DirTree.this.base);
			private Iterator<String> dirIter = this.currentTree.directories
					.keySet().iterator();
			private Iterator<String> fileIter = this.currentTree.files.keySet()
					.iterator();
			private final ArrayDeque<Iterator<String>> dirIterStack = new ArrayDeque<>();
			private final ArrayDeque<Iterator<String>> fileIterStack = new ArrayDeque<>();

			@Override
			public boolean hasNext() {
				while (true) {
					if (this.fileIter.hasNext()) {
						return true;
					}
					if (this.dirIter.hasNext()) {
						final String nextDir = this.dirIter.next();
						if (this.currentTree.directories.get(nextDir) != null) {
							this.dirIterStack.add(this.dirIter);
							this.fileIterStack.add(this.fileIter);
							this.currentTree = this.currentTree.directories
									.get(nextDir);
							this.dirIter = this.currentTree.directories
									.keySet().iterator();
							this.fileIter = this.currentTree.files.keySet()
									.iterator();
						}
						continue;
					}
					if (this.dirIterStack.isEmpty()) {
						return false;
					}
					// pop
					this.currentTree = this.currentTree.parent;
					this.fileIter = this.fileIterStack.removeLast();
					this.dirIter = this.dirIterStack.removeLast();
				}
			}

			@Override
			public Path next() {
				final String next = this.fileIter.next();
				return this.currentTree.buildPath().resolve(next);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	final SongData get(final Path path) {
		final DirTree tree = walkTo(path.getParent());
		synchronized (tree) {
			return tree.files.get(path.getFilename());
		}
	}

	final int[] getCountIn(final Path path) {
		final DirTree target = walkTo(path);
		return new int[] { target.directories.size(), target.files.size() };
	}

	final int getFilesCount() {
		return this.size.get();
	}

	final Path getRoot() {
		if (this.parent != null) {
			return this.parent.getRoot();
		}
		return this.base;
	}

	final void put(final SongData songData) {
		add(songData);
	}

	final DirTree walkTo(final Path path) {
		if (path == null) {
			return null;
		}
		DirTree t = this;
		if (path == this.base) {
			return t;
		}
		final String[] walkingPath = path.relativize(this.base).split("/");
		int layer = 0;
		while (layer < walkingPath.length) {
			final String base_ = walkingPath[layer++];
			final DirTree last = t;
			t = t.directories.get(base_);
			if (t == null) {
				t = last;
				synchronized (t) {
					t = t.directories.get(base_);
					if (t == null) {
						t = new DirTree(last, base_);
						last.directories.put(base_, t);
					}
				}
			}
		}
		return t;
	}
}
