package stone.modules.midiData;

import java.util.Map.Entry;
import java.util.TreeMap;


final class Duration {
	/**
	 * 
	 */
	private final MidiParserImpl midiParser;

	int tmp;

	private TempoChangeState last;
	private final TreeMap<Integer, TempoChangeState> tempoIntervals = new TreeMap<>();

	Entry<Integer, TempoChangeState> lastChange;

	Duration(MidiParserImpl midiParserImpl) {
		this.midiParser = midiParserImpl;
	}

	@Override
	public final String toString() {
		return this.tempoIntervals.toString();
	}

	final void addTempoChange(final TempoChange event) {
		this.last = new TempoChangeState(this.midiParser, event, this.last);
		this.tempoIntervals.put(this.tmp, this.last);
	}

	final double getMinutes(int delta) {
		this.lastChange = this.tempoIntervals.floorEntry(delta);
		if (this.lastChange == null) {
			// createDefault and retry
			final TempoChangeState last1 = this.last;
			final int tmp1 = this.tmp;
			this.tmp = 0;
			this.last = null;
			addTempoChange(new TempoChange(0x7a120, 0));
			this.tmp = tmp1;
			if (last1 != null) {
				this.last = last1;
			}
			return getMinutes(delta);
		}
		return this.lastChange.getValue().minutes
				+ this.lastChange.getValue().getMinutes(
						delta - this.lastChange.getKey().intValue());
	}

	final void progress(final MidiEvent event) {
		this.tmp += event.delta;
	}

	final void reset() {
		this.tmp = 0;
		this.last = null;
		this.tempoIntervals.clear();
	}
}