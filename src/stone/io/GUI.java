package stone.io;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import stone.MasterThread;
import stone.ModuleInfo;
import stone.modules.Main;
import stone.util.BooleanOption;
import stone.util.MaskedStringOption;
import stone.util.Option;
import stone.util.Path;
import stone.util.PathOption;
import stone.util.StringOption;

/**
 * Simple GUI handling all interaction with the user
 * 
 * @author Nelphindal
 */
public class GUI implements GUIInterface {

	final class ButtonListener implements MouseListener {

		private final GUI.Button button;

		public ButtonListener(final GUI.Button button) {
			this.button = button;
		}

		@Override
		public final void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mouseEntered(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mouseExited(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mouseReleased(final MouseEvent e) {
			e.consume();
			synchronized (GUI.Button.class) {
				if (GUI.this.pressed == null) {
					GUI.this.pressed = this.button;
				}
				GUI.Button.class.notifyAll();
			}
		}

	}

	private static final String waitText = "Please wait ...";

	/**
	 * Enables a component by calling {@link Component#setEnabled(boolean)}
	 * 
	 * @param c
	 * @param b
	 */
	public final static void setEnabled(final Component c, boolean b) {
		if (c instanceof Container) {
			for (final Component o : ((Container) c).getComponents()) {
				GUI.setEnabled(o, b);
			}
		} else {
			c.setEnabled(b);
		}
	}

	private final JFrame mainFrame;

	private final JTextArea text;
	private final JLabel wait;

	private final JProgressBar bar;

	Button pressed;

	private final MasterThread master;

	private boolean destroyed;

	/**
	 * Creates a GUI from a temporarily GUI
	 * 
	 * @param gui
	 * @param master
	 * @param icon
	 */
	public GUI(final GUI gui, final MasterThread master) {
		this.master = master;

		this.text = new JTextArea();
		this.text.setBackground(Color.YELLOW);
		this.text.setEditable(false);
		this.text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		this.wait = gui.wait;

		this.mainFrame = gui.mainFrame;

		this.mainFrame.addWindowListener(new WindowListener() {

			@Override
			public final void windowActivated(final WindowEvent e) {
				// nothing to do
			}

			@Override
			public final void windowClosed(final WindowEvent e) {
				// nothing to do
			}

			@Override
			public final void windowClosing(final WindowEvent e) {
				try {
					synchronized (Button.class) {
						master.interruptAndWait();
						Button.class.notifyAll();
					}
				} catch (final InterruptedException e1) {
					e1.printStackTrace();
				}

			}

			@Override
			public final void windowDeactivated(final WindowEvent e) {
				// nothing to do
			}

			@Override
			public final void windowDeiconified(final WindowEvent e) {
				// nothing to do
			}

			@Override
			public final void windowIconified(final WindowEvent e) {
				// nothing to do
			}

			@Override
			public final void windowOpened(final WindowEvent e) {
				// nothing to do
			}

		});
		this.mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		this.bar = new JProgressBar();
		for (final Button b : Button.values()) {
			b.getButton().addMouseListener(new ButtonListener(b));
		}
		// hide the exceptions for the Progress
		Thread.setDefaultUncaughtExceptionHandler(master
				.getUncaughtExceptionHandler());
		if (!this.mainFrame.isVisible()) {
			destroy();
		}
	}

	/**
	 * Creates a temporarily GUI
	 * 
	 * @param name
	 *            Title for the window
	 * @throws InterruptedException
	 */
	public GUI(final String name) throws InterruptedException {
		this.master = null;
		this.bar = null;
		this.text = null;

		final java.io.InputStream in = getClass().getResourceAsStream(
				"Icon.png");

		this.wait = new JLabel(GUI.waitText);

		this.mainFrame = new JFrame();
		this.mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// mainFrame.setResizable(false);
		this.mainFrame.setTitle(name);
		this.mainFrame.setLayout(new BorderLayout());
		this.mainFrame.setMinimumSize(new Dimension(360, 100));
		this.mainFrame.setMaximumSize(new Dimension(600, 680));
		this.mainFrame.add(this.wait);

		if (in != null) {
			try {
				this.mainFrame.setIconImage(ImageIO.read(in));
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

		this.mainFrame.pack();
		this.mainFrame.setVisible(true);
	}

	/**
	 * Prints a dialog, asking given question and shows two buttons to admit or
	 * decline.
	 * 
	 * @param string
	 *            the question to show
	 * @param progress
	 * @return <i>true</i> if and only if the user hit yes
	 */
	public final Button askNoYes(final String string, boolean progress) {
		final JPanel panel = new JPanel();
		final JPanel buttonBar = new JPanel();

		this.text.setText(string);

		panel.setLayout(new BorderLayout());
		if (progress) {
			this.bar.setStringPainted(false);
			panel.add(this.bar, BorderLayout.NORTH);
		}
		panel.add(this.text);
		panel.add(buttonBar, BorderLayout.SOUTH);

		buttonBar.setLayout(new BorderLayout());
		buttonBar.add(Button.YES.getButton(), BorderLayout.EAST);
		buttonBar.add(Button.NO.getButton(), BorderLayout.WEST);

		this.mainFrame.getContentPane().removeAll();
		this.mainFrame.add(panel);

		waitForButton();

		return this.pressed;
	}

	/** */
	@Override
	public final void destroy() {
		synchronized (this) {
			if (this.destroyed) {
				return;
			}
			this.destroyed = true;
		}
		try {
			this.master.interruptAndWait();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		this.mainFrame.setVisible(false);
		this.mainFrame.dispose();
	}

	/** */
	@Override
	public final void endProgress() {
		this.bar.setVisible(false);
		if (Thread.currentThread() == this.master) {
			this.mainFrame.remove(this.bar);
			this.mainFrame.remove(this.wait); // may by added to north by
												// another Thread
			// by calling this method
			this.wait.setText(GUI.waitText);
			this.mainFrame.add(this.wait);
			revalidate(true, false);
		} else {
			this.wait.setText("");
			this.mainFrame.getContentPane().remove(
					this.mainFrame.getContentPane().getComponentCount() - 1);
			this.mainFrame.add(this.wait, BorderLayout.NORTH);
			revalidate(false, false);
		}
	}

	/** */
	@Override
	public final void getOptions(final Collection<Option> options) {
		final ArrayList<BooleanOption> bo = new ArrayList<>();
		final ArrayList<StringOption> so = new ArrayList<>();
		final ArrayList<PathOption> po = new ArrayList<>();
		final ArrayList<MaskedStringOption> mo = new ArrayList<>();
		final KeyEventHandler keh = new KeyEventHandler() {

			@Override
			public void handleKeyEvent(int event) {
				switch (event) {
				case KeyEvent.VK_ENTER:
					synchronized (Button.class) {
						GUI.this.pressed = Button.OK;
						Button.class.notifyAll();
					}
				}
			}

		};

		for (final Option o : options) {
			if (o.isBoolean()) {
				bo.add((BooleanOption) o);
			} else if (o.isPath()) {
				po.add((PathOption) o);
			} else if (o.isMaskable()) {
				mo.add((MaskedStringOption) o);
			} else {
				so.add((StringOption) o);
			}
		}
		final JPanel mainPanel = new JPanel();
		final JPanel panelBoolean = new JPanel();
		final JPanel panelText = new JPanel();
		final JPanel panelPath = new JPanel();
		final JPanel panelString = new JPanel();
		final JPanel panelMasked = new JPanel();

		panelBoolean.setLayout(new GridLayout(0, 3));
		panelPath.setLayout(new GridLayout(0, 3));
		panelText.setLayout(new BorderLayout());
		panelString.setLayout(new GridLayout(0, 2));
		panelMasked.setLayout(new GridLayout(0, 1));

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(panelBoolean, BorderLayout.NORTH);
		mainPanel.add(panelText);
		mainPanel.add(panelPath, BorderLayout.SOUTH);

		panelText.add(panelString);
		panelText.add(panelMasked, BorderLayout.SOUTH);

		for (final BooleanOption o : bo) {
			o.displayWithGUI(panelBoolean, this, keh);
		}
		for (final StringOption o : so) {
			o.displayWithGUI(panelString, this, keh);
		}
		for (final MaskedStringOption o : mo) {
			o.displayWithGUI(panelMasked, this, keh);
		}
		for (final PathOption o : po) {
			o.displayWithGUI(panelPath, this, keh);
		}

		this.wait.setText("Settings");
		this.mainFrame.getContentPane().removeAll();
		this.mainFrame.add(this.wait, BorderLayout.NORTH);
		this.mainFrame.add(mainPanel);
		this.mainFrame.add(Button.OK.button, BorderLayout.SOUTH);

		waitForButton();

		for (final Option o : options) {
			o.endDisplay();
		}

	}

	/**
	 * Shows a dialog to chose an absolute path
	 * 
	 * @param titleMsg
	 * @param filter
	 * @param initialDirectory
	 * @return the selected path or <i>null</i> if user aborted the dialog
	 */
	public final Path getPath(final String titleMsg, final FileFilter filter,
			final File initialDirectory) {
		final JFileChooser chooser;
		final JLabel title = new JLabel(titleMsg);
		this.mainFrame.isAlwaysOnTop();
		chooser = new JFileChooser(initialDirectory);
		chooser.setDialogTitle(title.getText());
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.removeChoosableFileFilter(chooser.getChoosableFileFilters()[0]);
		chooser.setFileFilter(filter);
		chooser.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				synchronized (Button.class) {
					if (GUI.this.pressed != null) {
						return;
					}
					if (e.getActionCommand().equals("ApproveSelection")) {
						GUI.this.pressed = Button.YES;
					} else {
						GUI.this.pressed = Button.NO;
					}
					Button.class.notifyAll();
				}
			}
		});

		final boolean aot = this.mainFrame.isAlwaysOnTop();
		this.mainFrame.setAlwaysOnTop(false);
		this.mainFrame.getContentPane().removeAll();
		this.mainFrame.add(chooser);
		this.mainFrame.add(title, BorderLayout.NORTH);

		waitForButton();

		this.mainFrame.setAlwaysOnTop(aot);

		if (this.pressed != Button.YES) {
			return null;
		}
		final File file = chooser.getSelectedFile();
		if (file == null) {
			return null;
		}
		return Path.getPath(file.toString());
	}

	/**
	 * @return the last pressed button
	 */
	@Override
	public final Button getPressedButton() {
		return this.pressed;
	}

	/** */
	@Override
	public final Component getProgressBar() {
		return this.bar;
	}

	/** */
	@Override
	public final void initProgress() {
		this.bar.setStringPainted(false);
		this.bar.setIndeterminate(true);
		this.bar.setVisible(true);
		if (Thread.currentThread() != this.master) {
			this.mainFrame.remove(this.wait);
			final JPanel panel = new JPanel();
			panel.add(this.wait, BorderLayout.NORTH);
			panel.add(this.bar);
			this.mainFrame.add(panel, BorderLayout.NORTH);
			revalidate(false, false);
		} else {
			this.mainFrame.getContentPane().removeAll();
			this.mainFrame.add(this.wait, BorderLayout.NORTH);
			this.mainFrame.add(this.bar);
			revalidate(true, false);
		}

	}

	/** */
	@Override
	public final void printErrorMessage(final String errorMessage) {
		final Color oldBG = this.text.getBackground();
		final Color oldFG = this.text.getForeground();
		this.text.setBackground(Color.DARK_GRAY);
		this.text.setForeground(Color.WHITE);
		printMessageFunc(null, errorMessage, true);
		this.text.setBackground(oldBG);
		this.text.setForeground(oldFG);
	}

	/** */
	@Override
	public final void printMessage(final String title, final String message,
			boolean toFront) {
		printMessageFunc(title, message, toFront);
	}

	/** */
	@Override
	public final void runPlugin(final GUIPlugin plugin) {
		final JPanel panel = new JPanel();
		this.mainFrame.getContentPane().removeAll();
		this.mainFrame.add(panel);
		if (plugin.display(panel, this)) {
			plugin.endDisplay();
			this.mainFrame.getContentPane().removeAll();
			this.mainFrame.add(this.wait);
			this.wait.setText(waitText);
			revalidate(true, false);
			return;
		}
		this.mainFrame.add(this.wait, BorderLayout.NORTH);
		this.wait.setText(plugin.getTitle());
		waitForButton();
	}

	/** */
	@Override
	public final List<String> selectModules(final List<String> modules) {
		this.mainFrame.getContentPane().removeAll();
		this.text
				.setText("Please select the actions you want to use. Selected actions\n"
						+ "may update themselves if outdated. In this case reselect them\n"
						+ "after the tool restarted.");
		this.text.setBackground(this.mainFrame.getBackground());
		this.text.setEditable(false);

		final List<String> selection = new ArrayList<>();
		final JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(0, 2));

		class BoxListener implements ChangeListener {

			private final String m;

			private BoxListener(final String m) {
				this.m = m;
			}

			@Override
			public void stateChanged(final ChangeEvent e) {
				if (!selection.remove(this.m)) {
					selection.add(this.m);
				}
			}
		}

		this.mainFrame.add(this.text, BorderLayout.NORTH);
		this.mainFrame.add(Button.OK.getButton(), BorderLayout.SOUTH);
		this.mainFrame.add(panel);
		for (final String m : modules) {
			final JCheckBox box;
			final ModuleInfo info = this.master.getModuleInfo(m);

			box = new JCheckBox(info.name());
			box.setToolTipText(info.tooltip());
			box.addChangeListener(new BoxListener(m));
			panel.add(box);
		}
		final JCheckBox box = new JCheckBox(Main.REPAIR);
		box.setForeground(Color.RED);
		box.addChangeListener(new BoxListener(Main.REPAIR));
		panel.add(box);
		waitForButton();

		this.text.setBackground(Color.YELLOW);

		return selection;
	}

	/** */
	@Override
	public final void setProgress(int pos) {
		this.bar.setValue(pos);
	}

	/** */
	@Override
	public final void setProgressSize(int size) {
		if (size <= 0) {
			this.bar.setStringPainted(false);
			this.bar.setIndeterminate(true);
			this.bar.setMaximum(1);
		} else {
			this.bar.setStringPainted(true);
			this.bar.setIndeterminate(false);
			this.bar.setMaximum(size);
		}
		this.bar.setValue(0);
	}

	/** */
	@Override
	public final void setProgressSize(int size, final String action) {
		this.wait.setText(action);
		this.bar.setValue(0);
		if (size <= 0) {
			this.bar.setStringPainted(false);
			this.bar.setIndeterminate(true);
			this.bar.setMaximum(1);
		} else {
			this.bar.setStringPainted(true);
			this.bar.setIndeterminate(false);
			this.bar.setMaximum(size);
		}
	}

	/** */
	@Override
	public final void setProgressTitle(final String action) {
		this.wait.setText(action);
	}

	private final void printMessageFunc(final String title,
			final String message, boolean toFront) {
		if (Thread.currentThread() == this.master) {
			synchronized (Button.class) {
				this.pressed = Button.ABORT;
				Button.class.notifyAll();
			}
		}
		this.mainFrame.getContentPane().removeAll();
		this.text.setEditable(false);
		this.text.setText(message);
		final int cols = this.text.getColumns() + 1;
		final JPanel panel = new JPanel();
		final JScrollPane scrollPane = new JScrollPane(panel);
		panel.setLayout(new BorderLayout());
		panel.add(this.text);
		scrollPane.setPreferredSize(new Dimension(600, cols < 20 ? cols * 8
				: 800));
		this.mainFrame.add(scrollPane);
		if (toFront) {
			if (title != null) {
				this.wait.setText(title);
			} else {
				this.wait.setText("");
			}
			panel.add(this.wait, BorderLayout.NORTH);
			panel.add(Button.OK.getButton(), BorderLayout.SOUTH);
			this.mainFrame.pack();
			waitForButton();
		} else {
			this.mainFrame.pack();
			synchronized (this) {
				revalidate(true, false);
			}
		}
	}

	private final void waitForButton() {
		try {
			synchronized (Button.class) {
				if (this.master.isInterrupted()) {
					return;
				}
				revalidate(true, true);
				this.pressed = null;
				if (Thread.currentThread() != this.master) {
					this.master.interrupt();
				}
				Button.class.wait();
				if (this.master.isInterrupted()) {
					destroy();
				}
			}
		} catch (final InterruptedException e) {
			this.master.interrupt();
		}
		if (this.pressed == Button.ABORT) {
			destroy();
		}
		this.mainFrame.getContentPane().removeAll();
		this.mainFrame.add(this.wait);
		this.wait.setText(GUI.waitText);
		revalidate(true, false);
	}

	final Dimension getFrameSize() {
		return this.mainFrame.getSize();
	}

	final Image getIcon() {
		return this.mainFrame.getIconImage();
	}

	final void revalidate(boolean pack, boolean toFront) {
		if (this.master.isInterrupted()) {
			return;
		}
		if (pack) {
			this.mainFrame.pack();
		}
		this.mainFrame.revalidate();
		this.mainFrame.repaint();
		if (toFront) {
			this.mainFrame.toFront();
		}
	}

	final void setFrameSize(final Dimension d) {
		this.mainFrame.setSize(d);
	}

	final void setResizable(boolean b) {
		this.mainFrame.setResizable(b);
	}

}
