package stone.modules.midiData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import stone.StartupContainer;
import stone.io.InputStream;
import stone.util.Path;
import stone.util.TaskPool;


/**
 * MidiParser to read from a midi file.
 * 
 * @author Nelphindal
 */
final class MidiParserImpl extends MidiParser {

	private final byte[] midiHeaderBufferBytes = new byte[14];
	private final ByteBuffer midiHeaderBuffer = ByteBuffer
			.wrap(this.midiHeaderBufferBytes);
	private int nextN;
	private boolean empty = true;
	final Duration d = new Duration(this);

	int deltaTicksPerQuarter;
	private int eventCount;

	@SuppressWarnings("unused")
	private final TaskPool taskPool; // for later multi-threaded decoding

	MidiParserImpl(final StartupContainer sc) {
		super(sc.getIO(), sc.getMaster());
		this.taskPool = sc.getTaskPool();
	}

	/**
	 * Parses format 1
	 * 
	 * @param in
	 * @throws IOException
	 * @throws ParsingException
	 */
	private final void parse1(final InputStream in) throws IOException,
			ParsingException {
		in.read(this.midiHeaderBufferBytes); // discard the file header it has
												// been
		// read and parsed before
		while ((this.activeTrack < this.ntracks)
				&& !this.master.isInterrupted()) {
			parseEvents(in);
		}
	}

	private final void parseEvents(final InputStream in) throws IOException,
			ParsingException {
		final int n_ = this.activeTrack;
		final MidiEvent event = parse(in);
		if (event != null) {
			int track;
			track = n_;
			if (n_ == 0) {
				if (event.getType() == EventType.TEMPO) {
					this.d.addTempoChange((TempoChange) event);
				}
				// record TimeSignature if needed
				else {
					this.d.progress(event);
				}
			} else if (event.getType() == EventType.NOP) {
				if (event.delta != 0) {
					++this.eventCount;
					this.eventsEncoded.get(track).add(event);
				}
			} else {
				if ((event.getType() != EventType.TEMPO)
						&& (event.getType() != EventType.TIME)) {
					this.empty = false;
				}
				++this.eventCount;
				this.eventsEncoded.get(track).add(event);
			}
		} else if (n_ != this.activeTrack) {
			assert (n_ + 1) == this.activeTrack;
			final Object channel = this.tracksToChannel
					.get(Integer.valueOf(n_));
			if ((channel == null) && (n_ > 0)) {
				System.out.println("No channel assigned to track " + n_);
			} else if (channel != null) {
				final Object instrument = this.channelsToInstrument
						.get(channel);
				if (instrument == null) {
					System.out.println("No instrument assigned to channel "
							+ channel + " which is assigned to track " + n_);
				}
			}
			//
			if (in.EOFreached()) {
				this.activeTrack = this.ntracks;
			}
			if (n_ == 0) {
				this.renumberMap.put(n_, ++this.nextN);
			} else if (this.empty) {
				this.eventCount -= this.eventsEncoded.remove(n_).size();
				System.out.println("skipping empty midi track " + n_
						+ "\n next track (" + this.activeTrack
						+ ") is numbered as track " + (this.nextN + 1));
				// empty tracks do not count
			} else {
				this.renumberMap.put(n_, ++this.nextN);
			}
			this.empty = true;
		} else if (this.master.isInterrupted()) {
			return;
		}
	}

	@Override
	protected final void createMidiMap() throws ParsingException, IOException {
		final InputStream in = this.io.openIn(this.midi.toFile());
		in.registerProgressMonitor(this.io);
		try {
			if (this.format == 1) {
				parse1(in);
			} else {
				this.io.close(in);
				throw new ParsingException() {

					/** */
					private static final long serialVersionUID = 1L;

					@Override
					public final String toString() {
						return "Unknown midi format "
								+ MidiParserImpl.this.format
								+ " : Unable to parse selected midi";
					}
				};
			}
		} finally {
			this.io.close(in);
			this.io.endProgress("Map created");
		}
	}

	@Override
	protected final void decodeMidiMap() {
		this.io.startProgress("Decoding midi", this.eventCount);
		this.eventsEncoded.remove(0);
		for (final Integer track : new java.util.TreeSet<>(
				this.eventsEncoded.keySet())) {
			final ArrayDeque<MidiEvent> eventList = new ArrayDeque<>(
					this.eventsEncoded.get(track));
			int deltaAbs = 0;
			double durationTrack = 0;
			while (!eventList.isEmpty()) {
				if (this.master.isInterrupted()) {
					return;
				}

				final MidiEvent event = eventList.remove();
				deltaAbs += event.delta;

				switch (event.getType()) {
				// case NOTE_OFF:
				// io.updateProgress(1);
				// break;
				case NOTE_ON:
					// search for NOTE_OFF
					final double start = this.d.getMinutes(deltaAbs);
					final TempoChangeState ts = this.d.lastChange.getValue();
					final NoteOnEvent noteOn = (NoteOnEvent) event;
					int durationTicks = 0;
					for (final MidiEvent iter : eventList) {
						durationTicks += iter.delta;
						if (iter.getType() == EventType.NOTE_OFF) {
							final NoteOffEvent noteOff = (NoteOffEvent) iter;
							if (noteOff.getKey() == noteOn.getKey()) {
								final double end = start
										+ ts.getMinutes(durationTicks);
								if (end > durationTrack) {
									durationTrack = end;
									if (end > this.duration) {
										this.duration = end;
									}
								}
								this.eventsDecoded.addNote(
										this.renumberMap.get(track) - 1,
										noteOn.getKey(), start, end,
										noteOn.getVelocity());
								break;
							}
						}
					}
					this.io.updateProgress(1);
					break;
				default:
					this.io.updateProgress(1);
				}
			}
			System.out.printf("duration of track %2d -> %2d: %02d:%02d,%03d\n",
					track, this.renumberMap.get(track), (int) durationTrack,
					(int) ((durationTrack * 60) % 60),
					(int) ((durationTrack * 60 * 1000) % 1000));
		}
		this.io.endProgress("Midi decoded");
	}

	@Override
	protected final void prepareMidi(@SuppressWarnings("hiding") final Path midi)
			throws Exception {
		final InputStream in = this.io.openIn(midi.toFile());
		try {
			in.read(this.midiHeaderBufferBytes);
		} finally {
			this.io.close(in);
		}
		this.nextN = 0;
		this.eventCount = 0;
		this.midiHeaderBuffer.rewind();
		if (this.midiHeaderBuffer.getInt() != MidiParser.MIDI_HEADER_INT) {
			this.io.close(in);
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		if (this.midiHeaderBuffer.getInt() != 6) {
			this.io.close(in);
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		final byte format_H = this.midiHeaderBuffer.get();
		final byte format_L = this.midiHeaderBuffer.get();
		final byte ntracks_H = this.midiHeaderBuffer.get();
		final byte ntracks_L = this.midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_H = this.midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_L = this.midiHeaderBuffer.get();

		this.format = ((0xff & format_H) << 8) | (0xff & format_L);
		this.ntracks = ((0xff & ntracks_H) << 8) | (0xff & ntracks_L);
		this.deltaTicksPerQuarter = ((0xff & deltaTicksPerQuarter_H) << 8)
				| (0xff & deltaTicksPerQuarter_L);
		if (this.format != 1) {
			throw new IOException(
					"Invalid format: unable to parse selected midi");
		} else if (this.format == 0) {
			throw new RuntimeException("Format 0 not supported");
		}

		this.midi = midi;
		this.d.reset();
	}
}
