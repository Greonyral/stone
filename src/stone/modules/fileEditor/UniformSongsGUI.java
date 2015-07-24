package stone.modules.fileEditor;

import javax.swing.JPanel;

import stone.io.GUIPlugin;
import stone.modules.FileEditor;
import stone.util.Path;


/**
 * GUIPlugin to apply the global name scheme to one or more songs
 * 
 * @author Nelphindal
 */
public class UniformSongsGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 *            -
	 * @param root
	 *            -
	 * @param text
	 *            displayed if {@link GUIPlugin#display(JPanel)} returns false
	 */
	public UniformSongsGUI(
			@SuppressWarnings("hiding") final FileEditor fileEditor,
			final Path root, final String text) {
		super(fileEditor, root, text);
	}

	@Override
	protected final String getTitle() {
		return "Select songs to uniform the titles";
	}

}
