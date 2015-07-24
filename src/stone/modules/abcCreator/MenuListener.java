package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.GUI;


abstract class MenuListener extends ReleaseListener implements ChangeListener {

	private boolean triggered = false;

	private final AbstractButton button;

	protected MenuListener(final ReleaseListener listener,
			@SuppressWarnings("hiding") final AbstractButton button) {
		super(listener);
		this.button = button;
	}


	@Override
	public final void mouseReleased(final MouseEvent e) {
		if (!this.triggered) {
			this.triggered = true;
			trigger();
		}
	}

	@Override
	public final void stateChanged(final ChangeEvent e) {
		if (this.triggered && !this.button.isSelected()) {
			this.triggered = false;
		}
	}

	protected final void exit() {
		this.panelCenter.remove(this.globalMenu);
		this.globalMenu.removeAll();
		this.panelCenter.add(this.globalParamsButton, BorderLayout.SOUTH);
		GUI.Button.OK.getButton().setVisible(true);
		this.splitButton.setVisible(true);
		GUI.Button.ABORT.getButton().setVisible(true);
		this.testButton.setVisible(true);
		this.panel.revalidate();
	}

	protected abstract void trigger();
}