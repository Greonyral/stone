package stone.util;

final class IntegerPointer {
	private int i;

	IntegerPointer(int i) {
		this.i = i;
	}

	public final boolean isZero() {
		return i == 0;
	}

	public final boolean greaterZero() {
		return i > 0;
	}

	public final void decrement() {
		--i;
	}

	public final void decrement(int value) {
		i -= value;
	}
}