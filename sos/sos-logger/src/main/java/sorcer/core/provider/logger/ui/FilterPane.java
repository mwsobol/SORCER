package sorcer.core.provider.logger.ui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The levels in descending order are:
 * <ul>
 * <li>SEVERE (highest value)
 * <li>WARNING
 * <li>INFO
 * <li>CONFIG
 * <li>FINE
 * <li>FINER
 * <li>FINEST (lowest value)
 * </ul>
 * 
 */
public class FilterPane extends Observable {
	private JPanel filterPanel;
	private GridBagConstraints c;
	private String selectedLevel;
	private JComboBox levelComboBox;
	private JTextField searchField;
	private final static String[] levels = { "ALL", "FINEST", "FINER", "FINE",
			"CONFIG", "INFO", "WARNING", "SEVERE" };

	public FilterPane() {
		initialize();
	}

	public void initialize() {
		filterPanel = new JPanel();
		filterPanel.setLayout(new BorderLayout());
		filterPanel.setBorder(BorderFactory.createTitledBorder("Filter"));
		JLabel level = new JLabel("Level:");
		levelComboBox = new JComboBox(levels);
		levelComboBox.addActionListener(new LevelListener());

		JLabel searchLabel = new JLabel("Expression:");
		searchField = new JTextField(16);

		JButton filterButton = new JButton("Filter Out");
		filterButton.setActionCommand("Filter Out");
		filterButton.addActionListener(new FilterActionListener());

		JButton clearButton = new JButton("Clear");
		clearButton.setActionCommand("Clear");
		clearButton.addActionListener(new FilterActionListener());
		
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		c.weightx = .5;
		c.gridx = 0;
		c.gridy = 0;
		inputPanel.add(level, c);
		c.gridx = 1;
		c.gridy = 0;
		inputPanel.add(levelComboBox, c);

		c.insets = new Insets(20, 0, 0, 10);
		c.gridx = 0;
		c.gridy = 1;
		inputPanel.add(searchLabel, c);
		c.insets = new Insets(20, 0, 0, 10);
		c.gridx = 1;
		c.gridy = 1;
		inputPanel.add(searchField, c);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		controlPanel.add(filterButton);
		controlPanel.add(clearButton);
		
		filterPanel.add(inputPanel, BorderLayout.CENTER);
		filterPanel.add(controlPanel, BorderLayout.PAGE_END);
	}

	public JPanel getFilterPanel() {
		return filterPanel;
	}

	public void resetLevel() {
		levelComboBox.setSelectedIndex(0);
	}

	public String getText() {
		return searchField.getText();
	}

	public String getLevel() {
		return this.selectedLevel;
	}

	private class LevelListener implements ActionListener {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			selectedLevel = (String) cb.getSelectedItem();
			setChanged();
			notifyObservers("level");
		}
	}

	private class FilterActionListener implements ActionListener {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			String cmd = ae.getActionCommand();
			if (cmd.equals("Filter Out")) {
				setChanged();
				notifyObservers("search");
			} else if (cmd.equals("Clear")) {
				searchField.setText("");
				resetLevel();
			}
		}
	}
}