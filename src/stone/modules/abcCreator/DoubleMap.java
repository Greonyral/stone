package stone.modules.abcCreator;

import java.util.HashMap;
import java.util.Map;


class DoubleMap<K0, K1, V> {


	private final Map<K0, Map<K1, V>> map = new HashMap<>();

	public final void clear() {
		this.map.clear();
	}

	public V get(final K0 key0, final K1 key1) {
		final Map<K1, V> e0 = this.map.get(key0);
		if (e0 == null) {
			return null;
		}
		return e0.get(key1);
	}

	public V put(final K0 key0, final K1 key1, final V value) {
		final Map<K1, V> e0 = this.map.get(key0);
		final Map<K1, V> e1;
		if (e0 == null) {
			e1 = new HashMap<>();
			this.map.put(key0, e1);
		} else {
			e1 = e0;
		}
		return e1.put(key1, value);
	}

	@Override
	public String toString() {
		return this.map.toString();
	}

}
