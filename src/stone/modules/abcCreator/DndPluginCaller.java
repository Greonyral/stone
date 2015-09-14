package stone.modules.abcCreator;

import java.awt.Container;
import java.io.File;
import java.util.TreeSet;

import stone.util.Path;


/**
 * Interface for use in drag-and-drop-plugins
 * 
 * @author Nelphindal
 * @param <C>
 *            -
 * @param <D>
 *            -
 * @param <T>
 *            -
 */
public interface DndPluginCaller<C extends Container, D extends Container, T extends Container> {

	/**
	 * Helper interface for
	 * {@link DndPluginCaller#loadMap(File, LoadedMapEntry)}
	 * 
	 * @author Nelphindal
	 */
	interface LoadedMapEntry {

		/**
		 * Creates a new entry within a part
		 * 
		 * @param string
		 */
		void addEntry(final String string);

		/**
		 * Creates a new part
		 * 
		 * @param string
		 */
		void addPart(final String string);

		boolean error();

		void setError();

	}

	/**
	 * 
	 * @param task
	 *            run <i>task</i>
	 */
	public void exec(final Runnable task);

	/**
	 * @return the file being displayed
	 */
	public Path getFile();

	/**
	 * Puts all parsed data into given container
	 * 
	 * @param mapToLoad
	 *            -
	 * @param container
	 *            -
	 */
	public void loadMap(File mapToLoad, LoadedMapEntry container);

	/**
	 * @param object0
	 * @param object1
	 * @param abcTracks
	 * @return <i>true</i> on success
	 */
	Object call_back(Object object0, Object object1, int abcTracks);

	/**
	 * Links object with target.
	 * 
	 * @param object
	 * @param target
	 */
	void link(DragObject<C, D, T> object, DropTarget<C, D, T> target);

	/**
	 * Print an error
	 * 
	 * @param string
	 */
	void printError(final String string);

	/**
	 * @return a set of all allocated targets.
	 */
	TreeSet<DropTarget<C, D, T>> sortedTargets();

	/**
	 * Unlinks object from target.
	 * 
	 * @param object
	 * @param target
	 * @return <i>true</i> if target is now empty and can be removed.
	 */
	boolean unlink(DragObject<C, D, T> object, DropTarget<C, D, T> target);

	/**
	 * @return an array of all global DndPluginCallerParams
	 */
	DndPluginCallerParams<?>[] valuesGlobal();

}
