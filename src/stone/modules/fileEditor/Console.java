package stone.modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import stone.io.GUIInterface;
import stone.util.ByteBuffer;
import stone.util.Path;

public class Console {
	
	private final OutputStream out;
	private final InputStream in;
	private final Object endSignal;
	
	private Console(final InputStream inStream, final OutputStream outStream, final Object endSignal) {
		out = outStream;
		in = inStream;
		this.endSignal = endSignal;
	}

	static Console createConsoleGUI(final JPanel panel) {

		final JTextArea out = new JTextArea();
		final JTextField in = new JTextField();
		final Object endSignal = GUIInterface.Button.class;
		final OutputStream outStream = new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				out.setText(out.getText() + (char) b);
			}};
		
		final ByteBuffer bb = new ByteBuffer();
		final InputStream inStream = new InputStream() {
			

			@Override
			public int read() throws IOException {
				return bb.read();
			}};
		
		out.setEditable(false);
		in.setEditable(false);

		out.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				in.requestFocus();
			}

			@Override
			public void focusLost(FocusEvent e) {
				return;
			}
		});

		in.setBackground(Color.BLACK);
		in.setForeground(Color.LIGHT_GRAY);

		panel.add(new JScrollPane(out));
		panel.add(in, BorderLayout.SOUTH);

		out.setPreferredSize(new Dimension(600, 400));
		in.setPreferredSize(new Dimension(600, 40));
		in.setSize(600, 40);

		
		in.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void keyTyped(final KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(final KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});

		panel.setPreferredSize(new Dimension(600, 450));
		return new Console(inStream, outStream, endSignal);
	}

	public static Console createConsole(final Object endSignal) {
		return new Console(System.in, System.out, endSignal);
	}

	public void run(final Path root) {
		// TODO Auto-generated method stub
		
	}
}
