package stone.modules.fileEditor;

import stone.modules.FileEditor;
import stone.util.Path;


/**
 * GUIPlugin to change the numbering of songs
 * 
 * @author Nelphindal
 */
public final class ChangeNumberingGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 *            -
	 * @param root
	 *            -
	 * @param text
	 *            message to be displayed at end of plugin
	 */
	public ChangeNumberingGUI(
			@SuppressWarnings("hiding") final FileEditor fileEditor,
			final Path root, final String text) {
		super(fileEditor, root, text);
	}


	/** */
	@Override
	protected final String getTitle() {
		return "Selected songs to change numbering";
	}

}
