package stone.modules.midiData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;

import stone.modules.abcCreator.DndPluginCaller;
import stone.modules.abcCreator.DragObject;
import stone.modules.abcCreator.DropTarget;
import stone.modules.abcCreator.DropTargetContainer;


class EmptyMidiInstrumentDropTarget implements
		DropTarget<JPanel, JPanel, JPanel> {
	final Set<DragObject<JPanel, JPanel, JPanel>> objects =
			new HashSet<>();
	private final DropTargetContainer<JPanel, JPanel, JPanel> container;

	EmptyMidiInstrumentDropTarget(
			final DropTargetContainer<JPanel, JPanel, JPanel> container) {
		this.container = container;
	}

	@Override
	public final int compareTo(final DropTarget<?, ?, ?> o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void displayParam(final String key, final JPanel panel,
			final JPanel menu,
			final DndPluginCaller<JPanel, JPanel, JPanel> caller) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel>
			getContainer() {
		return container;
	}

	@Override
	public final JPanel getDisplayableComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Map<String, Integer> getParams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Set<String> getParamsToSet() {
		return Collections.emptySet();
	}

	@Override
	public final Iterator<DragObject<JPanel, JPanel, JPanel>> iterator() {
		return objects.iterator();
	}

	@Override
	public final void link(final DragObject<JPanel, JPanel, JPanel> o) {
		objects.add(o);
	}

	@Override
	public final String printParam(final Entry<String, Integer> param) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setParam(final String key, final Integer value) {
		throw new UnsupportedOperationException();
	}
}
