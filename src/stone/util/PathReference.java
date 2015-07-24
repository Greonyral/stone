package stone.util;

import java.lang.ref.WeakReference;

/**
 * {@link WeakReference} refering to a {@link Path} object.
 * @author Nelphindal
 *
 */
public class PathReference extends WeakReference<Path> {

	/**
	 * Creates a new [@link PathReference}
	 * @param referent {@link Path} object the new weak reference will refer to
	 */
	public PathReference(final Path referent) {
		super(referent);
	}

	@Override
	public boolean enqueue() {
		return super.enqueue();
	}

	@Override
	public Path get() {
		return isEnqueued() ? null : super.get();
	}

	@Override
	public boolean isEnqueued() {
		return super.isEnqueued();
	}

}
