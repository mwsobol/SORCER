package sorcer.core.context.model.par;

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
public class ParSet extends TreeSet<Par> {
	
	private static final long serialVersionUID = -4662755904016297879L;
	
	public ParSet() {
		super();
	}

	public ParSet(ParList parList) {
		addAll(parList);
	}
	
	public ParSet(Set<Par> parEntrySet) {
		addAll(parEntrySet);
	}

	
	public ParSet(ParList...  parLists) {
		for (ParList vl : parLists) {
			addAll(vl);
		}
	}
	
	public ParSet(Par<?>... parEntries) {
		for (Par<?> v : parEntries) {
			add(v);
		}
	}
	
	public Par<?> getPar(String parName) throws ParException {
		for (Par<?> v : this) {
			if (v.getName().equals(parName))
				return v;
		}
		return null;
	}
	
	public void setValue(String parName, Object value)
			throws EvaluationException {
		Par parEntry = null;
		for (Par<?> p : this) {
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
	
	public ParList selectPars(List<String>... parnames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parnames) {
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
	
	public ParSet selectPars(String... parnames) {
		List<String> vnames = Arrays.asList(parnames);
		ParSet out = new ParSet();
		for (Par<?> v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Par<?>))
			return false;
		else {
			for (Par<?> v : this) {
				if (v.getName().equals(((Par<?>)obj).getName()))
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
		 Iterator<Par> i = iterator();
		 while (i.hasNext()) {
			 names.add(i.next().getName());
		 }
		 return names;
	 }
	 
	 public List<Object> getValues() throws EvaluationException, RemoteException {
		 List<Object> values = new ArrayList<Object>(size());
		 Iterator<Par> i = iterator();
		 while (i.hasNext()) {
			 values.add(i.next().getValue());
		 }
		 return values;
	 }
	 
	 public Par<?>[] toArray() {
		 Par<?>[] va = new Par[size()];
		 return toArray(va);
	 }
			
	 public ParList toParList() {
		 ParList vl = new ParList(size());
		 for (Par<?> v : this)
			 vl.add(v);
		 return vl;
	 }

	 public static ParSet asParSet(ParList list) {
		 return new ParSet(list);
	 }

	 public static ParList asList(Par<?>[] array) {
		 ParList vl = new ParList(array.length);
		 for (Par<?> v : array)
			 vl.add(v);
		 return vl;
	 }

	 public void clearPars() throws EvaluationException {
			for (Par p : this) {
				try {
					p.setValue(null);
				} catch (Exception e) {
					throw new EvaluationException(e);
				} 
			}
		}
}
