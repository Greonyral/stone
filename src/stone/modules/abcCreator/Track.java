package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
class Track implements Comparable<Track>, DragObject<JPanel, JPanel, JPanel> {

	public final static BruteParams<?>[] getParams() {
		return BruteParams.valuesLocal();
	}

	private final int idBrute;

	private final String name;
	private final Set<DropTarget<JPanel, JPanel, JPanel>> targets = new HashSet<>();
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
	public Track(int idInBrute, int idInMidi,
			@SuppressWarnings("hiding") final String name) {
		this.idBrute = idInBrute;
		if (name == null) {
			this.name = "<Track " + idInMidi + ">";
		} else {
			this.name = name;
		}
		this.aliases = new HashSet<>();
		this.original = null;

		this.panel.setLayout(new BorderLayout());
	}

	private Track(final Track track) {
		this.idBrute = track.idBrute;
		this.name = track.name;
		this.aliases = null;
		this.original = track;
		track.aliases.add(this);
		this.panel.setLayout(new BorderLayout());
	}

	/** */
	@Override
	public final boolean addTarget(
			final DropTarget<JPanel, JPanel, JPanel> target) {
		if (this.c != target.getContainer()) {
			this.c = target.getContainer();
		}
		if (this.targets.size() == 4) {
			return false;
		}
		this.targets.add(target);
		return true;
	}

	/** */
	@Override
	public Iterator<DropTarget<JPanel, JPanel, JPanel>> clearTargets() {
		final Iterator<DropTarget<JPanel, JPanel, JPanel>> t = new HashSet<>(
				this.targets).iterator();
		this.targets.clear();
		BruteParams.clear();
		return t;
	}

	/**
	 * @return an alias of this track
	 */
	@Override
	public final Track clone() {
		if (this.original != null) {
			return this.original.clone();
		}
		return new Track(this);
	}

	/**
	 * Compares the id of this track with the id of the other Track o
	 */
	@Override
	public final int compareTo(final Track o) {
		return this.idBrute - o.idBrute;
	}

	/** */
	@Override
	public final void forgetAlias() {
		this.original.aliases.remove(this);
	}

	/**  */
	@Override
	public final DragObject<JPanel, JPanel, JPanel>[] getAliases() {
		if (this.original != null) {
			return this.original.getAliases();
		}
		return this.aliases.toArray(new Track[this.aliases.size()]);

	}

	@Override
	public final JPanel getDisplayableComponent() {
		return this.panel;
	}

	/**
	 * @return the id used for BruTE
	 */
	@Override
	public final int getId() {
		return this.idBrute;
	}

	/** */
	@Override
	public final String getName() {
		return this.name;
	}

	/** */
	@Override
	public final DragObject<JPanel, JPanel, JPanel> getOriginal() {
		return this.original;
	}

	/** */
	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel> getTargetContainer() {
		return this.c;
	}

	@Override
	public final int getTargets() {
		return this.targets.size();
	}


	/** */
	@Override
	public final boolean isAlias() {
		return this.aliases == null;
	}

	/** */
	@Override
	public final Iterator<DropTarget<JPanel, JPanel, JPanel>> iterator() {
		return this.targets.iterator();
	}


	/**
	 * Returns a string representing this track. Format is "id name [targets]
	 */
	@Override
	public final String toString() {
		return this.idBrute + " " + this.name + " " + this.targets;
	}


}
