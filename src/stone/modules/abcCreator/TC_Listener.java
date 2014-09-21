package stone.modules.abcCreator;

import java.awt.Container;


final class TC_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTargetContainer<C, D, T> targetC;

	public TC_Listener(final DropTargetContainer<C, D, T> targetC,
			final DragAndDropPlugin<C, D, T>.State state) {
		super(state);
		this.targetC = targetC;
		targetC.getDisplayableComponent().setBackground(
				DNDListener.C_INACTIVE_TARGET);
	}

	@Override
	protected final void enter(boolean enter) {
		if (enter) {
			state.targetC = targetC;
		} else {
			state.targetC = null;
		}
		if (enter && (state.dragging != null)) {
			targetC.getDisplayableComponent().setBackground(
					enter ? DNDListener.C_DROP
							: DNDListener.C_INACTIVE_TARGET);
		} else {
			targetC.getDisplayableComponent().setBackground(
					enter ? DNDListener.C_ACTIVE
							: DNDListener.C_INACTIVE_TARGET);
			if (state.dragging == null) {
				for (final DropTarget<C, D, T> t : targetC) {
					if (t != state.emptyTarget) {
						t.getDisplayableComponent().setBackground(
								enter ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE_TARGET);
					}
					for (final DragObject<C, D, T> d : t) {
						d.getDisplayableComponent().setBackground(
								enter ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE);
					}
				}
			}
		}
	}

	@Override
	protected final void trigger(boolean release, int button) {
		// nothing to do

	}

}
