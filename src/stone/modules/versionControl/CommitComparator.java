package stone.modules.versionControl;

import java.util.Comparator;

import org.eclipse.jgit.revwalk.RevCommit;


/**
 * Compares to commits by time and if equal by their hash
 * 
 * @author Nelphindal
 */
public final class CommitComparator implements Comparator<RevCommit> {

	/**
	 * Compares to commit by there commit time. If this is equal the commits are
	 * compared themselves with each other.
	 */
	@Override
	public int compare(final RevCommit o1, final RevCommit o2) {
		final int delta = o1.getCommitTime() - o2.getCommitTime();
		if (delta == 0) {
			return o1.compareTo(o2);
		}
		return delta;
	}

}
