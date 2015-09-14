package stone.modules;

import java.util.List;
import java.util.Set;

import stone.util.Option;


/**
 * This interface indicates that the implementing class is an module. All
 * implementing classes have to provide the default constructor and a
 * constructor providing an instance of StartupContainer
 * 
 * @author Nelphindal
 */
public interface Module {

	/**
	 * Adds modules on which this module depends to given set
	 * 
	 * @param set
	 *            {@link Set} to add dependent modules to
	 */
	void dependingModules(Set<String> set);

	/**
	 * @return a list of options needed to set by the user
	 */
	List<Option> getOptions();

	/**
	 * @return the current version
	 */
	int getVersion();

	/**
	 * @param sc
	 * @return the fully initialized Module ready to be run
	 */
	Module init(final stone.StartupContainer sc);

	/**
	 * Requests to repair all permanently stored data
	 */
	void repair();

	/**
	 * Executes this module
	 */
	void run();
}
