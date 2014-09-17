package stone.modules.abcCreator;

/**
 * @author Nelphindal
 */
public class ExecutableJarFileFilter extends FileEndingFilter {

	/**
	 * 
	 */
	public ExecutableJarFileFilter() {
		super(1);
	}

	/** checks for *.exe or *.jar */
	@Override
	public boolean ending(final String s) {
		return s.equals(".jar");
	}

	/** */
	@Override
	public String getDescription() {
		return "executables (.jar)";
	}
}
