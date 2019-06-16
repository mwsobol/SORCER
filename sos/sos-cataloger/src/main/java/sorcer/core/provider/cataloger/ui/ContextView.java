/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.core.provider.cataloger.ui;

//function prc to model to getValue context.
//also add buttons to bottom for tree and list view. 
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;

import sorcer.core.SorcerConstants;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.node.ContextNode;
import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * This class implements the Context editing window, which is used in the
 * Cataloger UI and the task manager integrated into providers. The edit window
 * shows either a tree @see ContextTree or a list view @see ContextList of a
 * context. The edit window contains two tabs (one for input and one for output)
 * displaying the Contexts. The bottom of the window contains the control panel
 * which holds the various operations that can be performed on the context.
 * These operations include Add Path, Add Data Node, Remove Item, Clear Context,
 * Load Other, Delete Context, Save Context, Save As, and Exert Task.
 */
public class ContextView extends JPanel implements Observer, ActionListener,
		SorcerConstants {

	/**
	 * id used for serialization
	 */
	private static final long serialVersionUID = 3256754238398101331L;

	/**
	 * current model the data is based off of
	 */
	private BrowserModel model;

	/**
	 * Label used on the load other page
	 */
	private JLabel loadOtherLabel;

	/**
	 * combo box used on the load other screen
	 */
	private JComboBox loadOtherBox;

	/**
	 * text field used for save as
	 */
	private JTextField saveAsBox;

	/**
	 * panel used for loading other window
	 */
	private JFrame loadOtherPanel;

	/**
	 * ContextTree for input context
	 */
	private ContextTree treePanelInput;

	/**
	 * ContextTree for output context
	 */
	private ContextTree treePanelOutput;

	/**
	 * Tabbed pane to hold the input and output ContextTrees
	 */
	private JTabbedPane tabbedContextPane;

	/**
	 * Private copy of the context used for various operations, like load other
	 * and save as.
	 */
	private Context theContext;

	/**
	 * current explorer used to gather more information, like load other or to
	 * perform operations: save, save as, delete, and exerting the service
	 */
	private SignatureDispatchment signatureDispatcher;

	/**
	 * counter used to add new nodes, each new node requested will be uniquely
	 * identified.
	 */
	private int newNodeSuffix = 1;

	/**
	 * String command for adding a returnPath
	 */
	private static String ADD_ATTRIBUTE = "addPath";
	/**
	 * String command for adding a data node
	 */
	private static String ADD_DATA = "addData";
	/**
	 * String command for removing an impl
	 */
	private static String REMOVE_NODE = "remove";
	/**
	 * String command for clearing the context
	 */
	private static String CLEAR_CONTEXT = "clearSessions";
	/**
	 * String command for saving the context
	 */
	private static String SAVE_CONTEXT = "saveContext";
	/**
	 * String command for loading a different context
	 */
	private static String LOAD_CONTEXT = "loadOtherContext";
	/**
	 * String command for confirming loading a different context
	 */
	private static String LOAD_FROM_CONTEXT_CONFIRM = "loadOtherContextConfirm";
	/**
	 * String command for save as
	 */
	private static String SAVE_CONTEXT_AS = "saveASContext";
	/**
	 * String command to confirm the save as box
	 */
	private static String SAVE_AS_CONTEXT_CONFIRM = "saveASContextConfirm";
	/**
	 * String command for loading the delete context window
	 */
	private static String DELETE_CONTEXT = "deleteContext";
	/**
	 * String command for confirming the deletion request
	 */
	private static String DELETE_CONTEXT_CONFIRM = "deleteContextConfirm";
	/**
	 * String command for exerting the service
	 */
	private static String LAUNCH_SERVICE = "liftoff";

	/**
	 * Constructor for creating the context view panel
	 * 
	 * @param signatureDispatcher
	 *            Remote explorer to use.
	 */
	public ContextView(SignatureDispatchment signatureDispatcher) {
		super(new BorderLayout());
		// needed for exerting the service
		System.setSecurityManager(new RMISecurityManager());
		// create the components
		treePanelInput = new ContextTree();
		treePanelOutput = new ContextTree();
		this.signatureDispatcher = signatureDispatcher;

		tabbedContextPane = new JTabbedPane();

		tabbedContextPane.addTab("Input Context", null, treePanelInput,
				"Input Context");
		tabbedContextPane.setMnemonicAt(0, KeyEvent.VK_1);

		tabbedContextPane.addTab("Output Context", null, treePanelOutput,
				"Output Context");
		tabbedContextPane.setMnemonicAt(1, KeyEvent.VK_2);

		// Lay everything out.
		treePanelInput.setPreferredSize(new Dimension(300, 400));
		add(tabbedContextPane, BorderLayout.CENTER);

		// add all of those buttons

		// JButton addPathButton = new JButton("Add Path");
		// addPathButton.setActionCommand(ADDPATH_COMMAND);
		// addPathButton.addActionListener(this);
		//
		// JButton addDataNodeButton = new JButton("Add Data Node");
		// addDataNodeButton.setActionCommand(ADDDATANODE_COMMAND);
		// addDataNodeButton.addActionListener(this);
		//
		// JButton removeButton = new JButton("Remove Item");
		// removeButton.setActionCommand(REMOVE_COMMAND);
		// removeButton.addActionListener(this);
		//
		// JButton clearButton = new JButton("Clear Context");
		// clearButton.setActionCommand(CLEAR_COMMAND);
		// clearButton.addActionListener(this);
		//
		// JButton saveButton = new JButton("Save Context");
		// saveButton.setActionCommand(SAVE_CONTEXT_COMMAND);
		// saveButton.addActionListener(this);
		//
		JButton launchService = new JButton("Exert");
		launchService.setActionCommand(LAUNCH_SERVICE);
		launchService.addActionListener(this);
		//
		// JButton loadOtherButton = new JButton("Load Other");
		// loadOtherButton.setActionCommand(LOAD_FROM_CONTEXT_COMMAND);
		// loadOtherButton.addActionListener(this);
		//
		// JButton saveAsButton = new JButton("Save As");
		// saveAsButton.setActionCommand(SAVE_AS_CONTEXT_COMMAND);
		// saveAsButton.addActionListener(this);
		//
		// JButton deleteContextButton = new JButton("Delete Context");
		// deleteContextButton.setActionCommand(DELETE_CONTEXT_COMMAND);
		// deleteContextButton.addActionListener(this);

		// Lay everything out.
		// add(treePanelInput, BorderLayout.CENTER);

		JPopupMenu popup = new JPopupMenu("Context Editor");
		JMenuItem addAttribute = new JMenuItem("Add Attribute Node");
		addAttribute.setActionCommand(ADD_ATTRIBUTE);
		addAttribute.addActionListener(this);
		popup.add(addAttribute);
		JMenuItem addData = new JMenuItem("Add Data Node");
		addData.setActionCommand(ADD_DATA);
		addData.addActionListener(this);
		popup.add(addData);
		JMenuItem removeNode = new JMenuItem("Remove Node");
		removeNode.setActionCommand(REMOVE_NODE);
		removeNode.addActionListener(this);
		popup.add(removeNode);
		popup.addSeparator();

		JMenuItem clearContext = new JMenuItem("Clear Context");
		clearContext.setActionCommand(CLEAR_CONTEXT);
		clearContext.addActionListener(this);
		popup.add(clearContext);
		JMenuItem deleteContext = new JMenuItem("Delete Context");
		deleteContext.setActionCommand(DELETE_CONTEXT);
		deleteContext.addActionListener(this);
		popup.add(deleteContext);
		JMenuItem loadContext = new JMenuItem("Load Context");
		loadContext.setActionCommand(LOAD_CONTEXT);
		loadContext.addActionListener(this);
		popup.add(loadContext);
		popup.addSeparator();

		JMenuItem saveContext = new JMenuItem("Save Context");
		saveContext.setActionCommand(SAVE_CONTEXT);
		saveContext.addActionListener(this);
		popup.add(saveContext);
		JMenuItem saveContextAs = new JMenuItem("Save Context...");
		saveContextAs.setActionCommand(SAVE_CONTEXT_AS);
		saveContextAs.addActionListener(this);
		popup.add(saveContextAs);
		popup.addSeparator();

		JMenuItem exertIt = new JMenuItem("Exert");
		exertIt.setActionCommand(LAUNCH_SERVICE);
		exertIt.addActionListener(this);
		popup.add(exertIt);

		treePanelInput.getTree().addMouseListener(new PopupListener(popup));

		// JPanel panel = new JPanel(new GridLayout(0,3));
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		//       
		// panel.add(addPathButton); //row one
		// panel.add(addDataNodeButton);
		// panel.add(removeButton);
		//        
		// panel.add(clearButton); //row 2
		// panel.add(loadOtherButton);
		// panel.add(deleteContextButton);
		//       
		// panel.add(saveButton); //row 3
		// panel.add(saveAsButton);
		panel.add(launchService);
		add(panel, BorderLayout.SOUTH);
		this.setVisible(false);

	}

	class PopupListener extends MouseAdapter {
		private JPopupMenu popupMenu;

		PopupListener(JPopupMenu popupMenu) {
			this.popupMenu = popupMenu;
		}

		/**
		 * Invoked when a mouse button has been pressed on a component.
		 */
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		/**
		 * Invoked when a mouse button has been released on a component.
		 */
		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

	}

	/**
	 * This functions rebuilds the ContextTree with the context specified. This
	 * is called whenever the context is updated. The method works by iterating
	 * thru the context creating returnPath and datanodes on the tree.
	 * 
	 * @param treePanel
	 * @link ContexTree to update to theContext
	 * @param theContext
	 *            Context the tree should be updated with
	 */
	@SuppressWarnings("unchecked")
	public void buildTreeFromContext(ContextTree treePanel, Context theContext) throws RemoteException {

		String path = null;
		int cnt = 0;

		// Enumeration e = null;
		// Hashtable linkPaths = new Hashtable();
		if (theContext != null) {
			Map<String,String> inputs = new HashMap<String,String>();
			Hashtable outputs = new Hashtable();
			try {
				inputs = Contexts.getInPathsMap(theContext);
				outputs = Contexts.getOutPathsMap(theContext);

			} catch (ContextException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			treePanel.rootNode.setUserObject(new ContextNodeType(theContext
					.getSubjectPath(), false));
			ArrayList<String> keys = new ArrayList<String>(
					((ServiceContext) theContext).keySet());
			Collections.sort(keys);

			for (String key : keys) {
				// for(int i = 0; i< sortedVec.size();i++) {
				cnt++;
				// returnPath = (String)sortedVec.elementAt(i);
				path = (String) key;
				// System.out.println("requestPath: "+requestPath);
				String data = "";
				String splitPath[] = path.split("/");
				DefaultMutableTreeNode parentNode = null;
				String currentPath = "";
				for (String indPath : splitPath) // add or getValue the current returnPath
				// element
				{
					if (currentPath.length() != 0)
						currentPath += "/";
					currentPath += indPath;
					DefaultMutableTreeNode exists = treePanel
							.nodeExists(currentPath);
					if (exists == null) {
						ContextNodeType tempNode = new ContextNodeType(indPath,
								false);
						if (inputs.containsKey(currentPath)
								&& !outputs.containsKey(currentPath)) {
							tempNode.setDirection(ContextNodeType.INPUTTYPE);
						}
						if (!inputs.containsKey(currentPath)
								&& outputs.containsKey(currentPath)) {
							tempNode.setDirection(ContextNodeType.OUTPUTTYPE);
						}
						if (!inputs.containsKey(currentPath)
								&& !outputs.containsKey(currentPath)) {
							tempNode.setDirection(ContextNodeType.DEFAULTTYPE);
						}
						if (inputs.containsKey(currentPath)
								&& outputs.containsKey(currentPath)) {
							tempNode.setDirection(ContextNodeType.INOUTPUTTYPE);
						}
						parentNode = treePanel.addObject(parentNode, tempNode,
								true);
					} else
						parentNode = exists;
				}
				try // try to add the node
				{
					if (theContext.getValue(path) instanceof ContextNode) // handle
					// ContextNodes
					// a
					// wee
					// bit
					// differently
					{
						data = ((ContextNode) theContext.getValue(path))
								.getLabel();
						if (data.length() != 0)
							treePanel.addObject(parentNode,
									new ContextNodeType(
											((ContextNode) theContext
													.getValue(path)), true),
									true);
					} else {
						if (theContext.getValue(path).toString().length() != 0)
							treePanel.addObject(parentNode,
									new ContextNodeType(theContext
											.getValue(path), true), true);
					}
				} catch (ContextException e1) {
					e1.printStackTrace();
				}

			}

		}
	}

	/**
	 * This method is called when an action is performed for one of the
	 * registered buttons.
	 * 
	 * Depending on the button the action is different.
	 */
	public void actionPerformed(ActionEvent actionEvent) {
		// System.out.println("Current Context is " + theContext.toString());
		String command = actionEvent.getActionCommand();

		if (ADD_ATTRIBUTE.equals(command)) {
			// Add returnPath button clicked
			if (tabbedContextPane.getSelectedIndex() == 0)
				treePanelInput.addObject(new ContextNodeType("New ATTRIBUTE "
						+ newNodeSuffix++, false));
			if (tabbedContextPane.getSelectedIndex() == 1)
				treePanelOutput.addObject(new ContextNodeType("New ATTRIBUTE "
						+ newNodeSuffix++, false));
		} else if (ADD_DATA.equals(command)) {
			// Add data node button clicked
			if (tabbedContextPane.getSelectedIndex() == 0)
				treePanelInput.addObject(new ContextNodeType("New DATA "
						+ newNodeSuffix++, true));
			if (tabbedContextPane.getSelectedIndex() == 1)
				treePanelOutput.addObject(new ContextNodeType("New DATA "
						+ newNodeSuffix++, true));
		} else if (REMOVE_NODE.equals(command)) {
			// Remove button clicked
			if (tabbedContextPane.getSelectedIndex() == 0)
				treePanelInput.removeCurrentNode();
			if (tabbedContextPane.getSelectedIndex() == 1)
				treePanelOutput.removeCurrentNode();
		} else if (CLEAR_CONTEXT.equals(command)) {
			// Clear button clicked.
			if (tabbedContextPane.getSelectedIndex() == 0)
				treePanelInput.clear();
			if (tabbedContextPane.getSelectedIndex() == 1)
				treePanelOutput.clear();
		} else if (SAVE_CONTEXT.equals(command)) {
			// Save button clicked.
			if (tabbedContextPane.getSelectedIndex() == 0)
				signatureDispatcher.saveContext(treePanelInput
						.generateContext());
			if (tabbedContextPane.getSelectedIndex() == 1)
				signatureDispatcher.saveContext(treePanelOutput
						.generateContext());
		} else if (LOAD_CONTEXT.equals(command)) {
			// Load other button clicked

			loadOtherPanel = new JFrame("Load Context From Different Method");
			JPanel panel = new JPanel();
			loadOtherLabel = new JLabel("Load From a Different Method");
			loadOtherBox = new JComboBox(signatureDispatcher
					.getSavedContextList());
			JButton loadOtherButton = new JButton("Load IT!");
			loadOtherButton.setActionCommand(LOAD_FROM_CONTEXT_CONFIRM);
			loadOtherButton.addActionListener(this);

			panel.add(loadOtherLabel, BorderLayout.PAGE_START);
			panel.add(loadOtherBox, BorderLayout.CENTER);
			panel.add(loadOtherButton, BorderLayout.PAGE_END);
			loadOtherPanel.add(panel);
			loadOtherPanel.setVisible(true);
			loadOtherPanel.setSize(300, 100);

		} else if (LOAD_FROM_CONTEXT_CONFIRM.equals(command)) {
			// load from other confirm
			loadOtherPanel.dispose();
			theContext = signatureDispatcher.getContext(loadOtherBox
				.getSelectedItem().toString()); // getValue the requested context
			treePanelInput.clear();
			try {
				buildTreeFromContext(treePanelInput, theContext);
				Context newOutput = new ServiceContext("No Output");
				treePanelOutput.clear();
				buildTreeFromContext(treePanelOutput, newOutput);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else if (SAVE_CONTEXT_AS.equals(command)) {
			// load save as dialog
			loadOtherPanel = new JFrame("Save Context As: ");
			JPanel panel = new JPanel();
			loadOtherLabel = new JLabel("Save Context As: ");
			saveAsBox = new JTextField();
			saveAsBox.setPreferredSize(new Dimension(100, 20));
			JButton loadOtherButton = new JButton("Save It!");
			loadOtherButton.setActionCommand(SAVE_AS_CONTEXT_CONFIRM);
			loadOtherButton.addActionListener(this);
			panel.add(loadOtherLabel, BorderLayout.PAGE_START);
			panel.add(saveAsBox, BorderLayout.CENTER);
			panel.add(loadOtherButton, BorderLayout.PAGE_END);
			loadOtherPanel.add(panel);
			loadOtherPanel.setVisible(true);
			loadOtherPanel.setSize(300, 100);

		} else if (SAVE_AS_CONTEXT_CONFIRM.equals(command)) {
			// Confirm a save as
			loadOtherPanel.dispose();

			// do save!
			if (tabbedContextPane.getSelectedIndex() == 0)
				signatureDispatcher.saveContext(saveAsBox.getText(),
						treePanelInput.generateContext());
			if (tabbedContextPane.getSelectedIndex() == 1)
				signatureDispatcher.saveContext(saveAsBox.getText(),
						treePanelOutput.generateContext());

		} else if (DELETE_CONTEXT.equals(command)) {
			// load delete context window
			loadOtherPanel = new JFrame("Delete Context");
			JPanel panel = new JPanel();
			loadOtherLabel = new JLabel("Delete Context: ");
			loadOtherBox = new JComboBox(signatureDispatcher
					.getSavedContextList());
			JButton loadOtherButton = new JButton("Delete IT!");
			loadOtherButton.setActionCommand(DELETE_CONTEXT_CONFIRM);
			loadOtherButton.addActionListener(this);
			panel.add(loadOtherLabel, BorderLayout.PAGE_START);
			panel.add(loadOtherBox, BorderLayout.CENTER);
			panel.add(loadOtherButton, BorderLayout.PAGE_END);
			loadOtherPanel.add(panel);
			loadOtherPanel.setVisible(true);
			loadOtherPanel.setSize(300, 100);

		} else if (DELETE_CONTEXT_CONFIRM.equals(command)) {
			// Confirm a delete and compute the action
			loadOtherPanel.dispose();
			if (signatureDispatcher.deleteContext(loadOtherBox
				.getSelectedItem().toString())) {
				theContext = new ServiceContext("root");
				treePanelInput.clear();
				try {
					buildTreeFromContext(treePanelInput, theContext);
					treePanelOutput.clear();
					buildTreeFromContext(treePanelOutput, theContext);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		} else if (LAUNCH_SERVICE.equals(command)) {
			ServiceContext result = new ServiceContext("FAILED"); // start off
			// with
			// failure,
			// will getValue
			// replaced
			// if it
			// works
			try {
				if (tabbedContextPane.getSelectedIndex() == 0) // do the
					// currently
					// displayed
					// context
					result = (ServiceContext) signatureDispatcher
							.exertService(treePanelInput.generateContext());
				if (tabbedContextPane.getSelectedIndex() == 1)
					result = (ServiceContext) signatureDispatcher
							.exertService(treePanelOutput.generateContext());
			} catch (Exception e) {
				System.out.println("e is " + e.getMessage());

			}

			// build output tree and select it
			tabbedContextPane.setSelectedIndex(1);
			treePanelOutput.clear();
			try {
				buildTreeFromContext(treePanelOutput, result);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			/*
			 * execEnt this on for the cool output window! JFrame servicePanel=new
			 * JFrame("Service Output"); JTabbedPane tabbedPane = new
			 * JTabbedPane();
			 * 
			 * JComponent panel1 =
			 * makeTextPanel("INPUT CONTEXT\n\n"+model.context.toString());
			 * tabbedPane.addTab("Input Context", null, panel1,
			 * "Input Context"); tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
			 * 
			 * JComponent panel4 =
			 * makeTextPanel("OUTPUT CONTEXT\n\n"+result.toString());
			 * panel4.setPreferredSize(new Dimension(410, 200));
			 * 
			 * tabbedPane.addTab("Output Context", null, panel4,
			 * "Output Context"); tabbedPane.setMnemonicAt(1, KeyEvent.VK_4);
			 * tabbedPane.setSelectedIndex(1);
			 * 
			 * servicePanel.add(tabbedPane); servicePanel.pack();
			 * servicePanel.setVisible(true);
			 */
		}

	}

	/**
	 * this function is called if you are using the the debug output panel for
	 * exerting services
	 * 
	 * @param text
	 *            String to put in the new text area
	 * @return JComponent with the text area in it
	 */
	protected JComponent makeTextPanel(String text) {
		JPanel panel = new JPanel(false);
		JTextArea filler = new JTextArea(text);
		filler.setLineWrap(true);
		filler.setEditable(false);
		panel.setLayout(new GridLayout(1, 1));
		panel.add(filler);
		return panel;
	}

	/**
	 * Updates the context view when the BrowserModel has changed.
	 * 
	 * @param context
	 *            : the context that will be updated
	 * @param update
	 *            : object that is updated
	 */
	public void update(Observable context, Object updated) {
		model = (BrowserModel) context;

		// hide myself if anything but the context changes
		if (updated.toString().equals(BrowserModel.PROVIDER_UPDATED))
			this.setVisible(false);
		if (updated.toString().equals(BrowserModel.INTERFACE_UPDATED))
			this.setVisible(false);
		if (updated.toString().equals(BrowserModel.METHOD_UPDATED))
			this.setVisible(false);

		if (updated.equals(BrowserModel.CONTEXT_UPDATED)) // show on new context
		{
			if (model.getSelectedMethod() == null) // validate the request
			{
				this.setVisible(false);
				return;
			}
			this.setVisible(true); // show it

			// clearSessions input and output and create tree for new input
			treePanelInput.clear();
			theContext = model.getContext();
			try {
				buildTreeFromContext(treePanelInput, theContext);
				treePanelOutput.clear();
				Context newOutput = new ServiceContext("No Output");
				buildTreeFromContext(treePanelOutput, newOutput);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

}
