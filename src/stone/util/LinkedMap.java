package stone.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Nelphindal
 * 
 *         A implementation of {@link Map} using linked lists.
 * @param <K>
 *            Key
 * @param <V>
 *            Value
 */
public class LinkedMap<K, V> implements Map<K, V> {

	class Entry {
		private final K key;
		private V value;
		private Entry next;

		@SuppressWarnings("hiding")
		Entry(final K key, final V value) {
			this.key = key;
			this.value = value;
		}
	}

	private final Map<K, Entry> lookUpMap = new HashMap<>();

	private Entry tail = null;
	private Entry head = null;

	@Override
	public void clear() {
		this.head = this.tail = null;
		this.lookUpMap.clear();
	}

	@Override
	public V compute(
			final K key,
			final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfAbsent(final K key,
			final Function<? super K, ? extends V> mappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfPresent(
			final K key,
			final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(final Object key) {
		return this.lookUpMap.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return this.lookUpMap.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new Set<java.util.Map.Entry<K, V>>() {

			@Override
			public boolean add(java.util.Map.Entry<K, V> e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean addAll(
					Collection<? extends java.util.Map.Entry<K, V>> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean contains(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void forEach(
					Consumer<? super java.util.Map.Entry<K, V>> action) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isEmpty() {
				return LinkedMap.this.isEmpty();
			}

			@Override
			public Iterator<java.util.Map.Entry<K, V>> iterator() {
				return new Iterator<java.util.Map.Entry<K, V>>() {

					private Entry ptr = LinkedMap.this.head;

					@Override
					public void forEachRemaining(
							Consumer<? super java.util.Map.Entry<K, V>> action) {
						throw new UnsupportedOperationException();

					}

					@Override
					public boolean hasNext() {
						return this.ptr != null;
					}

					@Override
					public java.util.Map.Entry<K, V> next() {
						final K key = this.ptr.key;
						final V value = this.ptr.value;
						this.ptr = this.ptr.next;
						return new Map.Entry<K, V>() {

							@Override
							public K getKey() {
								return key;
							}

							@Override
							public V getValue() {
								return value;
							}

							@Override
							public V setValue(
									@SuppressWarnings("hiding") V value) {
								throw new UnsupportedOperationException();
							}


						};
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();

					}

				};
			}

			@Override
			public Stream<java.util.Map.Entry<K, V>> parallelStream() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean removeAll(Collection<?> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean removeIf(
					Predicate<? super java.util.Map.Entry<K, V>> filter) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean retainAll(Collection<?> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int size() {
				return LinkedMap.this.size();
			}

			@Override
			public Spliterator<java.util.Map.Entry<K, V>> spliterator() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Stream<java.util.Map.Entry<K, V>> stream() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object[] toArray() {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> T[] toArray(T[] a) {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public void forEach(final BiConsumer<? super K, ? super V> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(final Object key) {
		final Entry e = this.lookUpMap.get(key);
		if (e == null) {
			return null;
		}
		return e.value;
	}

	/**
	 * Unsupported
	 * 
	 * @throws UnsupportedOperationException
	 *             whenever called
	 */
	@Override
	public V getOrDefault(final Object key, final V defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return this.tail == null;
	}

	@Override
	public Set<K> keySet() {
		final Set<K> list = new ArraySetList<>();
		Entry cur = this.head;
		while (cur != null) {
			list.add(cur.key);
			cur = cur.next;
		}
		return list;
	}

	@Override
	public V merge(
			final K key,
			final V value,
			final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V put(final K key, final V value) {
		final Entry newEntry = new Entry(key, value);
		if (this.tail == null) {
			this.head = this.tail = newEntry;
			this.lookUpMap.put(key, this.tail);
			return null;
		}
		final Entry oldEntry = this.lookUpMap.put(key, newEntry);
		if (oldEntry == null) {
			this.tail.next = newEntry;
			this.tail = this.tail.next;
			return null;
		}
		oldEntry.value = value;
		return oldEntry.value;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		for (final java.util.Map.Entry<? extends K, ? extends V> entry : m
				.entrySet()) {
			final K key = entry.getKey();
			final Entry newEntry = new Entry(key, entry.getValue());
			final Entry old = this.lookUpMap.put(key, newEntry);
			if (old == null) {
				this.tail.next = newEntry;
				this.tail = this.tail.next;
			}
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V replace(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(
			BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.lookUpMap.size();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

}
