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
			.wrap(midiHeaderBufferBytes);
	private int nextN;
	private boolean empty = true;
	final Duration d = new Duration(this);

	int deltaTicksPerQuarter;
	private int eventCount;

	@SuppressWarnings("unused")
	private final TaskPool taskPool; // for later multi-threaded decoding

	MidiParserImpl(final StartupContainer sc) {
		super(sc.getIO(), sc.getMaster());
		taskPool = sc.getTaskPool();
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
		in.read(midiHeaderBufferBytes);
		while ((activeTrack < ntracks) && !master.isInterrupted()) {
			parseEvents(in);
		}
	}

	private final void parseEvents(final InputStream in) throws IOException,
	ParsingException {
		final int n_ = activeTrack;
		final MidiEvent event = parse(in);
		if (event != null) {
			int track;
			track = n_;
			if (n_ == 0) {
				if (event.getType() == EventType.TEMPO) {
					d.addTempoChange((TempoChange) event);
				}
				// record TimeSignature if needed
				else {
					d.progress(event);
				}
			} else if (event.getType() == EventType.NOP) {
				if (event.delta != 0) {
					++eventCount;
					eventsEncoded.get(track).add(event);
				}
			} else {
				if ((event.getType() != EventType.TEMPO)
						&& (event.getType() != EventType.TIME)) {
					empty = false;
				}
				++eventCount;
				eventsEncoded.get(track).add(event);
			}
		} else if (n_ != activeTrack) {
			assert (n_ + 1) == activeTrack;
			if (in.EOFreached()) {
				activeTrack = ntracks;
			}
			if (n_ == 0) {
				renumberMap.put(n_, ++nextN);
			} else if (empty) {
				eventCount -= eventsEncoded.remove(n_).size();
				System.out.println("skipping empty midi track " + n_
						+ "\n next track (" + activeTrack
						+ ") is numbered as track " + (nextN + 1));
				// empty tracks do not count
			} else {
				renumberMap.put(n_, ++nextN);
			}
			empty = true;
		} else if (master.isInterrupted())
			return;
	}

	@SuppressWarnings("resource")
	@Override
	protected final void createMidiMap() throws ParsingException, IOException {
		final InputStream in = io.openIn(midi.toFile());
		in.registerProgressMonitor(io);
		try {
			if (format == 1) {
				parse1(in);
			} else {
				io.close(in);
				throw new ParsingException() {

					/** */
					private static final long serialVersionUID = 1L;

					@Override
					public final String toString() {
						return "Unknown midi format " + format
								+ " : Unable to parse selected midi";
					}
				};
			}
		} finally {
			io.close(in);
			io.endProgress();
		}
	}

	@Override
	protected final void decodeMidiMap() {
		io.startProgress("Decoding midi", eventCount);
		eventsEncoded.remove(0);
		for (final Integer track : new java.util.TreeSet<>(
				eventsEncoded.keySet())) {
			final ArrayDeque<MidiEvent> eventList =
					new ArrayDeque<>(eventsEncoded.get(track));
					int deltaAbs = 0;
					double durationTrack = 0;
					while (!eventList.isEmpty()) {
						if (master.isInterrupted())
							return;

						final MidiEvent event = eventList.remove();
						deltaAbs += event.delta;

						switch (event.getType()) {
							//					case NOTE_OFF:
							//						io.updateProgress(1);
							//						break;
							case NOTE_ON:
								// search for NOTE_OFF
								final double start = d.getMinutes(deltaAbs);
								final TempoChangeState ts = d.lastChange.getValue();
								final NoteOnEvent noteOn = (NoteOnEvent) event;
								int durationTicks = 0;
								for (final MidiEvent iter : eventList) {
									durationTicks += iter.delta;
									if (iter.getType() == EventType.NOTE_OFF) {
										final NoteOffEvent noteOff =
												(NoteOffEvent) iter;
										if (noteOff.getKey() == noteOn.getKey()) {
											final double end =
													start
													+ ts.getMinutes(durationTicks);
											if (end > durationTrack) {
												durationTrack = end;
												if (end > duration) {
													duration = end;
												}
											}
											eventsDecoded.addNote(
													renumberMap.get(track) - 1,
													noteOn.getKey(), start, end,
													noteOn.getVelocity());
											break;
										}
									}
								}
								io.updateProgress(1);
								break;
							default:
								io.updateProgress(1);
						}
					}
					System.out.printf("duration of track %2d -> %2d: %02d:%02d,%03d\n",
							track, renumberMap.get(track), (int) durationTrack,
							(int) ((durationTrack * 60) % 60),
							(int) ((durationTrack * 60 * 1000) % 1000));
		}
		io.endProgress();
	}

	@SuppressWarnings("resource")
	@Override
	protected final void prepareMidi(final Path midi) throws Exception {
		final InputStream in = io.openIn(midi.toFile());
		try {
			in.read(midiHeaderBufferBytes);
		} finally {
			io.close(in);
		}
		nextN = 0;
		eventCount = 0;
		midiHeaderBuffer.rewind();
		if (midiHeaderBuffer.getInt() != MidiParser.MIDI_HEADER_INT) {
			io.close(in);
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		if (midiHeaderBuffer.getInt() != 6) {
			io.close(in);
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		final byte format_H = midiHeaderBuffer.get();
		final byte format_L = midiHeaderBuffer.get();
		final byte ntracks_H = midiHeaderBuffer.get();
		final byte ntracks_L = midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_H = midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_L = midiHeaderBuffer.get();

		format = ((0xff & format_H) << 8) | (0xff & format_L);
		ntracks = ((0xff & ntracks_H) << 8) | (0xff & ntracks_L);
		deltaTicksPerQuarter =
				((0xff & deltaTicksPerQuarter_H) << 8) | (0xff
						& deltaTicksPerQuarter_L);
		if (format != 1)
			throw new IOException(
					"Invalid format: unable to parse selected midi");
		else if (format == 0)
			throw new RuntimeException("Format 0 not supported");

		this.midi = midi;
		d.reset();
	}
}
