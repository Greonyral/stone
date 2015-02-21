package stone;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import stone.util.LinkedMap;

public final class Config {

	private static final String CONFIG_FILE = "config.txt";

	private final Map<String, Map<String, String>> mapSKV = new HashMap<>();
	private final Map<String, String> mapKV = new HashMap<>();

	private final static Config instance = new Config();

	public static Config getInstance() {
		return instance;
	}

	private Config() {
		final InputStream in = getClass().getClassLoader().getResourceAsStream(
				CONFIG_FILE);
		if (in == null) {
			return;
		}
		byte[] bytes;
		try {
			bytes = new byte[in.available()];
			in.read(bytes);
			final String[] lines = new String(bytes).split("\n");
			Map<String, String> map = this.mapKV;
			for (final String line : lines) {
				if (!line.contains(" ")) {
					final String section = line;
					map = this.mapSKV.get(section);
					if (map == null) {
						final Map<String, String> _map = new LinkedMap<String, String>();
						this.mapSKV.put(section, _map);
						map = _map;
					}
				} else {
					final String[] s = line.split(" ", 2);
					final String key = s[0];
					final String value = s[1];
					map.put(key, value);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Set<String> getSection(final String section) {
		return this.mapSKV.get(section).keySet();
	}

	public final String getValue(final String key) {
		return this.mapKV.get(key);
	}

}
