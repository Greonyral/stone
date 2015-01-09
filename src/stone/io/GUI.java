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
				if (pressed == null) {
					pressed = button;
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

		text = new JTextArea();
		text.setBackground(Color.YELLOW);
		text.setEditable(false);
		text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		wait = gui.wait;

		mainFrame = gui.mainFrame;

		mainFrame.addWindowListener(new WindowListener() {

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
		mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		bar = new JProgressBar();
		for (final Button b : Button.values()) {
			b.getButton().addMouseListener(new ButtonListener(b));
		}
		// hide the exceptions for the Progress
		Thread.setDefaultUncaughtExceptionHandler(master
				.getUncaughtExceptionHandler());
		if (!mainFrame.isVisible()) {
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
		master = null;
		bar = null;
		text = null;

		final java.io.InputStream in = getClass().getResourceAsStream(
				"Icon.png");

		wait = new JLabel(GUI.waitText);

		mainFrame = new JFrame();
		mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// mainFrame.setResizable(false);
		mainFrame.setTitle(name);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setMinimumSize(new Dimension(360, 100));
		mainFrame.setMaximumSize(new Dimension(600, 680));
		mainFrame.add(wait);

		if (in != null)
			try {
				mainFrame.setIconImage(ImageIO.read(in));
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

		mainFrame.pack();
		mainFrame.setVisible(true);
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

		text.setText(string);

		panel.setLayout(new BorderLayout());
		if (progress) {
			bar.setStringPainted(false);
			panel.add(bar, BorderLayout.NORTH);
		}
		panel.add(text);
		panel.add(buttonBar, BorderLayout.SOUTH);

		buttonBar.setLayout(new BorderLayout());
		buttonBar.add(Button.YES.getButton(), BorderLayout.EAST);
		buttonBar.add(Button.NO.getButton(), BorderLayout.WEST);

		mainFrame.getContentPane().removeAll();
		mainFrame.add(panel);

		waitForButton();

		return pressed;
	}

	/** */
	@Override
	public final void destroy() {
		synchronized (this) {
			if (destroyed) {
				return;
			}
			destroyed = true;
		}
		master.interrupt();
		mainFrame.setVisible(false);
		mainFrame.dispose();
	}

	/** */
	@Override
	public final void endProgress() {
		bar.setVisible(false);
		if (Thread.currentThread() == master) {
			mainFrame.remove(bar);
			mainFrame.remove(wait); // may by added to north by another Thread
									// by calling this method
			wait.setText(GUI.waitText);
			mainFrame.add(wait);
			revalidate(true, false);
		} else {
			wait.setText("");
			mainFrame.getContentPane().remove(
					mainFrame.getContentPane().getComponentCount() - 1);
			mainFrame.add(wait, BorderLayout.NORTH);
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
			o.displayWithGUI(panelBoolean, this);
		}
		for (final StringOption o : so) {
			o.displayWithGUI(panelString, this);
		}
		for (final MaskedStringOption o : mo) {
			o.displayWithGUI(panelMasked, this);
		}
		for (final PathOption o : po) {
			o.displayWithGUI(panelPath, this);
		}

		wait.setText("Settings");
		mainFrame.getContentPane().removeAll();
		mainFrame.add(wait, BorderLayout.NORTH);
		mainFrame.add(mainPanel);
		mainFrame.add(Button.OK.button, BorderLayout.SOUTH);

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
		mainFrame.isAlwaysOnTop();
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
					if (pressed != null) {
						return;
					}
					if (e.getActionCommand().equals("ApproveSelection")) {
						pressed = Button.YES;
					} else {
						pressed = Button.NO;
					}
					Button.class.notifyAll();
				}
			}
		});

		final boolean aot = mainFrame.isAlwaysOnTop();
		mainFrame.setAlwaysOnTop(false);
		mainFrame.getContentPane().removeAll();
		mainFrame.add(chooser);
		mainFrame.add(title, BorderLayout.NORTH);

		waitForButton();

		mainFrame.setAlwaysOnTop(aot);

		if (pressed != Button.YES) {
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
		return pressed;
	}

	/** */
	@Override
	public final Component getProgressBar() {
		return bar;
	}

	/** */
	@Override
	public final void initProgress() {
		bar.setStringPainted(false);
		bar.setIndeterminate(true);
		bar.setVisible(true);
		if (Thread.currentThread() != master) {
			mainFrame.remove(wait);
			final JPanel panel = new JPanel();
			panel.add(wait, BorderLayout.NORTH);
			panel.add(bar);
			mainFrame.add(panel, BorderLayout.NORTH);
			revalidate(false, false);
		} else {
			mainFrame.getContentPane().removeAll();
			mainFrame.add(wait, BorderLayout.NORTH);
			mainFrame.add(bar);
			revalidate(true, false);
		}

	}

	/** */
	@Override
	public final void printErrorMessage(final String errorMessage) {
		final Color oldBG = text.getBackground();
		final Color oldFG = text.getForeground();
		text.setBackground(Color.DARK_GRAY);
		text.setForeground(Color.WHITE);
		printMessageFunc(null, errorMessage, true);
		text.setBackground(oldBG);
		text.setForeground(oldFG);
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
		mainFrame.getContentPane().removeAll();
		mainFrame.add(panel);
		if (plugin.display(panel, this)) {
			plugin.endDisplay();
			mainFrame.getContentPane().removeAll();
			mainFrame.add(wait);
			wait.setText(waitText);
			revalidate(true, false);
			return;
		}
		mainFrame.add(wait, BorderLayout.NORTH);
		wait.setText(plugin.getTitle());
		waitForButton();
	}

	/** */
	@Override
	public final List<String> selectModules(final List<String> modules) {
		mainFrame.getContentPane().removeAll();
		text.setText("Please select the actions you want to use. Selected actions\n"
				+ "may update themselves if outdated. In this case reselect them\n"
				+ "after the tool restarted.");
		text.setBackground(mainFrame.getBackground());
		text.setEditable(false);

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
				if (!selection.remove(m)) {
					selection.add(m);
				}
			}
		}

		mainFrame.add(text, BorderLayout.NORTH);
		mainFrame.add(Button.OK.getButton(), BorderLayout.SOUTH);
		mainFrame.add(panel);
		for (final String m : modules) {
			final JCheckBox box;
			final ModuleInfo info = master.getModuleInfo(m);
		
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

		text.setBackground(Color.YELLOW);

		return selection;
	}

	/** */
	@Override
	public final void setProgress(int pos) {
		bar.setValue(pos);
	}

	/** */
	@Override
	public final void setProgressSize(int size) {
		if (size <= 0) {
			bar.setStringPainted(false);
			bar.setIndeterminate(true);
			bar.setMaximum(1);
		} else {
			bar.setStringPainted(true);
			bar.setIndeterminate(false);
			bar.setMaximum(size);
		}
		bar.setValue(0);
	}

	/** */
	@Override
	public final void setProgressSize(int size, final String action) {
		wait.setText(action);
		bar.setValue(0);
		if (size <= 0) {
			bar.setStringPainted(false);
			bar.setIndeterminate(true);
			bar.setMaximum(1);
		} else {
			bar.setStringPainted(true);
			bar.setIndeterminate(false);
			bar.setMaximum(size);
		}

	}

	/** */
	@Override
	public final void setProgressTitle(final String action) {
		wait.setText(action);
	}

	private final void printMessageFunc(final String title,
			final String message, boolean toFront) {
		if (Thread.currentThread() == master) {
			synchronized (Button.class) {
				pressed = Button.ABORT;
				Button.class.notifyAll();
			}
		}
		mainFrame.getContentPane().removeAll();
		text.setEditable(false);
		text.setText(message);
		final int cols = text.getColumns() + 1;
		final JPanel panel = new JPanel();
		final JScrollPane scrollPane = new JScrollPane(panel);
		panel.setLayout(new BorderLayout());
		panel.add(text);
		scrollPane.setPreferredSize(new Dimension(600, cols < 20 ? cols * 8
				: 800));
		mainFrame.add(scrollPane);
		if (toFront) {
			if (title != null) {
				wait.setText(title);
			} else {
				wait.setText("");
			}
			panel.add(wait, BorderLayout.NORTH);
			panel.add(Button.OK.getButton(), BorderLayout.SOUTH);
			mainFrame.pack();
			waitForButton();
		} else {
			mainFrame.pack();
			synchronized (this) {
				revalidate(true, false);
			}
		}
	}

	private final void waitForButton() {
		try {
			synchronized (Button.class) {
				if (master.isInterrupted()) {
					return;
				}
				revalidate(true, true);
				pressed = null;
				if (Thread.currentThread() != master) {
					master.interrupt();
				}
				Button.class.wait();
				if (master.isInterrupted()) {
					destroy();
				}
			}
			revalidate(true, false);
		} catch (final InterruptedException e) {
			master.interrupt();
		}
		if (pressed == Button.ABORT) {
			destroy();
		}
		mainFrame.getContentPane().removeAll();
		mainFrame.add(wait);
		wait.setText(GUI.waitText);
		revalidate(true, false);
	}

	final Dimension getFrameSize() {
		return mainFrame.getSize();
	}

	final void revalidate(boolean pack, boolean toFront) {
		if (master.isInterrupted()) {
			return;
		}
		if (pack) {
			mainFrame.pack();
		}
		mainFrame.revalidate();
		mainFrame.repaint();
		if (toFront) {
			mainFrame.toFront();
		}
	}

	final void setFrameSize(final Dimension d) {
		mainFrame.setSize(d);
	}

	final void setResizable(boolean b) {
		mainFrame.setResizable(b);
	}

	final Image getIcon() {
		return mainFrame.getIconImage();
	}

}
