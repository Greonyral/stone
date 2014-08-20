package stone.modules.abcCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import stone.modules.midiData.MidiInstrument;

final class MapLoadingThread extends Thread {

	/**
	 * 
	 */
	private final AbcMapPlugin abcMapPlugin;
	private final File mapToLoad;
	final Map<String, Track> idToTrackMap;

	MapLoadingThread(AbcMapPlugin abcMapPlugin, File mapToLoad, Map<String, Track> idToTrackMap) {
		this.abcMapPlugin = abcMapPlugin;
		this.mapToLoad = mapToLoad;
		this.idToTrackMap = idToTrackMap;
	}

	@Override
	public final void run() {

		final List<DropTarget<JPanel, JPanel, JPanel>> targetList =
				new ArrayList<>();
		final Map<Track, DropTargetContainer<JPanel, JPanel, JPanel>> cloneDeciderMap =
				new HashMap<>();
		final Map<Track, Map<DropTargetContainer<JPanel, JPanel, JPanel>, Track>> cloneMap =
				new HashMap<>();

		class LoadedMapEntry implements DndPluginCaller.LoadedMapEntry {

			boolean error;
			private DropTarget<JPanel, JPanel, JPanel> target;

			@Override
			public final void addEntry(final String string) {
				final String[] s = string.split(" ");
				final Track t;
				synchronized (idToTrackMap) {
					t = idToTrackMap.get(s[0]);
				}
				final DropTargetContainer<JPanel, JPanel, JPanel> m =
						cloneDeciderMap.get(t);
				final Track o;
				if (m == null) {
					cloneDeciderMap.put(t, target.getContainer());
					t.getTargetContainer().removeAllLinks(t);
					t.clearTargets();
					// initTarget(target);
					o = t;
				} else if (m != target.getContainer()) {
					final Map<DropTargetContainer<JPanel, JPanel, JPanel>, Track> cloneEntry =
							cloneMap.get(t);
					if (cloneEntry == null) {
						cloneMap.put(
								t,
								new HashMap<DropTargetContainer<JPanel, JPanel, JPanel>, Track>());
					}
					final Track tClone = cloneMap.get(t).get(m);
					if (tClone == null) {
						final Track clone = t.clone();
						cloneMap.get(t).put(m, clone);
						MapLoadingThread.this.abcMapPlugin.panelLeft.add(clone.getDisplayableComponent());
						MapLoadingThread.this.abcMapPlugin.panelLeft.validate();
						MapLoadingThread.this.abcMapPlugin.initObject(clone);
						// initTarget(target);
					}
					o = cloneMap.get(t).get(m);
				} else {
					o = t;
				}
				MapLoadingThread.this.abcMapPlugin.link(o, target);
				for (int i = 1; i < s.length;) {
					if (s[i].equals("split")) {
						i += 3;
					} else {
						try {
							o.setParam(BruteParams.valueOf(s[i]),
									target, Integer.parseInt(s[i + 1]));
							i += 2;
						} catch (final Exception e) {
							error = true;
						}
					}
				}
			}

			@Override
			public final void addPart(final String string) {
				final String[] s = string.split(" ");
				final MidiInstrument m = MidiInstrument.valueOf(s[0]);
				target = m.createNewTarget();
				targetList.add(target);
				if (s.length == 2) {
					try {
						target.setParam("map", Integer.parseInt(s[1]));
					} catch (final Exception e) {
						error = true;
					}
				}
			}

			@Override
			public final void error() {
				error = true;
			}
		}

		final LoadedMapEntry loader = new LoadedMapEntry();

		this.abcMapPlugin.caller.loadMap(mapToLoad, loader);
		if (loader.error) {
			this.abcMapPlugin.state.label.setText("Loading map failed");
		} else {
			this.abcMapPlugin.state.loadingMap = false;
			this.abcMapPlugin.state.label
					.setText("Parsing completed - Updating GUI ...");
			for (final DropTarget<JPanel, JPanel, JPanel> t : targetList) {
				t.getDisplayableComponent();
				this.abcMapPlugin.addToCenter(t);
			}
			this.abcMapPlugin.state.label.setText("Loading map completed");
		}
		synchronized (this.abcMapPlugin.state) {
			this.abcMapPlugin.state.upToDate = false;
			this.abcMapPlugin.state.running = false;
			this.abcMapPlugin.state.notifyAll();
		}
	}
}