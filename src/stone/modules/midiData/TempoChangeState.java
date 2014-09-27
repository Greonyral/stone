package stone.modules.midiData;

final class TempoChangeState {
	/**
	 * 
	 */
	private final MidiParserImpl midiParser;
	private final int microsPerQuarter;
	private final int ticks;
	final double minutes;

	TempoChangeState(final MidiParserImpl midiParser,
			final TempoChange tc, final TempoChangeState last) {
		this.midiParser = midiParser;
		microsPerQuarter = tc.tempo;
		ticks = this.midiParser.d.tmp += tc.delta;
		if (last == null) {
			if (tc.delta == 0) {
				minutes = 0;
			} else {
				// generate default
				minutes = this.midiParser.d.getMinutes(tc.delta);
			}
		} else {
			minutes = last.minutes + last.getMinutes(ticks - last.ticks);
		}
	}

	@Override
	public final String toString() {
		return minutes + "@" + ticks + "[" + microsPerQuarter + "]";
	}

	final double getMinutes(int deltaTicks) {
		final double quarters =
				(double) deltaTicks
						/ (double) midiParser.deltaTicksPerQuarter;
		return (quarters * microsPerQuarter) / 6e7;
	}
}