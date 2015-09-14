package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import stone.io.GUIPlugin;
import stone.io.IOHandler;
import stone.modules.midiData.MidiMap;
import stone.modules.midiData.MidiParser;
import stone.util.TaskPool;


/**
 * A plugin for drag-and-drop
 * 
 * @author Nelphindal
 * @param <C>
 *            Implementing class for object
 * @param <D>
 *            Implementing class for target
 * @param <T>
 *            Implementing class for container
 */
public abstract class DragAndDropPlugin<C extends Container, D extends Container, T extends Container>
		extends GUIPlugin {

	class State {

		final DropTarget<C, D, T> emptyTarget;

		DragObject<C, D, T> object;
		DropTarget<C, D, T> target;
		DropTargetContainer<C, D, T> targetC;

		DragObject<C, D, T> dragging;

		boolean running; // midi2abc.exe running
		boolean split; // split button pushed down
		boolean upToDate; // no changes since last run
		boolean loadingMap; // while loading no relinking allowed

		final IOHandler io;
		final JLabel label = new JLabel();
		final DragAndDropPlugin<C, D, T> plugin = DragAndDropPlugin.this;


		@SuppressWarnings("hiding")
		State(final IOHandler io,
				final List<DropTargetContainer<C, D, T>> targets) {
			this.io = io;
			this.emptyTarget = targets.get(targets.size() - 1)
					.createNewTarget();
		}

	}

	/**
	 * The parser providing the file
	 */
	protected final MidiParser parser;

	/**
	 * The targets for <i>this</i> plugin
	 */
	protected final List<DropTargetContainer<C, D, T>> targets;

	/**
	 * Task pool allocated by initializing instance
	 */
	protected final TaskPool taskPool;

	/**
	 * Container used for the listeners
	 */
	protected final State state;

	/**
	 * The caller initializing <i>this</i> DragAndDropPlugin
	 */
	protected final DndPluginCaller<C, D, T> caller;

	/**
	 * Panel displayed in center
	 */
	protected final JPanel panelCenter = new JPanel();
	/**
	 * Panel displayed to left
	 */
	protected final JPanel panelLeft = new JPanel();

	/**
	 * @param caller
	 *            -
	 * @param taskPool
	 *            -
	 * @param parser
	 *            -
	 * @param targets
	 *            -
	 * @param io
	 *            -
	 */
	@SuppressWarnings("hiding")
	protected DragAndDropPlugin(final DndPluginCaller<C, D, T> caller,
			final TaskPool taskPool, final MidiParser parser,
			final List<DropTargetContainer<C, D, T>> targets, final IOHandler io) {
		super("");
		this.parser = parser;
		this.targets = targets;
		this.taskPool = taskPool;
		this.caller = caller;
		this.state = new State(io, targets);
	}

	/**
	 * @return the size.
	 */
	public abstract int size();

	/**
	 * Adds given target to the component created by {@link #initCenter(Map)}
	 * 
	 * @param target
	 *            -
	 */
	protected abstract void addToCenter(final DropTarget<C, D, T> target);


	/**
	 * @return the component to be shown on the bottom.
	 */
	protected abstract Component createButtonPanel();

	/**
	 * Displays the tracks, instruments and their association
	 */
	@Override
	protected final boolean display(final JPanel panel) {
		final JFrame frame = new JFrame();
		final Runnable r = new Runnable() {

			@Override
			public void run() {

				final MidiMap map = DragAndDropPlugin.this.parser.parse();
				final JPanel mainPanel = new JPanel() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void paint(final Graphics g) {
						map.paint(g);
					}
				};
				final JScrollPane pane = new JScrollPane(mainPanel);
				pane.setPreferredSize(new Dimension(600, 400));
				pane.setBackground(Color.black);

				map.init(mainPanel);

				frame.setIconImage(DragAndDropPlugin.this.state.io.getIcon());
				frame.addWindowListener(new WindowListener() {

					@Override
					public void windowActivated(final WindowEvent e) {
						// nothing to do
					}

					@Override
					public final void windowClosed(final WindowEvent e) {
						// nothing to do
					}

					@Override
					public final void windowClosing(final WindowEvent e) {
						frame.dispose();
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

				frame.add(pane);
				frame.pack();
				frame.setTitle(DragAndDropPlugin.this.parser.getMidi());
				frame.setVisible(true);
			}
		};

		this.taskPool.addTask(r);

		final JPanel mainPanel;
		final Map<Integer, DragObject<C, D, T>> initListLeft = initInitListLeft();
		this.state.label.setText("Drag the tracks from left to right");
		this.state.running = false;
		this.state.upToDate = false;
		this.state.target = null;
		this.state.targetC = null;
		this.state.dragging = null;
		this.state.object = null;

		assert (this.panelCenter.getComponentCount() == 0)
				&& (this.panelLeft.getComponentCount() == 0);
		initListLeft.clear();
		this.taskPool.waitForTasks();

		mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 3));
		mainPanel.add(initLeft(initListLeft));
		mainPanel.add(initCenter(initListLeft));
		mainPanel.add(initRight());

		panel.setLayout(new BorderLayout());
		panel.add(this.state.label, BorderLayout.NORTH);
		panel.add(mainPanel);
		panel.add(createButtonPanel(), BorderLayout.SOUTH);
		return false;
	}

	/**
	 * Notifies <i>this</i> plugin that last target has been removed and the
	 * center should be now empty
	 */
	protected abstract void emptyCenter();

	/** */
	@Override
	protected final String getTitle() {
		return "BruTE";
	}

	/**
	 * @param initListLeft
	 *            -
	 * @return component to be shown in the center
	 */
	protected abstract Component initCenter(
			final Map<Integer, DragObject<C, D, T>> initListLeft);

	/**
	 * @return the map needed for {@link #initLeft(Map)} and
	 *         {@link #initCenter(Map)}
	 */
	protected abstract Map<Integer, DragObject<C, D, T>> initInitListLeft();

	/**
	 * @param initListLeft
	 *            -
	 * @return component to be shown to the left
	 */
	protected abstract Component initLeft(
			Map<Integer, DragObject<C, D, T>> initListLeft);

	/**
	 * Handles everything needed to show given object
	 * 
	 * @param object
	 *            -
	 */
	protected abstract void initObject(final DragObject<C, D, T> object);

	/**
	 * @return component to be shown to the right
	 */
	protected abstract Component initRight();

	/**
	 * Handles everything needed to show given target
	 * 
	 * @param target
	 *            -
	 */
	protected abstract void initTarget(final DropTarget<C, D, T> target);

	/** */
	@Override
	protected void repack() {
		super.repack();
	}
}
