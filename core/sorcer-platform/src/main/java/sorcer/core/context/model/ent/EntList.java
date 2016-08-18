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

package sorcer.core.context.model.ent;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import sorcer.co.tuple.Tuple2;
import sorcer.service.Arg;
import sorcer.service.EvaluationException;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class EntList extends ArrayList<Proc> {

	static final long serialVersionUID = -4997255102658715823L;

	public EntList() {
		super();
	}

	public EntList(int size) {
		super(size);
	}

	public EntList(Set<Proc> procEntrySet) {
		addAll(procEntrySet);
	}

	public EntList(EntList... entLists) {
		super();
		for (EntList pl : entLists) {
			addAll(pl);
		}
	}

	public EntList(Proc<?>[] procEntryArray) {
		super();
		for (Proc<?> p : procEntryArray) {
			add(p);
		}
	}

	public <T> EntList(List<Proc<T>> procEntryList) {
		super();
		for (Proc<T> p : procEntryList) {
			add(p);
		}
	}

	public Proc<? extends Object> getPar(String parName) throws EntException {
		for (Proc<?> p : this) {
			if (p.getName().equals(parName)) {
				return p;
			}
		}
		return null;
	}

	public void setParValue(String parName, Object value)
			throws EvaluationException {
		Proc procEntry = null;
		for (Proc p : this) {
			if (p.getName().equals(parName)) {
				procEntry = p;
				try {
					procEntry.setValue(value);
				} catch (Exception e) {
					throw new EvaluationException(e);
				} 
				break;
			}
		}
		if (procEntry == null)
			throw new EntException("No such Proc in the list: " + parName);
	}

	public EntList selectPars(List<String>... parNames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parNames) {
			allParNames.addAll(nl);
		}
		EntList out = new EntList();
		for (Proc<?> v : this) {
			if (allParNames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public EntList selectPars(String... parNames) {
		List<String> vnames = Arrays.asList(parNames);
		EntList out = new EntList();
		for (Proc<?> v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public boolean containsParName(String name) {
		return contains(new Proc(name));
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Proc<?>))
			return false;
		else {
			for (Proc<?> v : this) {
				if (v.getName().equals(((Proc<?>) obj).getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Proc<?>)) {
			return false;
		} else {
			for (Proc<?> v : this) {
				if (v.getName().equals(((Proc<?>) obj).getName())) {
					super.remove(v);
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

	public Proc<?>[] toArray() {
		Proc<?>[] pa = new Proc[size()];
		return toArray(pa);
	}

	public static EntList asList(Proc<?>[] array) {
		EntList pl = new EntList(array.length);
		for (Proc<?> p : array)
			pl.add(p);
		return pl;
	}

	public EntList setParValues(Tuple2<String, ?>... entries)
			throws EntException {
		try {
			for (Tuple2<String, ?> entry : entries) {
				setParValue(entry._1, entry._2);
			}
		} catch (Exception e) {
			throw new EntException(e);
		}
		return this;
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

	@Override
	public String toString() {
		return getNames().toString();
	}
	
	public ParSet toParSet() {
		ParSet out = new ParSet();
		for (Arg a : this) {
			if (!(a instanceof Proc<?>))
				throw new RuntimeException("wrong argument");
			out.add((Proc<?>)a);
		}
		return out;
	}
	
	public EntList toParList() {
		EntList out = new EntList();
		for (Arg a : this) {
			if (!(a instanceof Proc<?>))
				throw new RuntimeException("wrong argument");
			out.add((Proc<?>)a);
		}
		return out;
	}
	
}
