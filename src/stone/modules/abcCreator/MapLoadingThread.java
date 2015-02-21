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

		final List<MidiInstrumentDropTarget> targetList = new ArrayList<>();

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
					t = ParamMap.parseParams(this.activeInstrument, s);
				} catch (final Exception e1) {
					e1.printStackTrace();
					this.error = true;
					return;
				}
				MapLoadingThread.this.abcMapPlugin.link(t,
						this.activeInstrument);
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
				this.activeInstrument = m.createNewTarget();
				targetList.add(this.activeInstrument);
				if (s.length == 2) {
					try {
						this.activeInstrument.setParam("map",
								Integer.valueOf(s[1]));
					} catch (final Exception e) {
						this.error = true;
					}
				}
			}

			@Override
			public final boolean error() {
				return this.error;
			}

			@Override
			public final void setError() {
				this.error = true;
			}
		}

		final LoadedMapEntry loader = new LoadedMapEntry();

		this.abcMapPlugin.caller.loadMap(this.mapToLoad, loader);
		if (loader.error) {
			this.abcMapPlugin.state.label.setText("Loading map failed");
		} else {
			this.abcMapPlugin.state.loadingMap = false;
			this.abcMapPlugin.state.label
					.setText("Parsing completed - Updating GUI ...");
			final Set<Track> clonesAdded = new HashSet<>();
			for (final MidiInstrumentDropTarget t : targetList) {
				this.abcMapPlugin.addToCenter(t);
				for (final DragObject<JPanel, JPanel, JPanel> track : t) {
					if (track.isAlias()) {
						clonesAdded.add((Track) track);
					}
				}
			}
			for (final Track clone : clonesAdded) {
				this.abcMapPlugin.initObject(clone);
			}
			this.abcMapPlugin.state.label.setText("Loading map completed");
		}
		synchronized (this.abcMapPlugin.state) {
			this.abcMapPlugin.state.upToDate = false;
			this.abcMapPlugin.state.running = false;
			assert this.abcMapPlugin.state.loadingMap == true; // locks future
																// loads
			this.abcMapPlugin.state.notifyAll();
		}
	}
}