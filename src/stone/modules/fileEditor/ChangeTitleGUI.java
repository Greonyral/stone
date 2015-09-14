package stone.modules.fileEditor;

import stone.modules.FileEditor;
import stone.util.Path;


/**
 * GUIPlugin to change the title of one or more songs
 * 
 * @author Nelphindal
 */
public final class ChangeTitleGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 *            -
	 * @param root
	 *            -
	 * @param text
	 *            message displayed at end of plugin
	 */
	public ChangeTitleGUI(
			@SuppressWarnings("hiding") final FileEditor fileEditor,
			final Path root, final String text) {
		super(fileEditor, root, text);
	}

	/** */
	@Override
	protected final String getTitle() {
		return "Selected songs to change titles";
	}
}
