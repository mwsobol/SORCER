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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import sorcer.core.context.ServiceContext;
import sorcer.core.context.node.ContextNode;
import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * ContextTree is a JPanel that contains the Jtree as well as the TreeModel it
 * represents. This class contains methods for creating the context from the
 * tree as well as adding and removing nodes. This class also contains several
 * subclasses to control mouse behavior, the tree rendering and monitor changes
 * to the tree.
 * 
 * @author Greg McChesney
 * 
 */
public class ContextTree extends JPanel {

	/**
	 * Used for serialization of the object
	 */
	private static final long serialVersionUID = 3257654238358101331L;

	/**
	 * rootNode of the tree
	 */
	protected DefaultMutableTreeNode rootNode;

	/**
	 * Domain the tree is based off of
	 */
	protected DefaultTreeModel treeModel;

	/**
	 * the actual tree that is displayed and edited
	 */
	protected JTree tree;

	public JTree getTree() {
		return tree;
	}

	/**
	 * Used to make beep sounds in the event of an error.
	 */
	private Toolkit toolkit = Toolkit.getDefaultToolkit();

	/**
	 * A reference to this object, we will use this in our MouseListener
	 */
	private ContextTree thisThat;

	/**
	 * treeMouseListener is used to react to double clicking by the user, with
	 * this we can detect the double click and open the edit window with the
	 * currently selected node.
	 */
	MouseListener treeMouseListener = new MouseAdapter() {
		/**
		 * Callable called when the user presses the mouse
		 */
		public void mousePressed(MouseEvent e) {
			int selRow = tree.getRowForLocation(e.getX(), e.getY()); // get the
			// current row
			TreePath selPath = tree.getPathForLocation(e.getX(), e.getY()); // find
			// that spot in the tree
			if (selRow != -1) // make sure its valid
			{
				if (e.getClickCount() == 1) {
					// System.out.println("Single click "+selPath);
				} else if (e.getClickCount() == 2) // handle 2 clicks
				{
					// System.out.println("double click "+selPath);
					ContextEditWindow temp = new ContextEditWindow(thisThat,
							selPath); // run the edit window
				}
			}
		}
	};

	/**
	 * Constructor for the ContextTree class, creates a blank tree and default
	 * tree model.
	 * 
	 */
	public ContextTree() {
		super(new GridLayout(1, 0));

		thisThat = this; // get a reference to our current object

		// make a new root node
		rootNode = new DefaultMutableTreeNode(new ContextNodeType(new String(
				"Root Node"), false));

		// make the new model
		treeModel = new DefaultTreeModel(rootNode);

		// add a listener to our model, not in use currently
		// treeModel.addTreeModelListener(new ContextTreeModelListener());

		// create the tree with the model
		tree = new JTree(treeModel);
		tree.setEditable(false);// maybe later we might want it

		// make it single select
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.setShowsRootHandles(true);
		tree.addMouseListener(treeMouseListener); // add the mouse listener to
		// the tree

		tree.setCellRenderer(new ContextTreeRenderer()); // add the cell
		// renderer, where
		// we change the
		// colors and stuff

		JScrollPane scrollPane = new JScrollPane(tree); // put the tree in a
		// scroll pane
		add(scrollPane);
	}

	/**
	 * This method generates the context from the current tree model, it is
	 * significantly easier to make a new context from the tree than it is to
	 * try to maintain the current context thru the changes to the tree. This
	 * function calls doRootGenerateContext which recursively creates the
	 * context.
	 * 
	 * @return Context representing the tree.
	 */
	public Context generateContext() {
		ServiceContext context = new ServiceContext(rootNode.getUserObject()
				.toString()); // put root name here!
		context = (ServiceContext) doRootGenerateContext((Context) context,
				treeModel.getRoot(), "");
		return context;
	}

	/**
	 * This function recursively goes thru the tree adding items to the context
	 * using the tree path as the path for the context and the datanode as the
	 * data. The function also marks the node for the type of data it is
	 * (input/output).
	 * 
	 * @param theContext
	 *            Context we are currently adding to
	 * @param current
	 *            Object representing were we currently are in the tree
	 * @param currentPath
	 *            String representing the current path we are in the tree, added
	 *            to with the context path separator of /
	 * @return Context we added to.
	 */
	public Context doRootGenerateContext(Context theContext, Object current,
			String currentPath) {

		int childcount = treeModel.getChildCount(current); // get the number of
		// kids

		if (childcount == 0 && current != treeModel.getRoot()) // make sure we
		// have kids and
		// that we are
		// not at root
		{ // dont want root because it is technically the name of the context
			// and not a valid path

			// we have no kids so we just output the path.
			try {
				Object child = ((DefaultMutableTreeNode) current)
						.getUserObject();
				if (child instanceof ContextNodeType) // check if is of our
				// ContextNodeType
				{
					ContextNodeType cnt = (ContextNodeType) child;

					// output the path with the correct type
					if (cnt.getDirection().equals(ContextNodeType.INPUTTYPE))
						theContext.putInValue(currentPath, "");
					if (cnt.getDirection().equals(ContextNodeType.OUTPUTTYPE))
						theContext.putOutValue(currentPath, "");
					if (cnt.getDirection().equals(ContextNodeType.INOUTPUTTYPE))
						theContext.putInoutValue(currentPath, "");
					if (cnt.getDirection().equals(ContextNodeType.DEFAULTTYPE))
						theContext.putValue(currentPath, "");
				}
			} catch (ContextException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else // has kids or is the root
		{
			for (int i = 0; i < childcount; i++) // go thru each kid
			{
				Object child = treeModel.getChild(current, i); // get the kid
				// add is datanode check
				// System.out.println("checking child number "+i);
				if (child instanceof DefaultMutableTreeNode) // make sure the
				// kid is a tree
				// node
				{
					// System.out.println("child "+i+" is a tree nody ");
					if (((DefaultMutableTreeNode) child).getUserObject() instanceof ContextNodeType) // check
					// if
					// its
					// our
					// special
					// type
					{
						// System.out.println("child user object is a context node "
						// +i);
						ContextNodeType cnt = (ContextNodeType) ((DefaultMutableTreeNode) child)
								.getUserObject(); // get the special type

						if (cnt.isDataNode()) // if its a datanode we need to
						// output it, as data nodes can
						// have no children
						{
							// System.out.println("current is a dataNode "+i+" data "+cnt.toString());
							try {
								child = ((DefaultMutableTreeNode) current)
										.getUserObject();
								if (child instanceof ContextNodeType) {
									ContextNodeType cnt2 = (ContextNodeType) child;
									// output the data and direction marking
									if (cnt2.getDirection().equals(
											ContextNodeType.INPUTTYPE))
										theContext.putInValue(currentPath, cnt
												.getObject());
									if (cnt2.getDirection().equals(
											ContextNodeType.OUTPUTTYPE))
										theContext.putOutValue(currentPath, cnt
												.getObject());
									if (cnt2.getDirection().equals(
											ContextNodeType.INOUTPUTTYPE))
										theContext.putInoutValue(currentPath,
												cnt.getObject());
									if (cnt2.getDirection().equals(
											ContextNodeType.DEFAULTTYPE))
										theContext.putValue(currentPath, cnt
												.getObject());
								}
							} catch (ContextException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else // child is another path node, recursivly call
						// this function on that node
						{

							String tempPath = currentPath;
							if (currentPath.length() != 0) // currentPath+"/"+((DefaultMutableTreeNode)current).getUserObject().toString();
								tempPath += "/";
							tempPath += cnt.getValue(); // update the path
							// System.out.println(" not data node "+i+" tp="+tempPath+" curr");

							// call on the child node
							theContext = doRootGenerateContext(theContext,
									treeModel.getChild(current, i), tempPath);
						}

					}
				}

			}
		}

		/*
		 * String cxt=new String(); StringBuffer buf=new StringBuffer();
		 * ((ServiceContext)theContext).toStringComplete(cxt, buf);
		 * System.out.println("current context is "+cxt);
		 */
		return theContext;
	}

	/**
	 * Remove all nodes except the root node.
	 * 
	 */
	public void clear() {
		rootNode.removeAllChildren();
		treeModel.reload();
	}

	/**
	 * Forces the tree to be re-rendered, notifies the model that a change
	 * occurred to changedNode.
	 * 
	 * @param changedNode
	 *            DefaultMutableTreeNode that was changed
	 */
	public void updateModel(DefaultMutableTreeNode changedNode) {
		treeModel.nodeChanged(changedNode);
	}

	/**
	 * Remove the currently selected node, if it has children their children are
	 * removed too. Nothing happens if called on the root node.
	 */
	public void removeCurrentNode() {
		TreePath currentSelection = tree.getSelectionPath();
		if (currentSelection != null) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection
					.getLastPathComponent());
			MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());

			if (parent != null) // check if root
			{
				treeModel.removeNodeFromParent(currentNode);
				return;
			}
		}

		// Either there was no select, or the root was selected.
		toolkit.beep();
	}

	/**
	 * Add child to the currently selected node, if no node is selected child is
	 * added to the root.
	 * 
	 * @param child
	 *            Object representing data to be stored in the tree
	 * @return DefaultMutableTreeNode of the newly created treenode
	 */
	public DefaultMutableTreeNode addObject(Object child) {
		DefaultMutableTreeNode parentNode = null;
		TreePath parentPath = tree.getSelectionPath();

		if (parentPath == null) // no node selected use root
		{
			parentNode = rootNode;
		} else {
			parentNode = (DefaultMutableTreeNode) (parentPath
					.getLastPathComponent());
		}

		return addObject(parentNode, child, true);
	}

	/**
	 * Add child to the tree under the parent node specified.
	 * 
	 * @param parent
	 *            DefaultMutableTreeNode node where the data should be added to
	 * @param child
	 *            Object representing data to be stored in the tree
	 * @return DefaultMutableTreeNode of the newly created treenode
	 */
	public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
			Object child) {
		return addObject(parent, child, false);
	}

	/**
	 * Add child to the tree under parent, and setValue the visibility of it. If
	 * parent is incorrect the child will be added under the root. This method
	 * enforces rules prevents any item from appears as a child to a datanode,
	 * also only allows one data node for path.
	 * 
	 * 
	 * @param parent
	 *            DefaultMutableTreeNode node where the data should be added to
	 * @param child
	 *            Object representing data to be stored in the tree
	 * @param shouldBeVisible
	 *            Boolean indicating if the new node should be expanded or
	 *            collapsed
	 * @return DefaultMutableTreeNode of the newly created treenode
	 */
	public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
			Object child, boolean shouldBeVisible) {
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

		if (parent == null) {
			parent = rootNode;
		}

		if (parent.getUserObject() instanceof ContextNodeType) // check if the
		// current type
		// is our
		// special type
		{
			ContextNodeType parentObject = (ContextNodeType) parent
					.getUserObject(); // get the parent

			if (parentObject.isDataNode()) // can't add to datanode object type
				return null;

			if (child instanceof ContextNodeType) {
				if (((ContextNodeType) child).isDataNode()) {
					if (parent == rootNode)
						return null; // prevent datanode children on the root
					for (int i = 0; i < parent.getChildCount(); i++) // only one
					// datanode
					// per-path
					{
						DefaultMutableTreeNode cNode = (DefaultMutableTreeNode) treeModel
								.getChild(parent, i);
						if (cNode.getUserObject() instanceof ContextNodeType) {
							if (((ContextNodeType) cNode.getUserObject())
									.isDataNode())
								return null; // enforce only one datanode per
							// path
						}
					}
				}
			}
		}

		// It is key to invoke this on the TreeModel, and NOT
		// DefaultMutableTreeNode
		treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

		// Make sure the user can see the lovely new node.
		if (shouldBeVisible) {
			tree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}
		return childNode; // return the new node
	}

	/**
	 * Returns the DefaultMutableTreeNode represented by the path specified.
	 * 
	 * @param path
	 *            String representing the path to find
	 * @return DefaultMutableTreeNode for the path requested, returns null if
	 *         the path was not found
	 */
	public DefaultMutableTreeNode nodeExists(String path) {
		String splitPath[] = path.split("/"); // get each part of the path
		Object nodes = treeModel.getRoot();
		for (String indPath : splitPath) // go thru each part of the path
		{
			Boolean foundNode = false; // indicate we have not found the node
			// System.out.println("working on "+indPath);
			int count = treeModel.getChildCount(nodes);
			// System.out.println(nodes.toString()+" has "+count+" kids");
			for (int i = 0; i < count; i++) // go true each child
			{
				Object node = treeModel.getChild(nodes, i);
				// System.out.println(node.toString()+" is node number "+i);
				if (indPath.equals(node.toString())) // matched this part, keep
				// going
				{
					// System.out.println("nodes matched");
					nodes = node;
					foundNode = true;
					break;
				}
			}
			if (!foundNode) // no match kill it off
				return null;

			// System.out.println("Children of path"+indPath+" num:"+treeModel.getChildCount(new
			// DefaultMutableTreeNode(indPath)));
		}

		return (DefaultMutableTreeNode) nodes; // return the node found
	}

	/*
	 * we don't currently need the listener but we may need it for something in
	 * the future so I will leave the code in for any future coders. class
	 * ContextTreeModelListener implements TreeModelListener {
	 * 
	 * public void treeNodesChanged(TreeModelEvent e) {
	 */
	// System.out.println("editing here");
	// DefaultMutableTreeNode node;
	// node = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());

	/*
	 * If the event lists children, then the changed node is the child of the
	 * node we've already gotten. Otherwise, the changed node and the specified
	 * node are the same.
	 */
	// add check for root?
	// http://www.velocityreviews.com/forums/t124781-double-click-in-a-jtree.html
	// for double click
	// http://www.coderanch.com/t/334193/Swing-AWT-SWT-JFace/java/JTree-double-click-event
	//
	// int index = e.getChildIndices()[0];
	// node = (DefaultMutableTreeNode)(node.getChildAt(index));

	// System.out.println("The user has finished editing the node.");
	// System.out.println("New eval: " + node.getUserObject());
	/*
	 * } public void treeNodesInserted(TreeModelEvent e) { } public void
	 * treeNodesRemoved(TreeModelEvent e) { } public void
	 * treeStructureChanged(TreeModelEvent e) { } }
	 */

	/**
	 * This class overrides the default rendering of a tree cell. We use this to
	 * setValue the colors and icons of nodes.
	 */
	private class ContextTreeRenderer extends DefaultTreeCellRenderer {
		/**
		 * id used for serialization
		 */
		private static final long serialVersionUID = 3257654248358101731L;

		/**
		 * default constructor with no arguments
		 */
		public ContextTreeRenderer() {
		}

		/**
		 * this is the method called when the cell is being rendered. Parameters
		 * are straight from the DefaultTreeCellRenderer
		 * 
		 * @return Component for the rendering
		 */
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			Object userObject = ((DefaultMutableTreeNode) value)
					.getUserObject(); // get the inner object
			if (userObject instanceof ContextNodeType) // validate its our type
			{

				Component c; // get the original component by calling super

				if (((ContextNodeType) userObject).getObject() instanceof ContextNode) // handle
					// context
					// nodes
					// by
					// getting
					// the
					// label
					c = super.getTreeCellRendererComponent(tree,
							((ContextNode) ((ContextNodeType) userObject)
									.getObject()).getLabel(), sel, expanded,
							leaf, row, hasFocus);
				else
					// handle all other types
					c = super.getTreeCellRendererComponent(tree,
							((ContextNodeType) userObject).toString(), sel,
							expanded, leaf, row, hasFocus);

				if (((ContextNodeType) userObject).isDataNode()) // setValue the
					// datanodes
					// to leaf
					// icons
					setIcon(leafIcon);
				else
					setIcon(openIcon); // setValue the paths to folder icons

				// setValue those crazy input/output colors
				if (((ContextNodeType) userObject).getDirection().equals(
						ContextNodeType.INPUTTYPE)) {
					c.setForeground(Color.blue);

				}
				if (((ContextNodeType) userObject).getDirection().equals(
						ContextNodeType.OUTPUTTYPE)) {

					c.setForeground(Color.red);
				}
				if (((ContextNodeType) userObject).getDirection().equals(
						ContextNodeType.INOUTPUTTYPE)) {

					c.setForeground(Color.green);
				}
				if (((ContextNodeType) userObject).getDirection().equals(
						ContextNodeType.DEFAULTTYPE)) {

				}
			} else
				// not a ContextNodeType so show default rendering
				super.getTreeCellRendererComponent(tree, value, sel, expanded,
						leaf, row, hasFocus);

			return this;
		}
	}

}
