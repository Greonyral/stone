package stone.modules.abcCreator;

import java.awt.Container;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class ValueInt extends Value<Integer> {

	class SliderListener implements ChangeListener {


		SliderListener() {
		}

		@Override
		public final void stateChanged(final ChangeEvent e) {
			final int value_ = slider.getValue();
			if (object == null) {
				bruteParams.setGlobalValue(Integer.valueOf(value_));
			} else {
				bruteParams.setLocalValue(object, target, Integer
						.valueOf(value_));
			}
			label.setText(String.format("%s %d", value_ == 0 ? " "
					: value_ > 0 ? "+" : "-", Integer.valueOf(Math
							.abs(value_))));
			if (interval > 0)
				if (value_ == min) {
					min -= interval;
					if (value_ < (max - (3 * interval))) {
						max -= interval;
					}
				} else if (value_ == max) {
					max += interval;
					if (value_ > (min + (3 * interval))) {
						min += interval;
					}
				} else
					return;
			else
				return;
			display();
			slider.revalidate();
		}
	}

	/**
	 * 
	 */
	final BruteParams<Integer> bruteParams;
	int value;
	int min;
	int max;
	final int interval;
	private final int ticks;
	final DragObject<Container, Container, Container> object;
	final DropTarget<Container, Container, Container> target;

	@SuppressWarnings("unchecked")
	private <A extends Container, B extends Container, C extends Container> ValueInt(
			BruteParams<Integer> bruteParams, final ValueInt value,
			final DragObject<A, B, C> object,
			final DropTarget<A, B, C> target, final Integer valueLocal) {
		this.bruteParams = bruteParams;
		interval = value.interval;
		ticks = value.ticks;
		this.value = valueLocal.intValue(); // bruteParams.globalValue or
		// previously set value
		max = value.max;
		min = value.min;
		this.object = (DragObject<Container, Container, Container>) object;
		this.target = (DropTarget<Container, Container, Container>) target;
	}

	/** Creates a new Value with unbounded value */
	ValueInt(BruteParams<Integer> bruteParams, int initValue,
			int interval, int ticks) {
		this.bruteParams = bruteParams;
		value = initValue;
		min = initValue - interval;
		max = initValue + interval;
		this.interval = interval;
		this.ticks = ticks;
		object = null;
		target = null;
	}

	/** Creates a new Value with bounded value */
	ValueInt(BruteParams<Integer> bruteParams, int initValue, int min,
			int max, int ticks) {
		this.bruteParams = bruteParams;
		value = initValue;
		this.min = min;
		this.max = max;
		interval = 0;
		this.ticks = ticks;
		object = null;
		target = null;
	}

	@Override
	public synchronized final void display() {
		slider.setMinimum(min);
		slider.setMaximum(max);
		slider.setValue(value);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMajorTickSpacing(interval);
		slider.setMinorTickSpacing(ticks);
		label.setText(String.format("%s %d", value == 0 ? " "
				: value > 0 ? "+" : "-", Integer.valueOf(Math.abs(value))));

		slider.addChangeListener(new SliderListener());
	}

	@Override
	public <A extends Container, B extends Container, C extends Container>
	Value<Integer> localInstance(DragObject<A, B, C> object,
			DropTarget<A, B, C> target, final Integer value) {
		return new ValueInt(bruteParams, this, object, target, value);
	}

	@Override
	public final Integer parse(final String string) {
		return Integer.valueOf(string);
	}

	@Override
	public final Integer value() {
		return Integer.valueOf(value);
	}

	@Override
	public final synchronized void value(final Integer i) {
		value = i.intValue();
	}

	@Override
	public void value(final String string) {
		value(parse(string));
	}
}