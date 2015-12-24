package stone.modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import stone.io.GUIInterface;
import stone.util.Debug;
import stone.util.Path;

/**
 * Abstraction for basic I/O for supporting commands on GUI or console.
 * 
 * @author Nelphindal
 * 
 */
public class Console {

	private static final int MAX_OUTLINES_BUFFERED = 400;
	private static final String BR = "<br/>";
	private static final byte[] SP = "&nbsp;".getBytes();

	private final OutputStream out;
	private final CommandInterpreter ci;
	private boolean firstOut;

	final State state = new State();

	private Console(@SuppressWarnings("hiding") final CommandInterpreter ci,
			final OutputStream outStream) {
		out = outStream;
		out("type \"help\" to get a list of available commands\n");
		firstOut = true;
		this.ci = ci;
	}

	private class InputKeyListener implements KeyListener {

		private final char[] c1a = new char[1];
		private final JTextField textField;

		InputKeyListener(final JTextField in) {
			this.textField = in;
		}


		@Override
		public void keyTyped(final KeyEvent e) {
			c1a[0] = e.getKeyChar();
			if (c1a[0] < 0x20) {
				if (c1a[0] == '\b') {
					if (textField.getSelectionStart() == textField
							.getSelectionEnd()) {
						int pos = textField.getCaretPosition();
						if (pos == 0)
							return;
						textField.setSelectionStart(pos - 1);
					}
					textField.replaceSelection("");
				}
				return;
			} else if (c1a[0] == 0x7f) {
				if (textField.getSelectionStart() == textField
						.getSelectionEnd()) {
					int pos = textField.getCaretPosition();
					if (pos == textField.getText().length())
						return;

					textField.setSelectionEnd(pos + 1);
				}
				textField.replaceSelection("");
				return;
			}
			textField.replaceSelection(new String(c1a));
		}


		@Override
		public void keyReleased(final KeyEvent e) {
			final int keycode = e.getKeyCode();
			switch (keycode) {
			case KeyEvent.VK_ENTER:
				if (!ci.handleEnter(textField.getText()))
					out("No such command\n");
				textField.setText("");
				break;
			case KeyEvent.VK_BACK_SPACE:
				keyTyped(e);
				break;
			case KeyEvent.VK_DELETE:
				keyTyped(e);
				break;
			case KeyEvent.VK_LEFT:
				moveCursorLeft(e.isShiftDown());
				break;
			case KeyEvent.VK_RIGHT:
				moveCursorRight(e.isShiftDown());
				break;
			case KeyEvent.VK_HOME:
				textField.setCaretPosition(0);
				break;
			case KeyEvent.VK_END:
				textField.setCaretPosition(textField.getText().length());
				break;
			case KeyEvent.VK_TAB:
				// not called actually
				final String in = textField.getText();
				final Set<String> commands = ci.handleTab(in,
						c1a[0] == KeyEvent.VK_TAB);
				if (commands == null)
					break;
				if (c1a[0] != KeyEvent.VK_TAB && commands.size() == 1) {
					textField.setText(commands.iterator().next());
				} else {
					out(in + "[TAB]");
					for (final String s : commands) {
						out(s);
					}
				}
				c1a[0] = KeyEvent.VK_TAB;
				break;
			default:
				return;
			}
			e.consume();
		}


		@Override
		public void keyPressed(final KeyEvent e) {
			e.consume();
		}


		private void moveCursorLeft(boolean shiftDown) {
			if (shiftDown) {
				int start = textField.getSelectionStart();
				if (start > 0) {
					textField.setSelectionStart(start - 1);
				}
			} else {
				int pos = textField.getCaretPosition();
				if (pos > 0)
					textField.setCaretPosition(pos - 1);
			}
		}


		private void moveCursorRight(boolean shiftDown) {
			int length = textField.getText().length();
			if (shiftDown) {
				int end = textField.getSelectionEnd();
				if (end < length) {
					textField.setSelectionEnd(end + 1);
				}
			} else {
				int pos = textField.getCaretPosition();
				if (pos < length)
					textField.setCaretPosition(pos + 1);
			}
		}
	}

	void out(final String string) {

		try {
			synchronized (out) {
				if (firstOut) {
					firstOut = false;
					out.write('\r');
					out.write('\r');
				}
				Debug.print("%s", string);
				out.write(string.getBytes());
			}
		} catch (final IOException e) {
			exit();
		}
	}

	private static void parseStream(final InputStream in,
			final CommandInterpreter ci, final Object endSignal) {
		class ListenerThread extends Thread {


			public ListenerThread() {
				new Thread() {
					@Override
					public void run() {
						synchronized (endSignal) {
							try {
								endSignal.wait();
							} catch (final InterruptedException e) {
								ListenerThread.this.interrupt();
							}
						}
					}
				}.start();
			}

			@Override
			public void run() {
				BufferedReader r = new BufferedReader(new InputStreamReader(in));
				while (true) {
					String line;
					try {
						line = r.readLine();
					} catch (final IOException e) {
						return;
					}
					if (line == null)
						return;
					ci.handleEnter(line);
				}
			}
		}

		new ListenerThread().start();
	}

	static Console createConsoleGUI(final JPanel panel) {
		final JEditorPane out = new JEditorPane();
		final JScrollPane jscroll = new JScrollPane(out);
		final JTextField in = new JTextField();
		final Object endSignal = GUIInterface.Button.class;
		final CommandInterpreter ci = new CommandInterpreter(endSignal);


		final OutputStream outStream = new OutputStream() {

			private int lines = 0;
			private final StringBuilder sb = new StringBuilder();
			private final JScrollBar vscroll = jscroll.getVerticalScrollBar();

			@Override
			public void write(int b) throws IOException {
				switch (b) {
				case ' ':
					write(SP);
					return;
				case '\t':
					int startOfLastLine = sb.lastIndexOf(BR);
					int tabWidth = 4;
					final String line;
					if (startOfLastLine < 0) {
						line = sb.toString().replaceAll("<.*>", "");
					} else {
						line = sb.substring(startOfLastLine + 5).replaceAll(
								"<.*>", "");
					}

					int mod = line.length() % tabWidth;
					while (mod-- > 0) {
						write(' ');
					}
					return;
				case '\n':
					sb.append(BR);
					if (++lines >= MAX_OUTLINES_BUFFERED) {
						--lines;
						String truncated = sb.substring(sb.indexOf(BR)
								+ BR.length());
						if (truncated.startsWith("</")) {
							truncated = truncated.substring(truncated
									.indexOf(BR) + BR.length());
							--lines;
						}
						sb.setLength(0);
						sb.append(truncated);
					}

					while (true)
						try {
							out.setText("<font face=\"Courier New\">"
									+ sb.toString() + "</font>");
							out.invalidate();
							vscroll.invalidate();
							out.validate();
							vscroll.validate();
							vscroll.setValue(vscroll.getMaximum());
							break;
						} catch (final Exception e) {
							try {
								Thread.sleep(5);
							} catch (final Exception ie) {
								Thread.currentThread().interrupt();
								return;
							}
							continue;
						}
					return;
				case '\r':
					int index = sb.lastIndexOf(BR);
					if (index < 0) {
						sb.setLength(0);
					} else {
						sb.setLength(index);
					}
					return;
				default:
					sb.append((char) b);
				}
			}
		};

		out.setContentType("text/html");

		final Console c = new Console(ci, outStream);
		final InputKeyListener l = c.generateKeyListener(in);

		final FocusListener fl = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				in.requestFocus();
			}

			@Override
			public void focusLost(FocusEvent e) {
				return;
			}
		};

		out.setEditable(false);
		in.setEditable(false);

		panel.addFocusListener(fl);
		out.addFocusListener(fl);

		// remove all MouseListeners to identify pressed TAB
		for (final MouseListener ml : out.getMouseListeners()) {
			out.removeMouseListener(ml);
		}

		in.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				in.getCaret().setVisible(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				final Object source = e.getSource();
				final Object target = e.getOppositeComponent();
				final Object origin = e.getComponent();
				if (origin == source && source == in && target == out) {
					final Set<String> completions = c.ci.handleTab(
							in.getText(), l.c1a[0] == '\t');
					l.c1a[0] = '\t';
					if (completions != null)
						if (completions.size() == 1) {
							final String s = completions.iterator().next();
							in.setText(s);
						} else {
							c.out("Possible completions:\n");
							for (final String s : completions) {
								c.out(s);
								c.out("\n");
							}
							c.out("\n");
						}
				}
				return;

			}
		});

		in.setBackground(Color.BLACK);
		in.setForeground(Color.LIGHT_GRAY);
		in.setSelectionColor(Color.YELLOW);
		in.setCaretColor(Color.RED);
		in.getCaret().setVisible(true);

		panel.add(jscroll);
		panel.add(in, BorderLayout.SOUTH);

		in.addKeyListener(l);

		in.setPreferredSize(new Dimension(600, 40));
		in.setSize(600, 15);
		jscroll.setPreferredSize(new Dimension(600, 400));

		panel.setPreferredSize(new Dimension(620, 450));

		in.requestFocus();
		return c;
	}

	private InputKeyListener generateKeyListener(final JTextField textField) {
		return new InputKeyListener(textField);
	}

	/**
	 * Creates a new Console object for I/O-controlling.
	 * 
	 * @return created Console
	 */
	static Console createConsole() {
		final Object endSignal = new Object();
		final CommandInterpreter ci = new CommandInterpreter(endSignal);
		parseStream(System.in, ci, endSignal);
		return new Console(ci, System.out);
	}

	/**
	 * Registers commands to this Console
	 * 
	 * @param root
	 *            base directory for relative paths
	 * @param commands
	 *            supported Commands
	 */
	public void run(final Path root, final Set<Command> commands) {
		ci.registerCommands(commands);
	}

	Map<String, Command> getCommandMap() {
		return ci.getCommandMap();
	}

	void exit() {
		ci.exit();
	}

	void err(final String string) {
		if (out == System.out) {
			try {
				System.err.write(string.getBytes());
			} catch (final IOException e) {
				exit();
			}
		} else {
			synchronized (out) {
				out("<font color=red>");
				out(string);
				out("</font>");
			}
		}
	}
}
