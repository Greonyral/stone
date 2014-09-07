package stone.modules.abcCreator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public interface ReleaseMouseListenerParams {

	JPanel globalMenu();

	JButton globalParamsButton();

	JButton loadButton();

	JPanel panel();

	JPanel panelCenter();

	AbcMapPlugin plugin();

	JToggleButton splitButton();

	JButton testButton();

}