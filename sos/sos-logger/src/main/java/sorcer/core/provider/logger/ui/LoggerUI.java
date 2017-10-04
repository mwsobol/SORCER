package sorcer.core.provider.logger.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import net.jini.core.lookup.ServiceItem;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.logger.LogFilter;

/**
 * Component Service UI for the Logger Service
 */
public class LoggerUI extends JPanel implements Observer {
	private static final long serialVersionUID = 1L;
	private final static Logger logger = LoggerFactory.getLogger(LoggerUI.class
			.getName());
	private boolean activeLoggers = false;
	private boolean isOpen = false;
	private String[] loggerNames;
	private String fileName;
	private RemoteLogger remoteLogger = null;

	private JList list;
	private JPanel loggerListPanel;
	private JTextArea logText;
	private FilterPane filterComponent;
	private JScrollPane logScrollPane;
	private RandomAccessFile logFile;
	private String selectedLevel = "ALL";

	private LogFilter logFilter;
	private List<String> lines = new ArrayList<String>();
	private List<String> filteredResults = new ArrayList<String>();

	public LoggerUI(Object obj) {
		this.getAccessibleContext().setAccessibleName("Log Viewer");

		if ((obj != null) && (obj instanceof ServiceItem)) {
			ServiceItem svcItem = (ServiceItem) obj;
			if (svcItem.service instanceof RemoteLogger) {
				remoteLogger = (RemoteLogger) svcItem.service;
			}
		} else {
			logger.error("Could not find Logger.");
		}
		filterComponent = new FilterPane();
		filterComponent.addObserver(this);
		this.initialize();
	}

	private void initialize() {
		this.setLayout(new BorderLayout());
		// this.add(getMenuBar(), BorderLayout.NORTH);
		setLoggerListPanel();
		JSplitPane searchPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				loggerListPanel, filterComponent.getFilterPanel());
		searchPane.setResizeWeight(.5);

		JSplitPane sPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				getLoggerTextPanel(), searchPane);
		sPane.setResizeWeight(.8);
		this.add(sPane, BorderLayout.CENTER);
	}

	private JMenuBar getMenuBar() {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(java.awt.event.KeyEvent.VK_F);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(java.awt.event.KeyEvent.VK_E);

		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(java.awt.event.KeyEvent.VK_V);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(java.awt.event.KeyEvent.VK_H);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);
		return menuBar;
	}

	/**
	 * Creates the Panel that contains the list of known Loggers.
	 * 
	 * @return
	 */
	private void setLoggerListPanel() {
		loggerListPanel = new JPanel();
		try {
			loggerNames = remoteLogger.getLogNames();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		loggerListPanel.setBorder(BorderFactory.createTitledBorder("Logs"));
		loggerListPanel.setLayout(new BorderLayout());

		list = new JList(loggerNames);
		ListSelectionModel selectionModel = new SingleSelectionModel();
		list.setSelectionModel(selectionModel);
		list.setVisibleRowCount(-1);

		JButton refreshLoggerListButton = new JButton("Refresh");
		refreshLoggerListButton
				.addActionListener(new RefreshLoggerListAction());
		refreshLoggerListButton.setEnabled(true);

		JButton deleteLoggerButton = new JButton("Delete");
		deleteLoggerButton.addActionListener(new DeleteLoggerAction());
		deleteLoggerButton.setEnabled(true);

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(refreshLoggerListButton);
		buttonPane.add(deleteLoggerButton);

		loggerListPanel.add(new JScrollPane(list), BorderLayout.CENTER);
		loggerListPanel.add(buttonPane, BorderLayout.SOUTH);
	}

	/**
	 * Creates a panel that contains the log messages that have been stored by
	 * the logger
	 * 
	 * @return
	 */
	private JPanel getLoggerTextPanel() {
		JPanel loggerTextPanel = new JPanel();
		loggerTextPanel.setBorder(BorderFactory.createTitledBorder("Log File"));
		loggerTextPanel.setLayout(new BorderLayout());

		logText = new JTextArea();
		logText.setEditable(false);

		JButton clearLogTextAreaButton = new JButton("Clear");
		clearLogTextAreaButton.addActionListener(new ClearLoggerTextAction());
		clearLogTextAreaButton.setEnabled(true);

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(clearLogTextAreaButton);

		logScrollPane = new JScrollPane(logText);

		loggerTextPanel.add(logScrollPane, BorderLayout.CENTER);
		loggerTextPanel.add(buttonPane, BorderLayout.SOUTH);

		return loggerTextPanel;
	}

	public void update(Observable o, Object arg) {
		String text = null;
		if (arg.toString().equals("level")) {
			selectedLevel = filterComponent.getLevel();
			// filterResultsByLevel(level);
		} else if (arg.toString().equals("search")) {
			text = filterComponent.getText();
			filterResultsByPattern(text, selectedLevel);
		}
	}

	private void filterResultsByPattern(String text, String level) {
		if (!filteredResults.isEmpty())
			clearFilteredResults();

		logFilter = new LogFilter(lines);
		filteredResults = logFilter.textFilter(text, level);

		logText.setText("");

		for (String line : filteredResults)
			logText.append(line + "\n");
	}

	private void clearFilteredResults() {
		filteredResults.clear();
	}

	/**
	 * Refreshes the list of loggers
	 */
	private class RefreshLoggerListAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			if (activeLoggers == false) {
				try {
					loggerNames = remoteLogger.getLogNames();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				list.setListData(loggerNames);
			}
		}
	}

	/**
	 * Deletes a selected logger
	 */
	private class DeleteLoggerAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			String fileName = (String) list.getSelectedValue();
			if (fileName != null) {
				try {
					remoteLogger.deleteLog(fileName);
					loggerNames = remoteLogger.getLogNames();
					list.setListData(loggerNames);
					clearLoggerText();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void clearLoggerText() {
		logText.setText("");
		if (filterComponent != null) {
			filterComponent.resetLevel();
		}
		list.clearSelection();
	}

	/**
	 * Clears the logger text area when the clearSessions button is pressed
	 */
	private class ClearLoggerTextAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent ae) {
			clearLoggerText();
		}
	}

	/**
	 * Create a SINGLE_SELECTION ListSelectionModel that calls a new method,
	 * updateSingleSelection(), each time the select changes. This can be a
	 * little bit more convenient than using the ListModels
	 * ListSelectionListener, since ListSelectionListeners are only given the
	 * range of indices that the change spans.
	 */
	private class SingleSelectionModel extends DefaultListSelectionModel {
		private static final long serialVersionUID = 1L;

		public SingleSelectionModel() {
			setSelectionMode(SINGLE_SELECTION);
		}

		public void setSelectionInterval(int index0, int index1) {
			int oldIndex = getMinSelectionIndex();
			super.setSelectionInterval(index0, index1);
			int newIndex = getMinSelectionIndex();
			if (oldIndex != newIndex) {
				updateSingleSelection(oldIndex, newIndex);
			}
		}

		public void updateSingleSelection(int oldIndex, int newIndex) {
			if (isOpen) {
				try {
					logFile.close();
				} catch (Exception ex) {
					logger.warn("Error Closing File: " + ex.toString());
				}
				isOpen = false;
			}
			ListModel m = list.getModel();
			Object newValue = (newIndex == -1) ? "<none>" : m
					.getElementAt(newIndex);
			if (!filterComponent.equals(null))
				filterComponent.resetLevel();
			fileName = (String) newValue;
			logText.setText("");
			try {
				lines = remoteLogger.getLog(fileName);
				StringBuffer sb = new StringBuffer();
				for (String line : lines) {
					sb.append(line).append("\n");
				}
				logText.setText(sb.toString());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
