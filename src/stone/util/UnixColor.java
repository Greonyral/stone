package stone.util;

class UnixColor extends Color {

	private static int last;

	public final static UnixColor BLACK = new UnixColor(30);
	public final static UnixColor DARK_RED = new UnixColor();
	public final static UnixColor DARK_GREEN = new UnixColor();
	public final static UnixColor BROWN = new UnixColor();
	public final static UnixColor DARK_YELLOW = new UnixColor();
	public final static UnixColor DARK_BLUE = new UnixColor();
	public final static UnixColor DARK_PURPLE = new UnixColor();
	public final static UnixColor DARK_CYAN = new UnixColor();
	public final static UnixColor GRAY = new UnixColor();

	public final static UnixColor DARK_GRAY = new UnixColor(90);
	public final static UnixColor RED = new UnixColor();
	public final static UnixColor GREEN = new UnixColor();
	public final static UnixColor YELLOW = new UnixColor();
	public final static UnixColor BLUE = new UnixColor();
	public final static UnixColor PURPLE = new UnixColor();
	public final static UnixColor CYAN = new UnixColor();
	public final static UnixColor WHITE = new UnixColor();


	private UnixColor(@SuppressWarnings("hiding") int value) {
		super(value);
		last = value;
	}

	private UnixColor() {
		this(last + 1);
	}
	
	
	
}