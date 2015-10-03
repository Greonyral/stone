package stone.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for generating colored output for console
 * @author Nelphindal
 *
 */
public abstract class Color {
	
	/**
	 * Various available colors
	 * @author Nelphindal
	 *
	 */
	public enum ColorType {
		RED, GREEN, BLUE, BLACK;
		
		ColorType() {
		}
		
		private WindowsColor toWinColor() {
			return winMap.get(name());
		}


		private UnixColor toUnixColor() {
			return unixMap.get((name()));
		}
	}

	protected final int value;

	private static final Map<String, UnixColor> unixMap = createUnixMap();
	private static final Map<String, WindowsColor> winMap = new HashMap<>();

	Color(@SuppressWarnings("hiding") int value) {
		this.value = value;
	}


	public final static String print(ColorType fg, ColorType bg, final String string,
			final Object[] args) {
		switch (FileSystem.type) {
		case UNIX:
			return print(fg.toUnixColor(), bg.toUnixColor(), string, args);
		case WINDOWS:
			return print(fg.toWinColor(), bg.toWinColor(), string, args);
		}
		return null;
	}


	private static String print(WindowsColor fg, WindowsColor bg,
			String format, Object[] args) {
		// TODO colorize
		return String.format(format, args);
	}

	private static String print(final UnixColor fg, final UnixColor bg,
			final String format, final Object[] args) {
		final StringBuilder sb = new StringBuilder();
		sb.appendFirst(String.format("\033[%dm\033[%dm", fg.value,
				bg.value + 10));
		sb.appendLast(String.format(format, args));
		final boolean endWithNL;
		if (endWithNL = (sb.getLast() == '\n'))
			sb.removeLast();
		sb.appendLast(String.format("\033[0m"));
		if (endWithNL)
			sb.appendLast('\n');
		return sb.toString();
	}
	
	static final Map<String, UnixColor> createUnixMap() {
		final Field[] colors = UnixColor.class.getFields();
		final Map<String, UnixColor> map = new HashMap<>(colors.length);
		for (final Field f : colors) {
			final String name = f.getName().toUpperCase();
			try {
				final UnixColor color = (UnixColor) f.get(null);
				map.put(name, color);
			} catch (final IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return map;
	}
}