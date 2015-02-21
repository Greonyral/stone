package stone.io;

/**
 * Enum to determine how to handle and continue at a exception.
 * 
 * @author Nelphindal
 */
public enum ExceptionHandle {

	/**
	 * Log only
	 */
	SUPPRESS(true, false),
	/**
	 * Notify user, continue the program
	 */
	CONTINUE(false, false),

	/**
	 * Notify user and terminate the program
	 */
	TERMINATE(false, true);

	private final boolean suppress, abort;

	private ExceptionHandle(boolean suppress, boolean abort) {
		this.suppress = suppress;
		this.abort = abort;
	}

	/**
	 * Checks if a exception shall be suppressed and a notification shall be
	 * printed to log only
	 * 
	 * @return <i>true</i> a exception shall be suppressed and a notification
	 *         shall be printed to log only
	 */
	public final boolean suppress() {
		return this.suppress;
	}

	/**
	 * Checks if the program shall be aborted
	 * 
	 * @return <i>true</i> if program shall be aborted
	 */
	public final boolean terminate() {
		return this.abort;
	}
}
