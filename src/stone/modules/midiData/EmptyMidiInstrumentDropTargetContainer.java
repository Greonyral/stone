package stone.modules.midiData;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;

import stone.modules.abcCreator.DragObject;
import stone.modules.abcCreator.DropTarget;
import stone.modules.abcCreator.DropTargetContainer;


class EmptyMidiInstrumentDropTargetContainer implements
		DropTargetContainer<JPanel, JPanel, JPanel> {

	final EmptyMidiInstrumentDropTarget target =
			new EmptyMidiInstrumentDropTarget(this);
	private final JPanel panel = new JPanel();

	@Override
	public final void clearTargets() {
		for (final DragObject<JPanel, JPanel, JPanel> d : target.objects) {
			d.clearTargets();
		}
		target.objects.clear();
	}

	@Override
	public final DropTarget<JPanel, JPanel, JPanel> createNewTarget() {
		return target;
	}

	@Override
	public final void delete(
			final DropTarget<JPanel, JPanel, JPanel> dropTarget) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final JPanel getDisplayableComponent() {
		return panel;
	}

	@Override
	public final String getName() {
		return " - NONE - ";
	}

	@Override
	public final Iterator<DropTarget<JPanel, JPanel, JPanel>> iterator() {
		if (target.objects.isEmpty()) {
			return java.util.Collections.emptyIterator();
		}
		return new Iterator<DropTarget<JPanel, JPanel, JPanel>>() {

			boolean hasNext = true;

			@Override
			public final boolean hasNext() {
				return hasNext;
			}

			@Override
			public final DropTarget<JPanel, JPanel, JPanel> next() {
				hasNext = false;
				return target;
			}

			@Override
			public final void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}


	@Override
	public Set<DropTarget<JPanel, JPanel, JPanel>> removeAllLinks(
			final DragObject<JPanel, JPanel, JPanel> object) {
		target.objects.remove(object);
		if (target.objects.isEmpty()) {
			final Set<DropTarget<JPanel, JPanel, JPanel>> s =
					new HashSet<>();
			s.add(target);
			return s;
		}
		return java.util.Collections.emptySet();
	}

}