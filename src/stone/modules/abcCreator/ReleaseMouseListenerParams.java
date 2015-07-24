package stone.modules.abcCreator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;


/**
 * @author Nelphindal
 * 
 */
public interface ReleaseMouseListenerParams {

	/**
	 * @return -
	 */
	JPanel globalMenu();

	/**
	 * @return -
	 */
	JButton globalParamsButton();

	/**
	 * @return -
	 */
	JButton loadButton();

	/**
	 * @return -
	 */
	JPanel panel();

	/**
	 * @return -
	 */
	JPanel panelCenter();

	/**
	 * @return -
	 */
	AbcMapPlugin plugin();

	/**
	 * @return -
	 */
	JToggleButton splitButton();

	/**
	 * @return -
	 */
	JButton testButton();

}