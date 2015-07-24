package stone.modules.songData;

import stone.util.Path;


final class LongTitleInAbc extends AbtractEoWInAbc {

	@SuppressWarnings("hiding")
	public LongTitleInAbc(final Path song, int line) {
		super(song, line);
	}

	@Override
	final String getDetail() {
		return "Title is to long";
	}

	@Override
	final WarnOrErrorInAbc getType() {
		return WarnOrErrorInAbc.WARN;
	}
}
