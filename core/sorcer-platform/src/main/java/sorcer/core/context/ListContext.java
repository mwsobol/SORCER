/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.core.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import sorcer.service.ContextException;
import sorcer.service.IndexedContext;

/**
 * ServiceContext implementing the java.util.List interface.
 * Default context paths for elements in the list are in the form by contextReturn <code>element[i]</code>
 * with the context root <code>List</code>;
 */
@SuppressWarnings({ "serial", "unchecked" })
public class ListContext<T extends Object> extends ServiceContext<T> implements IndexedContext {
	List<T> elements = new ArrayList<T>();
	
	public ListContext() {
		super();
	}
	
	public ListContext(String name, String subjectPath, Object subjectValue) {
		super(name, subjectPath, subjectValue);
	}
	
	public ListContext(T... elements) throws ContextException {
		for (int i = 0; i < elements.length; i++ ) {
			add(elements[i]);
		}
	}
	
	public String pathFor(int index) {
		return ("element" + "[" + index + "]").intern();
	}
	
	/**
	 * Return an index of this ListContext contextReturn.
	 * 
	 * @param path
	 *            ListContext context contextReturn
	 * @return an index of the ListContext contextReturn
	 */
	public int pathIndex(String path) {
		int i1 = path.indexOf('[');
		if (i1 >= 0) {
			int i2 = path.indexOf(']');
			return Integer.parseInt(path.substring(i1 + 1, i2));
		}
		return -1;
	}

	
	/* (non-Javadoc)
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(T e) throws ContextException {
		putValue(pathFor(elements.size()), e);
		elements.add(e);
		return true;
	}

	public Object getValue(int i) {
		return get(i);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.context.IndexedContext#putValue(int, java.lang.Object)
	 */
	@Override
	public Object putValue(int i, Object value) throws ContextException {
		putValue(pathFor(i), (T)value);
		elements.set(i, (T)value);
		return value;
	}
	
	/* (non-Javadoc)
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, T element) throws ContextException {
		String path;
		int i;
		elements.add(index, element);
		Iterator en = keyIterator();
		while (en.hasNext()) {
			path = (String)en.next();
			i = pathIndex(path);
			if (i > index) {
				putValue(pathFor(i+1), elements.get(i+1));
			}
		}
		i = elements.size();
		// append last shifted already element
		putValue(pathFor(i), elements.get(i-1));
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<T> c) throws ContextException {
		for (T o : c) {
			putValue(pathFor(elements.size()), o);
			elements.add(o);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection c) throws ContextException {
		int i = index;
		for (Object o : c) {
			putValue(pathFor(i), (T)o);
			i++;
		}
		return addAll(index, c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		return elements.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#getValue(int)
	 */
	public Object get(int index) {
		return elements.get(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object o) {
		return elements.indexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#iterator()
	 */
	public Iterator iterator() {
		return elements.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object o) {
		return lastIndexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator()
	 */
	public ListIterator listIterator() {
		return elements.listIterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator listIterator(int index) {
		return elements.listIterator(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(int)
	 */
	public Object remove(int index) throws ContextException {
		removePath(pathFor(index));
		return elements.remove(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		int i;
		for (Object o : c) {
			i = elements.indexOf(o);
		}
		return removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		return elements.retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public Object set(int index, T element) throws ContextException {
		putValue(pathFor(index), element);
		return elements.set(index, element);
	}

	/* (non-Javadoc)
	 * @see java.util.List#subList(int, int)
	 */
	public List subList(int fromIndex, int toIndex) {
		return elements.subList(fromIndex, toIndex);
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return elements.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray(T[])
	 */
	public Object[] toArray(Object[] a) {
		return elements.toArray(a);
	}

	public List values() {
		return elements;
	}
}