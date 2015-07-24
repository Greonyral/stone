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
	/** set to value of constructor call */
	protected final AbcMapPlugin abcMapPlugin;
	/** set to value of constructor call */
	protected final JToggleButton splitButton;
	/** set to value of constructor call */
	protected final JButton globalParamsButton;
	/** set to value of constructor call */
	protected final JButton testButton;
	/** set to value of constructor call */
	protected final JButton loadButton;
	/** set to value of constructor call */
	protected final JPanel panel;
	/** set to value of constructor call */
	protected final JPanel panelCenter;
	/** set to value of constructor call */
	protected final JPanel globalMenu;

	/**
	 * clones a already created <i>listener</i>
	 * 
	 * @param listener
	 *            -
	 */
	protected ReleaseListener(final ReleaseListener listener) {
		this(listener.params);
	}

	/**
	 * @param params
	 *            container setting all necessary objects to call for triggered
	 *            action
	 */
	protected ReleaseListener(
			@SuppressWarnings("hiding") final ReleaseMouseListenerParams params) {
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
