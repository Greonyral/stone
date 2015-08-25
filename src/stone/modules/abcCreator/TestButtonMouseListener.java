package stone.modules.abcCreator;

import java.awt.event.MouseEvent;


final class TestButtonMouseListener extends ReleaseListener {

	TestButtonMouseListener(final ReleaseMouseListenerParams params) {
		super(params);
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		synchronized (this.abcMapPlugin.state) {
			if (this.abcMapPlugin.state.loadingMap) {
				return;
			}
			while (this.abcMapPlugin.state.running) {
				try {
					this.abcMapPlugin.state.wait();
				} catch (final InterruptedException ie) {
					ie.printStackTrace();
				}
			}
			if (this.abcMapPlugin.state.upToDate) {
				this.abcMapPlugin.state.io.endProgress("Creating abc");
			}
			this.abcMapPlugin.state.upToDate = false;
			this.abcMapPlugin.state.running = true;
		}
		this.abcMapPlugin.taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				final Object result = TestButtonMouseListener.this.abcMapPlugin.caller
						.call_back(null, null,
								TestButtonMouseListener.this.abcMapPlugin
										.size());
				final boolean success = result != null;
				synchronized (TestButtonMouseListener.this.abcMapPlugin.state) {
					TestButtonMouseListener.this.abcMapPlugin.state.notifyAll();
					TestButtonMouseListener.this.abcMapPlugin.state.upToDate = success;
					TestButtonMouseListener.this.abcMapPlugin.state.running = false;
					if (!success) {
						TestButtonMouseListener.this.abcMapPlugin.state.label
								.setText("Creating abc failed");
					} else {
						assert result != null;
						String critical = result.toString();
						TestButtonMouseListener.this.abcMapPlugin.state.label
								.setText("The abc is up-to-date - "
										+ critical);
					}
				}
			}
		});
		e.consume();
	}
}