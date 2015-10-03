package stone.modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import stone.io.GUIInterface.Button;
import stone.io.GUIPlugin;
import stone.io.IOHandler;


/**
 * Plugin to edit the index and the title numbers shown in the title
 * 
 * @author Nelphindal
 */
public class NumberingGUI extends GUIPlugin {

	private final Map<Integer, String> indices;
	private final Map<Integer, JTextField> idxToField = new HashMap<>();
	private final Map<Integer, JCheckBox> idxToOpt = new HashMap<>();
	private final Map<Integer, String> titles;
	private final Map<Integer, Set<Instrument>> instruments;
	private final Map<Integer, JTextField> idxToNum = new HashMap<>();
	private final SongChangeData scd;
	private final IOHandler io;

	/**
	 * @param data
	 *            -
	 * @param io
	 *            -
	 * @param text
	 *            displayed if {@link GUIPlugin#display(JPanel)} returns
	 *            <i>false</i>
	 */
	public NumberingGUI(final SongChangeData data,
			@SuppressWarnings("hiding") final IOHandler io, final String text) {
		super(text);
		this.indices = data.getIndices();
		this.titles = data.getTitles();
		this.instruments = data.getInstruments();
		this.scd = data;
		this.io = io;
	}

	/**
	 * After returning from {@link IOHandler#handleGUIPlugin(GUIPlugin)} this
	 * method should be called to transfer the entered data to the container(s).
	 */
	public final void copyFieldsToMaps() {
		final Map<Integer, Integer> mapIdx = buildRenumberIdxMap();
		final Map<Integer, String> mapNumber = buildRenumberNumMap();
		final Map<Integer, Boolean> mapOpt = buildOptMap();
		this.scd.renumber(mapIdx, mapNumber, mapOpt);
	}

	private Map<Integer, Boolean> buildOptMap() {
		final Map<Integer, Boolean> map = new HashMap<>();
		for (final Entry<Integer, JCheckBox> opts : this.idxToOpt.entrySet()) {
			map.put(opts.getKey(), opts.getValue().isSelected());
		}
		return map;
	}

	private Map<Integer, Integer> buildRenumberIdxMap() {
		final Map<Integer, Integer> map = new HashMap<>();
		for (final Map.Entry<Integer, JTextField> idcs : this.idxToField
				.entrySet()) {
			final Integer idxNew;
			try {
				idxNew = Integer.parseInt(idcs.getValue().getText().trim());
			} catch (final Exception e) {
				this.io.printError("Error parsing index\n"
						+ "Song will remain unchanged", false);
				return null;
			}
			map.put(idcs.getKey(), idxNew);
		}
		return map;
	}

	private final Map<Integer, String> buildRenumberNumMap() {
		final Map<Integer, String> map = new HashMap<>();
		for (final Entry<Integer, JTextField> idcs : this.idxToNum.entrySet()) {
			String value = idcs.getValue().getText().replaceAll("\t", " ");
			while (value.contains("  ")) {
				value = value.replaceAll("  ", " ");
			}
			map.put(idcs.getKey(), value);
		}
		return map;
	}

	@Override
	protected final boolean display(final JPanel panel) {
		panel.setLayout(new GridLayout(0, 1));
		final Font xFieldFont = Font.decode("Arial 12 bold");
		final Dimension xFieldSize = new Dimension(xFieldFont.getSize() * 4,
				xFieldFont.getSize());
		{
			final JPanel headerPanel = new JPanel();
			final JLabel headerLabelW = new JLabel("     Idx        ");
			final JLabel headerLabelE = new JLabel("Opt");
			final JLabel headerLabelC = new JLabel("	       Numbers");
			headerPanel.setLayout(new BorderLayout());
			headerPanel.add(headerLabelW, BorderLayout.WEST);
			headerPanel.add(headerLabelE, BorderLayout.EAST);
			headerPanel.add(headerLabelC);
			panel.add(headerPanel);
		}
		final List<Integer> keys = new ArrayList<>(this.indices.keySet());
		java.util.Collections.sort(keys);
		for (final Integer key : keys) {
			final JPanel trackPanel = new JPanel();
			final JPanel idxPanel = new JPanel();
			final JTextField xField = new JTextField(key.toString());
			final JCheckBox optBox = new JCheckBox();
			final String title;
			{
				final Set<?> instruments_ = this.instruments.get(key);
				final String titleString = this.titles.get(key);
				title = (titleString == null ? "<No title>" : titleString)
						+ " "
						+ (instruments_ == null ? "[?]" : instruments_
								.toString());
			}
			trackPanel.setLayout(new BorderLayout());
			idxPanel.setLayout(new GridLayout(1, 0));
			trackPanel.add(xField, BorderLayout.WEST);
			trackPanel.add(idxPanel);
			trackPanel.add(optBox, BorderLayout.EAST);

			trackPanel.add(new JLabel(title), BorderLayout.SOUTH);
			xField.setFont(xFieldFont);
			xField.setPreferredSize(xFieldSize);
			final JTextField numField = new JTextField(this.indices.get(key));
			this.idxToNum.put(key, numField);
			idxPanel.add(numField);
			panel.add(trackPanel);
			this.idxToField.put(key, xField);
			this.idxToOpt.put(key, optBox);
		}
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.add(Button.OK.getButton(), BorderLayout.EAST);
		buttonPanel.add(Button.ABORT.getButton(), BorderLayout.WEST);
		panel.add(buttonPanel);
		return false;
	}

	@Override
	protected final String getTitle() {
		return "Change Numbering - " + this.scd.file().getFilename();
	}


	@Override
	protected void textmode() {
		// TODO
		System.err.println("Text mode not supported - yet");
	}
}
