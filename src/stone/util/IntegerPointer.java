package stone.util;

final class IntegerPointer {
	private int i;

	IntegerPointer(int i) {
		this.i = i;
	}

	public final void decrement() {
		--this.i;
	}

	public final void decrement(int value) {
		this.i -= value;
	}

	public final boolean greaterZero() {
		return this.i > 0;
	}

	public final boolean isZero() {
		return this.i == 0;
	}
}