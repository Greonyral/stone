package stone.util;

import java.util.ArrayList;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

final class ArraySetList<E> extends ArrayList<E> implements Set<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public final Stream<E> parallelStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Spliterator<E> spliterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Stream<E> stream() {
		throw new UnsupportedOperationException();
	}

}
