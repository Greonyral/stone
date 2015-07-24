package stone.modules.fileEditor;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class Variable extends NameSchemeElement {

	class VariableDep extends NameSchemeElement {

		VariableDep(@SuppressWarnings("hiding") int[] idcs) {
			super(idcs);
		}

		@Override
		public final boolean equals(final Object o) {
			return Variable.this.equals(o);
		}

		@Override
		public final int hashCode() {
			return Variable.this.hashCode();
		}


		@Override
		public final String toString() {
			return Variable.this.s;
		}

		@Override
		final void print(final StringBuilder sb) {
			Variable.this.print(sb);
		}
	}

	final String s;

	private final static Set<WeakReference<Variable>> instances = new HashSet<>();

	static void clearAll() {
		synchronized (instances) {
			for (final WeakReference<Variable> wRef : instances) {
				final Variable v = wRef.get();
				if (v != null) {
					v.clear();
				}
			}
		}
	}

	private String value;

	Variable(@SuppressWarnings("hiding") final String s) {
		super(null);
		this.s = s;
		synchronized (instances) {
			instances.add(new WeakReference<>(this));
		}
	}

	@Override
	public final boolean equals(final Object o) {
		if (VariableDep.class.isInstance(o)) {
			return VariableDep.class.cast(o).equals(this);
		}
		return this == o;
	}

	@Override
	public final int hashCode() {
		return this.s.hashCode() ^ super.hashCode();
	}

	@Override
	public final String toString() {
		return this.s;
	}

	@Override
	protected final void finalize() {
		synchronized (instances) {
			for (final WeakReference<Variable> wRef : instances) {
				if ((wRef != null) && (wRef.get() == this)) {
					instances.remove(wRef);
					return;
				}
			}
		}
	}

	void clear() {
		this.value = null;
	}

	final NameSchemeElement dep(final int[] indices) {
		return new VariableDep(indices);
	}

	String getValue() {
		return this.value;
	}

	@Override
	final void print(final StringBuilder sb) {
		sb.append(this.value);
	}

	@SuppressWarnings("static-method")
	void value(@SuppressWarnings("unused") final Map<Integer, String> map) {
		return;
	}

	void value(@SuppressWarnings("hiding") final String value) {
		this.value = value;
	}
}
