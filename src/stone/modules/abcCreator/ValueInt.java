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
			final int value_ = ValueInt.this.slider.getValue();
			if (ValueInt.this.object == null) {
				ValueInt.this.bruteParams.setGlobalValue(Integer
						.valueOf(value_));
			} else {
				ValueInt.this.bruteParams.setLocalValue(ValueInt.this.object,
						ValueInt.this.target, Integer.valueOf(value_));
			}
			ValueInt.this.label.setText(String.format("%s %d",
					value_ == 0 ? " " : value_ > 0 ? "+" : "-",
					Integer.valueOf(Math.abs(value_))));
			if (ValueInt.this.interval > 0) {
				if (value_ == ValueInt.this.min) {
					ValueInt.this.min -= ValueInt.this.interval;
					if (value_ < (ValueInt.this.max - (3 * ValueInt.this.interval))) {
						ValueInt.this.max -= ValueInt.this.interval;
					}
				} else if (value_ == ValueInt.this.max) {
					ValueInt.this.max += ValueInt.this.interval;
					if (value_ > (ValueInt.this.min + (3 * ValueInt.this.interval))) {
						ValueInt.this.min += ValueInt.this.interval;
					}
				} else {
					return;
				}
			} else {
				return;
			}
			display();
			ValueInt.this.slider.revalidate();
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

	@SuppressWarnings({ "unchecked", "hiding" })
	private <A extends Container, B extends Container, C extends Container> ValueInt(
			BruteParams<Integer> bruteParams, final ValueInt value,
			final DragObject<A, B, C> object, final DropTarget<A, B, C> target,
			final Integer valueLocal) {
		this.bruteParams = bruteParams;
		this.interval = value.interval;
		this.ticks = value.ticks;
		this.value = valueLocal.intValue(); // bruteParams.globalValue or
		// previously set value
		this.max = value.max;
		this.min = value.min;
		this.object = (DragObject<Container, Container, Container>) object;
		this.target = (DropTarget<Container, Container, Container>) target;
	}

	@SuppressWarnings("hiding")
	/** Creates a new Value with unbounded value */
	ValueInt(BruteParams<Integer> bruteParams, int initValue, int interval,
			int ticks) {
		this.bruteParams = bruteParams;
		this.value = initValue;
		this.min = initValue - interval;
		this.max = initValue + interval;
		this.interval = interval;
		this.ticks = ticks;
		this.object = null;
		this.target = null;
	}

	@SuppressWarnings("hiding")
	/** Creates a new Value with bounded value */
	ValueInt(BruteParams<Integer> bruteParams, int initValue, int min, int max,
			int ticks) {
		this.bruteParams = bruteParams;
		this.value = initValue;
		this.min = min;
		this.max = max;
		this.interval = 0;
		this.ticks = ticks;
		this.object = null;
		this.target = null;
	}

	@Override
	public synchronized final void display() {
		this.slider.setMinimum(this.min);
		this.slider.setMaximum(this.max);
		this.slider.setValue(this.value);
		this.slider.setPaintTicks(true);
		this.slider.setPaintLabels(true);
		this.slider.setMajorTickSpacing(this.interval);
		this.slider.setMinorTickSpacing(this.ticks);
		this.label.setText(String.format("%s %d", this.value == 0 ? " "
				: this.value > 0 ? "+" : "-", Integer.valueOf(Math
				.abs(this.value))));

		this.slider.addChangeListener(new SliderListener());
	}

	@SuppressWarnings("hiding")
	@Override
	public <A extends Container, B extends Container, C extends Container> Value<Integer> localInstance(
			DragObject<A, B, C> object, DropTarget<A, B, C> target,
			final Integer value) {
		return new ValueInt(this.bruteParams, this, object, target, value);
	}

	@Override
	public final Integer parse(final String string) {
		return Integer.valueOf(string);
	}

	@Override
	public final Integer value() {
		return Integer.valueOf(this.value);
	}

	@Override
	public final synchronized void value(final Integer i) {
		this.value = i.intValue();
	}

	@Override
	public void value(final String string) {
		value(parse(string));
	}
}