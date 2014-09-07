package stone.util;

import stone.Main;


public abstract class Debug {

	private static final Debug instance = Flag.getInstance().isEnabled(
			Main.DEBUG_ID) ? new Debug() {

		@Override
		protected final synchronized void printImpl(final String string,
				final Object[] args) {
			System.out.printf(string, args);

		}

	} : new Debug() {

		@Override
		protected final void printImpl(final String string,
				final Object[] args) {
			return;
		}

	};

	public final static void print(final String string,
			final Object... args) {
		Debug.instance.printImpl(string, args);
	}

	protected abstract void printImpl(String string, Object[] args);

}
