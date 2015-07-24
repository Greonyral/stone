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

	@SuppressWarnings("hiding")
	PathOptionTask(final PathOption pathOption, final JTextField textField) {
		this.pathOption = pathOption;
		this.textField = textField;
	}

	@Override
	public void run() {
		final Path path = this.pathOption.getValue();
		final String value;
		if (path == null) {
			value = null;
		} else {
			value = path.toString();
		}
		final JFileChooser fileChooser = new JFileChooser(value);
		final JFrame frame = new JFrame();
		fileChooser.setFileFilter(this.pathOption.filter);
		fileChooser.setDialogTitle(this.pathOption.getDescription());
		fileChooser.setFileSelectionMode(this.pathOption.selectionMode);

		try {
			final int ret = fileChooser.showOpenDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION) {
				this.pathOption.value(fileChooser.getSelectedFile());
				this.textField.setText(this.pathOption.getValue().toString());
				this.textField.setForeground(Color.BLACK);
				this.textField.setBackground(Color.WHITE);
			} else {
				this.pathOption.value((File) null);
				this.textField.setText(this.pathOption.getTooltip());
				this.textField.setForeground(Color.GRAY);
				this.textField.setBackground(Color.WHITE);
			}
		} finally {
			frame.setVisible(false);
			frame.dispose();
		}
	}
}