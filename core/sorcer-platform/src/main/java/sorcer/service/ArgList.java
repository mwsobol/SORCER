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

import sorcer.core.Tag;
import sorcer.core.context.model.ent.EntException;
import sorcer.service.modeling.Functionality;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * @author Mike Sobolewski
 */
public class ArgList extends ArrayList<Arg> {

	static final long serialVersionUID = -4997255102658715823L;

	protected Functionality.Type type = Functionality.Type.ARG;;

	public ArgList() {
		super();
	}

	public ArgList(int size) {
		super(size);
	}

	public ArgList(Arg... array) {
		super();
		for (Arg arg : array) {
			add(arg);
		}
	}

	public ArgList(Set<Arg> argSet) {
		addAll(argSet);
	}

	public ArgList(String... names) {
		super();
		for (String s : names) {
			add(new Tag(s));
		}
	}

	public ArgList(ArgList... argLists) {
		super();
		for (ArgList pl : argLists) {
			addAll(pl);
		}
	}

	public Arg getArg(String parName) throws EntException {
		for (Arg p : this) {
			if (p.getName().equals(parName)) {
				return p;
			}
		}
		return null;
	}

	public Functionality.Type getType() {
		return type;
	}

	public void setType(Functionality.Type type) {
		this.type = type;
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
			throw new EntException("No such Arg in the list: " + parName);
	}
	
	public ArgList selectArgs(List<String>... names) {
		List<String> allParNames = new ArrayList<String>();
		for (List<String> nl : names) {
			allParNames.addAll(nl);
		}
		ArgList out = new ArgList();
		for (Arg a : this) {
			if (allParNames.contains(a.getName())) {
				out.add(a);
			}
		}
		return out;
	}

	public ArgList selectArgs(String... names) {
		List<String> anames = Arrays.asList(names);
		ArgList out = new ArgList();
		for (Arg v : this) {
			if (anames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	public boolean containsArgName(String name) {
		for (Arg a : this) {
			if (a.getName().equals(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Arg))
			return false;
		else {
			for (Arg a : this) {
				if (a.getName().equals(((Arg) obj).getName()))
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
			for (Arg a : this) {
				if (a.getName().equals(((Arg) obj).getName())) {
					super.remove(a);
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getNameList() {
		List<String> names = new ArrayList<String>(size());
		for (int i = 0; i < size(); i++) {
			names.add(get(i).getName());
		}
		return names;
	}

	public String[] getNameArray() {
		String[] names = new String[size()];
		for (int i = 0; i < size(); i++) {
			names[i] = get(i).getName();
		}
		return names;
	}

	public List<Object> getValues() throws EvaluationException, RemoteException {
		List<Object> values = new ArrayList<Object>(size());
		for (int i = 0; i < size(); i++) {
			Object obj = get(i);
			if (obj instanceof Evaluation)
				try {
					values.add(((Evaluation)obj).evaluate());
				} catch (ContextException e) {
					throw new EvaluationException(e);
				}
			else
				values.add(null);
		}
		return values;
	}
	
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(getNameList().toString());
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
		return getNameList().toString();
	}

}
