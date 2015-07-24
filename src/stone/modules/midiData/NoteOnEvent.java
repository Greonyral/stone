package stone.modules.midiData;

final class NoteOnEvent extends MidiEvent {
	private final byte k, v, channel;
	private final int format;

	@SuppressWarnings("hiding")
	NoteOnEvent(byte k, byte v, byte channel, int delta, int format) {
		super(delta, EventType.NOTE_ON);
		this.k = k;
		this.v = v;
		this.channel = channel;
		this.format = format;
	}

	@Override
	public final String toString() {
		if (this.format == 1) {
			return this.delta + " on: " + this.k + " " + this.v;
		}
		if (this.format == 0) {
			return this.delta + " on: " + this.k + " " + this.v + "@"
					+ this.channel;
		}
		return this.delta + " on: " + this.k + " " + this.v + "@"
				+ this.channel + "," + this.format;
	}

	@Override
	final int getChannel() {
		return this.channel;
	}

	final int getKey() {
		return this.k;
	}

	final int getVelocity() {
		return this.v;
	}
}
