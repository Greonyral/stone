package stone.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;


/**
 * StringBuilder implementing more or less the same functionality of
 * util.StringBuilder
 * 
 * @author Nelphindal
 */
public class StringBuilder {

	private final static int PATTERN_SIZE = 16;
	private final static Clipboard clip = Toolkit.getDefaultToolkit()
			.getSystemClipboard();

	private final char[][] content = new char[2][4 * StringBuilder.PATTERN_SIZE];
	private int head = StringBuilder.PATTERN_SIZE,
			tail = StringBuilder.PATTERN_SIZE;
	private int cIdx = 0;
	private int cIdxNext = 1;

	/**
	 * 
	 */
	public StringBuilder() {
	}

	/**
	 * @param value
	 *            initial value
	 */
	public StringBuilder(final String value) {
		if (value != null) {
			set(value);
		}
	}

	/**
	 * * Appends a char to the front. Succeeding calls of appendFirst('o'),
	 * appendFirst('o') and appendFirst('f') on a empty instance of
	 * StringBuilder will result in "foo".
	 * 
	 * @param c
	 *            char to append
	 */
	public final void appendFirst(char c) {
		if (this.head == 0) {
			if (this.tail == this.content[this.cIdx].length) {
				growAndCopy();
			} else {
				this.head = this.content[this.cIdx].length;
			}
		}
		this.content[this.cIdx][--this.head] = c;
	}

	/**
	 * Appends a string to the front. Succeeding calls of appendFirst("foo") and
	 * appendFirst("bar") on a empty instance of StringBuilder will result in
	 * "barfoo".
	 * 
	 * @param s
	 *            String to append
	 * @return <i>this</i>
	 */
	public final StringBuilder appendFirst(final String s) {
		if (isEmpty()) {
			set(s);
			return this;
		}
		final char[] array = s.toCharArray();
		if (this.head > this.tail) {
			growAndCopy();
			// TODO more efficient implementation?
		}
		if ((this.tail + array.length) >= this.content[this.cIdx].length) {
			final int length = length();
			grow();
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext],
					StringBuilder.PATTERN_SIZE + s.length(), length);
			switchBuffer();
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = length;
		}
		System.arraycopy(array, 0, this.content[this.cIdx],
				StringBuilder.PATTERN_SIZE, array.length);
		this.tail += array.length;
		return this;
	}

	/**
	 * Appends c to the end.
	 * 
	 * @param c
	 *            character to append
	 */
	public final void appendLast(char c) {
		if (this.tail == this.content[this.cIdx].length) {
			if (this.head == 0) {
				growAndCopy();
			} else {
				this.tail = 0;
			}
		}
		this.content[this.cIdx][this.tail++] = c;
	}

	/**
	 * Appends s to the end.
	 * 
	 * @param s
	 *            string to append
	 * @return <i>this</i>
	 */
	public final StringBuilder appendLast(final String s) {
		if (isEmpty()) {
			set(s);
			return this;
		}
		final char[] array = s.toCharArray();
		if (this.head > this.tail) {
			growAndCopy();
			// TODO more efficient implementation?
		}
		if ((this.tail + array.length) >= this.content[this.cIdx].length) {
			final int length = length();
			grow();
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					length);
			switchBuffer();
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = length + this.head;
		}
		System.arraycopy(array, 0, this.content[this.cIdx], this.tail,
				array.length);
		this.tail += array.length;
		return this;
	}

	/**
	 * @param pos
	 *            index in resulting string
	 * @return the character at <i>pos</i>.
	 */
	public final char charAt(int pos) {
		return this.content[this.cIdx][(this.head + pos)
				% this.content[this.cIdx].length];
	}

	/**
	 * Clears <i>this</i> content.
	 */
	public final void clear() {
		this.tail = this.head = StringBuilder.PATTERN_SIZE;
	}

	/**
	 * @return <i>true</i> if the length is 0 and {@link #toString()} would
	 *         return an empty string.
	 */
	public final boolean isEmpty() {
		return this.head == this.tail;
	}

	/**
	 * @return the length of currently contained string
	 */
	public final int length() {
		if (this.tail < this.head) {
			return (this.content[this.cIdx].length - this.head) + this.tail;
		}
		return this.tail - this.head;
	}

	/**
	 * Removes the last char and returns it.
	 * 
	 * @return the removed char or -1 if <i>this</i> has been empty
	 */
	public final int removeLast() {
		if (this.tail == this.head) {
			return -1;
		}
		final char c = this.content[this.cIdx][this.tail];
		if (--this.tail == 0) {
			copy();
		}
		return c;
	}

	/**
	 * Sets the content to <i>s</i>. It has equal effect like the calls clear()
	 * and append{First, Last}(s).
	 * 
	 * @param s
	 *            string to set to
	 */
	public final void set(final String s) {
		if ((s == null) || s.isEmpty()) {
			clear();
			return;
		}
		if (this.content[this.cIdx].length < (s.length() + (StringBuilder.PATTERN_SIZE * 2))) {
			this.content[this.cIdx] = new char[(s.length() + (StringBuilder.PATTERN_SIZE * 4))
					- (s.length() % (StringBuilder.PATTERN_SIZE * 2))];
		}
		System.arraycopy(s.toCharArray(), 0, this.content[this.cIdx],
				StringBuilder.PATTERN_SIZE, s.length());
		this.head = StringBuilder.PATTERN_SIZE;
		this.tail = this.head + s.length();
	}

	/**
	 * @param string
	 *            pattern to compare with
	 * @return <i>true</i> if resulting string starts with given string
	 */
	public final boolean startsWith(final String string) {
		return toString().startsWith(string);
	}

	/**
	 * @param string
	 *            pattern to compare with
	 * @param toffset
	 *            offset in resulting string
	 * @return <i>true</i> if resulting string starts at given offset with given
	 *         string
	 */
	public final boolean startsWith(final String string, int toffset) {
		return toString().startsWith(string, toffset);
	}

	/**
	 * Sets the contained string to start <i>offset</i> positions later.
	 * Unspecified behavior if offset is greater than actual length.
	 * 
	 * @param offset
	 *            index in resulting string
	 */
	public final void substring(int offset) {
		this.head = (this.head + offset) % this.content[this.cIdx].length;
	}

	/**
	 * @return the contained string with lower cased characters only.
	 */
	public final String toLowerCase() {
		return toString().toLowerCase();
	}

	/**
	 * @return the content of <i>this</i>.
	 */
	@Override
	public String toString() {
		if (this.head == this.tail) {
			return "";
		}
		if (this.head > this.tail) {
			grow();
			final int l = this.content[this.cIdx].length - this.head;
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], 0, l);
			System.arraycopy(this.content[this.cIdx], 0,
					this.content[this.cIdxNext], l, this.tail);
			return new String(this.content[this.cIdxNext], 0, this.tail + l);
		}
		return new String(this.content[this.cIdx], this.head, this.tail
				- this.head);
	}

	private final void copy() {
		final int length = length();
		if (this.tail < this.head) {
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					length - this.tail);
			System.arraycopy(this.content[this.cIdx], 0,
					this.content[this.cIdxNext], (length - this.tail)
							+ StringBuilder.PATTERN_SIZE, this.tail);

		} else if ((this.head > StringBuilder.PATTERN_SIZE)
				|| (this.head < (StringBuilder.PATTERN_SIZE >> 2))) {
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					length);
		} else {
			return;
		}
		this.head = StringBuilder.PATTERN_SIZE;
		this.tail = this.head + length;
		switchBuffer();
	}

	private final void grow() {
		if ((this.content[this.cIdxNext] == null)
				|| (this.content[this.cIdxNext].length <= this.content[this.cIdx].length)) {
			this.content[this.cIdxNext] = new char[this.content[this.cIdx].length
					+ (StringBuilder.PATTERN_SIZE * 2)];
		}
	}

	private final void growAndCopy() {
		grow();
		copy();
	}

	private final boolean handleControl(int keycode, int[] cursor, boolean alt) {
		switch (keycode) {
		case KeyEvent.VK_A:
			cursor[1] = 0;
			cursor[2] = length();
			return true;
		case KeyEvent.VK_C:
			if (this.head > this.tail) {
				growAndCopy();
			}
			StringBuilder.clip.setContents(new StringSelection(new String(
					this.content[this.cIdx], this.head + cursor[1], cursor[2]
							- cursor[1])), null);
			return true;
		case KeyEvent.VK_Q:
			if (alt) {
				insert('@', cursor);
			}
			return true;
		case KeyEvent.VK_V:
			try {
				final String pasted = (String) StringBuilder.clip.getContents(
						null).getTransferData(DataFlavor.stringFlavor);
				for (final char c : pasted.toCharArray()) {
					insert(c, cursor);
					cursor[1] = cursor[2] = cursor[0];
				}
			} catch (final UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
			}
			return true;
		default:
			break;
		}
		return false;
	}

	private final void insert(char c, int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (this.head > this.tail) {
				copy();
			}
			final int length = length();
			final int lengthDelta = cursorArray[2] - cursorArray[1] - 1;
			if (lengthDelta != 0) {
				grow();
				System.arraycopy(this.content[this.cIdx], this.head,
						this.content[this.cIdxNext],
						StringBuilder.PATTERN_SIZE, cursorArray[1]);
				System.arraycopy(this.content[this.cIdx], this.head
						+ cursorArray[2], this.content[this.cIdxNext],
						StringBuilder.PATTERN_SIZE + cursorArray[1] + 1, length
								- cursorArray[2]);
				switchBuffer();
			}
			this.content[this.cIdx][cursorArray[1] + this.head] = c;
			this.tail -= lengthDelta;
			cursorArray[0] = cursorArray[2] = ++cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0]++;
		if (cursor == length()) {
			appendLast(c);
		} else if (cursor == 0) {
			appendFirst(c);
		} else {
			final int length = length();
			if (length > ((this.content[this.cIdxNext].length * 3) / 4)) {
				grow();
			}

			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					cursor);
			System.arraycopy(this.content[this.cIdx], this.head + cursor,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE
							+ cursor + 1, length - cursor);
			switchBuffer();
			this.content[this.cIdx][StringBuilder.PATTERN_SIZE + cursor] = c;
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = StringBuilder.PATTERN_SIZE + length + 1;
		}
	}

	private final void insert(final String s, int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (this.head > this.tail) {
				copy();
			}
			final int length = length();
			final int lengthDelta = cursorArray[2] - cursorArray[1] - 1;
			if (lengthDelta != 0) {
				grow();
				System.arraycopy(this.content[this.cIdx], this.head,
						this.content[this.cIdxNext],
						StringBuilder.PATTERN_SIZE, cursorArray[1]);
				System.arraycopy(
						this.content[this.cIdx],
						this.head + cursorArray[2],
						this.content[this.cIdxNext],
						StringBuilder.PATTERN_SIZE + cursorArray[1]
								+ s.length(), length - cursorArray[2]);
				switchBuffer();
				this.tail -= this.head - StringBuilder.PATTERN_SIZE;
				this.head = StringBuilder.PATTERN_SIZE;
			}
			System.arraycopy(s.toCharArray(), 0, this.content[this.cIdx],
					StringBuilder.PATTERN_SIZE + cursorArray[1], s.length());
			this.tail -= lengthDelta;
			cursorArray[0] = cursorArray[2] = ++cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0]++;
		if (cursor == length()) {
			appendLast(s);
		} else if (cursor == 0) {
			appendFirst(s);
		} else {
			final int length = length();
			if (length > ((this.content[this.cIdx].length * 3) / 4)) {
				grow();
			}
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					cursor);
			System.arraycopy(this.content[this.cIdx], this.head + cursor,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE
							+ cursor + s.length(), length - cursor);
			switchBuffer();
			System.arraycopy(s.toCharArray(), 0, this.content[this.cIdx],
					StringBuilder.PATTERN_SIZE + cursor, s.length());
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = StringBuilder.PATTERN_SIZE + length + 1;
		}
	}

	private final void remove(int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (this.head > this.tail) {
				copy();
			}
			final int length = length();
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					cursorArray[1]);
			System.arraycopy(this.content[this.cIdx], this.head
					+ cursorArray[2], this.content[this.cIdxNext],
					StringBuilder.PATTERN_SIZE + cursorArray[1], length
							- cursorArray[2]);
			switchBuffer();
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = (StringBuilder.PATTERN_SIZE + length)
					- (cursorArray[2] - cursorArray[1]);
			cursorArray[0] = cursorArray[2] = cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0];
		if ((this.head == this.tail) || (cursor < 0)) {
			cursorArray[0] = 0;
			return;
		}
		if (cursor == 0) {
			removeFirst();
		} else if (cursor == (length() - 1)) {
			removeLast();
		} else {
			final int length = length();
			if (this.head > this.tail) {
				copy();
			}
			if (this.content[this.cIdxNext].length < this.content[this.cIdx].length) {
				this.content[this.cIdxNext] = new char[this.content[this.cIdx].length];
			}
			System.arraycopy(this.content[this.cIdx], this.head,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE,
					cursor);
			System.arraycopy(this.content[this.cIdx], this.head + cursor + 1,
					this.content[this.cIdxNext], StringBuilder.PATTERN_SIZE
							+ cursor, length - cursor - 1);
			switchBuffer();
			this.tail = (StringBuilder.PATTERN_SIZE + length) - 1;
			this.head = StringBuilder.PATTERN_SIZE;
		}
	}

	private final void removeFirst() {
		if (this.head == this.tail) {
			return;
		}
		if (length() == 1) {
			this.head = StringBuilder.PATTERN_SIZE;
			this.tail = StringBuilder.PATTERN_SIZE;
			return;
		}
		if (++this.head == this.content[this.cIdx].length) {
			this.head = 0;
		}
	}

	private final void switchBuffer() {
		this.cIdx = this.cIdxNext;
		this.cIdxNext = (this.cIdxNext + 1) & 0x1;
	}

	/**
	 * @return the last character of the currently contained string or -1 if
	 *         <i>this</i> is empty.
	 */
	final protected int getLast() {
		if (this.tail == this.head) {
			return -1;
		}
		final char c = this.content[this.cIdx][this.tail];
		return c;
	}


	final byte getByte(int pos) {
		int b0 = charAt(pos) - '0';
		int b1 = charAt(pos + 1) - '0';
		if (b0 > 9) {
			b0 -= 7;
			if (b0 > 16) {
				b0 -= 32;
			}
		}
		if (b1 > 9) {
			b1 -= 7;
			if (b1 > 16) {
				b1 -= 32;
			}
		}
		return (byte) ((b0 << 4) + b1);

	}

	final int handleEvent(final KeyEvent e, int[] cursor) {
		if (cursor[0] > length()) {
			cursor[0] = length();
		}
		if (e.isControlDown()) {
			if (handleControl(e.getKeyCode(), cursor, e.isAltDown())) {
				return 0;
			}
		}
		final int c = e.getKeyCode();
		switch (c) {
		case KeyEvent.VK_ENTER:
			return c;
		case KeyEvent.VK_ESCAPE:
			cursor[2] = cursor[0] = cursor[1];
			return 0;
		case KeyEvent.VK_HOME:
			if (e.isShiftDown()) {
				cursor[1] = 0;
			} else {
				cursor[0] = 0;
				cursor[2] = cursor[1] = cursor[0];
			}
			return 0;
		case KeyEvent.VK_END:
			if (e.isShiftDown()) {
				cursor[2] = length();
			} else {
				cursor[0] = length();
				cursor[2] = cursor[1] = cursor[0];
			}
			return 0;
		case KeyEvent.VK_LEFT:
			if (e.isShiftDown()) {
				if (cursor[1] > 0) {
					--cursor[1];
				}
			} else {
				if (cursor[0] > 0) {
					--cursor[0];
				}
				cursor[2] = cursor[1] = cursor[0];
			}
			return 0;
		case KeyEvent.VK_RIGHT:
			if (e.isShiftDown()) {
				if (cursor[2] < length()) {
					++cursor[2];
				}
			} else {
				if (cursor[0] < length()) {
					++cursor[0];
				}
				cursor[2] = cursor[1] = cursor[0];
			}
			return 0;
		case KeyEvent.VK_BACK_SPACE:
			if ((cursor[1] == cursor[2]) && (cursor[0] == 0)) {
				return 0;
			}
			--cursor[0];
			remove(cursor);
			if (length() < cursor[0]) {
				++cursor[0];
			}
			return 0;
		case KeyEvent.VK_DELETE:
			remove(cursor);
			return 0;
		default:
			break;
		}
		final char key = e.getKeyChar();
		if (key == KeyEvent.CHAR_UNDEFINED) {
			return 0;
		}
		insert(key, cursor);
		return 0;
	}

	final void replace(int pos, int len, final String string) {
		if (len != string.length()) {
			this.insert(string, new int[] { pos, pos, pos + len });
		} else {
			System.arraycopy(string.toCharArray(), 0, this.content[this.cIdx],
					this.head + len, string.length());
		}
	}

	final void setHead(int offset) {
		this.head += offset;
	}
}
