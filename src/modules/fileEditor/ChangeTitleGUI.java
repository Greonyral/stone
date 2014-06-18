package modules.fileEditor;

import util.Path;
import modules.FileEditor;


/**
 * GUIPlugin to change the title of one or more songs
 * 
 * @author Nelphindal
 */
public final class ChangeTitleGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 * @param root
	 */
	public ChangeTitleGUI(final FileEditor fileEditor, final Path root) {
		super(fileEditor, root);
	}

	@Override
	protected final String getTitle() {
		return "Selected songs to change titles";
	}
}
