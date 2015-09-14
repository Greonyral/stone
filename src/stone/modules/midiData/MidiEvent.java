package stone.modules.midiData;

/**
 * Superclass of all events occurring in a midi file
 * 
 * @author Nelphindal
 */
public abstract class MidiEvent {

	/** offset in milliseconds */
	protected final int delta;

	private final EventType type;

	/**
	 * Creates a new event, with delta milliseconds offset to previous event
	 * 
	 * @param delta
	 *            decoded delta ticks
	 * @param type
	 *            midi event
	 */
	@SuppressWarnings("hiding")
	protected MidiEvent(int delta, final EventType type) {
		this.delta = delta;
		this.type = type;
	}

	// final static List<MidiEvent> convert(final Track track, final MidiParser
	// parser) {
	// final List<MidiEvent> events = new ArrayList<>(track.size());
	// long lasttick = 0;
	// for (int i = 0; i < track.size(); i++) {
	// final javax.sound.midi.MidiEvent event = track.get(i);
	// final long tick = event.getTick();
	// final int delta = (int) (tick - lasttick);
	// lasttick = tick;
	// final MidiEvent createdEvent =
	// parser.createEvent(event.getMessage().getMessage(), delta);
	// if (createdEvent != null)
	// events.add(createdEvent);
	// }
	// return events;
	// }

	/**
	 * Returns a string representation of this midi event. All subclasses have
	 * to implement this method.
	 */
	@Override
	public abstract String toString();

	abstract int getChannel();

	final EventType getType() {
		return this.type;
	}
}