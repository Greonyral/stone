package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;


final class DO_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DragObject<C, D, T> object;
	private final DndPluginCaller<C, D, T> caller;
	private final BruteParams<?>[] params;
	BruteParams<?> param;

	private JPanel panelOption;
	private static final Font font = Font.decode("Arial bold 9");

	@SuppressWarnings("hiding")
	DO_Listener(final DragObject<C, D, T> object,
			final DragAndDropPlugin<C, D, T>.State state,
			final BruteParams<?>[] params, final DndPluginCaller<C, D, T> caller) {
		super(state);
		this.object = object;
		this.caller = caller;
		this.params = params;
		object.getDisplayableComponent().setBackground(DNDListener.C_INACTIVE);
	}

	private final void displayParamMenu() {
		if (this.object.getTargetContainer() != this.state.emptyTarget
				.getContainer()) {
			this.panelOption = new JPanel();
			this.panelOption.setLayout(new GridLayout(0, 2));
			for (final BruteParams<?> ps : this.params) {
				if (ps == null) {
					continue;
				}
				final JPanel optionPanel = new JPanel();
				final JLabel label = new JLabel(ps.toString());
				label.setFont(DO_Listener.font);
				optionPanel.add(label);
				optionPanel.setBackground(Color.LIGHT_GRAY);
				optionPanel.addMouseListener(new MouseListener() {

					@Override
					public final void mouseClicked(final MouseEvent e) {
						e.consume();
					}

					@Override
					public final void mouseEntered(final MouseEvent e) {
						optionPanel.setBackground(Color.GREEN);
						e.consume();
					}

					@Override
					public final void mouseExited(final MouseEvent e) {
						optionPanel.setBackground(Color.LIGHT_GRAY);
						e.consume();
					}

					@Override
					public final void mousePressed(final MouseEvent e) {
						e.consume();
					}

					@Override
					public final void mouseReleased(final MouseEvent e) {
						DO_Listener.this.param = ps;
						e.consume();
						displayParam();
					}
				});
				this.panelOption.add(optionPanel);
			}
			this.object.getDisplayableComponent().add(this.panelOption,
					BorderLayout.SOUTH);
			this.state.plugin.repack();
		}

	}

	private final void mark(boolean active) {
		if (!active || (this.state.dragging == null)) {
			final Set<DropTarget<?, ?, ?>> targets = new HashSet<>();
			final Set<DropTarget<?, ?, ?>> targetsCloned = new HashSet<>();
			final Set<DropTarget<?, ?, ?>> targetsIndirect = new HashSet<>();

			final Set<DragObject<?, ?, ?>> objectsCloned = new HashSet<>();
			final Set<DragObject<?, ?, ?>> objectsIndirect = new HashSet<>();

			final Color ct0 = active ? DNDListener.C_SELECTED0
					: DNDListener.C_INACTIVE_TARGET;
			final Color ct1 = active ? Color.CYAN
					: DNDListener.C_INACTIVE_TARGET;
			final Color ct2 = active ? DNDListener.C_SELECTED1
					: DNDListener.C_INACTIVE_TARGET;


			final Color co0 = active ? DNDListener.C_ACTIVE
					: DNDListener.C_INACTIVE;
			final Color co1 = active ? DNDListener.C_CLONE
					: DNDListener.C_INACTIVE;
			final Color co2 = active ? DNDListener.C_SELECTED1
					: DNDListener.C_INACTIVE;

			for (final DropTarget<?, ?, ?> t : this.object) {
				targets.add(t);
				for (final DragObject<?, ?, ?> o : t) {
					objectsIndirect.add(o);
					for (final DropTarget<?, ?, ?> tIndirect : o) {
						targetsIndirect.add(tIndirect);
					}
				}
			}
			this.object.getDisplayableComponent().setBackground(co0);
			this.object.getTargetContainer().getDisplayableComponent()
					.setBackground(ct0);
			for (final DropTarget<?, ?, ?> t : this.object.getTargetContainer()) {
				targetsIndirect.add(t);
			}
			if (this.object.isAlias()) {
				for (final DropTarget<?, ?, ?> t : this.object) {
					targetsCloned.add(t);
				}
			}
			for (final DragObject<?, ?, ?> alias0 : this.object.getAliases()) {
				final DragObject<?, ?, ?> alias1;
				if (alias0 == this.object) {
					alias1 = this.object.getOriginal();
				} else {
					alias1 = alias0;
				}
				for (final DropTarget<?, ?, ?> t : alias1) {
					targetsCloned.add(t);
				}
				objectsCloned.add(alias1);
			}
			targets.remove(this.state.emptyTarget);
			targetsCloned.remove(this.state.emptyTarget);
			targetsIndirect.remove(this.state.emptyTarget);
			objectsCloned.remove(this.object);
			objectsIndirect.remove(this.object);

			// know we know the distribution of the colors, apply them ...

			for (final DropTarget<?, ?, ?> t : targets) {
				t.getDisplayableComponent().setBackground(ct0);
				targetsIndirect.remove(t);
				targetsCloned.remove(t);
			}
			for (final DropTarget<?, ?, ?> t : targetsCloned) {
				t.getDisplayableComponent().setBackground(ct1);
				targetsIndirect.remove(t);
			}
			for (final DropTarget<?, ?, ?> t : targetsIndirect) {
				t.getDisplayableComponent().setBackground(ct2);
			}

			for (final DragObject<?, ?, ?> o : objectsCloned) {
				o.getDisplayableComponent().setBackground(co1);
				objectsIndirect.remove(o);
			}
			for (final DragObject<?, ?, ?> o : objectsIndirect) {
				o.getDisplayableComponent().setBackground(co2);
			}
		}
	}

	private final void wipeTargetsAndLink() {
		synchronized (this.state) {
			if (this.state.upToDate) {
				this.state.upToDate = false;
				this.state.io.endProgress("");
			}
		}
		this.object.getTargetContainer().removeAllLinks(this.object);
		final Iterator<DropTarget<C, D, T>> tIter = this.object.clearTargets();
		while (tIter.hasNext()) {
			final DropTarget<C, D, T> target = tIter.next();
			if (target == this.state.emptyTarget) {
				continue;
			}
			if (this.caller.unlink(this.object, target)) {
				if (target != this.state.target) {
					final Container panel = target.getDisplayableComponent();
					final Container parent = panel.getParent();
					parent.remove(panel);
					if (parent.getComponentCount() == 0) {
						this.state.plugin.emptyCenter();
					} else {
						parent.validate();
					}
				}
			}
		}
		this.object.addTarget(this.state.target);
		this.state.target.link(this.object);
		if (this.state.target != this.state.emptyTarget) {
			this.caller.link(this.object, this.state.target);
		}

	}

	@Override
	protected final void enter(boolean enter) {
		if (enter) {
			this.state.object = this.object;
			if (this.object != this.state.dragging) {
				mark(enter);
			}
		} else {
			this.state.object = null;
			if ((this.state.dragging == null)
					|| (this.state.dragging != this.object)) {
				mark(false);
			}
		}

	}

	@Override
	protected final void trigger(boolean released, int button) {
		if (!released) {
			mark(false);
			this.state.dragging = this.object;
			this.object.getDisplayableComponent().setBackground(
					DNDListener.C_DRAGGING);
		} else {
			this.state.dragging.getDisplayableComponent().setBackground(
					DNDListener.C_INACTIVE);
			this.state.dragging = null;
			synchronized (this.state) {
				if (this.state.loadingMap) {
					mark(false);
					return;
				}
				this.state.upToDate = false;
				this.state.label.setText("");
			}
			if (this.state.object != null) {
				mark(true);
				if (button == MouseEvent.BUTTON1) {
					if (this.panelOption == null) {
						displayParamMenu();
					} else {
						this.object.getDisplayableComponent().remove(
								this.panelOption);
						this.object.getDisplayableComponent().revalidate();
						this.panelOption = null;
					}
					return;
				} else if (button == MouseEvent.BUTTON3) {
					final DragObject<C, D, T> clone = this.object.clone();
					this.state.plugin.initObject(clone);
					clone.addTarget(this.state.emptyTarget);
					this.state.emptyTarget.link(clone);
				}
			} else if (this.state.target != null) {
				if (this.state.split) {
					if (this.object.getTargetContainer() == this.state.target
							.getContainer()) {
						if (!this.object.addTarget(this.state.target)) {
							this.caller.printError("To large split");
						}
					} else {
						wipeTargetsAndLink();
					}
				} else {
					wipeTargetsAndLink();
				}
			} else if (this.state.targetC != null) {
				if (this.state.emptyTarget.getContainer() == this.state.targetC) {
					this.state.target = this.state.emptyTarget;
				} else {
					this.state.target = this.state.targetC.createNewTarget();
					this.state.plugin.initTarget(this.state.target);
					this.state.plugin.addToCenter(this.state.target);
				}
				if (this.state.split) {
					if (this.state.targetC == this.object.getTargetContainer()) {
						if (!this.object.addTarget(this.state.target)) {
							this.state.targetC.delete(this.state.target);
							final Container c = this.state.target
									.getDisplayableComponent().getParent();
							c.remove(this.state.target
									.getDisplayableComponent());
							c.revalidate();
							this.caller.printError("To large split");
						}
						this.state.target.link(this.object);
					} else {
						wipeTargetsAndLink();
					}
				} else {
					wipeTargetsAndLink();
				}
				this.state.target = null;
				if (this.object.isAlias()
						&& (this.state.targetC == this.state.emptyTarget
								.getContainer())) {
					this.object.forgetAlias();
					this.state.emptyTarget.getContainer().removeAllLinks(
							this.object);
					final Container parent = this.object
							.getDisplayableComponent().getParent();
					parent.remove(this.object.getDisplayableComponent());
					parent.revalidate();
					mark(false);
					return;
				}
			} else {
				return;
			}
			mark(true);
			if (this.panelOption != null) {
				this.object.getDisplayableComponent().remove(this.panelOption);
				this.panelOption = null;
			}
			this.object.getDisplayableComponent().revalidate();
		}
	}

	final void displayParam() {
		this.panelOption.removeAll();
		final Iterator<DropTarget<C, D, T>> targets = this.object.iterator();
		this.param.display(this.panelOption, this.object, targets);
		this.panelOption.revalidate();
	}

}
