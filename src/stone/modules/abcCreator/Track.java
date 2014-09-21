package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
class Track implements Comparable<Track>,
		DragObject<JPanel, JPanel, JPanel> {

	public final static BruteParams<?>[] getParams() {
		return BruteParams.valuesLocal();
	}

	private final int idBrute;

	private final String name;
	private final Set<DropTarget<JPanel, JPanel, JPanel>> targets =
			new HashSet<>();
	private final Set<Track> aliases;
	private final Track original;

	private final JPanel panel = new JPanel();
	private DropTargetContainer<JPanel, JPanel, JPanel> c;

	/**
	 * Creates a new track
	 * 
	 * @param idInMidi
	 *            as in the midi
	 * @param idInBrute
	 *            subsequent number from 2
	 * @param name
	 *            as in the midi
	 */
	public Track(int idInBrute, int idInMidi, final String name) {
		idBrute = idInBrute;
		if (name == null) {
			this.name = "<Track " + idInMidi + ">";
		} else {
			this.name = name;
		}
		aliases = new HashSet<>();
		original = null;

		panel.setLayout(new BorderLayout());
	}

	private Track(final Track track) {
		idBrute = track.idBrute;
		name = track.name;
		aliases = null;
		original = track;
		track.aliases.add(this);
		panel.setLayout(new BorderLayout());
	}

	/** */
	@Override
	public final boolean addTarget(
			final DropTarget<JPanel, JPanel, JPanel> target) {
		if (c != target.getContainer()) {
			c = target.getContainer();
		}
		if (targets.size() == 4) {
			return false;
		}
		targets.add(target);
		return true;
	}

	/** */
	@Override
	public Iterator<DropTarget<JPanel, JPanel, JPanel>> clearTargets() {
		final Iterator<DropTarget<JPanel, JPanel, JPanel>> t =
				new HashSet<>(targets).iterator();
		targets.clear();
		BruteParams.clear();
		return t;
	}

	/**
	 * @return an alias of this track
	 */
	@Override
	public final Track clone() {
		if (original != null) {
			return original.clone();
		}
		return new Track(this);
	}

	/**
	 * Compares the id of this track with the id of the other Track o
	 */
	@Override
	public final int compareTo(final Track o) {
		return idBrute - o.idBrute;
	}

	/** */
	@Override
	public final void forgetAlias() {
		original.aliases.remove(this);
	}

	/**  */
	@Override
	public final DragObject<JPanel, JPanel, JPanel>[] getAliases() {
		if (original != null) {
			return original.getAliases();
		}
		return aliases.toArray(new Track[aliases.size()]);

	}

	@Override
	public final JPanel getDisplayableComponent() {
		return panel;
	}

	/**
	 * @return the id used for BruTE
	 */
	@Override
	public final int getId() {
		return idBrute;
	}

	/** */
	@Override
	public final String getName() {
		return name;
	}

	/** */
	@Override
	public final DragObject<JPanel, JPanel, JPanel> getOriginal() {
		return original;
	}

	/** */
	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel>
			getTargetContainer() {
		return c;
	}

	@Override
	public final int getTargets() {
		return targets.size();
	}


	/** */
	@Override
	public final boolean isAlias() {
		return aliases == null;
	}

	/** */
	@Override
	public final Iterator<DropTarget<JPanel, JPanel, JPanel>> iterator() {
		return targets.iterator();
	}


	/**
	 * Returns a string representing this track. Format is "id name [targets]
	 */
	@Override
	public final String toString() {
		return idBrute + " " + name + " " + targets;
	}


}
