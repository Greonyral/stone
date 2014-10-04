package stone.modules.abcCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import stone.modules.midiData.MidiInstrumentDropTarget;


final class ParamSet {

	private final Map<BruteParams<?>, String> params = new HashMap<>();

	ParamSet() {
		for (final BruteParams<?> p : BruteParams.valuesLocal()) {
			if (p == null) {
				continue;
			}
			params.put(p, null);
		}
	}

	@Override
	public final boolean equals(final Object o) {
		if (ParamSet.class.isInstance(o)) {
			return equals(ParamSet.class.cast(o));
		}
		return false;
	}

	public final boolean equals(final ParamSet o) {
		for (final Map.Entry<BruteParams<?>, String> entry : params
				.entrySet()) {
			final String valueO = o.params.get(entry.getKey());
			final String value = entry.getValue();
			if ((value == null) && (valueO == null)) {
				continue;
			} else if (value == null) {
				if (!entry.getKey().defaultValue().toString().equals(
						valueO)) {
					return false;
				}
			} else if (valueO == null) {
				if (!entry.getKey().defaultValue().toString()
						.equals(value)) {
					return false;
				}
			} else if (!value.equals(valueO)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String toString() {
		return params.entrySet().toString();
	}

	final void adeptValues(final Track track,
			final MidiInstrumentDropTarget instrument) {
		for (final Entry<BruteParams<?>, String> entry : params.entrySet()) {
			entry.getKey().setLocalValue(track, instrument,
					entry.getValue());
		}
	}

	final void put(final BruteParams<?> param, final String string) {
		params.put(param, string);
	}
}