package stone.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class PathReference extends WeakReference<Path> {
	
	private static ReferenceQueue<Path> queue = new ReferenceQueue<>();
	
	public PathReference(Path referent) {
		super(referent, queue);
	}
	
	  public boolean enqueue() {
		  return super.enqueue();
	  }
	  
	  public Path get() {
		  return isEnqueued() ? null : super.get();
	  }
	  
	  public boolean isEnqueued() {
		  return super.isEnqueued();
	  }

}
