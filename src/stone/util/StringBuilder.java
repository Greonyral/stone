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

	private final char[][] content =
			new char[2][4 * StringBuilder.PATTERN_SIZE];
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
		if (head == 0) {
			if (tail == content[cIdx].length) {
				growAndCopy();
			} else {
				head = content[cIdx].length;
			}
		}
		content[cIdx][--head] = c;
	}

	/**
	 * Appends a string to the front. Succeeding calls of appendFirst("foo") and
	 * appendFirst("bar") on a empty instance of StringBuilder will result in
	 * "barfoo".
	 * 
	 * @param s
	 *            String to append
	 * @return <i>this<i>
	 */
	public final StringBuilder appendFirst(final String s) {
		if (isEmpty()) {
			set(s);
			return this;
		}
		final char[] array = s.toCharArray();
		if (head > tail) {
			growAndCopy();
			// TODO more efficient implementation?
		}
		if ((tail + array.length) >= content[cIdx].length) {
			final int length = length();
			grow();
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE + s.length(), length);
			switchBuffer();
			head = StringBuilder.PATTERN_SIZE;
			tail = length;
		}
		System.arraycopy(array, 0, content[cIdx],
				StringBuilder.PATTERN_SIZE, array.length);
		tail += array.length;
		return this;
	}

	/**
	 * Appends c to the end.
	 * 
	 * @param c
	 */
	public final void appendLast(char c) {
		if (tail == content[cIdx].length) {
			if (head == 0) {
				growAndCopy();
			} else {
				tail = 0;
			}
		}
		content[cIdx][tail++] = c;
	}

	/**
	 * Appends s to the end.
	 * 
	 * @param s
	 * @return <i>this</i>
	 */
	public final StringBuilder appendLast(final String s) {
		if (isEmpty()) {
			set(s);
			return this;
		}
		final char[] array = s.toCharArray();
		if (head > tail) {
			growAndCopy();
			// TODO more efficient implementation?
		}
		if ((tail + array.length) >= content[cIdx].length) {
			final int length = length();
			grow();
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length);
			switchBuffer();
			head = StringBuilder.PATTERN_SIZE;
			tail = length + head;
		}
		System.arraycopy(array, 0, content[cIdx], tail, array.length);
		tail += array.length;
		return this;
	}

	/**
	 * @param pos
	 * @return the character at <i>pos</i>.
	 */
	public final char charAt(int pos) {
		return content[cIdx][(head + pos) % content[cIdx].length];
	}

	/**
	 * Clears <i>this</i> content.
	 */
	public final void clear() {
		tail = head = StringBuilder.PATTERN_SIZE;
	}

	/**
	 * @return <i>true</i> if the length is 0 and {@link #toString()} would
	 *         return an empty string.
	 */
	public final boolean isEmpty() {
		return head == tail;
	}

	/**
	 * @return the length of currently contained string
	 */
	public final int length() {
		if (tail < head) {
			return (content[cIdx].length - head) + tail;
		}
		return tail - head;
	}

	/**
	 * Removes the last char and returns it.
	 * 
	 * @return the removed char or -1 if <i>this</i> has been empty
	 */
	public final int removeLast() {
		if (tail == head) {
			return -1;
		}
		final char c = content[cIdx][tail];
		if (--tail == 0) {
			copy();
		}
		return c;
	}

	/**
	 * Sets the content to <i>s</i>. It has equal effect like the calls clear()
	 * and append{First, Last}(s).
	 * 
	 * @param s
	 */
	public final void set(final String s) {
		if ((s == null) || s.isEmpty()) {
			clear();
			return;
		}
		if (content[cIdx].length < (s.length() + (StringBuilder.PATTERN_SIZE * 2))) {
			content[cIdx] =
					new char[(s.length() + (StringBuilder.PATTERN_SIZE * 4))
							- (s.length() % (StringBuilder.PATTERN_SIZE * 2))];
		}
		System.arraycopy(s.toCharArray(), 0, content[cIdx],
				StringBuilder.PATTERN_SIZE, s.length());
		head = StringBuilder.PATTERN_SIZE;
		tail = head + s.length();
	}

	/**
	 * Sets the contained string to start <i>offset</i> positions later.
	 * Unspecified behavior if offset is greater than actual length.
	 * 
	 * @param offset
	 */
	public final void substring(int offset) {
		head = (head + offset) % content[cIdx].length;
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
		if (head == tail) {
			return "";
		}
		if (head > tail) {
			grow();
			final int l = content[cIdx].length - head;
			System.arraycopy(content[cIdx], head, content[cIdxNext], 0, l);
			System.arraycopy(content[cIdx], 0, content[cIdxNext], l, tail);
			return new String(content[cIdxNext], 0, tail + l);
		}
		return new String(content[cIdx], head, tail - head);
	}

	private final void copy() {
		final int length = length();
		if (tail < head) {
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length - tail);
			System.arraycopy(content[cIdx], 0, content[cIdxNext],
					(length - tail) + StringBuilder.PATTERN_SIZE, tail);

		} else if ((head > StringBuilder.PATTERN_SIZE)
				|| (head < (StringBuilder.PATTERN_SIZE >> 2))) {
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length);
		} else {
			return;
		}
		head = StringBuilder.PATTERN_SIZE;
		tail = head + length;
		switchBuffer();
	}

	private final void grow() {
		if ((content[cIdxNext] == null)
				|| (content[cIdxNext].length <= content[cIdx].length)) {
			content[cIdxNext] =
					new char[content[cIdx].length
							+ (StringBuilder.PATTERN_SIZE * 2)];
		}
	}

	private final void growAndCopy() {
		grow();
		copy();
	}

	private final boolean handleControl(int keycode, int[] cursor,
			boolean alt) {
		switch (keycode) {
			case KeyEvent.VK_A:
				cursor[1] = 0;
				cursor[2] = length();
				return true;
			case KeyEvent.VK_C:
				if (head > tail) {
					growAndCopy();
				}
				StringBuilder.clip.setContents(new StringSelection(
						new String(content[cIdx], head + cursor[1],
								cursor[2] - cursor[1])), null);
				return true;
			case KeyEvent.VK_Q:
				if (alt) {
					insert('@', cursor);
				}
				return true;
			case KeyEvent.VK_V:
				try {
					final String pasted =
							(String) StringBuilder.clip.getContents(null)
									.getTransferData(
											DataFlavor.stringFlavor);
					for (final char c : pasted.toCharArray()) {
						insert(c, cursor);
						cursor[1] = cursor[2] = cursor[0];
					}
				} catch (final UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
				}
				return true;
		}
		return false;
	}

	private final void insert(char c, int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (head > tail) {
				copy();
			}
			final int length = length();
			final int lengthDelta = cursorArray[2] - cursorArray[1] - 1;
			if (lengthDelta != 0) {
				grow();
				System.arraycopy(content[cIdx], head, content[cIdxNext],
						StringBuilder.PATTERN_SIZE, cursorArray[1]);
				System.arraycopy(content[cIdx], head + cursorArray[2],
						content[cIdxNext], StringBuilder.PATTERN_SIZE
								+ cursorArray[1] + 1, length
								- cursorArray[2]);
				switchBuffer();
			}
			content[cIdx][cursorArray[1] + head] = c;
			tail -= lengthDelta;
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
			if (length > ((content[cIdxNext].length * 3) / 4)) {
				grow();
			}

			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursor);
			System.arraycopy(content[cIdx], head + cursor,
					content[cIdxNext], StringBuilder.PATTERN_SIZE + cursor
							+ 1, length - cursor);
			switchBuffer();
			content[cIdx][StringBuilder.PATTERN_SIZE + cursor] = c;
			head = StringBuilder.PATTERN_SIZE;
			tail = StringBuilder.PATTERN_SIZE + length + 1;
		}
	}

	private final void insert(final String s, int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (head > tail) {
				copy();
			}
			final int length = length();
			final int lengthDelta = cursorArray[2] - cursorArray[1] - 1;
			if (lengthDelta != 0) {
				grow();
				System.arraycopy(content[cIdx], head, content[cIdxNext],
						StringBuilder.PATTERN_SIZE, cursorArray[1]);
				System.arraycopy(content[cIdx], head + cursorArray[2],
						content[cIdxNext], StringBuilder.PATTERN_SIZE
								+ cursorArray[1] + s.length(), length
								- cursorArray[2]);
				switchBuffer();
				tail -= head - StringBuilder.PATTERN_SIZE;
				head = StringBuilder.PATTERN_SIZE;
			}
			System.arraycopy(s.toCharArray(), 0, content[cIdx],
					StringBuilder.PATTERN_SIZE + cursorArray[1], s
							.length());
			tail -= lengthDelta;
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
			if (length > ((content[cIdx].length * 3) / 4)) {
				grow();
			}
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursor);
			System.arraycopy(content[cIdx], head + cursor,
					content[cIdxNext], StringBuilder.PATTERN_SIZE + cursor
							+ s.length(), length - cursor);
			switchBuffer();
			System.arraycopy(s.toCharArray(), 0, content[cIdx],
					StringBuilder.PATTERN_SIZE + cursor, s.length());
			head = StringBuilder.PATTERN_SIZE;
			tail = StringBuilder.PATTERN_SIZE + length + 1;
		}
	}

	private final void remove(int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (head > tail) {
				copy();
			}
			final int length = length();
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursorArray[1]);
			System.arraycopy(content[cIdx], head + cursorArray[2],
					content[cIdxNext], StringBuilder.PATTERN_SIZE
							+ cursorArray[1], length - cursorArray[2]);
			switchBuffer();
			head = StringBuilder.PATTERN_SIZE;
			tail =
					(StringBuilder.PATTERN_SIZE + length)
							- (cursorArray[2] - cursorArray[1]);
			cursorArray[0] = cursorArray[2] = cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0];
		if ((head == tail) || (cursor < 0)) {
			cursorArray[0] = 0;
			return;
		}
		if (cursor == 0) {
			removeFirst();
		} else if (cursor == (length() - 1)) {
			removeLast();
		} else {
			final int length = length();
			if (head > tail) {
				copy();
			}
			if (content[cIdxNext].length < content[cIdx].length) {
				content[cIdxNext] = new char[content[cIdx].length];
			}
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursor);
			System.arraycopy(content[cIdx], head + cursor + 1,
					content[cIdxNext],
					StringBuilder.PATTERN_SIZE + cursor, length - cursor
							- 1);
			switchBuffer();
			tail = (StringBuilder.PATTERN_SIZE + length) - 1;
			head = StringBuilder.PATTERN_SIZE;
		}
	}

	private final void removeFirst() {
		if (head == tail) {
			return;
		}
		if (length() == 1) {
			head = StringBuilder.PATTERN_SIZE;
			tail = StringBuilder.PATTERN_SIZE;
			return;
		}
		if (++head == content[cIdx].length) {
			head = 0;
		}
	}

	private final void switchBuffer() {
		cIdx = cIdxNext;
		cIdxNext = (cIdxNext + 1) & 0x1;
	}

	/**
	 * @return the last character of the currently contained string or -1 if
	 *         <i>this</i> is empty.
	 */
	final protected int getLast() {
		if (tail == head) {
			return -1;
		}
		final char c = content[cIdx][tail];
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

	final void handleEvent(final KeyEvent e, int[] cursor) {
		if (cursor[0] > length()) {
			cursor[0] = length();
		}
		if (e.isControlDown()) {
			if (handleControl(e.getKeyCode(), cursor, e.isAltDown())) {
				return;
			}
		}
		final int c = e.getKeyCode();
		switch (c) {
			case KeyEvent.VK_HOME:
				if (e.isShiftDown()) {
					cursor[1] = 0;
				} else {
					cursor[0] = 0;
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
			case KeyEvent.VK_END:
				if (e.isShiftDown()) {
					cursor[2] = length();
				} else {
					cursor[0] = length();
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
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
				return;
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
				return;
			case KeyEvent.VK_BACK_SPACE:
				if ((cursor[1] == cursor[2]) && (cursor[0] == 0)) {
					return;
				}
				--cursor[0];
				remove(cursor);
				if (length() < cursor[0]) {
					++cursor[0];
				}
				return;
			case KeyEvent.VK_DELETE:
				remove(cursor);
				return;
		}
		final char key = e.getKeyChar();
		if (key == KeyEvent.CHAR_UNDEFINED) {
			return;
		}
		insert(key, cursor);
	}

	final void replace(int pos, int len, final String string) {
		if (len != string.length()) {
			this.insert(string, new int[] { pos, pos, pos + len });
		} else {
			System.arraycopy(string.toCharArray(), 0, content[cIdx], head
					+ len, string.length());
		}
	}

	final void setHead(int offset) {
		head += offset;
	}
}
