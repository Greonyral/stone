package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import stone.io.GUI;


abstract class MenuListener extends ReleaseListener implements
		ChangeListener {

	protected MenuListener(final ReleaseListener listener,
			final AbstractButton button) {
		super(listener);
		this.button = button;
	}

	private boolean triggered = false;
	private final AbstractButton button;


	@Override
	public final void mouseReleased(final MouseEvent e) {
		if (!triggered) {
			triggered = true;
			trigger();
		}
	}

	@Override
	public final void stateChanged(final ChangeEvent e) {
		if (triggered && !button.isSelected()) {
			triggered = false;
		}
	}

	protected final void exit() {
		panelCenter.remove(globalMenu);
		globalMenu.removeAll();
		panelCenter.add(globalParamsButton, BorderLayout.SOUTH);
		GUI.Button.OK.getButton().setVisible(true);
		splitButton.setVisible(true);
		GUI.Button.ABORT.getButton().setVisible(true);
		testButton.setVisible(true);
		panel.revalidate();
	}

	protected abstract void trigger();
}