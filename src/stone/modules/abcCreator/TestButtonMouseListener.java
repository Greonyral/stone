package stone.modules.abcCreator;

import java.awt.event.MouseEvent;

final class TestButtonMouseListener extends ReleaseListener {

	TestButtonMouseListener(final ReleaseMouseListenerParams params) {
		super(params);
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		synchronized (abcMapPlugin.state) {
			if (abcMapPlugin.state.loadingMap)
				return;
			while (abcMapPlugin.state.running) {
				try {
					abcMapPlugin.state.wait();
				} catch (final InterruptedException ie) {
					ie.printStackTrace();
				}
			}
			if (abcMapPlugin.state.upToDate) {
				abcMapPlugin.state.io.endProgress();
			}
			abcMapPlugin.state.upToDate = false;
			abcMapPlugin.state.running = true;
		}
		abcMapPlugin.taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				final Object result =
						TestButtonMouseListener.this.abcMapPlugin.caller.call_back(null, null, TestButtonMouseListener.this.abcMapPlugin.size());
				final boolean success = result != null;
				synchronized (TestButtonMouseListener.this.abcMapPlugin.state) {
					TestButtonMouseListener.this.abcMapPlugin.state.notifyAll();
					TestButtonMouseListener.this.abcMapPlugin.state.upToDate = success;
					TestButtonMouseListener.this.abcMapPlugin.state.running = false;
					if (!success) {
						TestButtonMouseListener.this.abcMapPlugin.state.label.setText("Creating abc failed");
					} else {
						TestButtonMouseListener.this.abcMapPlugin.state.label
						.setText("The abc is up-to-date - "
								+ result.toString()
								.substring(
										0,
										result.toString()
										.indexOf(
												"%") + 1));
					}
				}
			}
		});
		e.consume();
	}
}