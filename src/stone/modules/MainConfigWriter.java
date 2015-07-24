package stone.modules;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stone.util.FileSystem;


final class MainConfigWriter implements Runnable {

	/**
	 * 
	 */
	private final Main main;

	public MainConfigWriter(@SuppressWarnings("hiding") Main main) {
		this.main = main;
	}

	@Override
	public void run() {
		final java.io.OutputStream out;
		final StringBuilder sb = new StringBuilder();

		synchronized (this.main.configOld) {
			synchronized (this.main.configNew) {
				// throw all values into configOld
				for (final Map.Entry<String, Map<String, String>> entryMap : this.main.configNew
						.entrySet()) {
					final Map<String, String> map = this.main.configOld
							.get(entryMap.getKey());
					if (map == null) {
						this.main.configOld.put(entryMap.getKey(),
								entryMap.getValue());
					} else {
						map.putAll(entryMap.getValue());
					}
				}
				this.main.configNew.clear();
			}

			// search for null keys
			final Set<String> sectionsToRemove = new HashSet<>();
			for (final Map.Entry<String, Map<String, String>> entryMap : this.main.configOld
					.entrySet()) {
				final Set<String> keysToRemove = new HashSet<>();
				for (final Map.Entry<String, String> map : entryMap.getValue()
						.entrySet()) {
					if ((map.getValue() == null) || map.getValue().isEmpty()) {
						keysToRemove.add(map.getKey());
					}
				}
				for (final String key : keysToRemove) {
					entryMap.getValue().remove(key);
				}
				if (entryMap.getValue().isEmpty()) {
					sectionsToRemove.add(entryMap.getKey());
				}
			}
			for (final String section : sectionsToRemove) {
				this.main.configOld.remove(section);
			}
		}
		// write some short info at the start
		sb.append("# config file for SToNe by Nelphindal");
		sb.append(FileSystem.getLineSeparator());
		sb.append("# Lines leading with # will be inonred");
		sb.append(FileSystem.getLineSeparator());
		sb.append("# Use at own risk according to the documentation in the manual");
		sb.append(FileSystem.getLineSeparator());
		sb.append(FileSystem.getLineSeparator());

		for (final Map.Entry<String, Map<String, String>> sections : this.main.configOld
				.entrySet()) {
			sb.append(sections.getKey());
			sb.append(FileSystem.getLineSeparator());
			for (final Map.Entry<String, String> entries : sections.getValue()
					.entrySet()) {
				sb.append("\t");
				sb.append(entries.getKey());
				sb.append(" = ");
				sb.append(entries.getValue());
				sb.append(FileSystem.getLineSeparator());
			}
		}

		try {
			out = new java.io.FileOutputStream(Main.homeSetting.toFile());
			try {
				out.write(sb.toString().getBytes());
				out.flush();
			} finally {
				out.close();
			}
		} catch (final IOException e) {
			Main.homeSetting.delete();
		}
	}
}