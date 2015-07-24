package stone.modules.fileEditor;

import java.util.HashSet;
import java.util.Set;


class Instrument {

	private final InstrumentType type;
	private final Set<Id> numbers;

	@SuppressWarnings("hiding")
	public Instrument(final InstrumentType type, final Set<Id> numbers) {
		this.type = type;
		if (numbers.isEmpty()) {
			this.numbers = new HashSet<>();
		} else {
			this.numbers = new HashSet<>(numbers);
		}
	}

	public final String name() {
		return this.type.name();
	}

	public final void print(final StringBuilder sb, final PrintType pt) {
		sb.append(NameScheme.printInstrumentName(this.type, pt));
		NameScheme.printInstrumentNumbers(sb, this.numbers);
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder(this.type.name()
				.toLowerCase());
		sb.setCharAt(0, (char) ((sb.charAt(0) + 'A') - 'a'));
		for (final Id number : this.numbers) {
			sb.append(" ");
			sb.append(number);
		}
		return sb.toString();
	}

	final InstrumentType type() {
		return this.type;
	}

	boolean uniqueIdx() {
		return this.numbers.size() < 2;
	}
}