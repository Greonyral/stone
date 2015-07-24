package stone.modules.abcCreator;

import java.awt.Container;
import java.util.HashSet;
import java.util.Set;


final class DT_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTarget<C, D, T> target;

	@SuppressWarnings("hiding")
	DT_Listener(final DropTarget<C, D, T> target,
			final DragAndDropPlugin<C, D, T>.State state) {
		super(state);
		this.target = target;
		target.getDisplayableComponent().setBackground(
				DNDListener.C_INACTIVE_TARGET);
	}

	private final void mark(boolean active) {
		final Set<DragObject<?, ?, ?>> objects = new HashSet<>();
		for (final DragObject<?, ?, ?> o : this.target) {
			objects.add(o);
			if (!active || (this.state.dragging == null)) {
				if (o.isAlias()) {
					markAlias(active, o.getOriginal());
				}
				markAlias(active, o.getAliases());
			}
			o.getDisplayableComponent().setBackground(
					active ? DNDListener.C_SELECTED0 : DNDListener.C_INACTIVE);
		}
		this.target
				.getContainer()
				.getDisplayableComponent()
				.setBackground(
						active ? DNDListener.C_SELECTED0
								: DNDListener.C_INACTIVE_TARGET);
		this.target.getDisplayableComponent().setBackground(
				active ? DNDListener.C_ACTIVE : DNDListener.C_INACTIVE_TARGET);
		for (final DropTarget<?, ?, ?> t : this.target.getContainer()) {
			if (t == this.target) {
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
		for (final DragObject<?, ?, ?> o : this.target) {
			blackList.add(o);
		}
		for (final DragObject<?, ?, ?> o : objects) {
			if (!blackList.contains(o)) {
				o.getDisplayableComponent().setBackground(
						active ? DNDListener.C_CLONE : DNDListener.C_INACTIVE);
			}
			for (final DropTarget<?, ?, ?> t : o) {
				if ((t != this.target) && (t != this.state.emptyTarget)) {
					t.getDisplayableComponent().setBackground(
							active ? DNDListener.C_CLONE
									: DNDListener.C_INACTIVE_TARGET);
				}
			}
			if (o.getTargetContainer() != this.target.getContainer()) {
				o.getTargetContainer()
						.getDisplayableComponent()
						.setBackground(
								active ? DNDListener.C_CLONE
										: DNDListener.C_INACTIVE_TARGET);
			}
		}
	}

	@Override
	protected final void enter(boolean enter) {
		if (enter) {
			this.state.target = this.target;
		} else {
			this.state.target = null;
		}
		mark(enter);
	}

	@Override
	protected final void trigger(boolean release, int button) {
		// nothing to do
	}

}
