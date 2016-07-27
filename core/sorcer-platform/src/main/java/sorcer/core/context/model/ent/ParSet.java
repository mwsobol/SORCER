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
public class ParSet extends TreeSet<Proc> {
	
	private static final long serialVersionUID = -4662755904016297879L;
	
	public ParSet() {
		super();
	}

	public ParSet(EntList entList) {
		addAll(entList);
	}
	
	public ParSet(Set<Proc> procEntrySet) {
		addAll(procEntrySet);
	}

	
	public ParSet(EntList... entLists) {
		for (EntList vl : entLists) {
			addAll(vl);
		}
	}
	
	public ParSet(Proc<?>... procEntries) {
		for (Proc<?> v : procEntries) {
			add(v);
		}
	}
	
	public Proc<?> getPar(String parName) throws EntException {
		for (Proc<?> v : this) {
			if (v.getName().equals(parName))
				return v;
		}
		return null;
	}
	
	public void setValue(String parName, Object value)
			throws EvaluationException {
		Proc procEntry = null;
		for (Proc<?> p : this) {
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
	
	public EntList selectPars(List<String>... parnames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parnames) {
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
	
	public ParSet selectPars(String... parnames) {
		List<String> vnames = Arrays.asList(parnames);
		ParSet out = new ParSet();
		for (Proc<?> v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Proc<?>))
			return false;
		else {
			for (Proc<?> v : this) {
				if (v.getName().equals(((Proc<?>)obj).getName()))
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
		 Iterator<Proc> i = iterator();
		 while (i.hasNext()) {
			 names.add(i.next().getName());
		 }
		 return names;
	 }
	 
	 public List<Object> getValues() throws EvaluationException, RemoteException {
		 List<Object> values = new ArrayList<Object>(size());
		 Iterator<Proc> i = iterator();
		 while (i.hasNext()) {
			 values.add(i.next().getValue());
		 }
		 return values;
	 }
	 
	 public Proc<?>[] toArray() {
		 Proc<?>[] va = new Proc[size()];
		 return toArray(va);
	 }
			
	 public EntList toParList() {
		 EntList vl = new EntList(size());
		 for (Proc<?> v : this)
			 vl.add(v);
		 return vl;
	 }

	 public static ParSet asParSet(EntList list) {
		 return new ParSet(list);
	 }

	 public static EntList asList(Proc<?>[] array) {
		 EntList vl = new EntList(array.length);
		 for (Proc<?> v : array)
			 vl.add(v);
		 return vl;
	 }

	 public void clearPars() throws EvaluationException {
			for (Proc p : this) {
				try {
					p.setValue(null);
				} catch (Exception e) {
					throw new EvaluationException(e);
				} 
			}
		}
}
