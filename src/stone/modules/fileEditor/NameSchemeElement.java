package stone.modules.fileEditor;

abstract class NameSchemeElement {

	final int[] idcs;

	NameSchemeElement(int[] idcs) {
		this.idcs = idcs;
	}

	abstract void print(StringBuilder sb);

	void print(final StringBuilder sb, int track) {
		if ((this.idcs == null) || (this.idcs.length == 0)) {
			print(sb);
		} else {
			for (final int i : this.idcs) {
				if (i == track) {
					print(sb);
					return;
				}
			}
		}
	}
}