package stone.modules.songData;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.util.Path;


/**
 * class for holding a uniform song title
 * 
 * @author Nelphindal
 */
public class SongName implements Map<Integer, String> {

	enum Month {
		Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec;
	}

	private static final int findEnd(final StringBuilder sb, final String line,
			int pos) {
		while (true) {
			if (line.length() == pos) {
				return pos;
			}
			int i = pos;
			final char c = line.charAt(i++);
			switch (c) {
			case '/':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				sb.append(c);
				break;

			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'Z':
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'z':
			case '[':
			case ']':
				return i - 1;

			case ' ':
				return i;

			case '+':
				return line.indexOf('+', i) + 1;

			case '\'':
			case ',':
				break;

			case '-':
			case '^':
				return i;

			default:
				return i;
			}
		}
	}

	private String duration = null, title = null;
	private Integer n = null;

	private Path path = null;
	private final Path basePath;
	private boolean init = false;
	private final Map<Integer, Integer> renumberMap = new HashMap<>();

	private final Map<Integer, Integer[]> partsIndices = new TreeMap<>();

	private final Map<Integer, String> partsName = new HashMap<>();

	SongName(final Path base) {
		this.basePath = base;
	}

	/**
	 * adds a new part of this song
	 * 
	 * @param idx
	 *            the partNumbers, idx[0] is considered as total parts
	 * @param instrument
	 *            -
	 * @param xIdx
	 *            -
	 */
	public final void addPart(final Integer[] idx, final String instrument,
			int xIdx) {
		this.partsIndices.put(xIdx, idx);
		this.partsName.put(xIdx, instrument);

	}

	/** */
	@Override
	public final void clear() {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final boolean containsKey(final Object key) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final boolean containsValue(final Object value) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final Set<java.util.Map.Entry<Integer, String>> entrySet() {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final String get(final Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the title for given song part. Missing data will be implied, all
	 * song parts will be reordered to guarantee that parts numbers are [1,n]
	 * 
	 * @param xIdx
	 *            the part to return
	 * @param io
	 *            IO-Handler to use
	 * @return the title for given song part
	 */
	public final String getTitle(int xIdx, final IOHandler io) {
		if (!this.init) {
			align();
			if (this.duration == null) {
				calculateDuration(io);
			}
			if (this.title == null) {
				this.title = this.path.getFilename().substring(0,
						this.path.getFilename().length() - 4);
			}
		}
		final Integer x = this.renumberMap.get(xIdx);
		if (x != null) {
			return getTitle(x.intValue(), io);
		}
		final StringBuilder sb = new StringBuilder(60);
		sb.append("X:");
		sb.append(xIdx);
		sb.append("\r\n");
		sb.append("T: ");
		sb.append(this.title);

		if (this.n == 1) {
			/*
			 * name schema?
			 */
		} else {
			final Integer[] idx = this.partsIndices.get(Integer.valueOf(xIdx));
			sb.append(" ");
			sb.append(idx[1]);
			for (int i = 2; i < idx.length; i++) {
				sb.append(",");
				sb.append(idx[i]);
			}
			sb.append("/");
			sb.append(this.n);
		}
		final String partsName_ = this.partsName.get(xIdx);
		if (partsName_ != null) {
			sb.append(" ");
			sb.append(partsName_);
		}

		if (xIdx == 1) {
			sb.append(" (");
			sb.append(this.duration);
			sb.append(")");
		} else if (xIdx == 2) {
			sb.append(" ");
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(this.path.toFile().lastModified());
			sb.append(Month.values()[calendar.get(Calendar.MONTH)]);
			sb.append(" ");
			sb.append(calendar.get(Calendar.DAY_OF_MONTH));
		}
		return sb.toString();
	}

	/** */
	@Override
	public final boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final Set<Integer> keySet() {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final String put(final Integer key, final String value) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public void putAll(final Map<? extends Integer, ? extends String> m) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final String remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the title
	 * 
	 * @param name
	 *            -
	 * @param duration
	 *            -
	 * @param n
	 *            total number of parts
	 * @param path
	 *            -
	 */
	@SuppressWarnings("hiding")
	public final void setTitle(final String name, final String duration,
			final Integer n, final Path path) {
		this.title = name;
		this.duration = duration;
		if (n == null) {
			this.n = this.partsName.size();
		} else {
			this.n = n;
		}
		this.path = path;

	}

	/** */
	@Override
	public final int size() {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final String toString() {
		String ret;
		if (this.title == null) {
			ret = "<title>";
		} else {
			ret = this.title;
		}
		ret += " ";
		if (this.duration == null) {
			ret += "<time>";
		} else {
			ret += this.duration;
		}
		ret += " ";
		if (this.n == null) {
			ret += "?";
		} else {
			ret += this.n;
		}
		ret += " parts ";
		if (this.path == null) {
			ret += "<path>";
		} else {
			ret += this.path.relativize(this.basePath);
		}
		ret += " " + this.partsName;
		return ret;
	}

	/** */
	@Override
	public final Collection<String> values() {
		throw new UnsupportedOperationException();
	}

	private final void align() {
		this.init = true;
		final Set<Integer> toRenumber = new TreeSet<>();
		for (final Map.Entry<Integer, Integer[]> e : this.partsIndices
				.entrySet()) {
			if (e.getKey().intValue() > this.partsIndices.size()) {
				toRenumber.add(e.getKey());
			}
		}
		if (toRenumber.isEmpty()) {
			return;
		}
		final Iterator<Integer> toRenumberIter = toRenumber.iterator();
		for (int i = 1; toRenumberIter.hasNext()
				&& (i <= this.partsIndices.size()); i++) {
			if (this.partsIndices.get(i) == null) {
				final Integer old = toRenumberIter.next();
				final Integer[] idx = this.partsIndices.remove(old);
				for (int j = 1; j < idx.length; j++) {
					if (idx[j].intValue() == old.intValue()) {
						idx[j] = i;
					}
				}
				this.partsIndices.put(i, idx);
				this.partsName.put(i, this.partsName.remove(old));
				this.renumberMap.put(old, i);
				for (final Integer j : this.partsIndices.keySet()) {
					if (j.intValue() == i) {
						continue;
					}
					final Integer[] idx_j = this.partsIndices.get(j);
					for (int k = 1; k < idx_j.length; k++) {
						if (idx_j[k].intValue() == old.intValue()) {
							idx_j[k] = i;
						}
					}

				}
				for (int j = 1; j < i; j++) {
					final Integer[] idx_j = this.partsIndices.get(j);
					for (int k = 1; k < idx_j.length; k++) {
						if ((idx_j[k] != null)
								&& (idx_j[k].intValue() == old.intValue())) {
							idx_j[k] = i;
						}
					}

				}
			}
		}
	}

	private final void calculateDuration(final IOHandler io) {
		final InputStream in = io.openIn(this.path.toFile());
		double max = 0;
		double quantity = 1;
		double base = 0.125;
		try {
			double sum = 0;
			double meter = 1.0;
			double breakDuration = 0;
			while (true) {
				String line = in.readLine();
				if (line == null) {
					sum /= quantity * meter;
					if (sum > max) {
						max = sum;
					}
					break;
				}
				line = line.trim();
				if (line.startsWith("%") || line.isEmpty()) {
					continue;
				}
				if (line.startsWith("X:")) {
					// new song
					sum /= quantity * meter;
					if (sum > max) {
						max = sum;
					}
					sum = 0;
					breakDuration = 0;
					continue;
				} else if (line.startsWith("L:")) {
					final String[] baseParts = line.substring(2).trim()
							.split("/");
					base = Double.parseDouble(baseParts[0].trim())
							/ Double.parseDouble(baseParts[1].trim());
					continue;
				} else if (line.startsWith("M:")) {
					// meter change within a tune body not allowed
					final String[] meterParts = line.substring(2).trim()
							.split("/");
					meter = Double.parseDouble(meterParts[0].trim())
							/ Double.parseDouble(meterParts[1].trim());
					continue;
				} else if (line.startsWith("Q:")) {
					if (line.contains("=")) {
						final String[] lineParts = line.split("=");
						final String[] baseParts = lineParts[0].substring(2)
								.trim().split("/");
						base = Double.parseDouble(baseParts[0].trim())
								/ Double.parseDouble(baseParts[1].trim());
						quantity = Integer.parseInt(lineParts[1].trim());
					} else {
						quantity = Integer.parseInt(line.substring(2).trim());
					}
					continue;
				} else if ((line.length() >= 2) && (line.charAt(1) == ':')) {
					continue;
				}
				int pos = 0;
				double max_accord = 0;
				boolean accord = false;
				while (pos < line.length()) {
					if ((line.charAt(pos) == '|') || (line.charAt(pos) == ' ')) {
						++pos;
					} else if (line.charAt(pos) == '[') {
						accord = true;
						max_accord = 0;
						++pos;
					} else if (line.charAt(pos) == ']') {
						accord = false;
						sum += max_accord;
						max_accord = 0;
						++pos;
					} else if ((line.charAt(pos) == '^')
							|| (line.charAt(pos) == '=')
							|| (line.charAt(pos) == '_')) {
						++pos;
					} else if (line.charAt(pos) == '+') {
						pos = line.indexOf('+', ++pos) + 1;
					} else if ((line.charAt(pos) == 'z')
							|| (line.charAt(pos) == 'Z')) {
						final double d;
						final StringBuilder sb = new StringBuilder();
						pos = SongName.findEnd(sb, line, ++pos);

						final String[] meterParts = sb.toString().split("/");
						if (meterParts.length == 0) {
							d = Math.pow(2, -sb.length());
						} else {
							if (meterParts[0].isEmpty()) {
								meterParts[0] = "1";
							}
							if (meterParts.length == 1) {
								d = Double.parseDouble(meterParts[0]);
							} else {
								d = Double.parseDouble(meterParts[0])
										/ Double.parseDouble(meterParts[1]);
							}
						}
						breakDuration += d;
					} else {
						final double d;
						// find end of note
						final StringBuilder sb = new StringBuilder();
						pos = SongName.findEnd(sb, line, ++pos);
						final String[] meterParts = sb.toString().split("/");
						if (meterParts.length == 0) {
							d = Math.pow(2, -sb.length());
						} else {
							if (meterParts[0].isEmpty()) {
								meterParts[0] = "1";
							}
							if (meterParts.length == 1) {
								d = Double.parseDouble(meterParts[0]);
							} else {
								d = Double.parseDouble(meterParts[0])
										/ Double.parseDouble(meterParts[1]);
							}
						}
						if (breakDuration != 0) {
							sum += breakDuration;
							breakDuration = 0;
						}
						if (accord) {
							if (max_accord < d) {
								max_accord = d;
							}
						} else {
							sum += d;
						}
					}

				}
			}
			max *= base / 0.25;
			final int minutes = (int) max;
			final int seconds = (int) ((max - minutes) * 60);
			this.duration = String.format("%01d:%02d", minutes, seconds);
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			this.duration = "??:??";
		} finally {
			io.close(in);
		}
	}
}
