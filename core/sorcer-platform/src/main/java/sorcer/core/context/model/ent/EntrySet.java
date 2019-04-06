package sorcer.core.context.model.ent;

/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import sorcer.service.EvaluationException;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class EntrySet extends TreeSet<Entry> {

	private static final long serialVersionUID = -4662755904016297879L;

	public EntrySet() {
		super();
	}

	public EntrySet(EntryList entList) {
		addAll(entList);
	}

	public EntrySet(Set<Value> procEntrySet) {
		addAll(procEntrySet);
	}


	public EntrySet(EntryList... entLists) {
		for (EntryList vl : entLists) {
			addAll(vl);
		}
	}

	public EntrySet(Value<?>... procEntries) {
		for (Value<?> v : procEntries) {
			add(v);
		}
	}

	public Entry getPar(String name) throws EntException {
		for (Entry v : this) {
			if (v.getName().equals(name))
				return v;
		}
		return null;
	}

	public void setValue(String name, Object value)
			throws EvaluationException {
		Entry ent = null;
		for (Entry v : this) {
			if (v.getName().equals(name)) {
				ent = v;
				try {
					ent.setValue(value);
				} catch (Exception e) {
					throw new EvaluationException(e);
				}
				break;
			}
		}
		if (ent == null)
			throw new EntException("No such Value in the list: " + name);
	}

	public EntryList selectEntries(List<String>... nameLists) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : nameLists) {
			allParNames.addAll(nl);
		}
		EntryList out = new EntryList();
		for (Entry v : this) {
			if (allParNames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public EntrySet selectPars(String... names) {
		List<String> vnames = Arrays.asList(names);
		EntrySet out = new EntrySet();
		for (Entry v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Value))
			return false;
		else {
			for (Entry v : this) {
				if (v.getName().equals(((Value)obj).getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Value)) {
			return false;
		} else {
			for (Entry v : this) {
				if (v.getName().equals(((Value) obj).getName())) {
					super.remove(v);
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getNames() {
		List<String> names = new ArrayList<String>(size());
		Iterator<Entry> i = iterator();
		while (i.hasNext()) {
			names.add(i.next().getName());
		}
		return names;
	}

	public List<Object> getValues() throws EvaluationException, RemoteException {
		List<Object> values = new ArrayList<Object>(size());
		Iterator<Entry> i = iterator();
		while (i.hasNext()) {
			values.add(i.next().getImpl());
		}
		return values;
	}

	public Value[] toArray() {
		Value[] va = new Value[size()];
		return toArray(va);
	}

	public EntryList toEntryList() {
		EntryList vl = new EntryList(size());
		for (Entry v : this)
			vl.add(v);
		return vl;
	}

	public static EntrySet asEntSet(EntryList list) {
		return new EntrySet(list);
	}

	public static EntryList asList(Value[] array) {
		EntryList vl = new EntryList(array.length);
		for (Value v : array)
			vl.add(v);
		return vl;
	}

	public void clearEntries() throws EvaluationException {
		for (Entry v : this) {
			try {
				v.setValue(null);
			} catch (Exception e) {
				throw new EvaluationException(e);
			}
		}
	}
}
