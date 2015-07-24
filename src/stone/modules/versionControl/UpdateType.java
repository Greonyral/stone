package stone.modules.versionControl;

/**
 * Basic questions for git diff
 * 
 * @author Nelphindal
 */
public enum UpdateType {

	/** Reset missing file */
	RESTORE_MISSING("Detected missing file",
			"Do you want to restore it?\nThe file will be the version of last commit."),
	/** Delete missing file */
	DELETE(RESTORE_MISSING.p0,
			"Do you want to remove it from the remote and your\nlocal repository?"),
	/** Add untracked file */
	ADD(
			"Detected new file",
			"Do you want to add it?\n It will be part of the remote and your\nlocal repository."),
	/** Add changed file */
	UPDATE("Detected changed file", ADD.p1),
	/** Reset changed file */
	RESTORE_CHANGED(UPDATE.p0, RESTORE_MISSING.p1);

	private final String p0, p1;

	@SuppressWarnings("hiding")
	private UpdateType(final String p0, final String p1) {
		this.p0 = p0;
		this.p1 = p1;
	}

	/**
	 * @return first part of the question
	 */
	public final String getQuestionPart0() {
		return this.p0;
	}

	/**
	 * @return secind part of the question
	 */
	public final String getQuestionPart1() {
		return this.p1;
	}
}
