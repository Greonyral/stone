package stone.modules.midiData;

final class TempoChange extends MidiEvent {
	final int tempo;

	TempoChange(int tempo, int delta) {
		super(delta, EventType.TEMPO);
		this.tempo = tempo;
	}

	@Override
	public final String toString() {
		return this.delta + " tempo: " + this.tempo;
	}

	@Override
	final int getChannel() {
		return 0; // tempo map
	}
}
