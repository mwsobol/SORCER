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

package sorcer.service;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import sorcer.core.context.model.par.ParException;


/**
 * @author Mike Sobolewski
 */
public class ArgList extends ArrayList<Arg> {

	static final long serialVersionUID = -4997255102658715823L;

	public ArgList() {
		super();
	}

	public ArgList(int size) {
		super(size);
	}

	public ArgList(Set<Arg> parSet) {
		addAll(parSet);
	}

	public ArgList(ArgList... parLists) {
		super();
		for (ArgList pl : parLists) {
			addAll(pl);
		}
	}

	public Arg getArg(String parName) throws ParException {
		for (Arg p : this) {
			if (p.getName().equals(parName)) {
				return p;
			}
		}
		return null;
	}

	public void setArgValue(String parName, Object value)
			throws EvaluationException {
		Arg par = null;
		for (Arg p : this) {
			if (p.getName().equals(parName)) {
				par = p;
				if (par instanceof Setter)
					try {
						((Setter) par).setValue(value);
					} catch (Exception e) {
						throw new EvaluationException(e);
					}
				break;
			}
		}
		if (par == null)
			throw new ParException("No such Arg in the list: " + parName);
	}
	
	public ArgList selectArgs(List<String>... parNames) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : parNames) {
			allParNames.addAll(nl);
		}
		ArgList out = new ArgList();
		for (Arg v : this) {
			if (allParNames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public ArgList selectArgs(String... parNames) {
		List<String> vnames = Arrays.asList(parNames);
		ArgList out = new ArgList();
		for (Arg v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public boolean containsArgName(String name) {
		for (Arg v : this) {
			if (v.getName().equals(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Arg))
			return false;
		else {
			for (Arg v : this) {
				if (v.getName().equals(((Arg) obj).getName()))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Arg)) {
			return false;
		} else {
			for (Arg v : this) {
				if (v.getName().equals(((Arg) obj).getName())) {
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
			Object obj = get(i);
			if (obj instanceof Evaluation)
				values.add(((Evaluation)obj).getValue());
			else
				values.add(null);
		}
		return values;
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

}
