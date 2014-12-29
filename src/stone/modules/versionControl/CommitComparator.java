package stone.modules.versionControl;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import stone.io.IOHandler;
import stone.util.Time;

/**
 * Compares to commits by time and if equal by their hash
 * 
 * @author Nelphindal
 */
public final class CommitComparator implements Comparator<RevCommit> {

	class CommitHistoryParser {
		private final RevWalk walk;
		private final IOHandler io;
		private final TreeSet<RevCommit> localList = new TreeSet<>(
				CommitComparator.this), remoteList = new TreeSet<>(
				CommitComparator.this);

		private RevCommit local, remote;
		private long localTime, remoteTime;
		private long diff, start, time;

		CommitHistoryParser(final RevWalk walk,
				final IOHandler io) {
			this.io = io;
			this.walk = walk;
		}

		final RevCommit getParent(final RevCommit commitLocal,
				final RevCommit commitRemote) throws MissingObjectException, IncorrectObjectTypeException, IOException {
			local = commitLocal;
			remote = commitRemote;
			localTime = local.getCommitTime();
			remoteTime = remote.getCommitTime();
			diff = Math.abs(localTime - remoteTime);
			io.startProgress("Merging " + Time.delta(diff * 1000), (int) diff);
			start = Math.max(localTime, remoteTime);
			while(true) {
				if (localTime == remoteTime) {
					if (remote.equals(local)) {
						io.endProgress();
						return remote;
					}
					
				}
				if (localTime > remoteTime) {
					for (final RevCommit c : local.getParents()) {
						walk.parseCommit(c);
						localList.add(c);
					}
					long timeBefore = localTime;
					local = localList.pollLast();
					localTime = local.getCommitTime();
					if (start - localTime > diff) {
						diff = start - localTime;
						io.startProgress("Merging " + Time.delta(diff * 1000), (int) diff);
						io.updateProgress((int) (start - timeBefore));
						time = 0;
					} else {
						if (time > 0) {
							io.updateProgress((int) (timeBefore - time));
						}
						time = timeBefore;
					}
				} else {
					for (final RevCommit c : remote.getParents()) {
						walk.parseCommit(c);
						remoteList.add(c);
					}
					long timeBefore = remoteTime;
					remote = remoteList.pollLast();
					remoteTime = remote.getCommitTime();
					if (start - remoteTime > diff) {
						diff = start - remoteTime;
						io.startProgress("Merging " + Time.delta(diff * 1000), (int) diff);
						io.updateProgress((int) (start - timeBefore));
						time = 0;
					} else {
						if (time > 0) {
							int delta = (int) (time - timeBefore);
							io.updateProgress((int) (time - timeBefore));
						}
						time = timeBefore;
					}
				}
			}
		}
	}

	private final static CommitComparator instance = new CommitComparator();

	private final CommitHistoryParser chp;

	private CommitComparator() {
		chp = null;
	}

	private CommitComparator(final RevWalk walk, final Git gitSession,
			final IOHandler io) {
		chp = new CommitHistoryParser(walk, io);
	}

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

	public final static CommitComparator init(final RevWalk walk,
			final Git gitSession, final IOHandler io) {
		return new CommitComparator(walk, gitSession, io);
	}

	public final RevCommit getParent(final RevCommit commitLocal,
			final RevCommit commitRemote) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		return chp.getParent(commitLocal, commitRemote);
	}

	public final static CommitComparator instance() {
		return instance;
	}

}
