package stone.modules.abcCreator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stone.modules.midiData.MidiInstrumentDropTarget;


class ParamMap {

	private final static DoubleMap<MidiInstrumentDropTarget, ParamSet, Track> instrument = new DoubleMap<>();

	private final static Map<Integer, Object> trackMap = new HashMap<>();
	private final static Set<Object> unassignedTracks = new HashSet<>();
	private final static Map<MidiInstrumentDropTarget, Track> instrumentToTrackMap = new HashMap<>();

	public final static Track parseParams(
			@SuppressWarnings("hiding") final MidiInstrumentDropTarget instrument,
			final String[] params) throws Exception {

		final Integer id;

		try {
			id = Integer.valueOf(params[0]);
		} catch (final Exception e) {
			return null;
		}
		final ParamSet paramSet = new ParamSet();
		for (int i = 1; i < params.length; i++) {
			if (params[i].equals("split")) {
				i += 2;
				continue;
			}

			final BruteParams<?> param = BruteParams.valueOf(params[i++]);
			if (param == null) {
				throw new Exception() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public final String toString() {
						return "Invalid param-key: " + param;
					}
				};
			}
			paramSet.put(param, params[i]);
		}
		final Track t = ParamMap.instrument.get(instrument, paramSet);
		if (t == null) {
			final Track orig = (Track) ParamMap.trackMap.get(id + 1);
			if (orig == null) {
				return null;
			}
			if (ParamMap.unassignedTracks.remove(orig)) {
				ParamMap.instrument.put(instrument, paramSet, orig);
				ParamMap.instrumentToTrackMap.put(instrument, orig);
				orig.getTargetContainer().removeAllLinks(orig);
				orig.clearTargets();
				paramSet.adeptValues(orig, instrument);
				return orig;
			}
			if (orig == ParamMap.instrumentToTrackMap.get(instrument)) {
				ParamMap.instrument.put(instrument, paramSet, orig);
				paramSet.adeptValues(orig, instrument);
				return orig;
			}
			final Track clone = orig.clone();
			ParamMap.instrument.put(instrument, paramSet, clone);
			ParamMap.instrumentToTrackMap.put(instrument, clone);
			paramSet.adeptValues(clone, instrument);
			return clone;
		}
		paramSet.adeptValues(t, instrument);
		return t;
	}

	public static final void setTracks(
			@SuppressWarnings("hiding") final Map<Integer, ?> trackMap) {
		ParamMap.trackMap.clear();
		for (final Map.Entry<Integer, ?> entry : trackMap.entrySet()) {
			ParamMap.trackMap.put(
					Integer.valueOf(entry.getKey().intValue() + 1),
					entry.getValue());
		}
		ParamMap.unassignedTracks.clear();
		ParamMap.unassignedTracks.addAll(trackMap.values());
	}


	private ParamMap() {
	}

	@Override
	public final String toString() {
		return null;
	}
}