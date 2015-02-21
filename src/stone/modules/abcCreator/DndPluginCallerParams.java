package stone.modules.abcCreator;

import java.awt.Container;
import java.util.Iterator;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 * @param <E>
 *            Type of underlying value.
 */
public interface DndPluginCallerParams<E> {

	/**
	 * @return the value to use whenever <i>this</i> paremeter has not been set.
	 */
	E defaultValue();

	/**
	 * Called by the GUI whenever <i>this</i> shall be displayed using given
	 * panel. Accessing the global parameters.
	 * 
	 * @param panel
	 */
	void display(final JPanel panel);

	/**
	 * Called by the GUI whenever <i>this</i> shall be displayed using given
	 * panel. Accessing the local parameters.
	 * 
	 * @param <C>
	 *            Container to display the DragObject
	 * @param <D>
	 *            Container to display the DropTarget
	 * @param <T>
	 *            Container to display the DropTargetContainer
	 * @param panel
	 * @param object
	 * @param targets
	 */
	<C extends Container, D extends Container, T extends Container> void display(
			JPanel panel, DragObject<C, D, T> object,
			Iterator<DropTarget<C, D, T>> targets);

	/**
	 * @return the name of <i>this</i> param, to be used in a GUI
	 */
	E value();
}
