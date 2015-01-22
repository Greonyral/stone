package stone.modules.abcCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import stone.modules.midiData.MidiInstrument;
import stone.modules.midiData.MidiInstrumentDropTarget;


final class MapLoadingThread implements Runnable {

	/**
	 * 
	 */
	private final AbcMapPlugin abcMapPlugin;
	private final File mapToLoad;

	MapLoadingThread(final AbcMapPlugin abcMapPlugin, final File mapToLoad) {
		this.abcMapPlugin = abcMapPlugin;
		this.mapToLoad = mapToLoad;
	}

	@Override
	public final void run() {

		final List<MidiInstrumentDropTarget> targetList =
				new ArrayList<>();

		class LoadedMapEntry implements DndPluginCaller.LoadedMapEntry {

			private boolean error;
			private MidiInstrumentDropTarget activeInstrument;

			@Override
			public final void addEntry(final String string) {
				// parse of "begin miditrack ..."
				// called after addPart(String)
				final Track t;
				final String[] s = string.split(" ");

				try {
					t = ParamMap.parseParams(activeInstrument, s);
				} catch (final Exception e1) {
					e1.printStackTrace();
					error = true;
					return;
				}
				abcMapPlugin.link(t, activeInstrument);
			}

			@Override
			public final void addPart(final String string) {
				// parse of "instrument ..."
				final String[] s = string.split(" ");
				final MidiInstrument m = MidiInstrument.valueOf(s[0]);
				if (m == null) {
					setError();
					return;
				}
				activeInstrument = m.createNewTarget();
				targetList.add(activeInstrument);
				if (s.length == 2) {
					try {
						activeInstrument.setParam("map", Integer
								.valueOf(s[1]));
					} catch (final Exception e) {
						error = true;
					}
				}
			}

			@Override
			public final boolean error() {
				return error;
			}

			@Override
			public final void setError() {
				error = true;
			}
		}

		final LoadedMapEntry loader = new LoadedMapEntry();

		abcMapPlugin.caller.loadMap(mapToLoad, loader);
		if (loader.error) {
			abcMapPlugin.state.label.setText("Loading map failed");
		} else {
			abcMapPlugin.state.loadingMap = false;
			abcMapPlugin.state.label
					.setText("Parsing completed - Updating GUI ...");
			final Set<Track> clonesAdded = new HashSet<>();
			for (final MidiInstrumentDropTarget t : targetList) {
				abcMapPlugin.addToCenter(t);
				for (final DragObject<JPanel, JPanel, JPanel> track : t) {
					if (track.isAlias()) {
						clonesAdded.add((Track) track);
					}
				}
			}
			for (final Track clone : clonesAdded) {
				abcMapPlugin.initObject(clone);
			}
			abcMapPlugin.state.label.setText("Loading map completed");
		}
		synchronized (abcMapPlugin.state) {
			abcMapPlugin.state.upToDate = false;
			abcMapPlugin.state.running = false;
			assert abcMapPlugin.state.loadingMap == true; // locks future loads
			abcMapPlugin.state.notifyAll();
		}
	}
}