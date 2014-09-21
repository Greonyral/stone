package stone.modules.midiData;

final class NoteOnEvent extends MidiEvent {
	private final byte k, v, channel;
	private final int format;

	NoteOnEvent(byte k, byte v, byte channel, int delta, int format) {
		super(delta, EventType.NOTE_ON);
		this.k = k;
		this.v = v;
		this.channel = channel;
		this.format = format;
	}

	@Override
	public final String toString() {
		if (format == 1) {
			return delta + " on: " + k + " " + v;
		}
		if (format == 0) {
			return delta + " on: " + k + " " + v + "@" + channel;
		}
		return delta + " on: " + k + " " + v + "@" + channel + ","
				+ format;
	}

	@Override
	final int getChannel() {
		return channel;
	}

	final int getKey() {
		return k;
	}

	final int getVelocity() {
		return v;
	}
}
