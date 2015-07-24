package stone.modules.songData;

import stone.util.Path;


final class MultipleTLinesInAbc extends AbtractEoWInAbc {

	MultipleTLinesInAbc(@SuppressWarnings("hiding") int line, final Path path) {
		super(path, line);
	}

	@Override
	final String getDetail() {
		return "Has succeeding T: line";
	}

	@Override
	final WarnOrErrorInAbc getType() {
		return WarnOrErrorInAbc.WARN;
	}
}