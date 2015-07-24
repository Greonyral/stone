package stone.modules.abcCreator;

import java.awt.Container;


final class TC_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTargetContainer<C, D, T> targetC;

	@SuppressWarnings("hiding")
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
			this.state.targetC = this.targetC;
		} else {
			this.state.targetC = null;
		}
		if (enter && (this.state.dragging != null)) {
			this.targetC.getDisplayableComponent().setBackground(
					enter ? DNDListener.C_DROP : DNDListener.C_INACTIVE_TARGET);
		} else {
			this.targetC.getDisplayableComponent().setBackground(
					enter ? DNDListener.C_ACTIVE
							: DNDListener.C_INACTIVE_TARGET);
			if (this.state.dragging == null) {
				for (final DropTarget<C, D, T> t : this.targetC) {
					if (t != this.state.emptyTarget) {
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
