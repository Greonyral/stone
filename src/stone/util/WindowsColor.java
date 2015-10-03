package stone.util;

class WindowsColor extends Color {
	
	private static int last = 0;
	
	public final static WindowsColor BLACK = new WindowsColor();
	public final static WindowsColor DARK_BLUE = new WindowsColor();
	public final static WindowsColor DARK_GREEN = new WindowsColor();
	public final static WindowsColor DARK_CYAN = new WindowsColor();
	public final static WindowsColor DARK_RED  = new WindowsColor();
	public final static WindowsColor DARK_PURPLE   = new WindowsColor();
	public final static WindowsColor BORWN  = new WindowsColor();
	
	public final static WindowsColor GRAY   = new WindowsColor();
	public final static WindowsColor DARK_GRAY   = new WindowsColor();
	
	public final static WindowsColor BLUE  = new WindowsColor();
	public final static WindowsColor GREEN  = new WindowsColor();
	public final static WindowsColor CYAN  = new WindowsColor();
	public final static WindowsColor RED   = new WindowsColor();
	public final static WindowsColor PURLBE   = new WindowsColor();
	public final static WindowsColor YELLOW   = new WindowsColor();
	public final static WindowsColor WHITE  = new WindowsColor();
	
	WindowsColor() {
		super(last++);
	}
}