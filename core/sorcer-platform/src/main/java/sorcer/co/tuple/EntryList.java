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

package sorcer.co.tuple;

import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.ParEntry;
import sorcer.core.context.model.par.ParSet;
import sorcer.service.EvaluationException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class EntryList extends ArrayList<Entry> {
	
	static final long serialVersionUID = 1L;
	
	public enum Type {
		INPUT, OUTPUT, INITIAL_DESIGN, OPTIMIZED_DESIGN, CONSTRAINTS, LINKED, CONSTNTS, INVARIANTS
	}
	
	private Type type = Type.INPUT;
	
	private String name;
	
	public EntryList() {
		super();
	}

	public EntryList(int size) {
		super(size);
	}

	public EntryList(Set<Entry> entrySet) {
		addAll(entrySet);
	}

	public EntryList(String name, Entry...  entryLists) {
		this(entryLists);
		this.name = name;
	}
	
	public EntryList(EntryList...  entryLists) {
		super();
		for (EntryList el : entryLists) {
			addAll(el);
		}
	}
	
	public EntryList(Entry... entryArray) {
		super();
		for (Entry e : entryArray) {
			add(e);
		}
	}
	
	public  EntryList(EntryList  entryList) {
		super();
		for (Entry t : entryList) {
			add(t);
		}
	}
	
	public EntryList(ParSet parSet) {
		super();
		for (ParEntry<?> p : parSet) {
			add(new Entry(p.getName(), p));
		}
	}
		
	public Entry getEntry(String entryName) {
		for (Entry e : this) {
			if (e.getName().equals(entryName)) {
				return e;
			}
		}
		return null;
	}

	public boolean containsEntryName(String name) {
		return contains(new Entry(name));
	}
	
	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Entry<?>))
			return false;
		else {
			for (Entry e : this) {
				if (e.getName().equals(((Entry)obj).getName()))
					return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Entry<?>)) {
			return false;
		} else {
			for (Entry e : this) {
				if (e.getName().equals(((Entry) obj).getName())) {
					super.remove(e);
					return true;
				}
			}
		}
		return false;
	}
	
	 public List<String> getNames() {
		 List<String> names = new ArrayList<String>(size());
		 for (int i = 0; i < size(); i++) {
			 names.add(get(i).getName());
		 }
		 return names;
	 }

	 public List<Object> getValues() throws EvaluationException, RemoteException {
		 List<Object> values = new ArrayList<Object>(size());
		 for (int i = 0; i < size(); i++) {
			 values.add(get(i).getValue());
		 }
		 return values;
	 }
	 
	 public Entry[] toArray() {
		 Entry[] va = new Entry[size()];
		 return toArray(va);
	 }
			
	 public static EntryList asList(Entry[] array) {
		 EntryList el = new EntryList(array.length);
		 for (Entry<?> e : array)
			 el.add(e);
		 return el;
	 }
	
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(getNames().toString());
		sb.append("\n");
		for (int i = 0; i < size(); i++) {
			 sb.append(get(i).getName());
			 sb.append("\n");
			 sb.append(get(i));
			 sb.append("\n");
		 }
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return getNames().toString();
	}
}
