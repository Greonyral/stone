package stone.modules.abcCreator;

import java.awt.Container;
import java.util.HashSet;
import java.util.Set;


final class DT_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTarget<C, D, T> target;

	DT_Listener(final DropTarget<C, D, T> target,
			final DragAndDropPlugin<C, D, T>.State state) {
		super(state);
		this.target = target;
		target.getDisplayableComponent().setBackground(
				DNDListener.C_INACTIVE_TARGET);
	}

	private final void mark(boolean active) {
		final Set<DragObject<?, ?, ?>> objects = new HashSet<>();
		for (final DragObject<?, ?, ?> o : target) {
			objects.add(o);
			if (!active || (state.dragging == null)) {
				if (o.isAlias()) {
					markAlias(active, o.getOriginal());
				}
				markAlias(active, o.getAliases());
			}
			o.getDisplayableComponent().setBackground(
					active ? DNDListener.C_SELECTED0
							: DNDListener.C_INACTIVE);
		}
		target.getContainer().getDisplayableComponent().setBackground(
				active ? DNDListener.C_SELECTED0
						: DNDListener.C_INACTIVE_TARGET);
		target.getDisplayableComponent().setBackground(
				active ? DNDListener.C_ACTIVE
						: DNDListener.C_INACTIVE_TARGET);
		for (final DropTarget<?, ?, ?> t : target.getContainer()) {
			if (t == target) {
				continue;
			}
			t.getDisplayableComponent().setBackground(
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE_TARGET);
			for (final DragObject<?, ?, ?> o : t) {
				if (!objects.contains(o)) {
					o.getDisplayableComponent().setBackground(
							active ? DNDListener.C_SELECTED1
									: DNDListener.C_INACTIVE);
				}
			}
		}
	}

	private final void markAlias(boolean active,
			final DragObject<?, ?, ?>... objects) {
		final Set<DragObject<?, ?, ?>> blackList = new HashSet<>();
		for (final DragObject<?, ?, ?> o : target) {
			blackList.add(o);
		}
		for (final DragObject<?, ?, ?> o : objects) {
			if (!blackList.contains(o)) {
				o.getDisplayableComponent().setBackground(
						active ? DNDListener.C_CLONE
								: DNDListener.C_INACTIVE);
			}
			for (final DropTarget<?, ?, ?> t : o) {
				if ((t != target) && (t != state.emptyTarget)) {
					t.getDisplayableComponent().setBackground(
							active ? DNDListener.C_CLONE
									: DNDListener.C_INACTIVE_TARGET);
				}
			}
			if (o.getTargetContainer() != target.getContainer()) {
				o.getTargetContainer().getDisplayableComponent()
						.setBackground(
								active ? DNDListener.C_CLONE
										: DNDListener.C_INACTIVE_TARGET);
			}
		}
	}

	@Override
	protected final void enter(boolean enter) {
		if (enter) {
			state.target = target;
		} else {
			state.target = null;
		}
		mark(enter);
	}

	@Override
	protected final void trigger(boolean release, int button) {
		// nothing to do
	}

}
