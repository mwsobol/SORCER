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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import sorcer.core.context.node.ContextNode;

/**
 * This class is used for the edit window displayed when a path or data node is
 * modified. The view is rendered differently depending on if the currently
 * selected impl is a path or if it is a data node.
 * 
 * <li>For paths user can specify the pathname and direction. <li>For data nodes
 * the user can select the data multitype and execEnt the types eval
 * 
 * <b>Note if the provider is not complied with Groovy support the Groovy option
 * will not be available</b>
 * 
 * This is where new data types can be introduced using the following three
 * steps: <li>add them to the dataTypes list defined in the constructor <li>
 * modify the save action (ActionPerformed) to support the new datatype creation
 * in the try/catch loop <li>add specific instructions in the @link
 * DataTypeListener actionPerformed event.
 * 
 * @author Greg McChesney
 * 
 */
public class ContextEditWindow extends JFrame implements ActionListener {
	/**
	 * id used for serialization
	 */
	private static final long serialVersionUID = 1050541545115151510L;

	/**
	 * ContextTree representing the current data tree.
	 */
	private ContextTree theTree;

	/**
	 * the node the user selected.
	 */
	private TreePath currentSelected;
	private JTextArea editField;
	private JLabel dataLabel;
	private JLabel instructions;
	private JComboBox dataType;
	private JComboBox direction;
	private JButton editSaveButton;
	private static String SAVE_BUTTON = "save";
	private ContextNodeType theNodeType;

	/**
	 * current node the user is modifying
	 */
	private DefaultMutableTreeNode currentNode;
	// private String currentlySelectedDataType;
	private JScrollPane scrollingArea;

	/**
	 * Constructor used to create the edit window.
	 * 
	 * @param theRootTree
	 *            the tree the node belongs to
	 * @param currentPath
	 *            the current path the user selected to modify, this is the data
	 *            the window will display/update
	 */
	public ContextEditWindow(ContextTree theRootTree, TreePath currentPath) {
		theTree = theRootTree;
		currentSelected = currentPath;

		currentNode = (DefaultMutableTreeNode) currentSelected
				.getLastPathComponent();

		Object nodeContext = currentNode.getUserObject();
		// check that the node of the tree is our special multitype
		if (nodeContext instanceof ContextNodeType) {
			theNodeType = (ContextNodeType) nodeContext;
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			JPanel listPane = new JPanel();
			listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));

			instructions = new JLabel("<html><b>Enter the eval below<b><br>");

			editField = new JTextArea(theNodeType.getValue());
			scrollingArea = new JScrollPane(editField);
			// editField.setPreferredSize(new Dimension())

			if (theNodeType.getObject() instanceof ContextNode)
				editField.setText(((ContextNode) theNodeType.getObject())
						.getLabel());
			// GroovyObject test=new GroovyObject("1+1");

			// editField.setText(test.execute().toString());
			editField.setColumns(60);
			editField.setRows(1);
			editField.setLineWrap(true);
			editField.setWrapStyleWord(true);

			// editField.setPreferredSize(new Dimension(350, 20));
			dataLabel = new JLabel("Data Types:");
			dataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

			String[] dataTypes = { "String", "Integer", "Boolean", "Groovy",
					"Float", "Double", "URL" };

			String[] directions = { ContextNodeType.DEFAULTTYPE,
					ContextNodeType.INPUTTYPE, ContextNodeType.OUTPUTTYPE,
					ContextNodeType.INOUTPUTTYPE };

			dataType = new JComboBox(dataTypes);
			dataType.addActionListener(new DataTypeListener(instructions,
					editField, this));
			direction = new JComboBox(directions);
			String currentDtype = theNodeType.getObject().getClass()
					.getSimpleName();
			if (theNodeType.getObject() instanceof ContextNode)
				currentDtype = ((ContextNode) theNodeType.getObject())
						.getDataTypeString();
			if (currentDtype.equals("ContextGroovyObject"))
				currentDtype = "Groovy";

			for (int i = 0; i < dataTypes.length; i++)
				if (dataTypes[i].equals(currentDtype))
					dataType.setSelectedIndex(i);
			String currentDirection = theNodeType.getDirection();
			for (int i = 0; i < directions.length; i++)
				if (directions[i].equals(currentDirection))
					direction.setSelectedIndex(i);
			// System.out.println(currentDtype+" dtype");
			editSaveButton = new JButton("Save");
			editSaveButton.setActionCommand(SAVE_BUTTON);
			editSaveButton.addActionListener(this);

			JPanel instructionLabelPanel = new JPanel();
			instructionLabelPanel.setLayout(new BoxLayout(
					instructionLabelPanel, BoxLayout.LINE_AXIS));

			instructionLabelPanel.add(instructions);
			instructionLabelPanel.add(Box.createHorizontalGlue());
			listPane.add(instructionLabelPanel);
			listPane.add(Box.createRigidArea(new Dimension(0, 5)));
			// panel.add(new JLabel("Value:"));
			listPane.add(scrollingArea);
			listPane.add(Box.createRigidArea(new Dimension(0, 5)));

			if (!theNodeType.isDataNode()) // output direction if its a path
			{
				dataType.setEnabled(false);
				JPanel dataLabelPanel = new JPanel();
				dataLabelPanel.setLayout(new BoxLayout(dataLabelPanel,
						BoxLayout.LINE_AXIS));

				dataLabelPanel.add(new JLabel("Direction: "));
				dataLabelPanel.add(Box.createRigidArea(new Dimension(10, 0)));
				dataLabelPanel.add(direction);
				// dataLabelPanel.add(Box.createHorizontalGlue());
				listPane.add(dataLabelPanel);
				// listPane.add(direction);
			} else // a data node output the multitype
			{

				JPanel dataLabelPanel = new JPanel();
				dataLabelPanel.setLayout(new BoxLayout(dataLabelPanel,
						BoxLayout.LINE_AXIS));

				dataLabelPanel.add(dataLabel);
				dataLabelPanel.add(Box.createRigidArea(new Dimension(10, 0)));
				direction.setEnabled(false);
				dataLabelPanel.add(dataType);
				// dataLabelPanel.add(Box.createHorizontalGlue());
				listPane.add(dataLabelPanel);

			}
			panel.add(listPane);
			panel.add(editSaveButton);
			this.add(panel);
			// this.setSize(500, 250);
			// center the frame on screen
			setLocationRelativeTo(null);
			this.setVisible(true);
			this.setTitle("Edit Node");
		} else
			// node is not our multitype so lets ignore this request
			this.dispose();

	}

	/**
	 * ActionPerformed is called when the save button is pressed. This method
	 * takes the data and the selected multitype or direction (depending on if its a
	 * path or node) and saves it back in the @link ContextNodeType.
	 * 
	 * @param actionEvent
	 *            ActionEvent specifying what action occurred.
	 * 
	 */
	public void actionPerformed(ActionEvent actionEvent) {
		// TODO Auto-generated method stub
		String command = actionEvent.getActionCommand();
		// System.out.println("command"+command);
		if (SAVE_BUTTON.equals(command)) {
			String dType = dataType.getSelectedItem().toString();
			Object value = new String(editField.getText());// default to String
			// datatype
			try // try to create the data multitype the user requested, add new
			// datatypes here
			{

				if (dType.equals("Integer"))
					value = new Integer(editField.getText());
				if (dType.equals("Boolean"))
					value = new Boolean(editField.getText());
				if (dType.equals("Groovy")) {
					value = new ContextNode(editField.getText(),
							new ContextGroovyObject(editField.getText()));
				}
				if (dType.equals("Float"))
					value = new Float(editField.getText());
				if (dType.equals("Double"))
					value = new Double(editField.getText());
				if (dType.equals("URL"))
					value = new ContextNode(editField.getText(), new URL(
							editField.getText()));
			} catch (Exception e) {
				// add some code to alert user they are not doing it right
			}

			theNodeType.setValue(value); // update the eval
			theNodeType.setDirection(direction.getSelectedItem().toString());
			// currentNode.setUserObject(theNodeType);
			theTree.updateModel(currentNode);
			TreeNode[] pathTo = currentNode.getPath();
			String path = "";
			int length = pathTo.length;

			if (!theNodeType.isDataNode()) // try to change the tree colors to
			// show input and output
			{
				// length--;
				for (int i = 1; i < length; i++) // ignore root and current
				// element!
				{
					// if(path.length()!=0)
					// /path+="/";
					// path+=pathTo[i].toString();
					if (((DefaultMutableTreeNode) pathTo[length - i - 1])
							.getUserObject() instanceof ContextNodeType) {
						if (((DefaultMutableTreeNode) pathTo[length - i - 1])
								.getChildCount() == 1) {
							ContextNodeType parentNode = (ContextNodeType) ((DefaultMutableTreeNode) pathTo[length
									- i - 1]).getUserObject();
							parentNode.setDirection(direction.getSelectedItem()
									.toString());
							theTree
									.updateModel((DefaultMutableTreeNode) pathTo[length
											- i - 1]);
							i = length + 2;
							break;
						}
					}
				}
				// execEnt of changing children on update, child coloring allows
				// all elements under a node
				// to getValue the same direction color, code works but can lead to
				// really annoying results
				// ex: change the root and all directions below it getValue
				// removed....
				// SetChildDirection(currentNode,
				// direction.getSelectedItem().toString());
			}
			this.dispose();
		}
	}

	/**
	 * This recursive function is used to change the directions of all elements
	 * underneath the current node. The function works but can lead to doing
	 * more than the user was probably attempting to do.
	 * 
	 * @param current
	 *            DefaultMutableTreeNode the current node we are working on
	 * @param direction
	 *            String specifying the current direction to set the children
	 *            nodes to
	 */
	void SetChildDirection(DefaultMutableTreeNode current, String direction) {
		for (int i = 0; i < current.getChildCount(); i++) // do it for each
		// child
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) current
					.getChildAt(i);
			if (child.getUserObject() instanceof ContextNodeType) // make sure
			// its of
			// our
			// special
			// multitype
			{
				((ContextNodeType) child.getUserObject())
						.setDirection(direction);
				theTree.updateModel(child);
				if (child.getChildCount() != 0)
					SetChildDirection(child, direction);
			}
		}
	}

	/**
	 * This class acts as a ActionListener for the DataType field on the form.
	 * This class's purpose is to update the instruction label based on the
	 * currently selected impl in the DataType field. This allows the user to
	 * instructions that are specific to current data multitype. The class also
	 * updates the current size of the window so that the instructions fit
	 * properly. The edit field is modified to allowing for a more accurate
	 * representation of the data. All instructions are written in HTML.
	 * 
	 * @author Greg McChesney
	 * 
	 */

	private class DataTypeListener implements ActionListener {
		/**
		 * the instruction label to be updated
		 */
		private JLabel instructions;

		/**
		 * the text field the user enters the data into.
		 */
		private JTextArea editField;
		/**
		 * the edit window itself, used to change the size of the window
		 */
		private JFrame mainPanel;

		/**
		 * This is the constructor for the DataTypeListener, which updates the
		 * editWindow based on the currently selected data multitype.
		 * 
		 * @param instructions
		 *            JLabel to update with the current instructions
		 * @param editField
		 *            JTextArea to modify for the current data multitype
		 * @param mainPanel
		 *            JFrame for the edit window to modify the size
		 */
		public DataTypeListener(JLabel instructions, JTextArea editField,
				JFrame mainPanel) {
			this.instructions = instructions;
			this.editField = editField;
			this.mainPanel = mainPanel;
		}

		/**
		 * The actionPerformed event takes the current select and updates the
		 * interface to reflect the data multitype. For example Groovy expressions
		 * have a larger edit window and more detailed instructions than an
		 * integer field. This is also how new instructions can be added to the
		 * interface, if an instruction is not specified the default one will be
		 * displayed.
		 */
		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			String currentSelection = (String) cb.getSelectedItem();
			if (currentSelection.equals("Groovy")) {
				instructions
						.setText("<html><b>This is groovy expression</b><p>Instructions for groovy expressions: <ul> <li>Specify paths using _ as a seperator<li>Provider must support the ContextNode object to compute</ul><p>  ");
				editField.setColumns(60);
				editField.setRows(4);
				// mainPanel.setPreferredSize(new Dimension(500,200));
				mainPanel.setSize(new Dimension(500, 250));

				// todo add groovy not complied check
			} else if (currentSelection.equals("URL")) {
				instructions
						.setText("<html><b>This is URL</b> <p>Enter using http:// or ftp://<p>");
				editField.setColumns(60);
				editField.setRows(1);
				// mainPanel.setPreferredSize(new Dimension(500,100));
				mainPanel.setSize(new Dimension(500, 140));
			} else {
				instructions.setText("<html><b>Enter a " + currentSelection
						+ " eval below<b><p>");
				editField.setColumns(60);
				editField.setRows(1);
				// mainPanel.setPreferredSize(new Dimension(500,100));
				mainPanel.setSize(new Dimension(500, 125));

			}
		}
	}
}
