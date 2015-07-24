package stone.modules.songData;

import stone.util.Path;


final class NoXLineInAbc extends AbtractEoWInAbc {

	@SuppressWarnings("hiding")
	public NoXLineInAbc(final Path song, int line) {
		super(song, line);
	}

	@Override
	final String getDetail() {
		return "Missing X: line";
	}

	@Override
	final WarnOrErrorInAbc getType() {
		return WarnOrErrorInAbc.ERROR;
	}
}
