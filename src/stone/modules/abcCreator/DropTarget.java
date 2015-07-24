package stone.modules.abcCreator;

import java.awt.Container;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 * @param <C> -
 * @param <D> -
 * @param <T> -
 */
public interface DropTarget<C extends Container, D extends Container, T extends Container>
		extends Comparable<DropTarget<?, ?, ?>>, Iterable<DragObject<C, D, T>> {

	/**
	 * @param key
	 * @param container
	 * @param menu
	 * @param caller
	 */
	void displayParam(String key, JPanel container, JPanel menu,
			DndPluginCaller<C, D, T> caller);

	/**
	 * @return the instance created this target.
	 */
	DropTargetContainer<C, D, T> getContainer();

	/**
	 * @return the Component to display <i>this</i> DropTarget
	 */
	D getDisplayableComponent();

	/**
	 * Returns a name usable for a GUI.
	 * 
	 * @return a name.
	 */
	String getName();

	/**
	 * @return a map of the parameters linked to this DropTarget
	 */
	Map<String, Integer> getParams();

	/**
	 * @return a set of the keys of parameters which has to be set
	 */
	Set<String> getParamsToSet();

	/**
	 * Links DragObjects to this DropTarget
	 * 
	 * @param o
	 */
	void link(final DragObject<C, D, T> o);

	/**
	 * @param param
	 * @return the string representing given param.
	 */
	String printParam(final Entry<String, Integer> param);

	/**
	 * @param key
	 * @param value
	 */
	void setParam(String key, Integer value);
}
