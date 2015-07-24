package stone.modules.fileEditor;

class Id {
	final Integer id;
	final char subid;
	final String string;

	Id(final String initstring, @SuppressWarnings("hiding") int subid) {
		this.id = Integer.parseInt(initstring);
		this.subid = (char) subid;
		if (subid != 0) {
			this.string = initstring + (char) subid;
		} else {
			this.string = initstring;
		}
	}

	@Override
	public String toString() {
		return this.string;
	}
}