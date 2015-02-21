package stone.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class PathReference extends WeakReference<Path> {

	private static ReferenceQueue<Path> queue = new ReferenceQueue<>();

	public PathReference(Path referent) {
		super(referent, queue);
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
