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
import sorcer.service.ArgList;
import sorcer.service.EvaluationException;
import sorcer.service.Identity;
import sorcer.service.SetterException;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ParList extends ArrayList<Par> {

	static final long serialVersionUID = -4997255102658715823L;

	public ParList() {
		super();
	}

	public ParList(int size) {
		super(size);
	}

	public ParList(Set<Par> parSet) {
		addAll(parSet);
	}

	public ParList(ParList... parLists) {
		super();
		for (ParList pl : parLists) {
			addAll(pl);
		}
	}

	public ParList(Par<?>[] parArray) {
		super();
		for (Par<?> p : parArray) {
			add(p);
		}
	}

	public <T> ParList(List<Par<T>> parList) {
		super();
		for (Par<T> p : parList) {
			add(p);
		}
	}

	public Par<? extends Object> getPar(String parName) throws ParException {
		for (Par<?> p : this) {
			if (p.getName().equals(parName)) {
				return p;
			}
		}
		return null;
	}

	public void setParValue(String parName, Object value)
			throws EvaluationException {
		Par par = null;
		for (Par p : this) {
			if (p.getName().equals(parName)) {
				par = p;
				try {
					par.setValue(value);
				} catch (Exception e) {
					throw new EvaluationException(e);
				} 
				break;
			}
		}
		if (par == null)
			throw new ParException("No such Par in the list: " + parName);
	}

	public ParList selectPars(List<String>... parNames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parNames) {
			allParNames.addAll(nl);
		}
		ParList out = new ParList();
		for (Par<?> v : this) {
			if (allParNames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public ParList selectPars(String... parNames) {
		List<String> vnames = Arrays.asList(parNames);
		ParList out = new ParList();
		for (Par<?> v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public boolean containsParName(String name) {
		return contains(new Par(name));
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Par<?>))
			return false;
		else {
			for (Par<?> v : this) {
				if (v.getName().equals(((Par<?>) obj).getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Par<?>)) {
			return false;
		} else {
			for (Par<?> v : this) {
				if (v.getName().equals(((Par<?>) obj).getName())) {
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

	public Par<?>[] toArray() {
		Par<?>[] pa = new Par[size()];
		return toArray(pa);
	}

	public static ParList asList(Par<?>[] array) {
		ParList pl = new ParList(array.length);
		for (Par<?> p : array)
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
			if (!(a instanceof Par<?>))
				throw new RuntimeException("wrong argument");
			out.add((Par<?>)a);
		}
		return out;
	}
	
	public ParList toParList() {
		ParList out = new ParList();
		for (Arg a : this) {
			if (!(a instanceof Par<?>))
				throw new RuntimeException("wrong argument");
			out.add((Par<?>)a);
		}
		return out;
	}
	
}
