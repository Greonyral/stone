package stone.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LinkedMap<K, V> implements Map<K, V> {

	class Entry {
		private final K key;
		private V value;
		private Entry next;

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
		throw new UnsupportedOperationException();
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
		} else {
			final Entry oldEntry = this.lookUpMap.put(key, newEntry);
			if (oldEntry == null) {
				this.tail.next = newEntry;
				this.tail = this.tail.next;
				return null;
			} else {
				oldEntry.value = value;
				return oldEntry.value;
			}
		}
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
