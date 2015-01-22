package stone.modules.songData;

import java.io.IOException;
import java.util.Map;

import stone.util.Path;

// uses multiple files to profit from multi threading
class Deserializer_0 extends Deserializer implements MTDeserializer {

	protected Deserializer_0(final SongDataContainer sdc) {
		super(sdc);
	}

	@Override
	protected void deserialize_() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	protected final void generateStream(final SongData data) {
		// TODO Auto-generated method stub
		data.serialize(this);
	}

	@Override
	protected final void abort_() {
		// TODO Auto-generated method stub
	}

	@Override
	protected final void finish_() {
		// TODO Auto-generated method stub
	}

	@Override
	protected final void crawlDone_() {
		// TODO Auto-generated method stub
	}

	public Runnable getDeserialTask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final void serialize(final SerializeConainer sc, final Path song) {
		// TODO Auto-generated method stub

	}

	@Override
	public final void serialize(final SerializeConainer sc, long modDate) {
		// TODO Auto-generated method stub

	}

	@Override
	public final void serialize(final SerializeConainer sc,
			final Map<Integer, String> voices) {
		// TODO Auto-generated method stub

	}

	@Override
	public final SerializeConainer createSerializeConainer() {
		// TODO Auto-generated method stub
		return null;
	}
}
