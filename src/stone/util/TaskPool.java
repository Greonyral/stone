package stone.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import stone.MasterThread;
import stone.StartupContainer;

/**
 * Simple thread management.
 * 
 * @author Nelphindal
 */
public class TaskPool {

	private final MasterThread master;

	private final ArrayDeque<Runnable> taskPool = new ArrayDeque<>();

	private final static int NUM_CPUS = Runtime.getRuntime()
			.availableProcessors();

	private boolean closed = false;

	private int runningTasks = 0;

	private final Set<Thread> runningTaskList = new HashSet<>();

	/**
	 * Creates a new task Pool
	 * 
	 * @param os
	 *            the StartupContainer for initialization
	 */
	public TaskPool(final StartupContainer os) {
		master = new MasterThread(os, this);
		Debug.print("%d available CPUs\n", NUM_CPUS);
	}

	/**
	 * Adds a new task to the pool to be executed.
	 * 
	 * @param task
	 */
	public final void addTask(final Runnable task) {
		synchronized (taskPool) {
			taskPool.add(task);
			taskPool.notify();
		}
	}

	@Deprecated
	public final void addTaskForAll(final Runnable... task) {
		for (final Runnable t : task)
			addTaskForAll(t);
	}


	/**
	 * Adds one task to the pool to be executed. Each task will be executed by
	 * all available WorkerThreads for the pool. The next task will be executed
	 * after the previous task has been completed with all threads.
	 * 
	 * @param tasks
	 */
	public final void addTaskForAll(final Runnable task) {
		addTaskForAll(task, 100);
	}

	/**
	 * Adds one task to the pool to be executed. Each task will be executed by
	 * set percent of available WorkerThreads for the pool. The next task will
	 * be executed after the previous task has been completed with all threads.
	 * 
	 * @param tasks
	 */
	public final void addTaskForAll(final Runnable task, int percent) {
		final int n = NUM_CPUS * Math.max(1, Math.min(100, percent) / 100);
		synchronized (taskPool) {
			for (int i = 0; i < n; i++) {
				taskPool.add(task);
			}
			taskPool.notifyAll();
		}
	}

	/**
	 * Closes this pool. All waiting tasks will be woken up.
	 */
	public final void close() {
		synchronized (taskPool) {
			if (closed) {
				return;
			}
			closed = true;
			taskPool.notifyAll();
			synchronized (runningTaskList) {
				for (final Thread t : runningTaskList)
					t.interrupt();
			}
			while (runningTasks > 0) {
				try {
					taskPool.wait();
				} catch (final InterruptedException e) {
					master.interrupt();
				}
			}
		}
	}

	/**
	 * @return The thread which is interrupted if any operation catches an
	 *         InterruptedException
	 */
	public final MasterThread getMaster() {
		return master;
	}

	/**
	 * Forks and starts the master thread.
	 */
	public final Runnable runMaster() {
		master.setName("master");
		master.start();
		final Runnable workerRun = new Runnable() {

			@Override
			public void run() {
				while (true) {
					if (!runTask()) {
						return;
					}
				}
			}
		};
		final int nMax = Math.max(NUM_CPUS, 2);
		for (int n = 0; ++n < nMax;) {
			final Thread t = new Thread(workerRun, "Worker-" + n);
			t.start();
		}
		return workerRun;
	}

	/**
	 * Waits for the queue to be emptied and all tasks finished being executed.
	 * The master thread has to be checked if it was not interrupted while
	 * waiting.
	 */
	public final void waitForTasks() {
		while ((runningTasks > 0) || !taskPool.isEmpty()) {
			synchronized (taskPool) {
				try {
					taskPool.wait();
				} catch (final InterruptedException e) {
					master.interrupt();
				}
			}
		}
	}

	/**
	 * Waits as long the task-queue of this pool is empty, and executes the next
	 * task.
	 * 
	 * @return <i>true</i> if a task has been executed, <i>false</i> if the pool
	 *         has been closed while waiting
	 */
	final boolean runTask() {
		final Runnable t;
		synchronized (taskPool) {
			while (taskPool.isEmpty()) {
				if (closed) {
					return false;
				}
				try {
					taskPool.wait();
				} catch (final InterruptedException e) {
					master.interrupt();
					return false;
				}
			}
			++runningTasks;
			t = taskPool.remove();
		}
		synchronized (runningTaskList) {
			runningTaskList.add(Thread.currentThread());
		}
		try {
			t.run();
		} catch (final Exception e) {
			Throwable tr = e;
			while (tr.getCause() != null)
				tr = tr.getCause();
			if (!InterruptedException.class.isAssignableFrom(tr.getClass()))
				e.printStackTrace();
		} finally {
			synchronized (taskPool) {
				taskPool.notifyAll();
				--runningTasks;
				runningTaskList.remove(Thread.currentThread());
			}
		}
		return !master.isInterrupted();
	}
}
