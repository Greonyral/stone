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
			value = slider.getValue() / factor;
			System.out.printf("min: %d value: %d max: %d\n", Integer
					.valueOf(min), Integer.valueOf(slider.getValue()),
					Integer.valueOf(max));
			label.setText(String.format("%s %.2f", value == 0 ? " "
					: value > 0 ? "+" : "-", Double.valueOf(Math
							.abs(value))));
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

	ValueFloat(BruteParams<Double> bruteParams, double initValue,
			double step, double ticks) {
		this.bruteParams = bruteParams;
		value = (int) (initValue * factor);
		min = (int) ((initValue - step) * factor);
		max = (int) ((initValue + step) * factor);
		this.ticks = (int) (ticks * factor);
		this.step = (int) (step * factor);
		object = null;
		target = null;
	}

	@Override
	public final synchronized void display() {
		slider.setMinimum(min);
		slider.setMaximum(max);
		slider.setValue((int) (value * factor));
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMajorTickSpacing(step);
		slider.setMinorTickSpacing(ticks);
		label.setText(String.format("%s %.2f", value == 0 ? " "
				: value > 0 ? "+" : "-", Double.valueOf(Math.abs(value)
						/ factor)));
		if (object != null) {
			bruteParams.setLocalValue(object, target, Double
					.valueOf(value));
		}

		@SuppressWarnings("unchecked")
		final Dictionary<Integer, JLabel> dict = slider.getLabelTable();
		final Enumeration<Integer> keys = dict.keys();
		while (keys.hasMoreElements()) {
			final Integer key = keys.nextElement();
			final JLabel labelDict = dict.get(key);
			labelDict.setText(String.format("%3.2f", Double.valueOf(key
					.intValue()
					/ factor)));
			final Dimension d = labelDict.getSize();
			d.width = 3 * labelDict.getFont().getSize();
			labelDict.setSize(d);
		}
		slider.addChangeListener(new SliderListener());

	}

	@Override
	public final Double parse(final String string) {
		return Double.valueOf(string);
	}

	@Override
	public final Double value() {
		return Double.valueOf(value / factor);
	}

	@Override
	public final synchronized void value(final Double d) {
		value = (int) (d.doubleValue() * factor);
	}

	@Override
	public final void value(final String string) {
		value(parse(string));
	}

	@Override
	final <A extends Container, B extends Container, C extends Container>
	Value<Double> localInstance(final DragObject<A, B, C> object,
			final DropTarget<A, B, C> target, final Double value) {
		throw new UnsupportedOperationException();
	}
}