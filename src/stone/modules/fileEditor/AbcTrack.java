package stone.modules.fileEditor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stone.io.IOHandler;
import stone.io.OutputStream;

class AbcTrack {

	class AbcParseState {
		private AbcChord chord = null;
		private char value = 0;
		private int n, d;
		private boolean readN = true;
		private final StringBuilder modifiers = new StringBuilder();

		void readFracSign() {
			readN = false;
			d = -2;
		}

		void startNewChord() {
			chord = new AbcChord();
		}

		void endChord() {
			generateNote();
			if (chord == null) {
				return;
			}
			notes.add(chord);
			chord = null;
		}

		void readNum(char c) {
			if (readN) {
				if (n < 0) {
					n = c - '0';
					d = -1;
				} else {
					n = ((n * 10) + c) - '0';
				}
			} else {
				if (d < 0) {
					d = c - '0';
				} else {
					d = ((d * 10) + c) - '0';
				}
			}
		}

		void endInput() {
			if (value != 0)
				generateNote();
			if (chord != null)
				throw new RuntimeException("end of chord");
		}

		void readVal(char c) {
			generateNote();
			n = -1;
			readN = true;
			value = c;
		}

		void generateNote() {
			if (value == 0)
				return;
			if (n < 0) {
				n = -n;
				d = 1;
			} else if (d < 0) {
				d = -d;
			} else if (d == 0) {
				d = 1;
			}

			final AbcNote note = new AbcNote(value, n, d, modifiers);
			modifiers.setLength(0);
			if (chord == null)
				notes.add(note);
			else
				chord.notes.add(note);
			value = 0;
		}

		void readVol(final String volume) {
			endChord();
			notes.add(new AbcVolumeChange(volume));
		}

		void addModifier(char c) {
			switch (c) {
			case '=':
			case '^':
				if (value != 0)
					generateNote();
				break;
			default:

			}
			modifiers.append(c);
		}
	}

	abstract class AbcEvent {

		class Modifier {

			final boolean isNatural;
			boolean isBeamed;
			final int sharps;

			public Modifier(String string) {
				int sharps = 0;
				while (string.contains("^")) {
					++sharps;
					string = string.replace("^", "");
				}
				while (string.contains(",")) {
					--sharps;
					string = string.replace(",", "");
				}
				isNatural = string.contains("=");
				isBeamed = string.contains("-");
				this.sharps = sharps;
			}

		}

		abstract double getDuration();

		@Override
		public abstract String toString();

		boolean isMeta() {
			return false;
		}

		abstract String abcLine();
	}

	abstract class ContiunableAbcEvent extends AbcEvent {

		abstract void reverseContinuation(
				final Map<String, List<AbcNote>> continuations);
	}

	class AbcNote extends ContiunableAbcEvent {
		final char value;
		final int n, d;
		final Modifier modifier;


		@SuppressWarnings("hiding")
		AbcNote(char value, int n, int d, final StringBuilder modifiers) {
			this.value = value;
			this.n = n;
			this.d = d;
			this.modifier = new Modifier(modifiers.toString());
		}

		@SuppressWarnings("hiding")
		public AbcNote(char value, int n, int d) {
			this.value = value;
			this.n = n;
			this.d = d;
			this.modifier = new Modifier("");
		}

		@Override
		double getDuration() {
			return (double) n / d;
		}

		@Override
		public String toString() {
			String s = "";
			for (int i = 0; i < modifier.sharps; ++i) {
				s += "^";
			}
			if (modifier.isNatural)
				s += "=";
			s += value;
			for (int i = modifier.sharps; i < 0; ++i) {
				s += ",";
			}
			return s;
		}

		@Override
		String abcLine() {
			String s = toString();
			if (n != 1) {
				s += n;
			}
			if (d != 1) {
				s += "/";
				if (d != 2)
					s += d;
			}
			if (modifier.isBeamed)
				s += "-";
			return s;
		}

		final List<AbcNote> getList(boolean createIfNotExistent,
				final Map<String, List<AbcNote>> continuations) {
			List<AbcNote> list;
			if (createIfNotExistent) {
				list = continuations.get(this.toString());
				if (list == null) {
					list = new ArrayList<>();
					continuations.put(this.toString(), list);
				}
			} else {
				list = continuations.remove(this.toString());
			}
			return list;
		}

		@Override
		void reverseContinuation(final Map<String, List<AbcNote>> continuations) {
			if (modifier.isBeamed) {
				final List<AbcNote> list = getList(true, continuations);
				list.add(this);
				return;
			}
			final List<AbcNote> list = getList(false, continuations);
			if (list == null)
				return;

			this.modifier.isBeamed = true;
			list.get(0).modifier.isBeamed = false;
		}
	}

	class AbcChord extends ContiunableAbcEvent {
		@SuppressWarnings("hiding")
		final Set<AbcNote> notes = new HashSet<>();

		@Override
		double getDuration() {
			double max = 0;
			for (final AbcNote note : notes) {
				max = Math.max(max, note.getDuration());
			}
			return max;
		}

		@Override
		public String toString() {
			return notes.toString();
		}

		@Override
		String abcLine() {
			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (final AbcNote note : notes) {
				sb.append(note.abcLine());
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		void reverseContinuation(final Map<String, List<AbcNote>> continuations) {
			for (final AbcNote note : notes) {
				note.reverseContinuation(continuations);
			}
		}
	}

	class AbcVolumeChange extends AbcEvent {
		final String volume;

		AbcVolumeChange(@SuppressWarnings("hiding") final String volume) {
			this.volume = volume;
		}

		@Override
		public String toString() {
			return volume;
		}

		@Override
		double getDuration() {
			return 0;
		}

		@Override
		boolean isMeta() {
			return true;
		}

		@Override
		String abcLine() {
			return "+" + volume + "+";
		}
	}

	private final int id;
	private final StringBuilder title = new StringBuilder();
	private List<String> lines = new ArrayList<>();
	private List<String> notesRaw = new ArrayList<>();
	private List<AbcEvent> notes = new ArrayList<>();

	private double base = AbcConstants.TIME_BASE; // L:
	private int speed = AbcConstants.DEFAULT_TEMPO; // Q:

	AbcTrack(final String idLine) {
		String line = idLine.toUpperCase().replaceAll(" ", "");
		if (!(line.charAt(0) == 'X' && line.charAt(1) == ':')) {
			throw new RuntimeException("Invalid ABC-Track header");
		}
		id = Integer.parseInt(line.substring(2));
	}

	@Override
	public String toString() {
		return id + " " + title.toString();
	}

	public int getId() {
		return id;
	}

	public void appendCommentLine(final String line) {
		if (!line.startsWith("%"))
			throw new IllegalArgumentException(line + " is no comment");
		lines.add(line);
	}

	public void appendTitle(final String line) {
		if (!line.startsWith("T:"))
			throw new IllegalArgumentException(line + " is no title");
		title.append(line);
		title.append("\n");
	}

	public boolean setSpeed(final String speed) {
		if (speed.contains("=")) {
			return setSpeed(speed.split("=")[1]);
		}
		try {
			this.speed = Integer.parseInt(speed);
			return true;
		} catch (final Exception e) {
			return false;
		}

	}

	public void appendNoteLine(final String line) {
		lines.add(line);
		notesRaw.add(line);
	}

	public int getSpeed() {
		return speed;
	}

	/**
	 * Calculates the duration in duration of notation. This means result of
	 * 0.25 is equivalent to duration of a quarter note.
	 * 
	 * @return duration
	 */
	public double getNoteDuration() {
		double duration = 0.0;
		if (notes.isEmpty())
			parse();
		for (final AbcEvent e : notes) {
			duration += e.getDuration();
		}
		return duration;
	}

	private void parse() {
		AbcParseState state = new AbcParseState();
		for (final String line : notesRaw) {
			int index = 0;
			while (index < line.length()) {
				final char c = line.charAt(index++);
				switch (c) {
				case '+':
					int delim = line.indexOf('+', index);
					final String volume = line.substring(index, delim);
					state.readVol(volume);
					index = delim + 1;
					assert index > 0;
					continue;

				case '/':
					state.readFracSign();
					continue;

				case '[':
					if (line.charAt(index) == '|')
						continue;
					state.startNewChord();
					continue;

				case ']':
					if (line.charAt(index - 2) == '|')
						continue;
					state.endChord();
					continue;

				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'A':
				case 'H':
				case 'B':

				case 'c':
				case 'd':
				case 'e':
				case 'f':
				case 'g':
				case 'a':
				case 'h':
				case 'b':

					// rests
				case 'x':
				case 'X':
				case 'z':
				case 'Z':
					state.readVal(c);
					continue;

				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					state.readNum(c);
					continue;

				case '=': // natural
				case '^': // sharp
				case ',': // flat
				case '-':
					state.addModifier(c);
					continue;

				case ':':
				case '|':
					continue;
				}
				state.generateNote();
			}
			// end of line
			state.generateNote();
		}
		// end of input
		state.endInput();
	}

	public void setBase(final String line) {
		int n = 0;
		int d = 0;
		boolean readN = true;
		for (final char c : line.toCharArray()) {
			if (c == '=') {
				break;
			} else if (c == '/') {
				readN = false;
			} else if (readN) {
				n += c - '0';
			} else {
				d += c - '0';
			}
		}
		base = (double) n / d;
	}

	public void addMeta(final String line) {
		lines.add(line);
	}

	public void appendPause(double duration) {
		while (duration > 8.0) {
			appendPause(8.0);
			duration -= 8.0;
		}
		if (duration < 1e-5) {
			// numeric 0
			// will result into */0 => omit
			return;
		}
		int n, d;
		final String frac = toDecFrac(duration);
		final String[] frac_a = frac.split("/");
		if (frac.isEmpty()) {
			n = 1;
			d = 1;
		} else if (frac_a.length == 0) {
			n = 1;
			d = 2;
		} else if (frac_a.length == 1) {
			n = Integer.parseInt(frac_a[0]);
			if (frac.endsWith("/"))
				d = 2;
			else
				d = 1;
		} else {
			n = Integer.parseInt(frac_a[0]);
			d = Integer.parseInt(frac_a[1]);
		}
		final AbcNote pause = new AbcNote('z', n, d);
		lines.add(pause.abcLine());
		notesRaw.add(pause.abcLine());
		if (!notes.isEmpty())
			notes.add(pause);
	}

	private static long ggT(long a, long b) {
		while (true) {
			if (a > b) {
				a %= b;
				if (a == 0)
					return b;
			} else {
				b %= a;
				if (b == 0)
					return a;
			}
		}
	}

	public void reverseNotes() {
		final ArrayDeque<String> headReversed = new ArrayDeque<>();
		final ArrayDeque<AbcEvent> notesReversed = new ArrayDeque<>();
		int lineIdx = 0;
		boolean head = true;
		while (lineIdx < lines.size()) {
			final String line = lines.get(lineIdx++);
			if (notesRaw.get(0) == line) {
				break;
			} else if (head) {
				// keep only documentary lines at the start
				headReversed.addLast(line);
			}
		}
		final String title = this.title.toString();
		this.title.setLength(0);
		this.title.append(title);

		lines.clear();
		if (notes.isEmpty())
			parse();
		notesRaw.clear();
		lines.addAll(headReversed);

		final Map<Class<? extends AbcEvent>, AbcEvent> lastEventMap = new HashMap<>();
		lastEventMap.put(AbcVolumeChange.class, new AbcVolumeChange("mf"));
		final Map<String, List<AbcNote>> continuations = new HashMap<>();
		for (final AbcEvent e : notes) {
			if (e.isMeta()) {
				final AbcEvent lastEvent = lastEventMap.get(e.getClass());
				if (lastEvent != null) {
					notesReversed.addFirst(lastEvent);
				}
				lastEventMap.put(e.getClass(), e);
			} else {
				// a- <--last
				// a/ <-- e
				// reversed:
				// a/- <-- e
				// a <-- last
				ContiunableAbcEvent ce = (ContiunableAbcEvent) e;
				ce.reverseContinuation(continuations);
				notesReversed.addFirst(e);
			}
		}
		if (!continuations.isEmpty()) {
			// TODO possible and needs extra care?
		}
		notesReversed.addFirst(lastEventMap.get(AbcVolumeChange.class));
		notes.clear();
		notes.addAll(notesReversed);
	}

	static protected String toDecFrac(double duration) {
		long n, d, ggT;
		d = (long) 26240 * 12;
		n = (long) (duration * d);
		ggT = ggT(d, n);
		d /= ggT;
		n /= ggT;
		if (n == 1) {
			if (d == 1) {
				return "";
			} else if (d == 2)
				return "/";
		}
		if (d == 1)
			return Integer.toString((int) n);
		else if (d == 2)
			return Integer.toString((int) n) + "/";
		return Integer.toString((int) n) + "/" + Integer.toString((int) d);
	}

	public void write(final IOHandler io, final OutputStream out) {
		final String NL = stone.util.FileSystem.getLineSeparator();
		io.write(out, title.toString());
		io.write(out, "L: 1/" + (int) (1 / base) + NL);
		io.write(out, "Q: " + speed + NL);
		for (final String line : lines) {
			io.write(out, line);
			io.write(out, NL);
		}
		for (final AbcEvent e : notes) {
			io.write(out, e.abcLine());
			io.write(out, NL);
		}
	}
}
