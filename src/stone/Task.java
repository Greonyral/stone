package stone;

import stone.util.Path;


final class Task {
	final String name;
	final Path source;

	Task(final String s, final Path tmp) {
		this.name = s;
		this.source = tmp.resolve(s);
	}

	Task(final Task t, final String s) {
		this.name = t.name + "/" + s;
		this.source = t.source.resolve(s);
	}

	@Override
	public final String toString() {
		return this.name + "->" + this.source;
	}

	final void delete() {
		Path path = this.source.getParent();
		while (path.toFile().list().length == 0) {
			path.delete();
			path = path.getParent();
		}

	}
}
