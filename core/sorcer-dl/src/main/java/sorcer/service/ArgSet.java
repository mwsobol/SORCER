package sorcer.service;

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
import java.util.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ArgSet extends LinkedHashSet<Arg> {
	
	private static final long serialVersionUID = -4662755904016297879L;
	
	public ArgSet() {
		super();
	}
	
	public ArgSet(Arg...  args) {
		for (Arg arg : args) {
			add(arg);
		}
	}
	
	public Arg getArg(String argName) throws ArgException {
		for (Arg v : this) {
			if (v.getName().equals(argName))
				return v;
		}
		return null;
	}
	
	public void setValue(String argName, Object value)
			throws EvaluationException {
		Arg arg = null;
		for (Arg a : this) {
			if (a.getName().equals(argName)) {
				arg = a;
				if (arg instanceof Setter)
					try {
						((Setter) arg).setValue(value);
					} catch (Exception e) {
						throw new EvaluationException();
					} 
				break;
			}
		}
		if (arg == null)
			throw new ArgException("No such Arg in the list: " + argName);
	}
	
	public ArgSet selectArgs(String... argnames) {
		List<String> vnames = Arrays.asList(argnames);
		ArgSet out = new ArgSet();
		for (Arg v : this) {
			if (vnames.contains(v.getName())) {
				out.add(v);
			}
		}
		return out;
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof Arg))
			return false;
		else {
			for (Arg v : this) {
				if (v.getName().equals(((Arg)obj).getName()))
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
		 Iterator<Arg> i = iterator();
		 while (i.hasNext()) {
			 names.add(i.next().getName());
		 }
		 return names;
	 }
	 
	 public List<Object> getValues() throws ContextException, RemoteException {
		 List<Object> values = new ArrayList<Object>(size());
		 Iterator<Arg> i = iterator();
		 while (i.hasNext()) {
			 Object val = i.next();
			 if (val instanceof Evaluation) {
				 values.add(((Evaluation)val).getValue());
			 } else
				 values.add(null);
		 }
		 return values;
	 }
	 
	 public Arg[] toArray() {
		 Arg[] va = new Arg[size()];
		 return toArray(va);
	 }
			
	public static ArgSet asSet(Arg[] array) {
		ArgSet vl = new ArgSet();
		for (Arg v : array)
			vl.add(v);
		return vl;
	}

	public void clearArgs() throws EvaluationException {
		for (Arg p : this) {
			if (p instanceof Setter)
				try {
					((Setter) p).setValue(null);
				} catch (Exception e) {
					throw new EvaluationException();
				} 
		}
	}
	
}
