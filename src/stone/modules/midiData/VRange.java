package stone.modules.midiData;

import java.awt.Color;
import java.util.Iterator;

class VRange {

	private final static int R_MASK = 0x00ff0000;
	private final static int G_MASK = 0x0000ff00;
	private final static int B_MASK = 0x000000ff;

	private final int color_a, color_b;
	private final double distance;
	private final int low, high;

	VRange(int lowerBound, int upperBound, final Iterator<Color> c,
			final VRange previous) {
		if (previous == null) {
			this.color_a = c.next().getRGB();
		} else {
			this.color_a = previous.color_b;
		}
		this.color_b = c.next().getRGB();
		this.low = lowerBound;
		this.high = upperBound;
		this.distance = this.high - this.low - 1;
	}

	final Color getColor(int v) {
		final double ratio = (v - this.low) / this.distance;
		final int r, g, b;
		final int rgb;

		r = VRange.R_MASK
				& ((int) ((this.color_a & VRange.R_MASK) * (1 - ratio)) + (int) ((this.color_b & VRange.R_MASK) * ratio));
		g = VRange.G_MASK
				& ((int) ((this.color_a & VRange.G_MASK) * (1 - ratio)) + (int) ((this.color_b & VRange.G_MASK) * ratio));
		b = VRange.B_MASK
				& ((int) ((this.color_a & VRange.B_MASK) * (1 - ratio)) + (int) ((this.color_b & VRange.B_MASK) * ratio));

		rgb = 0xff000000 | r | g | b;
		return new Color(rgb);
	}

}