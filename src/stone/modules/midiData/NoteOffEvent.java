package stone.modules.midiData;

final class NoteOffEvent extends MidiEvent {
	private final byte k, v, channel;
	private final int format;

	NoteOffEvent(byte k, byte v, byte channel, int delta, int format) {
		super(delta, EventType.NOTE_OFF);
		this.k = k;
		this.v = v;
		this.channel = channel;
		this.format = format;
	}

	@Override
	public final String toString() {
		if (this.format == 1) {
			return this.delta + " off: " + this.k + " " + this.v;
		}
		if (this.format == 0) {
			return this.delta + " off: " + this.k + " " + this.v + "@"
					+ this.channel;
		}
		return this.delta + " off: " + this.k + " " + this.v + "@"
				+ this.channel + "," + this.format;
	}

	@Override
	final int getChannel() {
		return 0xff & this.channel;
	}

	final int getKey() {
		return this.k;
	}
}
