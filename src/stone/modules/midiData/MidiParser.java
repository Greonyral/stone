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
					+ MidiParser.this.trackLen + " bytes left";
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
			if (MidiParser.this.trackLen-- <= 0) {
				throw new NoEOT();
			}
			MidiParser.this.state = getNext(read);
		}

		abstract void reset();
	}

	private final class ParseState_Delta extends ParseState {

		int delta;

		public ParseState_Delta() {
		}

		@Override
		final ParseState getNext(byte read) {
			this.delta <<= 7;
			this.delta += 0x7f & read;
			if ((read & 0x80) != 0) {
				return this;
			}
			return MidiParser.this.TYPE;
		}

		@Override
		final void reset() {
			this.delta = 0;
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
			if (MidiParser.this.trackLen != 0) {
				throw new MissingBytesAtEOT();
			}
			if (MidiParser.this.activeChannel >= 0) {
				MidiParser.this.tracksToChannel.put(
						MidiParser.this.activeTrack,
						(byte) (0xff & MidiParser.this.activeChannel));
			}
			++MidiParser.this.activeTrack;
			MidiParser.this.activeChannel = -1;
			return MidiParser.this.HEADER;
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
			MidiParser.this.bytes.add(read);
			if (MidiParser.this.bytes.size() == 8) {
				int first4Bytes = 0xff & MidiParser.this.bytes.remove();
				for (int i = 1; i < 4; i++) {
					first4Bytes <<= 8;
					first4Bytes += 0xff & MidiParser.this.bytes.remove();
				}
				if (first4Bytes != MidiParser.TRACK_HEADER_INT) {
					throw new InvalidMidiTrackHeader();
				}
				MidiParser.this.trackLen = 0xff & MidiParser.this.bytes
						.remove();
				for (int i = 1; i < 4; i++) {
					MidiParser.this.trackLen <<= 8;
					MidiParser.this.trackLen += 0xff & MidiParser.this.bytes
							.remove();
				}
				MidiParser.this.state = MidiParser.this.DELTA;
				MidiParser.this.eventsEncoded.put(MidiParser.this.activeTrack,
						new ArrayList<MidiEvent>());
			}
		}

		@Override
		final void reset() {
			MidiParser.this.trackLen = 0;
		}
	}

	private final class ParseState_Meta extends ParseState {

		public ParseState_Meta() {
		}


		@Override
		final ParseState getNext(byte read) {
			switch (read) {
			// case 0x01:
			// return TEXT;
			// case 0x02:
			// return COPYRIGHT;
			case 0x03:
				return MidiParser.this.NAME;
				// case 0x04:
				// return INSTRUMENT;
			case 0x20:
				return MidiParser.this.CHANNEL;
				// case 0x21:
				// return PORT;
			case 0x2f:
				return MidiParser.this.EOT;
			case 0x51:
				return MidiParser.this.TEMPO;
			case 0x58:
				return MidiParser.this.TIME;
				// case 0x59:
				// return KEY_SIG;
			default:
				return MidiParser.this.DISCARD_N;
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
			if (this.k < 0) {
				this.k = 0xff & read;
				return this;
			} else if (this.v < 0) {
				this.v = 0xff & read;
				if (MidiParser.this.activeChannel >= 0) {
					this.channel = MidiParser.this.activeChannel;
				}
				MidiParser.this.lastEvent = new NoteOffEvent((byte) this.k,
						(byte) this.v, (byte) this.channel,
						MidiParser.this.DELTA.delta, MidiParser.this.format);
				return MidiParser.this.DELTA;
			}
			return null;
		}

		@Override
		final void reset() {
			this.k = -1;
			this.v = -1;
			this.channel = -1;
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
			if (MidiParser.this.activeChannel == -1) {
				MidiParser.this.activeChannel = this.channel;
			} else if (MidiParser.this.activeChannel != this.channel) {
				MidiParser.this.activeChannel = -2;
			}
			if (this.k < 0) {
				this.k = 0xff & read;
				return this;
			} else if (this.v < 0) {
				this.v = 0xff & read;
				if (this.v == 0) {
					MidiParser.this.lastEvent = new NoteOffEvent((byte) this.k,
							(byte) this.v, (byte) this.channel,
							MidiParser.this.DELTA.delta, MidiParser.this.format);
				} else {
					MidiParser.this.lastEvent = new NoteOnEvent((byte) this.k,
							(byte) this.v, (byte) this.channel,
							MidiParser.this.DELTA.delta, MidiParser.this.format);
				}
				return MidiParser.this.DELTA;
			}
			return null;
		}

		@Override
		final void reset() {
			this.k = -1;
			this.v = -1;
			this.channel = -1;
		}
	}

	private final class ParseState_ProgramChange extends ParseState {

		byte channel;

		public ParseState_ProgramChange() {
		}


		@Override
		final ParseState getNext(byte read) {
			MidiParser.this.channelsToInstrument.put(this.channel, read);
			return MidiParser.this.DELTA;
		}

		@Override
		final void reset() {
			this.channel = 0;
		}
	}

	private abstract class ParseState_ReadN extends ParseState {
		int len;
		private boolean check, byteToFollow;
		private final boolean firstByteIsLen;

		ParseState_ReadN() {
			this.firstByteIsLen = true;
		}

		abstract void end() throws ParsingException;

		@Override
		final ParseState getNext(byte read)
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		void parse(byte read) throws ParsingException {
			if (this.check) {
				this.check = false;
				if (this.len < 0) {
					this.len = 0x7f & read;
					byteToFollow = (0x80 & read) != 0;
					--MidiParser.this.trackLen;
				} else {
					MidiParser.this.bytes.add(read);
				}
				if (!byteToFollow && (MidiParser.this.trackLen -= this.len) < 0) {
					throw new NoEOT();
				}
			} else if (byteToFollow) {
				this.len <<= 7;
				this.len += 0x7f & read;
				byteToFollow = (0x80 & read) != 0;
				--MidiParser.this.trackLen;
				if (!byteToFollow && (MidiParser.this.trackLen -= this.len) < 0) {
					throw new NoEOT();
				}
			} else {
				MidiParser.this.bytes.add(read);
			}
			if (MidiParser.this.bytes.size() == this.len) {
				if (this.firstByteIsLen) {
					this.len = -1;
				}
				end();
				this.check = true;
				MidiParser.this.state = MidiParser.this.DELTA;
			}
		}

		@Override
		final void reset() {
			if (this.firstByteIsLen) {
				this.len = -1;
			}
			this.check = true;
		}
	}

	private final class ParseState_Tempo extends ParseState_ReadN {

		ParseState_Tempo() {
			super();
		}

		@Override
		final void end() {
			int tempo = 0;
			while (!MidiParser.this.bytes.isEmpty()) {
				tempo <<= 8;
				tempo += 0xff & MidiParser.this.bytes.remove().byteValue();
			}
			MidiParser.this.lastEvent = new TempoChange(tempo,
					MidiParser.this.DELTA.delta);
		}
	}

	private final class ParseState_Time extends ParseState_ReadN {

		public ParseState_Time() {
		}

		@Override
		final void end() {
			final byte n = MidiParser.this.bytes.remove().byteValue();
			final int d = (int) Math.pow(2, MidiParser.this.bytes.remove()
					.byteValue());
			final byte c = MidiParser.this.bytes.remove().byteValue();
			final byte b = MidiParser.this.bytes.remove().byteValue();
			MidiParser.this.lastEvent = new Time(n, d, c, b,
					MidiParser.this.DELTA.delta);
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
				status = (byte) ((0xf0 & this.lastStatus) >> 4);
				data = (byte) (0x0f & this.lastStatus);
				runningStatus = true;
			} else {
				status = (byte) ((read & 0xf0) >> 4);
				data = (byte) (read & 0x0f);
				runningStatus = false;
				this.lastStatus = read;
			}
			switch (status) {
			case 0x8:
				// note off
				if (runningStatus) {
					MidiParser.this.NOTE_OFF.k = read;
				} else {
					MidiParser.this.NOTE_OFF.k = -1;
				}
				MidiParser.this.NOTE_OFF.v = -1;
				MidiParser.this.NOTE_OFF.channel = data;
				return MidiParser.this.NOTE_OFF;
			case 0x9:
				// note on
				if (runningStatus) {
					MidiParser.this.NOTE_ON.k = read;
				} else {
					MidiParser.this.NOTE_ON.k = -1;
				}
				MidiParser.this.NOTE_ON.v = -1;
				MidiParser.this.NOTE_ON.channel = data;
				return MidiParser.this.NOTE_ON;
			case 0xa:
				// polyphonic after touch
				if (runningStatus) {
					MidiParser.this.DISCARD_N.len = 1;
				} else {
					MidiParser.this.DISCARD_N.len = 2;
				}
				return MidiParser.this.DISCARD_N;
			case 0xb:
				// control change
				if (runningStatus) {
					MidiParser.this.DISCARD_N.len = 1;
				} else {
					MidiParser.this.DISCARD_N.len = 2;
				}
				return MidiParser.this.DISCARD_N;
			case 0xc:
				// program change
				if (runningStatus) {
					throw new IllegalStateException("Running state 0xc.");
				}
				MidiParser.this.PROGRAM_CHANGE.channel = data;
				return MidiParser.this.PROGRAM_CHANGE;
			case 0xd:
				// channel pressure
				if (runningStatus) {
					MidiParser.this.DISCARD_N.len = 0;
				} else {
					MidiParser.this.DISCARD_N.len = 1;
				}
				return MidiParser.this.DISCARD_N;
			case 0xe:
				// pitch bend
				if (runningStatus) {
					MidiParser.this.DISCARD_N.len = 1;
				} else {
					MidiParser.this.DISCARD_N.len = 2;
				}
				return MidiParser.this.DISCARD_N;
			case 0xf:
				// control
				final ParseState nextMeta;
				switch (data) {
				case 0x0:
					MidiParser.this.lastEvent = new Break(
							MidiParser.this.DELTA.delta);
					nextMeta = MidiParser.this.DISCARD_UNTIL_EOX;
					break;
				case 0xf:
					nextMeta = MidiParser.this.META;
					break;
				default:
					nextMeta = null;
					throw new IllegalStateException();
				}
				return nextMeta;
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		final void reset() {
			this.lastStatus = 0;
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
	 *            -
	 * @return the selected parser
	 */
	public final static MidiParser createInstance(final StartupContainer sc) {
		return new MidiParserImpl(sc);
	}

	/** The master thread, to check for interruption */
	protected final MasterThread master;

	/** A map holding the parsed data */
	protected final Map<Integer, List<MidiEvent>> eventsEncoded = new HashMap<>();
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
			final byte c = MidiParser.this.bytes.remove();
			MidiParser.this.tracksToChannel.put(MidiParser.this.activeTrack, c);
			MidiParser.this.activeChannel = 0xff & c;
		}
	};
	final ParseState_Delta DELTA = new ParseState_Delta();
	final ParseState DISCARD_UNTIL_EOX = new ParseState() {

		@Override
		final ParseState getNext(byte read) {
			if (read == (byte) 0xf7) {
				return MidiParser.this.DELTA;
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
			MidiParser.this.bytes.clear();
			if (MidiParser.this.DELTA.delta != 0) {
				MidiParser.this.lastEvent = new Break(
						MidiParser.this.DELTA.delta);
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
			MidiParser.this.sb.setLength(0);
			while (!MidiParser.this.bytes.isEmpty()) {
				MidiParser.this.sb.append((char) MidiParser.this.bytes.remove()
						.byteValue());
			}
			if (MidiParser.this.sb.length() > 60) {
				MidiParser.this.sb.setLength(60);
			}
			MidiParser.this.titles.put(MidiParser.this.activeTrack,
					MidiParser.this.sb.toString().trim());
		}

	};
	final ParseState_NoteOn NOTE_ON = new ParseState_NoteOn();
	final ParseState_NoteOff NOTE_OFF = new ParseState_NoteOff();
	final ParseState_ProgramChange PROGRAM_CHANGE = new ParseState_ProgramChange();

	final ParseState TEMPO = new ParseState_Tempo();

	final ParseState TIME = new ParseState_Time();

	/** A map mapping channels to instruments */
	final Map<Byte, Byte> channelsToInstrument = new HashMap<>();
	/** A map holding the instruments */
	private final Map<Integer, MidiInstrument> instruments = new HashMap<>();
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
	ParseState state = this.HEADER;

	int trackLen;

	/**
	 * @param io
	 *            -
	 * @param master
	 *            -
	 */
	@SuppressWarnings("hiding")
	protected MidiParser(final IOHandler io, final MasterThread master) {
		this.io = io;
		this.master = master;
	}

	/**
	 * @return the duration of entire song in seconds
	 */
	public final double getDuration() {
		return this.duration;
	}

	/**
	 * @return the currently used midi-file
	 */
	public final String getMidi() {
		return this.midi.getFilename();
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
			while (this.lock > 0) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++this.lockRead;
		}
		try {
			parseIfNeeded();
			if (this.lastParsedMidi == null) {
				synchronized (this) {
					--this.lockRead;
					notifyAll();
				}
				return null;
			}
			final Map<Integer, MidiInstrument> map = new TreeMap<>(
					this.instruments);
			for (final Integer track : this.eventsEncoded.keySet()) {
				if (track == 0) {
					continue;
				}
				if (track == -1) {
					break;
				}
				if (!map.containsKey(track)) {
					final Byte channelObject = this.tracksToChannel.get(track);
					final byte channel = (byte) (channelObject == null ? track
							.byteValue() - 1 : channelObject.byteValue());
					if ((channel == 9) || (channel == 10)) {
						map.put(track, MidiInstrument.DRUMS);
					} else {
						final MidiInstrument i;
						if (channel == -1) {
							i = MidiInstrument.get(track.byteValue());
						} else {
							final Byte instrument = this.channelsToInstrument
									.get(channel);
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
			this.io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				--this.lockRead;
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
			while ((this.lock > 0) || (this.lockRead > 0)) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++this.lockRead;
		}
		try {
			parseIfNeeded();
			if (this.lastParsedMidi == null) {
				return null;
			}
			final MidiMap eventsDecoded_ = this.eventsDecoded.clone();
			return eventsDecoded_;
		} catch (final FileNotFoundException e) {
			this.io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				// exception will decrement sooner
				--this.lockRead;
				notifyAll();
			}
		}

	}

	/**
	 * @return the map mapping ids of midi-tracks to subsequent numbers
	 */
	public final Map<Integer, Integer> renumberMap() {
		return new HashMap<>(this.renumberMap);
	}

	/**
	 * Sets given midi to be parsed
	 * 
	 * @param midi
	 *            -
	 * @return <i>true</i> on success, <i>false</i> otherwise
	 */
	public final boolean setMidi(@SuppressWarnings("hiding") final Path midi) {
		if (midi == null) {
			throw new IllegalArgumentException();
		}
		synchronized (this) {
			++this.lock;
			while (this.lockRead > 0) {
				try {
					wait();
				} catch (final InterruptedException e) {
					--this.lock;
					notifyAll();
					return false;
				}
			}
			this.lockRead = 1;
		}
		if ((this.midi == midi) && (this.mod == midi.toFile().lastModified())) {
			synchronized (this) {
				this.lockRead = 0;
				--this.lock;
				notifyAll();
			}
			return true;
		}
		this.midi = null;
		this.duration = 0;
		this.lastParsedMidi = null;
		this.tracksToChannel.clear();
		this.channelsToInstrument.clear();
		this.titles.clear();
		this.eventsEncoded.clear();
		this.renumberMap.clear();
		this.bytes.clear();
		for (final ParseState ps : MidiParser.instancesOfParseState) {
			ps.reset();
		}
		this.state = this.HEADER;
		this.activeTrack = 0;
		this.activeChannel = -1;
		this.ntracks = -1;
		try {
			prepareMidi(midi);
			this.midi = midi;
			this.mod = midi.toFile().lastModified();
			return true;
		} catch (final Exception e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
			return false;
		} finally {
			synchronized (this) {
				this.lockRead = 0;
				--this.lock;
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
			while ((this.lock > 0) || (this.lockRead > 0)) {
				try {
					wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			++this.lockRead;
		}
		try {
			parseIfNeeded();
			return new HashMap<>(this.titles);
		} catch (final FileNotFoundException e) {
			this.io.printError("Selected midi does not exist", true);
			return null;
		} finally {
			synchronized (this) {
				--this.lockRead;
				notifyAll();
			}
		}

	}

	/**
	 * @return a set of all indices of last parsed midi.
	 */
	public final Set<Integer> tracks() {
		return this.eventsEncoded.keySet();
	}

	private final void parseIfNeeded() throws FileNotFoundException {
		if (this.lastParsedMidi != this.midi) {
			if (this.midi != null) {
				if (!this.midi.exists()) {
					throw new FileNotFoundException();
				}
				try {
					createMidiMap();
					decodeMidiMap();
				} catch (final IOException e) {
					this.lastParsedMidi = null;
					synchronized (this) {
						notifyAll();
					}
					e.printStackTrace();
					this.io.handleException(ExceptionHandle.CONTINUE, e);
					return;
				} catch (final DecodingException e) {
					this.lastParsedMidi = null;
					this.io.printError(e.toString(), false);
					return;
				} catch (final ParsingException e) {
					this.lastParsedMidi = null;
					this.io.printError(e.toString(), false);
					return;
				}
			}
		} else if (this.lastParsedMidi == null) {
			throw new IllegalStateException("Nothing parsed");
		}
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		this.lastParsedMidi = this.midi;
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
	 *             -
	 */
	protected final MidiEvent createEvent(byte[] message, int delta)
			throws ParsingException {
		if (this.state == this.HEADER) {
			this.state = this.DELTA;
		}
		assert (this.state == this.DELTA) || (this.state == this.HEADER);
		this.lastEvent = null;
		this.DELTA.delta = delta;
		this.state = this.TYPE;
		for (final byte element : message) {
			this.state.parse(element);
		}
		assert (this.state == this.DELTA) || (this.state == this.HEADER);
		this.DELTA.delta = 0;

		return this.lastEvent;
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
	 *             -
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
	protected final MidiEvent parse(final InputStream in) throws IOException,
			ParsingException {
		if (Thread.currentThread().isInterrupted()) {
			return null;
		}
		assert (this.state == this.DELTA) || (this.state == this.HEADER);
		this.lastEvent = null;
		this.DELTA.delta = 0;
		while ((this.state == this.DELTA) || (this.state == this.HEADER)) {
			this.state.parse((byte) in.read());
		}
		do {
			this.state.parse((byte) in.read());
		} while ((this.state != this.DELTA) && (this.state != this.HEADER));
		return this.lastEvent;
	}

	/**
	 * Requests to delete and clear all cached data to prepare next parsing
	 * routine
	 * 
	 * @param newMidi
	 *            -
	 * @throws Exception
	 *             -
	 */
	protected abstract void prepareMidi(final Path newMidi) throws Exception;
}
