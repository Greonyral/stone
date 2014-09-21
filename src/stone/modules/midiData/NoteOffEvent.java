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
		if (format == 1) {
			return delta + " off: " + k + " " + v;
		}
		if (format == 0) {
			return delta + " off: " + k + " " + v + "@" + channel;
		}
		return delta + " off: " + k + " " + v + "@" + channel + ","
				+ format;
	}

	@Override
	final int getChannel() {
		return 0xff & channel;
	}

	final int getKey() {
		return k;
	}
}
