package stone.modules.fileEditor;

class Id {
	final Integer id;
	final char subid;
	final String string;
	
	Id(final String initstring, int subid) {
		id = Integer.parseInt(initstring);
		this.subid= (char) subid;
		if (subid != 0) {
			string = initstring + (char) subid;
		} else
			string = initstring;
	}
	
	@Override
	public String toString() {
		return string;
	}
}