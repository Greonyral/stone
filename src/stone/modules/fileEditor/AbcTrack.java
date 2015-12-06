package stone.modules.fileEditor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AbcTrack {

	private final int id;
	private String title;
	private List<String> lines = new ArrayList<>();

	
	AbcTrack(final String idLine) throws IOException {
		String line = idLine.toUpperCase().replaceAll(" ", "");
		if (!(line.charAt(0) == 'X' && line.charAt(1) == ':')) {
			throw new RuntimeException("Invalid ABC-Track header");
		}
		id = Integer.parseInt(line.substring(2));
	}
	
	public int getId() {
		return id;
	}

	public void appendCommentLine(final String line) {
		// TODO Auto-generated method stub
		
	}

	public void setTitle(final String line) {
		// TODO Auto-generated method stub
		
	}
}
