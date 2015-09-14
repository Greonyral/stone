package stone.modules.fileEditor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import stone.util.Debug;
import stone.util.Time;


/**
 * @author Nelphindal
 */
public class NameScheme {

	class Constant extends NameSchemeElement {

		private final String s;

		@SuppressWarnings("hiding")
		Constant(final String s, int[] idcs) {
			super(idcs);
			this.s = s;
		}

		@Override
		public final String toString() {
			return this.s;
		}

		@Override
		final void print(final StringBuilder sb) {
			sb.append(this.s);
		}
	}

	final static void printIdx(final StringBuilder sb, final String idcs) {
		sb.append(idcs);
	}

	final static String printInstrumentName(final InstrumentType type,
			final PrintType pt) {
		return type.name(pt);
	}


	final static void printInstrumentNumbers(final StringBuilder sb,
			final Set<Id> numbers) {
		if (numbers.isEmpty()) {
			return;
		}
		final Iterator<Id> i = numbers.iterator();
		sb.append(" ");
		sb.append(i.next());
		while (i.hasNext()) {
			sb.append(",");
			sb.append(i.next());
		}
	}

	private final Map<Integer, Set<Instrument>> map = new HashMap<>();

	private final Variable DURATION = new Variable("DURATION"),
			PART_NUM = new Variable("PART_NUM") {

				@Override
				final void print(final StringBuilder sb, int track) {
					NameScheme.printIdx(sb, NameScheme.this.indices.get(track));
				}

			};

	private final Variable TOTAL_NUM = new Variable("TOTAL_NUM") {

		@Override
		void clear() {
			value("0");
		}
	};

	private final Variable TITLE = new Variable("TITLE") {

		@SuppressWarnings("hiding")
		private final Map<Integer, String> map = new HashMap<>();

		@Override
		final void clear() {
			super.clear();
			this.map.clear();
		}

		@Override
		final void print(final StringBuilder sb, int track) {
			final String title = this.map.get(track);
			if (title != null) {
				sb.append(title);
				return;
			}
			super.print(sb, track);
			return;
		}

		@Override
		void value(@SuppressWarnings("hiding") final Map<Integer, String> map) {
			this.map.putAll(map);
		}
	};


	@Deprecated
	private final Variable MOD_DATE = new Variable("MOD_DATE");

	private final Variable MOD_YEAR = new Variable("YEAR");
	private final Variable MOD_MONTH = new Variable("MONTH") {

		@Override
		void value(final String m) {
			final char c = m.charAt(0);
			if ((c >= '0') && (c <= '9')) {
				final int index = Integer.parseInt(m);
				value(Time.getShortMonthNames()[index - 1]);
				return;
			}
			super.value(m);
		}
	};
	private final Variable MOD_MONTH_STRING = new Variable("MONTH_STRING") {

		@Override
		void value(final String m) {
			final char c = m.charAt(0);
			if ((c >= '0') && (c <= '9')) {
				value(Time.getMonthName(m));
				return;
			}
			super.value(m);
		}
	};
	private final Variable MOD_DAY = new Variable("DAY");

	private final Variable INSTRUMENT = new Variable("INSTRUMENT") {

		@Override
		final void print(final StringBuilder sb, int track) {
			boolean first = true;
			for (final Instrument i : NameScheme.this.map.get(track)) {
				if (!first) {
					sb.append(", ");
				} else {
					first = false;
				}
				i.print(sb, PrintType.NORMAL);
			}
		}
	};
	private final Variable INSTRUMENT_CAPS = new Variable("INSTRUMENT_CAPS") {

		@Override
		final void print(final StringBuilder sb, int track) {
			boolean first = true;
			for (final Instrument i : NameScheme.this.map.get(track)) {
				if (!first) {
					sb.append(", ");
				} else {
					first = false;
				}
				i.print(sb, PrintType.UP);
			}
		}
	};
	private final Variable INSTRUMENT_START_UPPER_CASED = new Variable(
			"INSTRUMENT_START_UPPER_CASED") {

		@Override
		final void print(final StringBuilder sb, int track) {
			boolean first = true;
			for (final Instrument i : NameScheme.this.map.get(track)) {
				if (!first) {
					sb.append(", ");
				} else {
					first = false;
				}
				i.print(sb, PrintType.START_UP);
			}
		}
	};
	private final Map<String, Variable> variables = buildVariableMap();

	private final ArrayDeque<NameSchemeElement> elements = new ArrayDeque<>();

	private final Map<InstrumentType, Integer> countMap = new HashMap<>();

	final Map<Integer, String> indices = new HashMap<>();

	/**
	 * @param string
	 *            -
	 * @throws InvalidNameSchemeException
	 *             if parsing <i>string</i> raises an error
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
				if (idcs != null) {
					throw new InvalidNameSchemeException();
				}
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
					final Variable vTmp = this.variables.get(variableTmp);
					if (vTmp != null) {
						final char next = string.charAt(end);
						switch (next) {
						case '/':
						case ' ':
						case '(':
						case ')':
						case '$':
						case '{':
						case '}':
						case ']':
						case '[':
						case '-':
						case '.':
						case ':':
						case '%':
							v = vTmp;
							pos = end;
							break;
						default:
							continue;
						}
						break;
					}
				} while (true);
				if (idcs != null) {
					this.elements.add(v.dep(idcs));
				} else {
					this.elements.add(v);
				}
				continue;
			default:
				break;
			}
			final int[] ends = new int[] { string.indexOf('%', pos),
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
			this.elements.add(new Constant(constant, idcs));
		}
	}

	/**
	 * Sets titles for each track
	 * 
	 * @param titles
	 *            -
	 */
	public void title(final Map<Integer, String> titles) {
		this.TITLE.value(titles);
	}

	/**
	 * Checks if uniform title is set
	 * 
	 * @return <i>true</i> if uniform title is set
	 */
	public boolean titleIsEmpty() {
		return this.TITLE.getValue() == null;
	}


	/**
	 * @return the title matching the name-scheme. Non existing parts will be
	 *         omitted.
	 */
	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final NameSchemeElement e : this.elements) {
			sb.append(e.toString());
		}
		return sb.toString();
	}

	private final Map<String, Variable> buildVariableMap() {
		final Map<String, Variable> map_ = new HashMap<>();
		map_.put("%title", this.TITLE);
		map_.put("%index", this.PART_NUM);
		map_.put("%total", this.TOTAL_NUM);
		map_.put("%duration", this.DURATION);
		map_.put("%mod", this.MOD_DATE);
		map_.put("%y", this.MOD_YEAR);
		map_.put("%d", this.MOD_DAY);
		map_.put("%m", this.MOD_MONTH);
		map_.put("%b", this.MOD_MONTH_STRING);
		map_.put("%instrument", this.INSTRUMENT);
		map_.put("%Instrument", this.INSTRUMENT_START_UPPER_CASED);
		map_.put("%INSTRUMENT", this.INSTRUMENT_CAPS);
		return map_;
	}

	final void duration(final String duration) {
		this.DURATION.value(duration);
	}

	final void instrument(final Map<Integer, Set<Instrument>> instruments) {
		int countTotal = 0;
		for (final Set<Instrument> is : instruments.values()) {
			for (final Instrument i : is) {
				if (i.uniqueIdx()) {
					final Integer countOld = this.countMap.get(i.type());
					if (countOld == null) {
						this.countMap.put(i.type(), 1);
					} else {
						this.countMap.put(i.type(), countOld.intValue() + 1);
					}
					++countTotal;
				}
			}
		}
		this.TOTAL_NUM.value(String.valueOf(countTotal));
		this.map.putAll(instruments);
	}

	final void mod(final String month, final String day, final String year) {
		this.MOD_DAY.value(day);
		this.MOD_MONTH.value(month);
		this.MOD_MONTH_STRING.value(month);
		this.MOD_YEAR.value(year);
	}

	final boolean needsDuration() {
		return this.elements.contains(this.DURATION);
	}

	final boolean needsMod() {
		return this.elements.contains(this.MOD_DATE)
				|| this.elements.contains(this.MOD_DAY)
				|| this.elements.contains(this.MOD_MONTH)
				|| this.elements.contains(this.MOD_MONTH_STRING)
				|| this.elements.contains(this.MOD_YEAR);
	}

	final void partNum(final Map<Integer, String> newIndices) {
		int seq = 1;
		for (final Map.Entry<Integer, String> entry : newIndices.entrySet()) {
			if (entry.getValue().isEmpty()) {
				this.indices.put(entry.getKey(), Integer.valueOf(seq)
						.toString());
			} else {
				this.indices.put(entry.getKey(), entry.getValue());
			}
			seq++;
		}

	}

	final String print(int track) {
		final StringBuilder sb = new StringBuilder();
		for (final NameSchemeElement e : this.elements) {
			e.print(sb, track);
		}
		Debug.print("%s\n", sb.toString());
		return sb.toString();
	}

	final void reset() {
		Variable.clearAll();

		this.map.clear();
		this.countMap.clear();
		this.indices.clear();
	}

	final void title(final String title) {
		this.TITLE.value(title);
	}

	final void totalNum(int total) {
		this.TOTAL_NUM.value(String.valueOf(total));
	}
}
