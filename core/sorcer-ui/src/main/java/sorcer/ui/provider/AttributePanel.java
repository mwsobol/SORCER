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

package sorcer.ui.provider;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.jini.core.entry.Entry;
import sorcer.service.Exerter;

public class AttributePanel extends JPanel {
	private Exerter provider;

	private Entry[] attributes;

	private JTree tree;

	private AttributeRootNode root;

	private DefaultTreeModel model;

	private DefaultTreeSelectionModel smodel;

	private TreeSelectionListener slistener;

	private JScrollPane scrollPane;

	private JCheckBoxMenuItem showPackage;

	private JCheckBoxMenuItem showSupertypes;

	// key=Entry Class, val=EntryNode with FiledNodes (ValueNodes are cleared)
	private HashMap entryCache = new HashMap();

	private HashMap valueCache = new HashMap();

	public AttributePanel(Exerter prv) {
		this(prv, 0);
	}

	public AttributePanel(Exerter prv, int visibleRows) {
		provider = prv;
		try {
			attributes = provider.getAttributes();
		} catch (Exception pe) {
			pe.printStackTrace();
		}
		setLayout(new GridLayout(1, 1));
		// Init tree node and model (attribute tree nodes)
		root = new AttributeRootNode();
		model = new DefaultTreeModel(root);

		// Init tree view
		tree = new JTree(model);
		if (visibleRows != 0)
			tree.setVisibleRowCount(visibleRows);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		smodel = new DefaultTreeSelectionModel();
		smodel
				.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		smodel
				.addTreeSelectionListener(slistener = new AttributeSelectionListener());
		tree.setSelectionModel(smodel);
		tree.setCellRenderer(new AttributeCellRenderer());
		tree.addMouseListener(new AttributePopup());

		scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(scrollPane, BorderLayout.CENTER);

		// validate components
		tree.validate();
		scrollPane.validate();
	}

	private void updateTree() {
		// update EntryNode
		HashSet newEntrySet = getEntryClassSet();

		Set addedEntrySet = root.getAddedEntryClass(newEntrySet);
		for (Iterator iter = addedEntrySet.iterator(); iter.hasNext();) {
			Class entryClass = (Class) iter.next();
			if (entryCache.containsKey(entryClass)) {
				EntryNode enode = (EntryNode) entryCache.get(entryClass);
				int index = root.getLexicographicalIndex(enode);
				model.insertNodeInto(enode, root, index);
			} else {
				EntryNode enode = new EntryNode(entryClass);
				int index = root.getLexicographicalIndex(enode);
				model.insertNodeInto(enode, root, index);

				// add field nodes for each new entry
				Set fset = getFieldSet(entryClass);
				for (Iterator fiter = fset.iterator(); fiter.hasNext();) {
					FieldNode fnode = new FieldNode((Field) fiter.next());
					int findex = enode.getLexicographicalIndex(fnode);
					model.insertNodeInto(fnode, enode, findex);
				}

				entryCache.put(entryClass, enode);
			}
		}

		Set removedEntrySet = root.getRemovedEntryClass(newEntrySet);
		for (Iterator iter = removedEntrySet.iterator(); iter.hasNext();) {
			Class entryClass = (Class) iter.next();

			EntryNode enode = root.getEntryNode(entryClass);
			enode.clearValues();
			model.removeNodeFromParent(enode);
		}

		// update ValueNode
		for (int i = 0; i < root.getChildCount(); i++) {
			EntryNode enode = (EntryNode) root.getChildAt(i);
			Class eclass = (Class) enode.getEntryClass();

			HashMap subcache = (HashMap) valueCache.get(eclass);
			if (subcache == null)
				valueCache.put(eclass, subcache = new HashMap());

			try {
				Entry entry = (Entry) eclass.newInstance();
				for (int j = 0; j < enode.getChildCount(); j++) {
					FieldNode fnode = (FieldNode) enode.getChildAt(j);
					String fname = fnode.getFieldName();

					HashMap subsubcache = (HashMap) subcache.get(fname);
					if (subsubcache == null)
						subcache.put(fname, subsubcache = new HashMap());

					HashSet vnew = getFieldValueSet(entry, fname);

					Set addedValueSet = fnode.getAddedValue(vnew);
					for (Iterator iter = addedValueSet.iterator(); iter
							.hasNext();) {
						Object value = iter.next();

						if (subsubcache.containsKey(value)) {
							ValueNode vnode = (ValueNode) subsubcache
									.get(value);
							int vindex = fnode.getLexicographicalIndex(vnode);
							model.insertNodeInto(vnode, fnode, vindex);
						} else {
							ValueNode vnode = new ValueNode(value);
							int vindex = fnode.getLexicographicalIndex(vnode);
							model.insertNodeInto(vnode, fnode, vindex);
							subsubcache.put(value, vnode);
						}
					}

					Set removedValueSet = fnode.getRemovedValue(vnew);
					for (Iterator iter = removedValueSet.iterator(); iter
							.hasNext();) {
						Object value = iter.next();
						ValueNode vnode = fnode.getValueNode(value);
						model.removeNodeFromParent(vnode);
					}
				}
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		}

		// validate components
		tree.validate();
		scrollPane.validate();

		tree.repaint();
	}

	private HashSet getEntryClassSet() {
		HashSet eset = getEntryClasses();

		if (showSupertypes.getState()) {
			HashSet sset = new HashSet();
			for (Iterator iter = eset.iterator(); iter.hasNext();) {
				Class type = (Class) iter.next();
				if (Entry.class.isAssignableFrom(type.getSuperclass())) {
					// add superclass recursively
					addEntryClass(type.getSuperclass(), sset);
				}
			}
			for (Iterator iter = sset.iterator(); iter.hasNext();)
				eset.add((Class) iter.next());
		}

		return eset;
	}

	private void addEntryClass(Class type, HashSet sset) {
		// return if this class is "abstract"
		if (Modifier.isAbstract(type.getModifiers()))
			return;

		sset.add(type);
		if (Entry.class.isAssignableFrom(type.getSuperclass())) {
			addEntryClass(type.getSuperclass(), sset);
		}
	}

	private static boolean isValidField(Field f) {
		int mod = f.getModifiers();

		if (Modifier.isStatic(mod) || Modifier.isFinal(mod))
			return false;

		return true;
	}

	private Set getFieldSet(Class entryClass) {
		Set fset = new HashSet();
		Field[] fields = entryClass.getFields();
		for (int i = 0; i < fields.length; i++) {
			if (!isValidField(fields[i]))
				continue;

			fset.add(fields[i]);
		}
		return fset;
	}

	private static Field[] getValidFields(Class clazz) {
		Set fset = new HashSet();
		Field[] fields = clazz.getFields();
		for (int i = 0; i < fields.length; i++) {
			if (!isValidField(fields[i]))
				continue;

			fset.add(fields[i]);
		}
		return (Field[]) fset.toArray(new Field[0]);
	}

	public void refreshTree() {
		// clearSessions all selections
		tree.clearSelection();

		// clearSessions all nodes and notify it
		root.removeAllChildren();
		model.nodeStructureChanged(root);

		updateTree();

		model.nodeStructureChanged(root);
		tree.validate();
		scrollPane.validate();
	}

	// private abstract class AttributeNode extends DefaultMutableTreeNode {
	private abstract class AttributeNode extends SortableTreeNode {

		public AttributeNode(Object userObject, boolean allowsChildren) {
			super(userObject, allowsChildren);
		}

		public AttributeNode getEqualChild(AttributeNode child) {
			for (int i = 0; i < getChildCount(); i++) {
				AttributeNode node = (AttributeNode) getChildAt(i);
				if (node.equals(child)) {
					return node;
				}
			}
			return null;
		}

		public boolean containsSelectedNode() {
			HashSet children = new HashSet();

			addChildren(this, children);

			for (Iterator iter = children.iterator(); iter.hasNext();) {
				if (smodel.isPathSelected(new TreePath(((AttributeNode) iter
						.next()).getPath())))
					return true;
			}
			return false;
		}

		private void addChildren(AttributeNode node, Set set) {
			if (node.isLeaf())
				return;

			for (int i = 0; i < node.getChildCount(); i++) {
				AttributeNode child = (AttributeNode) node.getChildAt(i);
				addChildren(child, set);
				set.add(child);
			}
		}
	}

	private class AttributeRootNode extends AttributeNode {

		public AttributeRootNode() {
			this("root", true);
		}

		public AttributeRootNode(Object userObject, boolean allowsChildren) {
			super(userObject, allowsChildren);
		}

		public Set getAddedEntryClass(HashSet newEntrySet) {
			HashSet curr = getClassSet();
			HashSet next = (HashSet) newEntrySet.clone();

			next.removeAll(curr);
			return next;
		}

		public Set getRemovedEntryClass(HashSet newEntrySet) {
			HashSet curr = getClassSet();

			curr.removeAll(newEntrySet);
			return curr;
		}

		private HashSet getClassSet() {
			HashSet cset = new HashSet();

			for (int i = 0; i < getChildCount(); i++)
				cset.add(((EntryNode) getChildAt(i)).getEntryClass());

			return cset;
		}

		public EntryNode getEntryNode(Class clazz) {
			for (int i = 0; i < getChildCount(); i++) {
				EntryNode enode = (EntryNode) getChildAt(i);
				if (enode.getEntryClass().equals(clazz))
					return enode;
			}
			return null;
		}
	}

	/**
	 */
	private class EntryNode extends AttributeNode {

		private Class entryClass;

		public EntryNode(Class entryClass) {
			super(null, // user object
					true); // allows children

			this.entryClass = entryClass;
			setUserObject(entryClass);
		}

		public String toString() {
			if (showPackage.getState())
				return entryClass.getName();
			else
				return extractClassName(entryClass.getName());
		}

		public Object getUserObject() {
			return toString();
		}

		private String extractClassName(String fullName) {
			int index = fullName.lastIndexOf(".");
			return fullName.substring(index + 1);
		}

		public Class getEntryClass() {
			return entryClass;
		}

		public boolean isLeaf() {
			return false;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof EntryNode))
				return false;

			return entryClass.equals(((EntryNode) obj).getEntryClass());
		}

		public void clearValues() {
			for (int i = 0; i < getChildCount(); i++) {
				FieldNode fnode = (FieldNode) getChildAt(i);
				for (int j = 0; j < fnode.getChildCount(); j++) {
					ValueNode vnode = (ValueNode) fnode.getChildAt(j);
					vnode.removeFromParent();
				}
			}
		}
	}

	protected static String getFieldString(Field f, boolean showModifier,
			boolean showPackage) {
		StringBuffer sb = new StringBuffer();

		if (showModifier) {
			sb.append(Modifier.toString(f.getModifiers()));
			sb.append(" ");
		}

		sb.append(getTypename(f.getType(), showPackage));
		sb.append(" ");
		sb.append(f.getName());

		return sb.toString();
	}

	// Return the key of an interface or primitive multitype, handling arrays.
	private static String getTypename(Class t, boolean showPackage) {
		String brackets = "";
		while (t.isArray()) {
			brackets += "[]";
			t = t.getComponentType();
		}

		if (showPackage)
			return t.getName() + brackets;

		String fullName = t.getName();
		int index = fullName.lastIndexOf(".");
		return fullName.substring(index + 1);
	}

	private class FieldNode extends AttributeNode {

		private Field field;

		public FieldNode(Field field) {
			super(null, // user object
					true); // allows children

			this.field = field;
			setUserObject(field);
		}

		public String toString() {
			return getFieldString(field, false, // show modifier
					showPackage.getState());
		}

		public Object getUserObject() {
			return toString();
		}

		public Field getField() {
			return field;
		}

		public String getFieldName() {
			return field.getName();
		}

		public boolean isLeaf() {
			return false;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof FieldNode))
				return false;

			return field.equals(((FieldNode) obj).getField());
		}

		public Set getAddedValue(HashSet newValueSet) {
			HashSet curr = getValueSet();
			HashSet next = (HashSet) newValueSet.clone();

			next.removeAll(curr);
			return next;
		}

		public Set getRemovedValue(HashSet newValueSet) {
			HashSet curr = getValueSet();

			curr.removeAll(newValueSet);
			return curr;
		}

		private HashSet getValueSet() {
			HashSet vset = new HashSet();

			for (int i = 0; i < getChildCount(); i++)
				vset.add(((ValueNode) getChildAt(i)).getValue());

			return vset;
		}

		public ValueNode getValueNode(Object val) {
			for (int i = 0; i < getChildCount(); i++) {
				ValueNode vnode = (ValueNode) getChildAt(i);
				if (vnode.getValue().equals(val))
					return vnode;
			}
			return null;
		}
	}

	private class ValueNode extends AttributeNode {

		private Object value;

		public ValueNode(Object value) {
			super(null, // user object
					false); // allows children

			this.value = value;
			if (value instanceof String)
				setUserObject("\"" + value.toString() + "\"");
			else
				setUserObject(value.toString());
		}

		public Object getValue() {
			return value;
		}

		public boolean isLeaf() {
			return true;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof ValueNode))
				return false;

			return value.equals(((ValueNode) obj).getValue());
		}
	}

	private class AttributeSelectionListener implements TreeSelectionListener {

		private Collection prevSet = new HashSet(0);

		public void valueChanged(TreeSelectionEvent ev) {
			TreePath[] paths = smodel.getSelectionPaths();
			EntryTemplateHolder holder = new EntryTemplateHolder();

			if (paths != null) {
				// key=entryClass, eval=entryInstance
				for (int i = 0; i < paths.length; i++) {
					holder.addNode((AttributeNode) paths[i]
							.getLastPathComponent());
				}

			}

			prevSet = holder.getEntrySet();
		}
	}

	private class EntryTemplateHolder {

		// key=Class, eval=Entry
		private HashMap singleMap = new HashMap();

		// key=Class, eval=HashMap
		private HashMap multipleMap = new HashMap();

		public EntryTemplateHolder() {
		}

		public void addNode(AttributeNode node) {
			try {
				if (node instanceof EntryNode) {
					Class clazz = ((EntryNode) node).getEntryClass();
					if ((singleMap.get(clazz) == null)
							&& (multipleMap.get(clazz) == null)) {
						Entry entry = (Entry) clazz.newInstance();
						singleMap.put(clazz, entry);
					} else {
						// do not overwrite any existing eval.
					}
				} else if (node instanceof FieldNode) {
					Class clazz = ((EntryNode) node.getParent())
							.getEntryClass();
					if ((singleMap.get(clazz) == null)
							&& (multipleMap.get(clazz) == null)) {
						Entry entry = (Entry) clazz.newInstance();
						singleMap.put(clazz, entry);
					} else {
						// do not overwrite any existing eval.
					}
				} else if (node instanceof ValueNode) {
					ValueNode vnode = (ValueNode) node;
					FieldNode fnode = (FieldNode) node.getParent();
					EntryNode enode = (EntryNode) fnode.getParent();

					Class clazz = ((EntryNode) enode).getEntryClass();
					Field field = fnode.getField();
					Object value = vnode.getValue();

					HashMap fieldMap = (HashMap) multipleMap.get(clazz);
					if (fieldMap == null) {
						Entry entry = (Entry) singleMap.get(clazz);
						if (entry == null) {
							// new entry
							entry = (Entry) clazz.newInstance();
							singleMap.put(clazz, entry);
							field.set(entry, value);
						} else {
							if (field.get(entry) == null) {
								field.set(entry, value);
							} else {
								// entering multiple entry mode
								entry = (Entry) singleMap.remove(clazz);

								fieldMap = new HashMap();
								multipleMap.put(clazz, fieldMap);

								// save current field-values
								Field[] fs = getValidFields(clazz);
								for (int i = 0; i < fs.length; i++) {
									if (fs[i].get(entry) != null) {
										ArrayList vlist = new ArrayList();
										fieldMap.put(fs[i], vlist);
										vlist.add(fs[i].get(entry));
									}
								}

								// append new eval
								ArrayList valueList = (ArrayList) fieldMap
										.get(field);
								valueList.add(value);
							}
						}
					} else {
						ArrayList valueList = (ArrayList) fieldMap.get(field);
						if (valueList == null) {
							// new field
							valueList = new ArrayList();
							fieldMap.put(field, valueList);
						}
						valueList.add(value);
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}

		public Collection getEntrySet() {
			if (multipleMap.size() == 0)
				return singleMap.values();

			HashSet resolveSet = new HashSet();
			resolveSet.addAll(singleMap.values());

			for (Iterator iter = multipleMap.keySet().iterator(); iter
					.hasNext();) {
				try {
					Class clazz = (Class) iter.next();
					Entry entry = (Entry) clazz.newInstance();
					HashMap fieldMap = (HashMap) multipleMap.get(clazz);

					int[] indices = new int[fieldMap.size()];
					Field[] fields = new Field[fieldMap.size()];
					ArrayList[] vlists = new ArrayList[fieldMap.size()];
					int i = 0;
					for (Iterator fiter = fieldMap.keySet().iterator(); fiter
							.hasNext();) {
						fields[i] = (Field) fiter.next();
						vlists[i] = (ArrayList) fieldMap.get(fields[i]);

						i++;
					}

					addTemplate(clazz, 0, indices, fields, vlists, resolveSet);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				}
			}

			return resolveSet;
		}

		private void addTemplate(Class clazz, int depth, int[] indices,
				Field[] fields, ArrayList[] vlists, HashSet tmplSet)
				throws IllegalAccessException, InstantiationException {

			for (int i = 0; i < vlists[depth].size(); i++) {
				indices[depth] = i;
				if (depth == (indices.length - 1)) {
					Entry entry = (Entry) clazz.newInstance();
					for (int j = 0; j < indices.length; j++)
						fields[j].set(entry, vlists[j].get(indices[j]));
					tmplSet.add(entry);
				} else {
					addTemplate(clazz, depth + 1, indices, fields, vlists,
							tmplSet);
				}
			}
		}
	}

	private class SimpleEntryTemplateHolder {

		private HashMap entryMap = new HashMap();

		public SimpleEntryTemplateHolder() {
		}

		public void addNode(AttributeNode node) {
			try {
				if (node instanceof EntryNode) {
					Class clazz = ((EntryNode) node).getEntryClass();
					if (entryMap.get(clazz) == null) {
						Entry entry = (Entry) clazz.newInstance();
						entryMap.put(clazz, entry);
					}
				} else if (node instanceof FieldNode) {
					Class clazz = ((EntryNode) node.getParent())
							.getEntryClass();
					if (entryMap.get(clazz) == null) {
						Entry entry = (Entry) clazz.newInstance();
						entryMap.put(clazz, entry);
					}
				} else if (node instanceof ValueNode) {
					ValueNode vnode = (ValueNode) node;
					FieldNode fnode = (FieldNode) node.getParent();
					EntryNode enode = (EntryNode) fnode.getParent();

					Class clazz = ((EntryNode) enode).getEntryClass();
					Entry entry = (Entry) entryMap.get(clazz);
					if (entry == null) {
						entry = (Entry) clazz.newInstance();
						entryMap.put(clazz, entry);
					}
					fnode.getField().set(entry, vnode.getValue());
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}

		public Collection getEntrySet() {
			return entryMap.values();
		}
	}

	private static Color backgroundSelectionColor;

	private static Color backgroundFieldSelectionColor;

	private class AttributeCellRenderer extends DefaultTreeCellRenderer {

		public AttributeCellRenderer() {
			super();

			AttributePanel.backgroundSelectionColor = getBackgroundSelectionColor();
			AttributePanel.backgroundFieldSelectionColor = new Color(245, 222,
					179); // wheat
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			TreePath path = tree.getPathForRow(row);
			if (path != null) {
				Object node = path.getLastPathComponent();

				if (selected) {
					if (node instanceof FieldNode)
						setBackgroundSelectionColor(AttributePanel.backgroundFieldSelectionColor);
					else
						setBackgroundSelectionColor(AttributePanel.backgroundSelectionColor);
				}
			}

			return super.getTreeCellRendererComponent(tree, value, selected,
					expanded, leaf, row, hasFocus);
		}
	}

	private class AttributePopup extends JPopupMenu implements ActionListener,
			MouseListener {

		public AttributePopup() {
			super();

			// "Clear selections"
			JMenuItem unselect = new JMenuItem("Clear selections");
			unselect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					tree.clearSelection();
				}
			});
			add(unselect);

			add(new JSeparator());

			// "Show package"
			showPackage = new JCheckBoxMenuItem("Show package", false);
			showPackage.addActionListener(this);
			add(showPackage);

			// "Show supertypes"
			showSupertypes = new JCheckBoxMenuItem("Show supertypes", false);
			showSupertypes.addActionListener(this);
			add(showSupertypes);

			// addPopupMenuListener(this);
			setOpaque(true);
			setLightWeightPopupEnabled(true);
			setBorder(new BevelBorder(BevelBorder.RAISED));
		}

		public void actionPerformed(ActionEvent ev) {
			// remake list model without remote access.
			AttributePanel.this.refreshTree();
		}

		public void mousePressed(MouseEvent ev) {
			checkPopup(ev);
		}

		public void mouseClicked(MouseEvent ev) {
			checkPopup(ev);
		}

		public void mouseEntered(MouseEvent ev) {
		}

		public void mouseExited(MouseEvent ev) {
		}

		public void mouseReleased(MouseEvent ev) {
			checkPopup(ev);
		}

		private void checkPopup(MouseEvent ev) {
			if (ev.isPopupTrigger()) {
				show(ev.getComponent(), ev.getX(), ev.getY());
			}
		}
	}

	private HashSet getEntryClasses() {
		// use providers attributes
		HashSet classes = new HashSet();
		for (int i = 0; i < attributes.length; i++)
			classes.add(attributes[i].getClass());
		return classes;
	}

	private HashSet getFieldValueSet(Entry entry, String fieldName) {
		// use providers attributes
		HashSet vset = new HashSet();
		Class type = entry.getClass();
		for (int i = 0; i < attributes.length; i++) {
			try {
				Field field = attributes[i].getClass().getField(fieldName);
				Object value = field.get(attributes[i]);
				if (value != null) {
					vset.add(value);
				}
			} catch (Exception e) {
				// ignore
			}
		}
		return vset;
	}

	public JTree getTree() {
		return tree;
	}
}
