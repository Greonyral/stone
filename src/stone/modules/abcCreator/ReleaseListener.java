package stone.modules.abcCreator;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;


/**
 * MouseListener listening for none but releasing MouseEvent.
 * 
 * @author Nelphindal
 * 
 */
public abstract class ReleaseListener implements MouseListener {

	private final ReleaseMouseListenerParams params;

	protected final AbcMapPlugin abcMapPlugin;
	protected final JToggleButton splitButton;
	protected final JButton globalParamsButton;
	protected final JButton testButton, loadButton;
	protected final JPanel panel, panelCenter, globalMenu;

	protected ReleaseListener(final ReleaseListener listener) {
		this(listener.params);
	}

	protected ReleaseListener(final ReleaseMouseListenerParams params) {
		this.params = params;
		this.abcMapPlugin = params.plugin();
		this.panel = params.panel();
		this.panelCenter = params.panelCenter();
		this.globalMenu = params.globalMenu();
		this.globalParamsButton = params.globalParamsButton();
		this.splitButton = params.splitButton();
		this.testButton = params.testButton();
		this.loadButton = params.loadButton();
	}

	/** Ignores the event */
	@Override
	public final void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	/** Ignores the event */
	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
	}

	/** Ignores the event */
	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
	}

	/** Ignores the event */
	@Override
	public final void mousePressed(final MouseEvent e) {
		e.consume();
	}


}
