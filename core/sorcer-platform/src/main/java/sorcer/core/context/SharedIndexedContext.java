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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.jini.space.JavaSpace05;

import sorcer.service.ContextException;
import sorcer.service.IndexedContext;
import sorcer.service.SpaceContext;
import sorcer.service.space.SpaceAccessor;
import sorcer.space.array.DistribArray05;

/**
 * ServiceContext implementing the java.util.List interface.
 * Default context paths for elements in the list are in the form by requestReturn <code>element[i]</code>
 * with the context root <code>List</code>;
 */
@SuppressWarnings({ "serial", "unchecked" })
public class SharedIndexedContext<T extends Object> extends ServiceContext implements IndexedContext, SpaceContext {
	private List<T> elements = new ArrayList<T>();
	private DistribArray05 spaceElements;
	private String spaceName;
	
	public SharedIndexedContext(String spaceName) {
		super();
		this.spaceName = spaceName;
		JavaSpace05 space = SpaceAccessor.getSpace(spaceName);
		spaceElements = new DistribArray05(space, "" + mogramId);
	}
	
	public SharedIndexedContext(String spaceName, T... elements) throws ContextException {
		this(spaceName);
		for (int i = 0; i < elements.length; i++ ) {
			add(elements[i]);
		}
	}
	
	private String pathFor(int index) {
		return ("element" + "[" + index + "]").intern();
	}
	
	/**
	 * Return an index of this ListContext requestReturn.
	 * 
	 * @param path
	 *            ListContext context requestReturn
	 * @return an index of the ListContext requestReturn
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
		putValue(pathFor(i), value);
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
			putValue(pathFor(i), o);
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

	public List getElements() {
		return elements;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#readValue(java.lang.String)
	 */
	@Override
	public Object readValue(String path) throws ContextException, RemoteException {
		Object value = super.getValue(path);
		int index = -1;
		if (value instanceof String && ((String) value).startsWith(spacePrefix)) {
			index = new Integer(((String) value).substring(spacePrefix.length()));

		}
		if (index >= 0) {
			try {
				value = spaceElements.readElement(index);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#takeValue(java.lang.String)
	 */
	@Override
	public Object takeValue(String path) throws ContextException,
			RemoteException {
		Object value = super.getValue(path);
		int index = -1;
		if (value instanceof String && ((String) value).startsWith(spacePrefix)) {
			index = new Integer(((String) value).substring(spacePrefix.length()));

		}
		if (index >= 0) {
			try {
				value = spaceElements.takeElement(index);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#writeValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object writeValue(String path, Object value)
			throws ContextException, RemoteException {
		try {
			int index = spaceElements.append(value);
			putValue(path, spacePrefix + index);
		} catch (Exception e) {
			throw new ContextException(e);
		} 
		return value;
	}
	
	private void setSpace() {
		JavaSpace05 space = SpaceAccessor.getSpace(spaceName);
		spaceElements.setSpace(space);
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#aliasValue(java.lang.String, java.lang.String)
	 */
	@Override
	public Object aliasValue(String path, String alias)
			throws ContextException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#share()
	 */
	@Override
	public void share() throws ContextException, RemoteException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#unshare()
	 */
	@Override
	public void unshare() throws ContextException, RemoteException {
		// TODO Auto-generated method stub
		
	}
}