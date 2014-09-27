package stone.modules.midiData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.util.Path;


/**
 * Wrapper class for parsing midi files
 * <p>
 * Public methods are thread-safe
 * </p>
 * 
 * @author Nelphindal
 */
public abstract class MidiParser {

	private final class InvalidMidiTrackHeader extends ParsingException {

		/** */
		private static final long serialVersionUID = 1L;

		public InvalidMidiTrackHeader() {
		}


		@Override
		public final String toString() {
			return "Invalid track-header";
		}
	}

	private final class MissingBytesAtEOT extends ParsingException {

		/** */
		private static final long serialVersionUID = 1L;

		public MissingBytesAtEOT() {
		}


		@Override
		public final String toString() {
			return "End of track signaled, but header said its longer: "
					+ trackLen + " bytes left";
		}
	}

	private final class NoEOT extends ParsingException {

		/** */
		private static final long serialVersionUID = 1L;

		public NoEOT() {
		}


		@Override
		public final String toString() {
			return "Expected end of track (0xff 2f00)";
		}

	}

	private abstract class ParseState {

		protected ParseState() {
			MidiParser.instancesOfParseState.add(this);
			reset();
		}

		abstract ParseState getNext(byte read) throws ParsingException;

		void parse(byte read) throws ParsingException {
			if (trackLen-- <= 0) {
				throw new NoEOT();
			}
			state = getNext(read);
		}

		abstract void reset();
	}

	private final class ParseState_Delta extends ParseState {

		int delta;

		public ParseState_Delta() {
		}

		@Override
		final ParseState getNext(byte read) {
			delta <<= 7;
			delta += 0x7f & read;
			if ((read & 0x80) != 0) {
				return this;
			}
			return TYPE;
		}

		@Override
		final void reset() {
			delta = 0;
		}
	}

	private final class ParseState_EOT extends ParseState {

		public ParseState_EOT() {
		}

		@Override
		final ParseState getNext(byte read) throws ParsingException {
			if (read != 0) {
				return null;
			}
			if (trackLen != 0) {
				throw new MissingBytesAtEOT();
			}
			if (activeChannel >= 0) {
				tracksToChannel.put(activeTrack,
						(byte) (0xff & activeChannel));
			}
			++activeTrack;
			activeChannel = -1;
			return HEADER;
		}

		@Override
		final void reset() {
			// nothing to reste
		}

	}

	private final class ParseState_Header extends ParseState {

		ParseState_Header() {
		}

		@Override
		final ParseState getNext(byte read)
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		final void parse(byte read) throws ParsingException {
			bytes.add(read);
			if (bytes.size() == 8) {
				int first4Bytes = 0xff & bytes.remove();
				for (int i = 1; i < 4; i++) {
					first4Bytes <<= 8;
					first4Bytes += 0xff & bytes.remove();
				}
				if (first4Bytes != MidiParser.TRACK_HEADER_INT) {
					throw new InvalidMidiTrackHeader();
				}
				trackLen = 0xff & bytes.remove();
				for (int i = 1; i < 4; i++) {
					trackLen <<= 8;
					trackLen += 0xff & bytes.remove();
				}
				state = DELTA;
				eventsEncoded.put(activeTrack, new ArrayList<MidiEvent>());
			}
		}

		@Override
		final void reset() {
			trackLen = 0;
		}
	}

	private final class ParseState_Meta extends ParseState {

		public ParseState_Meta() {
		}


		@Override
		final ParseState getNext(byte read) {
			switch (read) {
			// case 0x02:
			// return COPYRIGHT;
				case 0x03:
					return NAME;
					// case 0x04:
					// return INSTRUMENT;
				case 0x20:
					return CHANNEL;
					// case 0x21:
					// return PORT;
				case 0x2f:
					return EOT;
				case 0x51:
					return TEMPO;
				case 0x58:
					return TIME;
					// case 0x59:
					// return KEY_SIG;
				default:
					return DISCARD_N;
			}
		}

		@Override
		final void reset() {
			// nothing to reste
		}
	}

	private final class ParseState_NoteOff extends ParseState {

		int k;
		int v;
		int channel;

		public ParseState_NoteOff() {
		}

		@Override
		final ParseState getNext(byte read) {
			if (k < 0) {
				k = 0xff & read;
				return this;
			} else if (v < 0) {
				v = 0xff & read;
				if (activeChannel >= 0) {
					channel = activeChannel;
				}
				lastEvent =
						new NoteOffEvent((byte) k, (byte) v,
								(byte) channel, DELTA.delta, format);
				return DELTA;
			}
			return null;
		}

		@Override
		final void reset() {
			k = -1;
			v = -1;
			channel = -1;
		}
	}

	private final class ParseState_NoteOn extends ParseState {

		int k;
		int v;
		int channel;

		public ParseState_NoteOn() {
		}


		@Override
		final ParseState getNext(byte read) {
			if (activeChannel == -1) {
				activeChannel = channel;
			} else if (activeChannel != channel) {
				activeChannel = -2;
			}
			if (k < 0) {
				k = 0xff & read;
				return this;
			} else if (v < 0) {
				v = 0xff & read;
				if (v == 0) {
					lastEvent =
							new NoteOffEvent((byte) k, (byte) v,
									(byte) channel, DELTA.delta, format);
				} else {
					lastEvent =
							new NoteOnEvent((byte) k, (byte) v,
									(byte) channel, DELTA.delta, format);
				}
				return DELTA;
			}
			return null;
		}

		@Override
		final void reset() {
			k = -1;
			v = -1;
			channel = -1;
		}
	}

	private final class ParseState_ProgramChange extends ParseState {

		byte channel;

		public ParseState_ProgramChange() {
		}


		@Override
		final ParseState getNext(byte read) {
			channelsToInstrument.put(channel, read);
			return DELTA;
		}

		@Override
		final void reset() {
			channel = 0;
		}
	}

	private abstract class ParseState_ReadN extends ParseState {
		int len;
		private boolean check;
		private final boolean firstByteIsLen;

		ParseState_ReadN() {
			firstByteIsLen = true;
		}

		abstract void end() throws ParsingException;

		@Override
		final ParseState getNext(byte read)
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		void parse(byte read) throws ParsingException {
			if (check) {
				check = false;
				if (len < 0) {
					len = 0xff & read;
					--trackLen;
				} else {
					bytes.add(read);
				}
				if ((trackLen -= len) < 0) {
					throw new NoEOT();
				}
			} else {
				bytes.add(read);
			}
			if (bytes.size() == len) {
				if (firstByteIsLen) {
					len = -1;
				}
				end();
				check = true;
				state = DELTA;
			}
		}

		@Override
		final void reset() {
			if (firstByteIsLen) {
				len = -1;
			}
			check = true;
		}
	}

	private final class ParseState_Tempo extends ParseState_ReadN {

		ParseState_Tempo() {
			super();
		}

		@Override
		final void end() {
			int tempo = 0;
			while (!bytes.isEmpty()) {
				tempo <<= 8;
				tempo += 0xff & bytes.remove().byteValue();
			}
			lastEvent = new TempoChange(tempo, DELTA.delta);
		}
	}

	private final class ParseState_Time extends ParseState_ReadN {

		public ParseState_Time() {
		}

		@Override
		final void end() {
			final byte n = bytes.remove().byteValue();
			final int d = (int) Math.pow(2, bytes.remove().byteValue());
			final byte c = bytes.remove().byteValue();
			final byte b = bytes.remove().byteValue();
			lastEvent = new Time(n, d, c, b, DELTA.delta);
		}
	}

	private final class ParseState_Type extends ParseState {
		int lastStatus;

		public ParseState_Type() {
		}


		@Override
		final ParseState getNext(byte read) throws ParsingException {
			final byte status, data;
			final boolean runningStatus;
			if ((read & 0xf0) < 0x80) {
				status = (byte) ((0xf0 & lastStatus) >> 4);
				data = (byte) (0x0f & lastStatus);
				runningStatus = true;
			} else {
				status = (byte) ((read & 0xf0) >> 4);
				data = (byte) (read & 0x0f);
				runningStatus = false;
				lastStatus = read;
			}
			switch (status) {
				case 0x8:
					// note off
					if (runningStatus) {
						NOTE_OFF.k = read;
					} else {
						NOTE_OFF.k = -1;
					}
					NOTE_OFF.v = -1;
					NOTE_OFF.channel = data;
					return NOTE_OFF;
				case 0x9:
					// note on
					if (runningStatus) {
						NOTE_ON.k = read;
					} else {
						NOTE_ON.k = -1;
					}
					NOTE_ON.v = -1;
					NOTE_ON.channel = data;
					return NOTE_ON;
				case 0xa:
					// polyphonic after touch
					if (runningStatus) {
						DISCARD_N.len = 1;
					} else {
						DISCARD_N.len = 2;
					}
					return DISCARD_N;
				case 0xb:
					// control change
					if (runningStatus) {
						DISCARD_N.len = 1;
					} else {
						DISCARD_N.len = 2;
					}
					return DISCARD_N;
				case 0xc:
					// program change
					if (runningStatus) {
						throw new IllegalStateException(
								"Running state 0xc.");
					}
					PROGRAM_CHANGE.channel = data;
					return PROGRAM_CHANGE;
				case 0xd:
					// channel pressure
					if (runningStatus) {
						DISCARD_N.len = 0;
					} else {
						DISCARD_N.len = 1;
					}
					return DISCARD_N;
				case 0xe:
					// pitch bend
					if (runningStatus) {
						DISCARD_N.len = 1;
					} else {
						DISCARD_N.len = 2;
					}
					return DISCARD_N;
				case 0xf:
					// control
					final ParseState nextMeta;
					switch (data) {
						case 0x0:
							lastEvent = new Break(DELTA.delta);
							nextMeta = DISCARD_UNTIL_EOX;
							break;
						case 0xf:
							nextMeta = META;
							break;
						default:
							nextMeta = null;
							throw new IllegalStateException();
					}
					return nextMeta;
			}
			throw new IllegalStateException();
		}

		@Override
		final void reset() {
			lastStatus = 0;
		}
	}

	/** Header of a midi file */
	protected static final String MIDI_HEADER = "MThd";
	/** Header of a midi file, byte equivalent */
	protected static final int MIDI_HEADER_INT = 0x4d546864;
	/** Header of a track within a midi file */
	protected static final String TRACK_HEADER = "MTrk";
	/** Header of a track within a midi file, byte equivalent */
	protected static final int TRACK_HEADER_INT = 0x4d54726b;

	final static Set<ParseState> instancesOfParseState = new HashSet<>();

	/**
	 * Creates a new Parser using giving implementation.
	 * 
	 * @param sc
	 * @return the selected parser
	 */
	public final static MidiParser
			createInstance(final StartupContainer sc) {
		return new MidiParserImpl(sc);
	}

	/** The master thread, to check for interruption */
	protected final MasterThread master;

	/** A map holding the parsed data */
	protected final Map<Integer, List<MidiEvent>> eventsEncoded =
			new HashMap<>();
	/** A map holding the parsed data */
	protected final MidiMap eventsDecoded = new MidiMap(this);

	/** A map to keep track of skipped tracks and resulting renumbering */
	protected final Map<Integer, Integer> renumberMap = new HashMap<>();

	/** IOHandler to use */
	protected final IOHandler io;

	/** Usable StringBuilder, no guarantee on the content */
	protected final StringBuilder sb = new StringBuilder();

	/** implementing sub classes have to set the actual parsed duration */
	protected double duration;
	/** File currently being handled in this parser */
	protected Path midi;

	/** Number of tracks */
	protected int ntracks;

	/** Number of track currently parsed */
	protected int activeTrack;

	/** Format of this midi */
	protected int format;

	final ParseState CHANNEL = new ParseState_ReadN() {

		@Override
		final void end() {
			final byte c = bytes.remove();
			tracksToChannel.put(activeTrack, c);
			activeChannel = 0xff & c;
		}
	};
	final ParseState_Delta DELTA = new ParseState_Delta();
	final ParseState DISCARD_UNTIL_EOX = new ParseState() {

		@Override
		final ParseState getNext(byte read) {
			if (read == (byte) 0xf7) {
				return DELTA;
			}
			return this;
		}

		@Override
		final void reset() {
			// nothing to reste
		}

	};

	final ParseState_ReadN DISCARD_N = new ParseState_ReadN() {

		@Override
		final void end() {
			bytes.clear();
			if (DELTA.delta != 0) {
				lastEvent = new Break(DELTA.delta);
			}
		}
	};
	final ParseState EOT = new ParseState_EOT();
	final ParseState_Header HEADER = new ParseState_Header();
	final ParseState_Type TYPE = new ParseState_Type();
	final ParseState META = new ParseState_Meta();
	final ParseState NAME = new ParseState_ReadN() {

		@Override
		final void end() {
			sb.setLength(0);
			while (!bytes.isEmpty()) {
				sb.append((char) bytes.remove().byteValue());
			}
			if (sb.length() > 60) {
				sb.setLength(60);
			}
			titles.put(activeTrack, sb.toString().trim());
		}

	};
	final ParseState_NoteOn NOTE_ON = new ParseState_NoteOn();
	final ParseState_NoteOff NOTE_OFF = new ParseState_NoteOff();
	final ParseState_ProgramChange PROGRAM_CHANGE =
			new ParseState_ProgramChange();

	final ParseState TEMPO = new ParseState_Tempo();

	final ParseState TIME = new ParseState_Time();

	/** A map mapping channels to instruments */
	final Map<Byte, Byte> channelsToInstrument = new HashMap<>();
	/** A map holding the instruments */
	private final Map<Integer, MidiInstrument> instruments =
			new HashMap<>();
	/** A map holding the titles */
	final Map<Integer, String> titles = new HashMap<>();
	/** A map mapping tracks to channels */
	final Map<Integer, Byte> tracksToChannel = new HashMap<>();
	final ArrayDeque<Byte> bytes = new ArrayDeque<>();
	/** Number of channel currently parsed */
	int activeChannel = -1;
	MidiEvent lastEvent;
	private Path lastParsedMidi = null;
	private int lock = 0;
	private int lockRead = 0;
	private long mod;
	ParseState state = HEADER;

	int trackLen;

	/**
	 * @param io
	 * @param master
	 */
	protected MidiParser(final IOHandler io, final MasterThread master) {
		this.io = io;
		this.master = master;
	}

	/**
	 * @return the duration of entire song in seconds
	 */
	public final double getDuration() {
		return duration;
	}

	/**
	 * @return the currently used midi-file
	 */
	public final String getMidi() {
		return midi.getFileName();
	}

	/**
	 * Parses the selected midi file and returns the instruments.
	 * <p>
	 * This method is thread-safe
	 * </p>
	 * 
	 * @return a map with parsed instruments
	 */
	public final Map<Integer, MidiInstrument> instruments() {
		synchronized (this) {
			while (lock > 0) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++lockRead;
		}
		try {
			parseIfNeeded();
			if (lastParsedMidi == null) {
				synchronized (this) {
					--lockRead;
					notifyAll();
				}
				return null;
			}
			final Map<Integer, MidiInstrument> map =
					new TreeMap<>(instruments);
			for (final Integer track : eventsEncoded.keySet()) {
				if (track == 0) {
					continue;
				}
				if (track == -1) {
					break;
				}
				if (!map.containsKey(track)) {
					final Byte channelObject = tracksToChannel.get(track);
					final byte channel =
							(byte) (channelObject == null ? track
									.byteValue() - 1 : channelObject
									.byteValue());
					if ((channel == 9) || (channel == 10)) {
						map.put(track, MidiInstrument.DRUMS);
					} else {
						final MidiInstrument i;
						if (channel == -1) {
							i = MidiInstrument.get(track.byteValue());
						} else {
							final Byte instrument =
									channelsToInstrument.get(channel);
							if (instrument == null) {
								i = null;
							} else {
								i = MidiInstrument.get(instrument);
							}
						}
						map.put(track, i);
					}
				}
			}
			return map;
		} catch (final FileNotFoundException e) {
			io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				--lockRead;
				notifyAll();
			}
		}

	}

	/**
	 * Creates the files remap.exe of BruTE is expecting. These are the midigram
	 * and the mftext from the midi with the like the midi2abc tool does.
	 * 
	 * @param wd
	 *            working directory for BruTE
	 */
	// public final void midi2abc(final Path wd) {
	// final io.OutputStream gram = io.openOut(wd.resolve("out.gram").toFile());
	// final io.OutputStream mf = io.openOut(wd.resolve("out.mf").toFile());
	// // make midi2abc yourself
	// io.close(gram);
	// io.close(mf);
	// throw new RuntimeException("Not yet implemented");
	// }

	/**
	 * Parses the selected midi file.
	 * <p>
	 * This method is thread-safe
	 * </p>
	 * 
	 * @return a map of all midi events
	 */
	public final MidiMap parse() {
		synchronized (this) {
			while ((lock > 0) || (lockRead > 0)) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++lockRead;
		}
		try {
			parseIfNeeded();
			if (lastParsedMidi == null) {
				return null;
			}
			final MidiMap eventsDecoded_ = eventsDecoded.clone();
			return eventsDecoded_;
		} catch (final FileNotFoundException e) {
			io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				// exception will decrement sooner
				--lockRead;
				notifyAll();
			}
		}

	}

	/**
	 * @return the map mapping ids of midi-tracks to subsequent numbers
	 */
	public final Map<Integer, Integer> renumberMap() {
		return new HashMap<>(renumberMap);
	}

	/**
	 * Sets given midi to be parsed
	 * 
	 * @param midi
	 * @return <i>true</i> on success, <i>false</i> otherwise
	 */
	public final boolean setMidi(final Path midi) {
		if (midi == null) {
			throw new IllegalArgumentException();
		}
		synchronized (this) {
			++lock;
			while (lockRead > 0) {
				try {
					wait();
				} catch (final InterruptedException e) {
					--lock;
					notifyAll();
					return false;
				}
			}
			lockRead = 1;
		}
		if ((this.midi == midi) && (mod == midi.toFile().lastModified())) {
			synchronized (this) {
				lockRead = 0;
				--lock;
				notifyAll();
			}
			return true;
		}
		this.midi = null;
		duration = 0;
		lastParsedMidi = null;
		tracksToChannel.clear();
		channelsToInstrument.clear();
		titles.clear();
		eventsEncoded.clear();
		renumberMap.clear();
		bytes.clear();
		for (final ParseState ps : MidiParser.instancesOfParseState) {
			ps.reset();
		}
		state = HEADER;
		activeTrack = 0;
		activeChannel = -1;
		ntracks = -1;
		try {
			prepareMidi(midi);
			this.midi = midi;
			mod = midi.toFile().lastModified();
			return true;
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return false;
		} finally {
			synchronized (this) {
				lockRead = 0;
				--lock;
				notifyAll();
			}
		}
	}

	/**
	 * Parses the selected midi file and returns the titles.
	 * <p>
	 * This method is thread-safe
	 * </p>
	 * 
	 * @return a map with parsed titles
	 */
	public final Map<Integer, String> titles() {
		synchronized (this) {
			while ((lock > 0) || (lockRead > 0)) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++lockRead;
		}
		try {
			parseIfNeeded();
			return new HashMap<>(titles);
		} catch (final FileNotFoundException e) {
			io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				--lockRead;
				notifyAll();
			}
		}

	}

	/**
	 * @return a set of all indices of last parsed midi.
	 */
	public final Set<Integer> tracks() {
		return eventsEncoded.keySet();
	}

	private final void parseIfNeeded() throws FileNotFoundException {
		if (lastParsedMidi != midi) {
			if (midi != null) {
				if (!midi.exists()) {
					throw new FileNotFoundException();
				}
				try {
					createMidiMap();
					decodeMidiMap();
				} catch (final IOException e) {
					lastParsedMidi = null;
					synchronized (this) {
						notifyAll();
					}
					e.printStackTrace();
					io.handleException(ExceptionHandle.CONTINUE, e);
					return;
				} catch (final DecodingException e) {
					lastParsedMidi = null;
					io.printError(e.toString(), false);
					return;
				} catch (final ParsingException e) {
					lastParsedMidi = null;
					io.printError(e.toString(), false);
					return;
				}
			}
		} else if (lastParsedMidi == null) {
			throw new IllegalStateException("Nothing parsed");
		}
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		lastParsedMidi = midi;
	}

	/**
	 * Creates a new midi event described by given bytes and delta ticks. Header
	 * chunks are not allowed (starting with MT).
	 * 
	 * @param message
	 *            bytes to parse
	 * @param delta
	 *            a time offset to prior event in milliseconds
	 * @return the parsed event
	 * @throws ParsingException
	 */
	protected final MidiEvent createEvent(byte[] message, int delta)
			throws ParsingException {
		if (state == HEADER) {
			state = DELTA;
		}
		assert (state == DELTA) || (state == HEADER);
		lastEvent = null;
		DELTA.delta = delta;
		state = TYPE;
		for (final byte element : message) {
			state.parse(element);
		}
		assert (state == DELTA) || (state == HEADER);
		DELTA.delta = 0;

		return lastEvent;
	}

	/**
	 * Reads the given midi-file
	 * 
	 * @throws ParsingException
	 *             if any errors occur while parsing
	 * @throws IOException
	 *             if an I/O-Error occurs
	 */
	protected abstract void createMidiMap() throws ParsingException,
			IOException;

	/**
	 * Decodes the previously read midi-map
	 * 
	 * @throws DecodingException
	 */
	protected abstract void decodeMidiMap() throws DecodingException;

	/**
	 * Parses a single midi event reading from given InputStream.
	 * 
	 * @param in
	 *            InputStream to read from
	 * @return next midi event
	 * @throws IOException
	 *             if an I/O-Error occurs.
	 * @throws ParsingException
	 *             if the midi file breaks the grammar of midi-files
	 */
	protected final MidiEvent parse(final InputStream in)
			throws IOException, ParsingException {
		if (Thread.currentThread().isInterrupted()) {
			return null;
		}
		assert (state == DELTA) || (state == HEADER);
		lastEvent = null;
		DELTA.delta = 0;
		while ((state == DELTA) || (state == HEADER)) {
			state.parse((byte) in.read());
		}
		do {
			state.parse((byte) in.read());
		} while ((state != DELTA) && (state != HEADER));
		return lastEvent;
	}

	/**
	 * Requests to delete and clear all cached data to prepare next parsing
	 * routine
	 * 
	 * @param newMidi
	 * @throws Exception
	 */
	protected abstract void prepareMidi(final Path newMidi)
			throws Exception;
}
