package stone.modules.abcCreator;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


abstract class DNDListener<C extends Container, D extends Container, T extends Container>
		implements MouseListener {

	protected static final Color C_INACTIVE = Color.WHITE;
	protected static final Color C_ACTIVE = Color.BLUE;
	protected static final Color C_INACTIVE_TARGET = Color.GRAY;
	protected static final Color C_DRAGGING = Color.RED;
	protected static final Color C_SELECTED0 = Color.YELLOW;
	protected static final Color C_SELECTED1 = Color.ORANGE;
	protected static final Color C_DROP = Color.BLUE;
	protected static final Color C_CLONE = Color.CYAN;

	protected final DragAndDropPlugin<C, D, T>.State state;

	protected DNDListener(
			@SuppressWarnings("hiding") final DragAndDropPlugin<C, D, T>.State state) {
		this.state = state;
	}

	@Override
	public final void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
		enter(true);
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
		enter(false);
	}

	@Override
	public final void mousePressed(final MouseEvent e) {
		e.consume();
		trigger(false, e.getButton());
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		e.consume();
		trigger(true, e.getButton());
	}

	protected abstract void enter(boolean enter);

	protected abstract void trigger(boolean release, int button);
}
