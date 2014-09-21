package stone.modules.fileEditor;

import java.util.HashSet;
import java.util.Set;


class Instrument {

	private final InstrumentType type;
	private final Set<Integer> numbers;

	public Instrument(final InstrumentType type, final Set<Integer> numbers) {
		this.type = type;
		if (numbers.isEmpty()) {
			this.numbers = new HashSet<>();
		} else {
			this.numbers = new HashSet<>(numbers);
		}
	}

	public final String name() {
		return type.name();
	}

	public final void print(final StringBuilder sb) {
		sb.append(NameScheme.printInstrumentName(type));
		NameScheme.printInstrumentNumbers(sb, numbers);
	}

	@Override
	public final String toString() {
		final StringBuilder sb =
				new StringBuilder(type.name().toLowerCase());
		sb.setCharAt(0, (char) ((sb.charAt(0) + 'A') - 'a'));
		for (final Integer number : numbers) {
			sb.append(" ");
			sb.append(number);
		}
		return sb.toString();
	}

	final InstrumentType type() {
		return type;
	}

	boolean uniqueIdx() {
		return numbers.size() < 2;
	}
}