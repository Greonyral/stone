package stone.modules.midiData;

final class TempoChangeState {
	/**
	 * 
	 */
	private final MidiParserImpl midiParser;
	private final int microsPerQuarter;
	private final int ticks;
	final double minutes;

	TempoChangeState(
			@SuppressWarnings("hiding") final MidiParserImpl midiParser,
			final TempoChange tc, final TempoChangeState last) {
		this.midiParser = midiParser;
		this.microsPerQuarter = tc.tempo;
		this.ticks = this.midiParser.d.tmp += tc.delta;
		if (last == null) {
			if (tc.delta == 0) {
				this.minutes = 0;
			} else {
				// generate default
				this.minutes = this.midiParser.d.getMinutes(tc.delta);
			}
		} else {
			this.minutes = last.minutes
					+ last.getMinutes(this.ticks - last.ticks);
		}
	}

	@Override
	public final String toString() {
		return this.minutes + "@" + this.ticks + "[" + this.microsPerQuarter
				+ "]";
	}

	final double getMinutes(int deltaTicks) {
		final double quarters = (double) deltaTicks
				/ (double) this.midiParser.deltaTicksPerQuarter;
		return (quarters * this.microsPerQuarter) / 6e7;
	}
}