package stone.modules.midiData;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JPanel;


/**
 * A Map holding all decoded notes.
 * 
 * @author Nelphindal
 */
public class MidiMap {

	/**
	 * A class representing a note
	 * 
	 * @author Nelphindal
	 */
	public final class Note implements Comparable<Note> {

		final int key, volumne, track;
		final double start, end;

		Note(int key, double start, double end, int volumne, int track) {
			this.key = key;
			this.volumne = volumne;
			this.start = start;
			this.end = end;
			this.track = track;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		/** */
		@Override
		public final int compareTo(final Note o) {
			if (this.key == o.key) {
				if (this.start == o.start) {
					return (int) Math.signum(o.end - this.end);
				}
				return (int) Math.sin(o.start - this.start);
			}
			return o.key - this.key;
		}

		/**
		 * @return the time in minutes when this note ends.
		 */
		public final double getEnd() {
			return this.end;
		}

		/**
		 * @return the note played as encoded in the underlying midi.
		 */
		public final int getKey() {
			return this.key;
		}

		/**
		 * @return the time in minutes when this note starts.
		 */
		public final double getStart() {
			return this.start;
		}

		/** */
		@Override
		public final String toString() {
			return String.format("%02d: [%4.2f %4.2f]", this.key, this.start,
					this.end);
		}
	}

	private final static int MAX_VOL = 0x7f;
	private final static List<Color> colors = MidiMap.buildColorMap();

	private final static TreeMap<Integer, VRange> vMap = MidiMap.buildVMap();

	private final static List<Color> buildColorMap() {
		// 0 ppp 0x00007f
		// 1 pp 0x007fff
		// 2 p 0x00ff7f
		// 3 mf 0x007f000
		// 4 f 0x7f0000
		// 5 ff 0xff7f00
		// 6 fff 0x7f0000
		final List<Color> c = new ArrayList<>(6);
		c.add(new Color(0x00, 0x00, 0x7f));
		c.add(new Color(0x00, 0x7f, 0xff));
		c.add(new Color(0x00, 0xff, 0x7f));
		c.add(new Color(0x00, 0x7f, 0x00));
		c.add(new Color(0x7f, 0x00, 0x00));
		c.add(new Color(0xff, 0x7f, 0x00));
		c.add(new Color(0x7f, 0x00, 0x00));
		return c;
	}

	private final static TreeMap<Integer, VRange> buildVMap() {
		final TreeMap<Integer, VRange> map = new TreeMap<>();
		final double step = (double) MidiMap.MAX_VOL / MidiMap.colors.size();
		double lowerBound = 0;
		VRange last = null;
		final Iterator<Color> color = MidiMap.colors.iterator();
		while (color.hasNext()) {
			final double upperBound = lowerBound + step;
			last = new VRange((int) lowerBound, (int) upperBound, color, last);
			map.put((int) lowerBound, last);
			lowerBound = upperBound;
		}
		return map;
	}

	private final static Color getColor(final Note note) {
		return MidiMap.vMap.floorEntry(note.volumne).getValue()
				.getColor(note.volumne);
	}

	private final Set<Note> notes = new HashSet<>();

	private final Map<Double, Map<Integer, List<Note>>> timeToTracksMap = new HashMap<>();

	private final Dimension d = new Dimension();
	private final int scale = 480; // 16th note at 200 bpm
	private final MidiParser parser;

	private final static int NOTE_BOT = 18;

	private final static int NOTE_TOP = 108;

	private final int NOTE_RANGE = MidiMap.NOTE_TOP - MidiMap.NOTE_BOT;

	private MidiMap(final MidiMap clone) {
		this.parser = clone.parser;
		this.timeToTracksMap.putAll(clone.timeToTracksMap);
		this.notes.addAll(clone.notes);
	}

	MidiMap(final MidiParser parser) {
		this.parser = parser;
	}

	/**
	 * Adds a note to this map
	 * 
	 * @param track
	 * @param key
	 * @param start
	 * @param end
	 * @param volumne
	 */
	public final void addNote(int track, int key, double start, double end,
			int volumne) {
		final Note note = new Note(key, start, end, volumne, track);
		final Map<Integer, List<Note>> mapTrack = this.timeToTracksMap
				.get(start);
		final List<Note> listTrack;
		this.notes.add(note);

		if (mapTrack == null) {
			final Map<Integer, List<Note>> map = new HashMap<>();
			listTrack = new ArrayList<>();
			map.put(track, listTrack);
			this.timeToTracksMap.put(start, map);
		} else {
			final List<Note> listTrack_tmp = mapTrack.get(track);
			if (listTrack_tmp == null) {
				listTrack = new ArrayList<>();
				mapTrack.put(track, listTrack);
			} else {
				listTrack = listTrack_tmp;
			}
		}

		listTrack.add(note);
	}

	/**
	 * Clears this map.
	 */
	public final void clear() {
		this.timeToTracksMap.clear();
		this.notes.clear();
	}

	/**
	 * Creates a shadowed copy of this map. Adding Notes before cleaning either
	 * copy or this instance will effect both instances.
	 */
	@Override
	public MidiMap clone() {
		return new MidiMap(this);
	}

	/**
	 * Gets all notes which started to play at time on track id.
	 * 
	 * @param id
	 * @param time
	 * @return a list of notes.
	 */
	public final List<Note> get(int id, double time) {
		final Map<Integer, List<Note>> map = this.timeToTracksMap.get(time);
		if (map == null) {
			return null;
		}
		return map.get(id);
	}

	/**
	 * Gets all notes which started to play at time
	 * 
	 * @param time
	 * @return a map of notes. Index is the track where the notes are played.
	 */
	public final Map<Integer, List<Note>> getNotes(double time) {
		return this.timeToTracksMap.get(time);
	}

	/**
	 * Sets all options of mainPanel to display this map.
	 * 
	 * @param mainPanel
	 */
	public final void init(final JPanel mainPanel) {
		// System.out.println(parser.getDuration());
		this.d.width = (int) ((this.parser.getDuration() + (1.0 / 60.0)) * this.scale);
		this.d.height = this.parser.tracks().size() * (this.NOTE_RANGE + 9);
		mainPanel.setMaximumSize(this.d);
		mainPanel.setMinimumSize(this.d);
		mainPanel.setPreferredSize(this.d);
		mainPanel.setSize(this.d);
	}

	/**
	 * Paints this map on previously set panel.
	 * 
	 * @param g
	 */
	public final void paint(final Graphics g) {
		g.clearRect(0, 0, this.d.width, this.d.height);
		final Graphics g0 = g.create();
		final int heightPerSong = this.NOTE_RANGE + 9;
		final Font f1 = Font.decode("Arial bold 14");
		final Font f0 = Font.decode("Arial 8");
		{
			final Graphics g1 = g0.create();
			g1.setColor(Color.BLACK);
			g1.setFont(f0);
			for (int x = 12; x < this.d.width; x += this.scale) {
				for (int y = f1.getSize(); y < this.d.height; y += heightPerSong) {
					g1.drawString(String.valueOf((y / heightPerSong) + 1), x,
							y + 4);
					g1.drawString(String.valueOf((y / heightPerSong) + 1), x,
							(y + heightPerSong) - (2 * f1.getSize()) - 4);
				}
			}

		}
		g0.setColor(Color.BLACK);
		g0.setFont(Font.decode("Times 11"));

		// draw grid
		for (int x = 0, sec = -1, min = 0; x < this.d.width; x += this.scale / 6) {
			g0.drawLine(x, 0, x, this.d.height);
			if (++sec == 6) {
				sec = 0;
				++min;
				g0.drawLine(x - 1, 0, x - 1, this.d.height);
				g0.drawLine(x + 1, 0, x + 1, this.d.height);
			}
			for (int y = 9; y < this.d.height; y += 5 * heightPerSong) {
				g0.drawString(String.format("%d:%d0", min, sec), x + 4, y);
			}
		}

		for (int y = heightPerSong - 1; y < this.d.height; y += heightPerSong) {
			g0.drawLine(0, y, this.d.width, y);
		}
		final int scale_ = getScale();

		// draw notes
		for (final Note note : this.notes) {
			final int x0 = (int) (note.start * scale_);
			final int x1 = (int) (note.end * scale_);
			final int y;
			if (note.key < MidiMap.NOTE_BOT) {
				y = (note.track * heightPerSong) - 1;
				g0.setColor(Color.RED);
			} else if (note.key >= MidiMap.NOTE_TOP) {
				y = (note.track * heightPerSong)
						- (MidiMap.NOTE_TOP - MidiMap.NOTE_BOT);
				g0.setColor(Color.RED);
			} else {
				y = (note.track * heightPerSong)
						- (note.key - MidiMap.NOTE_BOT);
				g0.setColor(MidiMap.getColor(note));

			}
			g0.drawLine(x0, y, x1, y);
		}
		// for (final Map.Entry<Integer, Map<Double, List<Note>>> entry :
		// tracksToTimeMap
		// .entrySet()) {
		// final int col = entry.getKey().intValue() * heightPerSong;
		// for (final Map.Entry<Double, List<Note>> noteMap : entry.getValue()
		// .entrySet()) {
		// final int row = (int) (noteMap.getKey() * scale);
		// for (final Note note : noteMap.getValue()) {
		// final int key = note.getKey();
		// final int y;
		// if (key < MidiMap.NOTE_BOT) {
		// y = col;
		// g.setColor(Color.RED);
		// } else if (key >= MidiMap.NOTE_TOP) {
		// y = col + MidiMap.NOTE_TOP;
		// g.setColor(Color.RED);
		// } else {
		// y = col + key - MidiMap.NOTE_BOT;
		// g0.setColor(getColor(note));
		// }
		// g0.drawLine(row, y + 3, (int) (note.getEnd() * scale), y + 3);
		// }
		// }
		// }
	}

	/** */
	@Override
	public final String toString() {
		return "MidiState";
	}

	private final int getScale() {
		final int scale_;
		synchronized (this) {
			scale_ = this.scale;
		}
		return scale_;
	}

}
