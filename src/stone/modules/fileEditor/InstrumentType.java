package stone.modules.fileEditor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


enum InstrumentType {
	BAGPIPES("bagpipe"), CLARINET("clarinets"), COWBELL("cowbells",
			"bells"), DRUMS("drum"), FLUTE("flutes"), HARP("harps"), HORN(
			"horns"), LUTE("lutes"), MOOR_COWBELL, THEORBO, PIBGORN;

	private final String[] keys;
	private static final Map<String, InstrumentType> map = buildMap();

	public final static InstrumentType get(final String string) {
		return InstrumentType.map.get(string);
	}

	private final static Map<String, InstrumentType> buildMap() {
		final Map<String, InstrumentType> map_ = new HashMap<>();
		try {
			for (final Field f : InstrumentType.class.getFields()) {
				final InstrumentType t = (InstrumentType) f.get(null);
				for (final String key : t.keys) {
					map_.put(key, t);
				}
				map_.put(f.getName().toLowerCase(), t);
			}
		} catch (final Exception e) {
			return null;
		}
		return map_;
	}

	private InstrumentType(final String... keys) {
		this.keys = keys;
	}
}
