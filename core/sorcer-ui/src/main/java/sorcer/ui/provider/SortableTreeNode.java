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

import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Generic tree node class based on <tt>DefaultMutableTreeNode</tt>. Added
 * features are follow:
 * <ul>
 * <li>Adding a child in lexicographical order.
 * <li>Finding a child/descendant node who has specified user object.
 * <li>Managing information about drop-over.
 * </ul>
 */
public class SortableTreeNode extends DefaultMutableTreeNode {
	// implements DropItem {
	private static final Comparator defaultComparator = new DefaultComparator();

	public SortableTreeNode() {
		super();
	}

	public SortableTreeNode(Object userObject) {
		super(userObject);
	}

	/**
	 * @see javax.swing.tree.DefaultMutableTreeNode
	 */
	public SortableTreeNode(Object userObject, boolean allowsChildren) {
		super(userObject, allowsChildren);
	}

	public SortableTreeNode getChildWith(Object userObject) {
		for (int i = 0; i < getChildCount(); i++) {
			SortableTreeNode node = (SortableTreeNode) getChildAt(i);
			if (userObject.equals(node.getUserObject()))
				return node;
		}
		return null;
	}

	public SortableTreeNode findDescendantWith(Object userObject) {
		SortableTreeNode node = null;
		for (int i = 0; i < getChildCount(); i++) {
			node = (SortableTreeNode) getChildAt(i);
			if (userObject.equals(node.getUserObject())) {
				break;
			} else {
				SortableTreeNode cnode = node.findDescendantWith(userObject);
				if (cnode != null) {
					node = cnode;
					break;
				}
			}
		}
		return node;
	}

	public static class DefaultComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			try {
				return ((Comparable) o1).compareTo(o2);
			} catch (ClassCastException e) {
			}
			return 0;
		}
	}

	public int getLexicographicalIndex(SortableTreeNode newNode) {
		return getOrderedIndex(newNode, defaultComparator);
	}

	public int getOrderedIndex(SortableTreeNode newNode, Comparator comparator) {
		String target = newNode.toString();
		for (int i = 0; i < getChildCount(); i++) {
			String str = ((SortableTreeNode) getChildAt(i)).toString();
			if (comparator.compare(target, str) < 0)
				return i;
		}
		// returns the last index
		return getChildCount();
	}

	public void insert(SortableTreeNode newNode) {
		super.insert(newNode, getLexicographicalIndex(newNode));
	}

	public void insert(SortableTreeNode newNode, Comparator comparator) {
		super.insert(newNode, getOrderedIndex(newNode, comparator));
	}

	public boolean isDroppable(java.util.List flavorList, int dropAction) {
		return true;
	}

	private boolean dropOver = false;

	public boolean isDropOver() {
		return dropOver;
	}

	public void setDropOver(boolean val) {
		this.dropOver = val;
	}
}
