package stone.modules.abcCreator;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;


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
		abcMapPlugin = params.plugin();
		panel = params.panel();
		panelCenter = params.panelCenter();
		globalMenu = params.globalMenu();
		globalParamsButton = params.globalParamsButton();
		splitButton = params.splitButton();
		testButton = params.testButton();
		loadButton = params.loadButton();
	}

	@Override
	public final void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mousePressed(final MouseEvent e) {
		e.consume();
	}


}
