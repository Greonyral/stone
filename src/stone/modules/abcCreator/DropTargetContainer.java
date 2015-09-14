package stone.modules.abcCreator;

import java.awt.Container;
import java.util.Set;


/**
 * @author Nelphindal
 * @param <C>
 *            -
 * @param <D>
 *            -
 * @param <T>
 *            -
 */
public interface DropTargetContainer<C extends Container, D extends Container, T extends Container>
		extends Iterable<DropTarget<C, D, T>> {

	/**
	 * Unlinks every DropTarget with its DragObjects and deletes all DropTargets
	 * by it.
	 */
	void clearTargets();

	/**
	 * Creates a new DropTarget and initializes it,
	 * 
	 * @return the created DropTarget
	 */
	DropTarget<C, D, T> createNewTarget();

	/**
	 * Deletes given target
	 * 
	 * @param target
	 */
	void delete(DropTarget<C, D, T> target);

	/**
	 * @return the Component to display <i>this</i> DropTarget
	 */
	T getDisplayableComponent();

	/**
	 * Returns a name usable for a GUI
	 * 
	 * @return a name
	 */
	String getName();

	/**
	 * Checks all DropTargets and unlinks each if they are linked to given
	 * object.
	 * 
	 * @param object
	 * @return A Set of formerly linked DropTargets
	 */
	Set<DropTarget<C, D, T>> removeAllLinks(final DragObject<C, D, T> object);
}
