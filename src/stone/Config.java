package stone;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import stone.util.LinkedMap;

final class Config {

	private static final String CONFIG_FILE = "config.txt";
	
	final Map<String, Map<String, String>> mapSKV = new HashMap<>();
	final Map<String, String> mapKV = new HashMap<>();

	Config() {
		final InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
		byte[] bytes;
		try {
			bytes = new byte[in.available()];
			in.read(bytes);
			final String[] lines = new String(bytes).split("\n");
			Map<String, String> map = mapKV;
			for (final String line : lines) {
				if (!line.contains(" ")) {
					final String section = line;
					map = mapSKV.get(section);
					if (map == null) {
						final Map<String, String> _map = new LinkedMap<String, String>();
						mapSKV.put(section, _map);
						map = _map;
					}
				} else {
					final String[] s = line.split(" ", 2);
					final String key = s[0];
					final String value = s[1];
					map.put(key, value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	final String getValue(final String key) {
		return mapKV.get(key);
	}

	public Set<String> getSection(final String section) {
		return mapSKV.get(section).keySet();
	}

}
