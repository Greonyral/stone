package stone.modules.midiData;

final class Break extends MidiEvent {

	Break(int delta) {
		super(delta, EventType.NOP);
	}

	@Override
	public final String toString() {
		return this.delta + " NOP";
	}

	@Override
	final int getChannel() {
		return 0; // tempo map
	}
}
