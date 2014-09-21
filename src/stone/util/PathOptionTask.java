package stone.util;

import java.awt.Color;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;


final class PathOptionTask implements Runnable {
	/**
	 * 
	 */
	private final PathOption pathOption;
	private final JTextField textField;

	PathOptionTask(final PathOption pathOption, final JTextField textField) {
		this.pathOption = pathOption;
		this.textField = textField;
	}

	@Override
	public void run() {
		final Path path = pathOption.getValue();
		final String value;
		if (path == null) {
			value = null;
		} else {
			value = path.toString();
		}
		final JFileChooser fileChooser = new JFileChooser(value);
		final JFrame frame = new JFrame();
		fileChooser.setFileFilter(pathOption.filter);
		fileChooser.setDialogTitle(pathOption.getDescription());
		fileChooser.setFileSelectionMode(pathOption.selectionMode);

		try {
			final int ret = fileChooser.showOpenDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION) {
				pathOption.value(fileChooser.getSelectedFile());
				textField.setText(pathOption.getValue().toString());
				textField.setForeground(Color.BLACK);
			} else {
				pathOption.value((File) null);
				textField.setText(pathOption.getTooltip());
				textField.setForeground(Color.GRAY);
			}
		} finally {
			frame.setVisible(false);
			frame.dispose();
		}
	}
}