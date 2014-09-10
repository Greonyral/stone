package stone.modules.fileEditor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author Nelphindal
 */
public class NameScheme {

	class Constant extends NameSchemeElement {
		private final String s;

		Constant(final String s, int[] idcs) {
			super(idcs);
			this.s = s;
		}

		@Override
		public final String toString() {
			return s;
		}

		@Override
		final void print(final StringBuilder sb) {
			sb.append(s);
		}
	}

	final static void printIdx(final StringBuilder sb, final String idcs) {
		sb.append(idcs);
	}

	final static StringBuilder printInstrumentName(final InstrumentType type) {
		final StringBuilder name = new StringBuilder(type.name().toLowerCase());
		name.setCharAt(0, name.substring(0, 1).toUpperCase().charAt(0));
		return name;
	}


	final static void printInstrumentNumbers(final StringBuilder sb,
			final Set<Integer> numbers) {
		if (numbers.isEmpty())
			return;
		final Iterator<Integer> i = numbers.iterator();
		sb.append(" ");
		sb.append(i.next());
		while (i.hasNext()) {
			sb.append(",");
			sb.append(i.next());
		}
	}
	final Map<Integer, Set<Instrument>> map = new HashMap<>();
	private final Variable DURATION = new Variable("DURATION"),
			PART_NUM = new Variable("PART_NUM") {

		@Override
		final void print(final StringBuilder sb, int track) {
			NameScheme.printIdx(sb, indices.get(track));
		}

	}, TOTAL_NUM = new Variable("TOTAL_NUM"), TITLE = new Variable(
			"TITLE"), MOD_DATE = new Variable("MOD_DATE"),
			INSTRUMENT = new Variable("INSTRUMENT") {

		@Override
		final void print(final StringBuilder sb, int track) {
			boolean first = true;
			for (final Instrument i : map.get(track)) {
				if (!first) {
					sb.append(", ");
				} else {
					first = false;
				}
				i.print(sb);
			}
		}
	};
	private final Map<String, Variable> variables = buildVariableMap();

	private final ArrayDeque<NameSchemeElement> elements = new ArrayDeque<>();

	private final Map<InstrumentType, Integer> countMap = new HashMap<>();

	final Map<Integer, String> indices = new HashMap<>();

	/**
	 * @param string
	 * @throws InvalidNameSchemeException
	 */
	public NameScheme(final String string) throws InvalidNameSchemeException {
		int pos = 0;
		int[] idcs = null;
		while (pos < string.length()) {
			final char c = string.charAt(pos);
			switch (c) {
				case '}':
					++pos;
					idcs = null;
					continue;
				case '$':
					if (idcs != null)
						throw new InvalidNameSchemeException();
					final int endIdx = string.indexOf('{', pos);
					final String idx = string.substring(pos + 1, endIdx);
					final String[] idcsS = idx.split(",");
					idcs = new int[idcsS.length];
					for (int i = 0; i < idcs.length; i++) {
						idcs[i] = Integer.parseInt(idcsS[i]);
					}
					pos = endIdx + 1;
					continue;
				case '%':
					final Variable v;
					int end = pos;
					do {
						final String variableTmp = string.substring(pos, ++end);
						final Variable vTmp = variables.get(variableTmp);
						if (vTmp != null) {
							v = vTmp;
							pos = end;
							break;
						}
					} while (true);
					if (idcs != null) {
						elements.add(v.dep(idcs));
					} else {
						elements.add(v);
					}
					continue;
			}
			final int[] ends =
					new int[] { string.indexOf('%', pos),
					string.indexOf('$', pos), string.indexOf('}', pos) };
			int end = string.length();
			for (final int endI : ends) {
				if (endI >= 0) {
					end = Math.min(end, endI);
				}
			}
			final String constant;
			if (end < 0) {
				constant = string.substring(pos);
			} else {
				constant = string.substring(pos, end);
			}
			pos += constant.length();
			elements.add(new Constant(constant, idcs));
		}
	}

	/** @return the title matching the name-scheme. Non existing parts will be omitted. */
	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final NameSchemeElement e : elements) {
			sb.append(e.toString());
		}
		return sb.toString();
	}

	private final Map<String, Variable> buildVariableMap() {
		final Map<String, Variable> map_ = new HashMap<>();
		map_.put("%title", TITLE);
		map_.put("%index", PART_NUM);
		map_.put("%total", TOTAL_NUM);
		map_.put("%duration", DURATION);
		map_.put("%mod", MOD_DATE);
		map_.put("%instrument", INSTRUMENT);
		return map_;
	}


	final void duration(final String duration) {
		DURATION.value(duration);
	}

	final void instrument(final Map<Integer, Set<Instrument>> instruments) {
		int countTotal = 0;
		for (final Set<Instrument> is : instruments.values()) {
			for (final Instrument i : is) {
				if (i.uniqueIdx()) {
					final Integer countOld = countMap.get(i.type());
					if (countOld == null) {
						countMap.put(i.type(), 1);
					} else {
						countMap.put(i.type(), countOld.intValue() + 1);
					}
					++countTotal;
				}
			}
		}
		TOTAL_NUM.value(String.valueOf(countTotal));
		map.putAll(instruments);
	}

	final void mod(final String mod) {
		MOD_DATE.value(mod);
	}

	final boolean needsDuration() {
		return elements.contains(DURATION);
	}

	final boolean needsMod() {
		return elements.contains(MOD_DATE);
	}

	final void partNum(final Map<Integer, String> newIndices) {
		int seq = 1;
		for (final Map.Entry<Integer, String> entry : newIndices.entrySet()) {
			if (entry.getValue().isEmpty()) {
				indices.put(entry.getKey(), Integer.valueOf(seq)
						.toString());
			} else {
				indices.put(entry.getKey(), entry.getValue());
			}
			seq++;
		}

	}

	final String print(int track) {
		final StringBuilder sb = new StringBuilder();
		for (final NameSchemeElement e : elements) {
			e.print(sb, track);
		}
		return sb.toString();
	}

	final void reset() {
		DURATION.clear();
		TITLE.clear();
		MOD_DATE.clear();
		TOTAL_NUM.value("0");
		PART_NUM.clear();
		map.clear();
		countMap.clear();
		indices.clear();
	}

	final void title(final String title) {
		TITLE.value(title);
	}

	final void totalNum(int total) {
		TOTAL_NUM.value(String.valueOf(total));
	}
}