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

package sorcer.core.context.model.par;

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
public class ParList extends ArrayList<ParEntry> {

	static final long serialVersionUID = -4997255102658715823L;

	public ParList() {
		super();
	}

	public ParList(int size) {
		super(size);
	}

	public ParList(Set<ParEntry> parEntrySet) {
		addAll(parEntrySet);
	}

	public ParList(ParList... parLists) {
		super();
		for (ParList pl : parLists) {
			addAll(pl);
		}
	}

	public ParList(ParEntry<?>[] parEntryArray) {
		super();
		for (ParEntry<?> p : parEntryArray) {
			add(p);
		}
	}

	public <T> ParList(List<ParEntry<T>> parEntryList) {
		super();
		for (ParEntry<T> p : parEntryList) {
			add(p);
		}
	}

	public ParEntry<? extends Object> getPar(String parName) throws ParException {
		for (ParEntry<?> p : this) {
			if (p.getName().equals(parName)) {
				return p;
			}
		}
		return null;
	}

	public void setParValue(String parName, Object value)
			throws EvaluationException {
		ParEntry parEntry = null;
		for (ParEntry p : this) {
			if (p.getName().equals(parName)) {
				parEntry = p;
				try {
					parEntry.setValue(value);
				} catch (Exception e) {
					throw new EvaluationException(e);
				} 
				break;
			}
		}
		if (parEntry == null)
			throw new ParException("No such Par in the list: " + parName);
	}

	public ParList selectPars(List<String>... parNames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parNames) {
			allParNames.addAll(nl);
		}
		ParList out = new ParList();
		for (ParEntry<?> v : this) {
			if (allParNames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public ParList selectPars(String... parNames) {
		List<String> vnames = Arrays.asList(parNames);
		ParList out = new ParList();
		for (ParEntry<?> v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public boolean containsParName(String name) {
		return contains(new ParEntry(name));
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof ParEntry<?>))
			return false;
		else {
			for (ParEntry<?> v : this) {
				if (v.getName().equals(((ParEntry<?>) obj).getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof ParEntry<?>)) {
			return false;
		} else {
			for (ParEntry<?> v : this) {
				if (v.getName().equals(((ParEntry<?>) obj).getName())) {
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

	public ParEntry<?>[] toArray() {
		ParEntry<?>[] pa = new ParEntry[size()];
		return toArray(pa);
	}

	public static ParList asList(ParEntry<?>[] array) {
		ParList pl = new ParList(array.length);
		for (ParEntry<?> p : array)
			pl.add(p);
		return pl;
	}

	public ParList setParValues(Tuple2<String, ?>... entries)
			throws ParException {
		try {
			for (Tuple2<String, ?> entry : entries) {
				setParValue(entry._1, entry._2);
			}
		} catch (Exception e) {
			throw new ParException(e);
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
			if (!(a instanceof ParEntry<?>))
				throw new RuntimeException("wrong argument");
			out.add((ParEntry<?>)a);
		}
		return out;
	}
	
	public ParList toParList() {
		ParList out = new ParList();
		for (Arg a : this) {
			if (!(a instanceof ParEntry<?>))
				throw new RuntimeException("wrong argument");
			out.add((ParEntry<?>)a);
		}
		return out;
	}
	
}
