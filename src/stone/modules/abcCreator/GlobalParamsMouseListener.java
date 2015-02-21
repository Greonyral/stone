package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import stone.io.GUI;


final class GlobalParamsMouseListener extends ReleaseListener {


	GlobalParamsMouseListener(final ReleaseMouseListenerParams params) {
		super(params);
	}


	@Override
	public final void mouseReleased(final MouseEvent e) {
		final JPanel globalMenuPanel = new JPanel();
		this.abcMapPlugin.panelCenter.remove(this.globalParamsButton);
		GUI.Button.OK.getButton().setVisible(false);
		this.splitButton.setVisible(false);
		GUI.Button.ABORT.getButton().setVisible(false);
		this.testButton.setVisible(false);

		for (final DndPluginCallerParams<?> m : this.abcMapPlugin.caller
				.valuesGlobal()) {
			if (m == null) {
				continue;
			}
			final JPanel optionPanel = new JPanel();
			final JButton button = new JButton(m.toString());
			final MenuListener listener = new MenuListener(
					GlobalParamsMouseListener.this, button) {

				@Override
				protected final void trigger() {
					globalMenuPanel.removeAll();
					final JPanel panel0 = new JPanel();
					globalMenuPanel.add(panel0);
					m.display(panel0);
					this.globalMenu.revalidate();
				}
			};
			button.addChangeListener(listener);
			button.addMouseListener(listener);
			optionPanel.add(button);
			globalMenuPanel.add(optionPanel);
		}
		final JPanel optionPanelClose = new JPanel();
		final JButton closeButton = new JButton("Close");
		final MenuListener listener = new MenuListener(
				GlobalParamsMouseListener.this, closeButton) {

			@Override
			protected final void trigger() {
				exit();
			}
		};
		closeButton.addMouseListener(listener);
		closeButton.addChangeListener(listener);
		optionPanelClose.add(closeButton);
		globalMenuPanel.setLayout(new GridLayout(0, 2));

		this.globalMenu.setLayout(new BorderLayout());
		this.globalMenu.add(globalMenuPanel);
		this.globalMenu.add(optionPanelClose, BorderLayout.SOUTH);
		this.abcMapPlugin.panelCenter.add(this.globalMenu, BorderLayout.SOUTH);
		this.panel.revalidate();
		e.consume();
	}
}