package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;


abstract class Value<E> {


	private final JPanel panel;
	protected final JLabel label;
	protected final JSlider slider;

	Value() {
		this.panel = new JPanel();
		this.label = new JLabel();
		this.slider = new JSlider();
		this.panel.setLayout(new BorderLayout());
		this.panel.add(this.slider);
		this.panel.add(this.label, BorderLayout.SOUTH);
	}

	public abstract void display();

	public final JPanel panel() {
		display();
		return this.panel;
	}

	/**
	 * @param object
	 * @param target
	 * @return the param value saved at object for given target
	 */
	abstract <A extends Container, B extends Container, C extends Container> Value<E> localInstance(
			DragObject<A, B, C> object, DropTarget<A, B, C> target,
			final E value);

	abstract E parse(String string);

	abstract E value();

	/**
	 * Sets global Value
	 * 
	 * @param s
	 */
	abstract void value(E s);

	abstract void value(String string);
}