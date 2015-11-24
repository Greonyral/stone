package stone.modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
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
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;

import stone.io.GUIInterface;
import stone.util.Path;

public class Console {

	private final OutputStream out;
	private final CommandInterpreter ci;

	private Console(final CommandInterpreter ci, final OutputStream outStream) {
		out = outStream;
		this.ci = ci;
	}

	private class InputKeyListener implements KeyListener {

		private final CommandInterpreter ci;
		private final char[] c1a = new char[1];
		private final JTextField textField;

		InputKeyListener(final JTextField in,
				@SuppressWarnings("hiding") final CommandInterpreter ci) {
			this.textField = in;
			this.ci = ci;
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
				ci.handleEnter(textField.getText());
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
				final Set<Command> commands = ci.handleTab(in,
						c1a[0] == KeyEvent.VK_TAB);
				if (commands == null)
					break;
				if (c1a[0] != KeyEvent.VK_TAB && commands.size() == 1) {
					textField.setText(commands.iterator().next()
							.getCommandText());
				} else {
					out(in + "[TAB]");
					for (final Command c : commands) {
						out(c.getCommandText() + " " + c.getHelpText());
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

	private void out(String string) {
		// TODO Auto-generated method stub

	}

	static void parseStream(final InputStream in, final CommandInterpreter ci) {
		new Thread() {

			@Override
			public void run() {
				BufferedReader r = new BufferedReader(new InputStreamReader(in));
				while (true) {
					String line;
					try {
						line = r.readLine();
					} catch (final IOException e) {
						e.printStackTrace();
						return;
					}
					if (line == null)
						return;
					ci.handleEnter(line);
				}
			}
		}.start();
	}

	static Console createConsoleGUI(final JPanel panel) {
		final JTextArea out = new JTextArea();
		final JTextField in = new JTextField();
		final Object endSignal = GUIInterface.Button.class;
		final CommandInterpreter ci = new CommandInterpreter(endSignal);
		final OutputStream outStream = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				out.setText(out.getText() + (char) b);
			}
		};
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
					c.ci.handleTab(in.getSelectedText(), l.c1a[0] == '\t');
					l.c1a[0] = '\t';
				}
				return;
				
			}});
		
		
		in.setBackground(Color.BLACK);
		in.setForeground(Color.LIGHT_GRAY);
		in.setSelectionColor(Color.YELLOW);
		in.setCaretColor(Color.RED);
		in.getCaret().setVisible(true);

		panel.add(new JScrollPane(out));
		panel.add(in, BorderLayout.SOUTH);

		in.addKeyListener(l);

		out.setPreferredSize(new Dimension(600, 400));
		in.setPreferredSize(new Dimension(600, 40));
		in.setSize(600, 40);


		panel.setPreferredSize(new Dimension(600, 450));

		return c;
	}


	private InputKeyListener generateKeyListener(final JTextField textField) {
		return new InputKeyListener(textField, ci);
	}

	public static Console createConsole(final Object endSignal) {
		final CommandInterpreter ci = new CommandInterpreter(endSignal);
		parseStream(System.in, ci);
		return new Console(ci, System.out);
	}

	public void run(final Path root, final Set<Command> commands) {
		ci.registerCommands(commands);
	}
}
