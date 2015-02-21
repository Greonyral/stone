package stone;

final class ThreadState {
	private boolean interrupted = false, locked = true;

	public final void handleEvent(final Event event) {
		switch (event) {
		case CLEAR_INT:
			this.interrupted = false;
			break;
		case INT:
			this.interrupted = true;
			break;
		case LOCK_INT:
			this.locked = false;
			break;
		case UNLOCK_INT:
			this.locked = true;
			break;
		default:
			break;
		}
	}

	public final boolean isInterrupted() {
		return this.interrupted && this.locked;
	}
}