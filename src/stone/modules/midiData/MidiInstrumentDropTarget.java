package stone.modules.midiData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import stone.modules.abcCreator.DndPluginCaller;
import stone.modules.abcCreator.DragObject;
import stone.modules.abcCreator.DropTarget;
import stone.modules.abcCreator.DropTargetContainer;


/**
 * @author Nelphindal
 */
public class MidiInstrumentDropTarget implements
		DropTarget<JPanel, JPanel, JPanel> {

	private final class ParamListener implements MouseListener {
		private final ActivePanelContainer shared;
		private final JPanel mapPanel;
		private final int i;
		private final String key;

		@SuppressWarnings("hiding")
		ParamListener(ActivePanelContainer shared, JPanel mapPanel, int i,
				String key) {
			this.shared = shared;
			this.mapPanel = mapPanel;
			this.i = i;
			this.key = key;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mouseEntered(final MouseEvent e) {
			this.mapPanel.setBackground(Color.BLUE);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			final Integer map = MidiInstrumentDropTarget.this.params
					.get(this.key);
			final int mapId;
			if (map == null) {
				mapId = 0;
			} else {
				mapId = map.intValue();
			}
			this.mapPanel.setBackground(this.i == mapId ? Color.LIGHT_GRAY
					: Color.WHITE);

		}

		@Override
		public final void mousePressed(final MouseEvent e) {
			e.consume();
		}

		@Override
		public final void mouseReleased(final MouseEvent e) {
			MidiInstrumentDropTarget.this.params.put(this.key, this.i);
			if (this.shared.activePanel != null) {
				this.shared.activePanel.setBackground(Color.WHITE);
			}
			this.shared.activePanel = this.mapPanel;
		}
	}

	private final MidiInstrument midiInstrument;
	private final Set<DragObject<JPanel, JPanel, JPanel>> objects = new HashSet<>();
	private final JPanel panel;
	private final int number;
	final Map<String, Integer> params = new HashMap<>();

	MidiInstrumentDropTarget(@SuppressWarnings("hiding") final MidiInstrument midiInstrument, int id) {
		this.midiInstrument = midiInstrument;
		this.number = id;
		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());
	}

	/** */
	@Override
	public final int compareTo(final DropTarget<?, ?, ?> o) {
		if (!MidiInstrumentDropTarget.class.isInstance(o)) {
			return this.getClass().hashCode() - o.getClass().hashCode();
		}
		return compareTo((MidiInstrumentDropTarget) o);
	}

	/**
	 * @param o -
	 * @return an integer according to the comparison of <i>this</i> and
	 *         <i>o</i>
	 */
	public final int compareTo(final MidiInstrumentDropTarget o) {
		if (this.midiInstrument != o.midiInstrument) {
			return this.midiInstrument.id - o.midiInstrument.id;
		}
		return this.number - o.number;
	}

	/** */
	@Override
	public final void displayParam(final String key, final JPanel container,
			final JPanel menu,
			final DndPluginCaller<JPanel, JPanel, JPanel> caller) {
		if (key.equals("map")) {
			final JPanel panel_ = new JPanel();
			final JPanel closePanel = new JPanel();
			final Set<Integer> maps = ((stone.modules.AbcCreator) caller)
					.getMaps();
			final Integer map = this.params.get(key);
			final int mapId;
			if (map == null) {
				mapId = 0xffffffff;
			} else {
				mapId = map.intValue();
			}
			closePanel.add(new JLabel("close"));
			closePanel.addMouseListener(new MouseListener() {

				@Override
				public final void mouseClicked(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseEntered(final MouseEvent e) {
					closePanel.setBackground(Color.GREEN);
					e.consume();
				}

				@Override
				public final void mouseExited(final MouseEvent e) {
					closePanel.setBackground(Color.WHITE);
					e.consume();
				}

				@Override
				public final void mousePressed(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseReleased(final MouseEvent e) {
					e.consume();
					final Container c = container.getParent();
					c.removeAll();
					c.add(menu);
					c.revalidate();

				}
			});
			panel_.setLayout(new GridLayout(0, 2));
			container.setLayout(new BorderLayout());
			container.add(panel_);
			container.add(closePanel, BorderLayout.SOUTH);

			final ActivePanelContainer shared = new ActivePanelContainer();
			for (final int i : maps) {
				final JPanel mapPanel = new JPanel();
				if (i == mapId) {
					mapPanel.setBackground(Color.LIGHT_GRAY);
					shared.activePanel = mapPanel;
				} else {
					mapPanel.setBackground(Color.WHITE);
				}
				mapPanel.addMouseListener(new ParamListener(shared, mapPanel,
						i, key));
				mapPanel.add(new JLabel("Map " + i));
				panel_.add(mapPanel);
			}
		} else {
			container.setLayout(new BorderLayout());
			container.add(new JLabel("TODO param"));
		}

	}

	/** */
	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel> getContainer() {
		return this.midiInstrument;
	}

	/** */
	@Override
	public final JPanel getDisplayableComponent() {
		return this.panel;
	}

	/** */
	@Override
	public final String getName() {
		return this.midiInstrument.getName();
	}

	/** */
	@Override
	public final Map<String, Integer> getParams() {
		return this.params;
	}

	/** */
	@Override
	public final Set<String> getParamsToSet() {
		return this.midiInstrument.paramKeys;
	}

	/** */
	@Override
	public final int hashCode() {
		return (this.number + this.midiInstrument.id) << 4;
	}

	/** */
	@Override
	public final Iterator<DragObject<JPanel, JPanel, JPanel>> iterator() {
		return this.objects.iterator();
	}

	/** */
	@Override
	public final void link(final DragObject<JPanel, JPanel, JPanel> o) {
		this.objects.add(o);
	}

	/** */
	@Override
	public final String printParam(final Entry<String, Integer> param) {
		if (param.getKey().equals("map")) {
			return String.valueOf(param.getValue());
		}
		return null;
	}

	/** */
	@Override
	public final void setParam(final String key, final Integer value) {
		this.params.put(key, value);
	}

	/** */
	@Override
	public final String toString() {
		return this.midiInstrument.getName() + " " + this.number;
	}

	/**
	 * Helper function to clear out all instruments playing object
	 * 
	 * @param object -
	 * @param empty -
	 */
	protected final void clearTargets(
			final DragObject<JPanel, JPanel, JPanel> object,
			final Set<DropTarget<JPanel, JPanel, JPanel>> empty) {
		if (this.objects.remove(object) && this.objects.isEmpty()) {
			empty.add(this);
		}
	}
}
