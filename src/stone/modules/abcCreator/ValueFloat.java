package stone.modules.abcCreator;

import java.awt.Container;
import java.awt.Dimension;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class ValueFloat extends Value<Double> {

	class SliderListener implements ChangeListener {

		SliderListener() {
		}

		@Override
		public final void stateChanged(final ChangeEvent e) {
			ValueFloat.this.value = ValueFloat.this.slider.getValue()
					/ ValueFloat.this.factor;
			System.out.printf("min: %d value: %d max: %d\n",
					Integer.valueOf(ValueFloat.this.min),
					Integer.valueOf(ValueFloat.this.slider.getValue()),
					Integer.valueOf(ValueFloat.this.max));
			ValueFloat.this.label.setText(String.format("%s %.2f",
					ValueFloat.this.value == 0 ? " "
							: ValueFloat.this.value > 0 ? "+" : "-", Double
							.valueOf(Math.abs(ValueFloat.this.value))));
		}
	}

	/**
	 * 
	 */
	private final BruteParams<Double> bruteParams;
	double value;
	private final int ticks, step;
	final int min;
	final int max;

	private final double factor = 1000.0;
	private final DragObject<Container, Container, Container> object;
	private final DropTarget<Container, Container, Container> target;

	@SuppressWarnings("hiding")
	ValueFloat(BruteParams<Double> bruteParams, double initValue, double step,
			double ticks) {
		this.bruteParams = bruteParams;
		this.value = (int) (initValue * this.factor);
		this.min = (int) ((initValue - step) * this.factor);
		this.max = (int) ((initValue + step) * this.factor);
		this.ticks = (int) (ticks * this.factor);
		this.step = (int) (step * this.factor);
		this.object = null;
		this.target = null;
	}

	@Override
	public final synchronized void display() {
		this.slider.setMinimum(this.min);
		this.slider.setMaximum(this.max);
		this.slider.setValue((int) (this.value * this.factor));
		this.slider.setPaintTicks(true);
		this.slider.setPaintLabels(true);
		this.slider.setMajorTickSpacing(this.step);
		this.slider.setMinorTickSpacing(this.ticks);
		this.label.setText(String.format("%s %.2f", this.value == 0 ? " "
				: this.value > 0 ? "+" : "-", Double.valueOf(Math
				.abs(this.value) / this.factor)));
		if (this.object != null) {
			this.bruteParams.setLocalValue(this.object, this.target,
					Double.valueOf(this.value));
		}

		@SuppressWarnings("unchecked")
		final Dictionary<Integer, JLabel> dict = this.slider.getLabelTable();
		final Enumeration<Integer> keys = dict.keys();
		while (keys.hasMoreElements()) {
			final Integer key = keys.nextElement();
			final JLabel labelDict = dict.get(key);
			labelDict.setText(String.format("%3.2f",
					Double.valueOf(key.intValue() / this.factor)));
			final Dimension d = labelDict.getSize();
			d.width = 3 * labelDict.getFont().getSize();
			labelDict.setSize(d);
		}
		this.slider.addChangeListener(new SliderListener());

	}

	@Override
	public final Double parse(final String string) {
		return Double.valueOf(string);
	}

	@Override
	public final Double value() {
		return Double.valueOf(this.value / this.factor);
	}

	@Override
	public final synchronized void value(final Double d) {
		this.value = (int) (d.doubleValue() * this.factor);
	}

	@Override
	public final void value(final String string) {
		value(parse(string));
	}

	@SuppressWarnings("hiding")
	@Override
	final <A extends Container, B extends Container, C extends Container> Value<Double> localInstance(
			final DragObject<A, B, C> object, final DropTarget<A, B, C> target,
			final Double value) {
		throw new UnsupportedOperationException();
	}
}