package stone.modules.songData;

import stone.util.Path;


final class MissingTLineInAbc extends AbtractEoWInAbc {

	@SuppressWarnings("hiding")
	MissingTLineInAbc(final Path song, int line) {
		super(song, line);
	}

	@Override
	final String getDetail() {
		return "Missing T: line";
	}

	@Override
	final WarnOrErrorInAbc getType() {
		return WarnOrErrorInAbc.ERROR;
	}

}