package stone;

import stone.util.Path;


final class Task {
	final String name;
	final Path source;

	Task(final String s, final Path tmp) {
		name = s;
		source = tmp.resolve(s);
	}

	Task(final Task t, final String s) {
		name = t.name + "/" + s;
		source = t.source.resolve(s);
	}

	@Override
	public final String toString() {
		return name + "->" + source;
	}

	final void delete() {
		Path path = source.getParent();
		while (path.toFile().list().length == 0) {
			path.delete();
			path = path.getParent();
		}

	}
}
