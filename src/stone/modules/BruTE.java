package stone.modules;

import java.util.List;
import java.util.Set;

import stone.StartupContainer;
import stone.util.Option;

/**
 * Dummy module for BruTE
 * @author Nelphindal
 *
 */
public final class BruTE implements Module {

	private static final int VERSION = 1;
	
	/**
	 * Constructor for building versionInfo
	 */
	public BruTE() {
		
	}
	

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * 
	 * @param sc
	 *            container providing runtime dependent information
	 */
	public BruTE(final StartupContainer sc) {
		// dummy
	}

	@Override
	public List<Option> getOptions() {
		return java.util.Collections.emptyList();
	}

	@Override
	public final int getVersion() {
		return VERSION;
	}

	@Override
	public Module init(final StartupContainer sc) {
		return new BruTE();
	}

	@Override
	public void repair() {
		// dummy nothing to do
	}

	@Override
	public void run() {
		// dummy nothing to do
	}

	@Override
	public void dependingModules(final Set<String> set) {
		// dummy nothing to do
	}

}
