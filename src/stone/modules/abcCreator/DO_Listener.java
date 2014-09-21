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

	DO_Listener(final DragObject<C, D, T> object,
			final DragAndDropPlugin<C, D, T>.State state,
			final BruteParams<?>[] params,
			final DndPluginCaller<C, D, T> caller) {
		super(state);
		this.object = object;
		this.caller = caller;
		this.params = params;
		object.getDisplayableComponent().setBackground(
				DNDListener.C_INACTIVE);
	}

	private final void displayParamMenu() {
		if (object.getTargetContainer() != state.emptyTarget
				.getContainer()) {
			panelOption = new JPanel();
			panelOption.setLayout(new GridLayout(0, 2));
			for (final BruteParams<?> ps : params) {
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
						param = ps;
						e.consume();
						displayParam();
					}
				});
				panelOption.add(optionPanel);
			}
			object.getDisplayableComponent().add(panelOption,
					BorderLayout.SOUTH);
			state.plugin.repack();
		}

	}

	private final void mark(boolean active) {
		if (!active || (state.dragging == null)) {
			final Set<DropTarget<?, ?, ?>> targets = new HashSet<>();
			final Color ct0 =
					active ? DNDListener.C_SELECTED0
							: DNDListener.C_INACTIVE_TARGET;
			final Color ct1 =
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE_TARGET;
			final Color ct2 =
					active ? Color.CYAN : DNDListener.C_INACTIVE_TARGET;
			// final Color co0 = active ? C_SELECTED0 : C_INACTIVE;
			final Color co1 =
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE;
			final Color co2 =
					active ? DNDListener.C_CLONE : DNDListener.C_INACTIVE;
			for (final DropTarget<?, ?, ?> t : object) {
				if (t == state.emptyTarget) {
					continue;
				}
				t.getDisplayableComponent().setBackground(ct0);
				targets.add(t);
				for (final DragObject<?, ?, ?> o : t) {
					if (o != object) {
						o.getDisplayableComponent().setBackground(co1);
					}
				}
			}
			object.getDisplayableComponent()
					.setBackground(
							active ? DNDListener.C_ACTIVE
									: DNDListener.C_INACTIVE);
			object.getTargetContainer().getDisplayableComponent()
					.setBackground(ct0);
			for (final DropTarget<?, ?, ?> t : object.getTargetContainer()) {
				if ((t == state.emptyTarget) || targets.contains(t)) {
					continue;
				}
				t.getDisplayableComponent().setBackground(ct1);
				targets.add(t);
				for (final DragObject<?, ?, ?> o : t) {
					if (o != object) {
						o.getDisplayableComponent().setBackground(co1);
					}
				}
			}
			if (object.isAlias()) {
				object.getOriginal().getDisplayableComponent()
						.setBackground(co2);
				if (object.getOriginal().getTargetContainer() != object
						.getTargetContainer()) {
					object.getOriginal().getTargetContainer()
							.getDisplayableComponent().setBackground(ct2);
				}
				for (final DropTarget<?, ?, ?> t : object.getOriginal()) {
					if ((t == state.emptyTarget) || targets.contains(t)) {
						continue;
					}
					t.getDisplayableComponent().setBackground(ct2);
				}
			}
			for (final DragObject<?, ?, ?> alias : object.getAliases()) {
				if (alias == object) {
					continue;
				}
				alias.getDisplayableComponent().setBackground(co2);
				if (alias.getTargetContainer() != object
						.getTargetContainer()) {
					alias.getTargetContainer().getDisplayableComponent()
							.setBackground(ct2);
				}
				for (final DropTarget<?, ?, ?> t : alias) {
					if ((t == state.emptyTarget) || targets.contains(t)) {
						continue;
					}
					t.getDisplayableComponent().setBackground(ct2);
				}
			}
		}
	}

	private final void wipeTargetsAndLink() {
		synchronized (state) {
			if (state.upToDate) {
				state.upToDate = false;
				state.io.endProgress();
			}
		}
		object.getTargetContainer().removeAllLinks(object);
		final Iterator<DropTarget<C, D, T>> tIter = object.clearTargets();
		while (tIter.hasNext()) {
			final DropTarget<C, D, T> target = tIter.next();
			if (target == state.emptyTarget) {
				continue;
			}
			if (caller.unlink(object, target)) {
				if (target != state.target) {
					final Container panel =
							target.getDisplayableComponent();
					final Container parent = panel.getParent();
					parent.remove(panel);
					if (parent.getComponentCount() == 0) {
						state.plugin.emptyCenter();
					} else {
						parent.validate();
					}
				}
			}
		}
		object.addTarget(state.target);
		state.target.link(object);
		if (state.target != state.emptyTarget) {
			caller.link(object, state.target);
		}

	}

	@Override
	protected final void enter(boolean enter) {
		if (enter) {
			state.object = object;
			if (object != state.dragging) {
				mark(enter);
			}
		} else {
			state.object = null;
			if ((state.dragging == null) || (state.dragging != object)) {
				mark(false);
			}
		}

	}

	@Override
	protected final void trigger(boolean released, int button) {
		if (!released) {
			mark(false);
			state.dragging = object;
			object.getDisplayableComponent().setBackground(
					DNDListener.C_DRAGGING);
		} else {
			state.dragging.getDisplayableComponent().setBackground(
					DNDListener.C_INACTIVE);
			state.dragging = null;
			synchronized (state) {
				if (state.loadingMap) {
					mark(false);
					return;
				}
				state.upToDate = false;
				state.label.setText("");
			}
			if (state.object != null) {
				mark(true);
				if (button == MouseEvent.BUTTON1) {
					if (panelOption == null) {
						displayParamMenu();
					} else {
						object.getDisplayableComponent().remove(
								panelOption);
						object.getDisplayableComponent().revalidate();
						panelOption = null;
					}
					return;
				} else if (button == MouseEvent.BUTTON3) {
					final DragObject<C, D, T> clone = object.clone();
					state.plugin.initObject(clone);
					clone.addTarget(state.emptyTarget);
					state.emptyTarget.link(clone);
				}
			} else if (state.target != null) {
				if (state.split) {
					if (object.getTargetContainer() == state.target
							.getContainer()) {
						if (!object.addTarget(state.target)) {
							caller.printError("To large split");
						}
					} else {
						wipeTargetsAndLink();
					}
				} else {
					wipeTargetsAndLink();
				}
			} else if (state.targetC != null) {
				if (state.emptyTarget.getContainer() == state.targetC) {
					state.target = state.emptyTarget;
				} else {
					state.target = state.targetC.createNewTarget();
					state.plugin.initTarget(state.target);
					state.plugin.addToCenter(state.target);
				}
				if (state.split) {
					if (state.targetC == object.getTargetContainer()) {
						if (!object.addTarget(state.target)) {
							state.targetC.delete(state.target);
							final Container c =
									state.target.getDisplayableComponent()
											.getParent();
							c.remove(state.target
									.getDisplayableComponent());
							c.revalidate();
							caller.printError("To large split");
						}
						state.target.link(object);
					} else {
						wipeTargetsAndLink();
					}
				} else {
					wipeTargetsAndLink();
				}
				state.target = null;
				if (object.isAlias()
						&& (state.targetC == state.emptyTarget
								.getContainer())) {
					object.forgetAlias();
					state.emptyTarget.getContainer()
							.removeAllLinks(object);
					final Container parent =
							object.getDisplayableComponent().getParent();
					parent.remove(object.getDisplayableComponent());
					parent.revalidate();
					mark(false);
					return;
				}
			} else {
				return;
			}
			mark(true);
			if (panelOption != null) {
				object.getDisplayableComponent().remove(panelOption);
				panelOption = null;
			}
			object.getDisplayableComponent().revalidate();
		}
	}

	final void displayParam() {
		panelOption.removeAll();
		final Iterator<DropTarget<C, D, T>> targets = object.iterator();
		param.display(panelOption, object, targets);
		panelOption.revalidate();
	}

}
