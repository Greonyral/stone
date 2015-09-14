package stone.modules.versionControl;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import stone.io.IOHandler;
import stone.util.Debug;
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

		@SuppressWarnings("hiding")
		CommitHistoryParser(final RevWalk walk, final IOHandler io) {
			this.io = io;
			this.walk = walk;
		}

		final RevCommit getParent(final RevCommit commitLocal,
				final RevCommit commitRemote) throws MissingObjectException,
				IncorrectObjectTypeException, IOException {
			this.local = commitLocal;
			this.remote = commitRemote;
			this.localTime = this.local.getCommitTime();
			this.remoteTime = this.remote.getCommitTime();
			this.diff = Math.abs(this.localTime - this.remoteTime);
			this.io.startProgress("Merging " + Time.delta(this.diff * 1000),
					(int) this.diff);
			this.start = Math.max(this.localTime, this.remoteTime);
			while (true) {
				if (this.localTime == this.remoteTime) {
					if (this.remote.equals(this.local)) {
						this.io.endProgress("Search done");
						return this.remote;
					}

				}
				if (this.localTime > this.remoteTime) {
					for (final RevCommit c : this.local.getParents()) {
						this.walk.parseCommit(c);
						this.localList.add(c);
					}
					final long timeBefore = this.localTime;
					this.local = this.localList.pollLast();
					this.localTime = this.local.getCommitTime();
					if ((this.start - this.localTime) > this.diff) {
						this.diff = this.start - this.localTime;
						this.io.startProgress(
								"Merging " + Time.delta(this.diff * 1000),
								(int) this.diff);
						this.io.updateProgress((int) (this.start - timeBefore));
						this.time = 0;
					} else {
						if (this.time > 0) {
							this.io.updateProgress((int) (timeBefore - this.time));
						}
						this.time = timeBefore;
					}
				} else {
					for (final RevCommit c : this.remote.getParents()) {
						this.walk.parseCommit(c);
						this.remoteList.add(c);
					}
					final long timeBefore = this.remoteTime;
					if (this.remoteList.isEmpty()) {
						this.io.endProgress("Search done");
						Debug.print("rewritten history\n");
						return null;
					}
					this.remote = this.remoteList.pollLast();
					this.remoteTime = this.remote.getCommitTime();
					if ((this.start - this.remoteTime) > this.diff) {
						this.diff = this.start - this.remoteTime;
						this.io.startProgress(
								"Merging " + Time.delta(this.diff * 1000),
								(int) this.diff);
						this.io.updateProgress((int) (this.start - timeBefore));
						this.time = 0;
					} else {
						if (this.time > 0) {
							this.io.updateProgress((int) (this.time - timeBefore));
						}
						this.time = timeBefore;
					}
				}
			}
		}
	}

	private final static CommitComparator instance = new CommitComparator();

	/**
	 * Creates a new comparator for commits
	 * 
	 * @param walk
	 *            -
	 * @param io
	 *            -
	 * @return the creates instance
	 */
	public final static CommitComparator init(final RevWalk walk,
			final IOHandler io) {
		return new CommitComparator(walk, io);
	}

	/**
	 * @return previsouly created instance
	 */
	public final static CommitComparator instance() {
		return instance;
	}

	private final CommitHistoryParser chp;

	private CommitComparator() {
		this.chp = null;
	}

	private CommitComparator(final RevWalk walk, final IOHandler io) {
		this.chp = new CommitHistoryParser(walk, io);
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

	/**
	 * @param commitLocal
	 *            one commit
	 * @param commitRemote
	 *            another commit
	 * @return the latest commit in both trees of given commits
	 * @throws MissingObjectException
	 *             -
	 * @throws IncorrectObjectTypeException
	 *             -
	 * @throws IOException
	 *             -
	 */
	public final RevCommit getParent(final RevCommit commitLocal,
			final RevCommit commitRemote) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		return this.chp.getParent(commitLocal, commitRemote);
	}

}
