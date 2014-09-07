package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.GUI;
import stone.io.IOHandler;
import stone.modules.AbcCreator;
import stone.modules.midiData.MidiInstrument;
import stone.modules.midiData.MidiInstrumentDropTarget;
import stone.modules.midiData.MidiParser;
import stone.util.TaskPool;


/**
 * @author Nelphindal
 */
public final class AbcMapPlugin extends
DragAndDropPlugin<JPanel, JPanel, JPanel> {

	final Map<MidiInstrumentDropTarget, Set<Integer>> instrumentToTrack =
			new TreeMap<>();
			final Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackMap =
					new HashMap<>();

					private JScrollPane center;
					private Container empty;

					private Color emptyC;

					/**
					 * @param abcCreator
					 * @param taskPool
					 * @param parser
					 * @param targets
					 * @param io
					 */
					public AbcMapPlugin(
							final AbcCreator abcCreator,
							final TaskPool taskPool,
							final MidiParser parser,
							final List<DropTargetContainer<JPanel, JPanel, JPanel>> targets,
							final IOHandler io) {
						super(abcCreator, taskPool, parser, targets, io);
					}

					/**
					 * Links object with target
					 * 
					 * @param object
					 * @param target
					 */
					public final void link(DragObject<JPanel, JPanel, JPanel> object,
							DropTarget<JPanel, JPanel, JPanel> target) {
						final Track track;
						final MidiInstrumentDropTarget i;
						if (target == state.emptyTarget) {
							track = null;
							i = null;
						} else {
							track = (Track) object;
							i = (MidiInstrumentDropTarget) target;
							final Set<Integer> set = instrumentToTrack.get(target);
							if (set == null) {
								final Set<Integer> setNew = new HashSet<>();
								instrumentToTrack.put(i, setNew);
								setNew.add(track.getId());
								initTarget(target);
							} else {
								set.add(track.getId());
							}
						}
						object.addTarget(target);
						target.link(object);
					}

					/**
					 * Prints an error
					 * 
					 * @param string
					 */
					public final void printError(final String string) {
						state.label.setText(string);
					}

					/**
					 * Cleans internal structures to display another abc-file.
					 */
					public final void reset() {
						taskPool.addTask(new Runnable() {
							@Override
							final public void run() {
								for (final DropTargetContainer<?, ?, ?> target : targets) {
									target.clearTargets();
								}
							}
						});
						trackMap.clear();
						instrumentToTrack.clear();
					}

					/**
					 * @return the count of instruments in created abc
					 */
					@Override
					public final int size() {
						return instrumentToTrack.size();
					}

					/**
					 * @return a tree containing all currently mapped instruments.
					 */
					public final TreeSet<DropTarget<JPanel, JPanel, JPanel>> targets() {
						return new TreeSet<DropTarget<JPanel, JPanel, JPanel>>(
								instrumentToTrack.keySet());
					}

					/**
					 * Unlinks object with target
					 * 
					 * @param object
					 * @param target
					 * @return <i>true</i> if the target is now empty
					 */
					public final boolean unlink(DragObject<?, ?, ?> object,
							DropTarget<?, ?, ?> target) {
						final Track t = (Track) object;
						final MidiInstrumentDropTarget i =
								(MidiInstrumentDropTarget) target;
						final Set<Integer> set = instrumentToTrack.get(target);
						set.remove(t.getId());
						if (set.isEmpty()) {
							instrumentToTrack.remove(i);
							return true;
						}
						return false;
					}

					/** */
					@Override
					protected final void addToCenter(
							final DropTarget<JPanel, JPanel, JPanel> target) {
						final Container c =
								(Container) ((Container) center.getComponent(0))
								.getComponent(0);
						if (empty != null) {
							empty = null;
							c.removeAll();
							c.setBackground(emptyC);
						}
						final Set<Integer> tracks = new HashSet<>();
						for (final DragObject<JPanel, JPanel, JPanel> o : target) {
							tracks.add(o.getId());
						}
						instrumentToTrack.put((MidiInstrumentDropTarget) target, tracks);
						c.removeAll();
						for (final MidiInstrumentDropTarget t : instrumentToTrack.keySet()) {
							c.add(t.getDisplayableComponent());
						}
						center.revalidate();
					}

					@Override
					protected final JPanel createButtonPanel() {
						final JPanel panel = new JPanel();
						final JToggleButton splitButton = new JToggleButton("Split");

						splitButton.addChangeListener(new ChangeListener() {

							@Override
							public final void stateChanged(final ChangeEvent e) {
								state.split ^= true;
							}
						});

						final JButton testButton = new JButton("Test");
						final JButton globalParamsButton = new JButton("Settings");
						final JButton loadButton = new JButton("Load");
						final JPanel globalMenu = new JPanel();
						final ReleaseMouseListenerParams params =
								new ReleaseMouseListenerParams() {


							@Override
							public JPanel globalMenu() {
								return globalMenu;
							}

							@Override
							public JButton globalParamsButton() {
								return globalParamsButton;
							}

							@Override
							public JButton loadButton() {
								return loadButton;
							}

							@Override
							public JPanel panel() {
								return panel;
							}

							@Override
							public JPanel panelCenter() {
								return panelCenter;
							}

							@Override
							public AbcMapPlugin plugin() {
								return AbcMapPlugin.this;
							}

							@Override
							public JToggleButton splitButton() {
								return splitButton;
							}

							@Override
							public JButton testButton() {
								return testButton;
							}

						};

						testButton.addMouseListener(new TestButtonMouseListener(params));

						loadButton.addMouseListener(new LoadButtonMouseListener(params));

						globalParamsButton.addMouseListener(new GlobalParamsMouseListener(
								params));


						globalParamsButton
						.setToolTipText("Sets global settings like pitch and location of created abc for testing");
						testButton
						.setToolTipText("Starts the transcription. After completion the song will be played using the Abc-Player, if it exists");
						splitButton
						.setToolTipText("Will split a midi track on multiple abc-tracks when enabled");
						loadButton
						.setToolTipText("Loads a previously saved map - IN  DEVELOPMENT will currently clear the map and fail afterwards");

						panelCenter.setLayout(new BorderLayout());
						panelCenter.add(GUI.Button.OK.getButton());
						panelCenter.add(splitButton, BorderLayout.EAST);
						panelCenter.add(globalParamsButton, BorderLayout.SOUTH);
						panelCenter.add(loadButton, BorderLayout.WEST);

						panel.setLayout(new BorderLayout());
						panel.add(panelCenter);
						panel.add(GUI.Button.ABORT.getButton(), BorderLayout.WEST);
						panel.add(testButton, BorderLayout.EAST);
						return panel;
					}

					/** */
					@Override
					protected final void emptyCenter() {
						if (empty != null)
							return;
						empty = center.getParent();
						final Container c =
								(Container) ((Container) center.getComponent(0))
								.getComponent(0);
						final JLabel label = new JLabel("       - empty -       ");
						label.setForeground(Color.WHITE);

						c.add(label);
						emptyC = c.getBackground();
						c.setBackground(Color.RED);
						empty.validate();
					}

					/**
					 * Initializes the panel displaying all future instrument of created
					 * abc-file.
					 */
					@Override
					protected final
					JScrollPane
					initCenter(
							final Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackList) {
						final JPanel panel = new JPanel();
						final TreeSet<DropTarget<JPanel, JPanel, JPanel>> set =
								new TreeSet<>();
								for (final DragObject<JPanel, JPanel, JPanel> o : trackList
										.values()) {
									for (final DropTarget<JPanel, JPanel, JPanel> t : o) {
										if (t != state.emptyTarget) {
											set.add(t);
										}
									}
								}
								panel.setLayout(new GridLayout(0, 1));
								for (final DropTarget<JPanel, JPanel, JPanel> t : set) {
									initTarget(t);
									panel.add(t.getDisplayableComponent());

								}
								center = new JScrollPane(panel);
								return center;
					}

					/** Creates a map, mapping the tracks. */
					@Override
					protected final Map<Integer, DragObject<JPanel, JPanel, JPanel>>
					initInitListLeft() {
						return trackMap;
					}

					/**
					 * Creates the tracks and initializes their listeners
					 */
					@Override
					protected final JScrollPane initLeft(
							Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackList) {
						final Set<Integer> midiIds = parser.tracks();
						final Map<Integer, String> titles = parser.titles();
						final Map<Integer, MidiInstrument> instruments =
								parser.instruments();
						final Map<Integer, Integer> idBrute = parser.renumberMap();
						final JScrollPane scrollPane;

						midiIds.remove(0);
						panelLeft.removeAll();
						panelLeft.setLayout(new GridLayout(0, 1));
						for (final Integer id : midiIds) {
							final Track track =
									new Track(idBrute.get(id), id, titles.get(id));
							trackList.put(id, track);
							panelLeft.add(track.getDisplayableComponent());
							track.getDisplayableComponent().add(
									new JLabel(track.getName()));
							track.getDisplayableComponent().addMouseListener(
									new DO_Listener<>(track, state, Track.getParams(),
											caller));
							final MidiInstrument instrument = instruments.get(id);
							final DropTarget<JPanel, JPanel, JPanel> target;
							if (instrument == null) {
								target = state.emptyTarget;
							} else {
								target = instrument.createNewTarget();
								track.addTarget(target);
							}
							target.link(track);
							link(track, target);
						}
						scrollPane = new JScrollPane(panelLeft);
						return scrollPane;
					}

					/** */
					@Override
					protected final void initObject(
							final DragObject<JPanel, JPanel, JPanel> object) {
						panelLeft.add(object.getDisplayableComponent());
						object.getDisplayableComponent().add(new JLabel(object.getName()));
						object.getDisplayableComponent()
						.addMouseListener(
								new DO_Listener<>(object, state,
										Track.getParams(), caller));
						panelLeft.revalidate();
					}


					/**
					 * Creates the panel for the instrument-categories.
					 */
					@Override
					protected final JPanel initRight() {
						final JPanel mainPanel = new JPanel();
						mainPanel.setLayout(new GridLayout(0, 1));

						for (final DropTargetContainer<JPanel, JPanel, JPanel> t : targets) {
							final JLabel label = new JLabel(t.getName());
							final JPanel panel = t.getDisplayableComponent();
							panel.removeAll(); // needed in case of not first run
							panel.addMouseListener(new TC_Listener<>(t, state));
							panel.setMinimumSize(new Dimension(120, 15));
							panel.setPreferredSize(new Dimension(120, 33));
							panel.add(label);
							mainPanel.add(panel);
						}
						return mainPanel;
					}

					/** */
					@Override
					protected final void initTarget(
							final DropTarget<JPanel, JPanel, JPanel> target) {
						final Font font = Font.decode("Arial bold 9");
						final JPanel panelNew = target.getDisplayableComponent();

						final JPanel labelPanel = new JPanel();
						final JLabel label = new JLabel(target.getName());

						labelPanel.add(label);

						final JPanel paramPanel = new JPanel();
						paramPanel.setLayout(new GridLayout(0, 1));

						for (final String param : target.getParamsToSet()) {
							final JLabel labelP = new JLabel(param);
							final JPanel panelP0 = new JPanel();
							final JPanel panelP1 = new JPanel();

							target.displayParam(param, panelP1, panelP0, caller);

							labelP.setFont(font);

							panelP0.setBackground(Color.WHITE);
							panelP0.addMouseListener(new MouseListener() {

								@Override
								public final void mouseClicked(final MouseEvent e) {
									e.consume();
								}

								@Override
								public final void mouseEntered(final MouseEvent e) {
									panelP0.setBackground(Color.GREEN);
									e.consume();
								}

								@Override
								public void mouseExited(final MouseEvent e) {
									panelP0.setBackground(Color.WHITE);
									e.consume();
								}

								@Override
								public final void mousePressed(final MouseEvent e) {
									e.consume();
								}

								@Override
								public final void mouseReleased(final MouseEvent e) {
									paramPanel.remove(panelP0);
									paramPanel.add(panelP1);
									panelP0.setBackground(Color.WHITE);
									paramPanel.revalidate();
								}

							});
							panelP0.add(labelP);
							paramPanel.add(panelP0);
						}

						panelNew.setLayout(new BorderLayout());
						panelNew.add(labelPanel);
						labelPanel.setBackground(null);
						panelNew.add(paramPanel, BorderLayout.SOUTH);

						target.getDisplayableComponent().addMouseListener(
								new DT_Listener<>(target, state));
					}

}
