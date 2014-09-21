package stone.modules.fileEditor;

class Variable extends NameSchemeElement {

	class VariableDep extends NameSchemeElement {

		VariableDep(int[] idcs) {
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
			return s;
		}

		@Override
		final void print(final StringBuilder sb) {
			Variable.this.print(sb);
		}
	}

	final String s;

	private String value;

	Variable(final String s) {
		super(null);
		this.s = s;
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
		return s.hashCode() ^ super.hashCode();
	}

	@Override
	public final String toString() {
		return s;
	}

	final void clear() {
		value = null;
	}

	final NameSchemeElement dep(final int[] indices) {
		return new VariableDep(indices);
	}

	@Override
	final void print(final StringBuilder sb) {
		sb.append(value);
	}

	final void value(final String value) {
		this.value = value;
	}
}
