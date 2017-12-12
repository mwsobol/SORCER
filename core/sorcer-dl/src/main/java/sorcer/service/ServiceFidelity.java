/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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
import sorcer.service.modeling.Reference;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Sobolewski
 *
 */
public class ServiceFidelity<T extends Arg> extends Fidelity<T> implements Multifidelity<T>  {

	private static final long serialVersionUID = -875629011139790420L;

	protected List<T> selects = new ArrayList<T>();

	public ServiceFidelity() {
		super();
		fiName = "fidelity" + count++;
	}

	public ServiceFidelity(Fi.Type type) {
		this();
		this.fiType = type;
	}
	public ServiceFidelity(String name) {
		this.fiName = name;
	}

	public ServiceFidelity(String name, String path) {
		this.fiName = name;
		this.path = path;
	}

	public ServiceFidelity(Arg name) {
		this.fiName = name.getName();
	}

	public ServiceFidelity(Fidelity<T> fi) {
		this.fiName = fi.getName();
		this.path = fi.getPath();
		this.select = fi.getSelect();
	}

	public ServiceFidelity(T[] selects) {
		fiName = "fidelity" + count++;
		for (T s : selects)
			this.selects.add(s);
		select =  selects[0];
	}

	public T getSelect(String name) {
		for (T s : selects) {
			if (s.getName().equals(name)) {
				return s;
			}
		}
		return null;
	}

	public ServiceFidelity(String... selects) {
		this.fiName = "";
		fiType = Fi.Type.NAME;
		for (String s : selects)
			this.selects.add((T) new Tag(s));
	}

	public ServiceFidelity(String name, String... selects) {
		this.fiName = name;
		fiType = Fi.Type.NAME;
		for (String s : selects)
			this.selects.add((T) new Tag(s));
	}

	public ServiceFidelity(String name, T... selects) {
		this.fiName = name;
		for (T s : selects)
			this.selects.add(s);
	}

	public ServiceFidelity(ServiceFidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.fiType = fidelity.fiType;
		if (fidelity.fiName != null)
			this.fiName = fidelity.fiName;
		else
			this.fiName = "fidelity" + count++;
	}

	public ServiceFidelity(String name, ServiceFidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.fiType = fidelity.fiType;
		this.fiName = name;
	}

	public ServiceFidelity(String name, List<T> selectors) {
		for (T s : selectors)
			selects.add(s);
		this.fiName = name;
	}

	public ServiceFidelity(String name, T selector) {
		selects.add(selector);
		this.fiName = name;
	}

	public T getSelect() {
		// if a select not set return the firts one option
		if (select == null && selects.size() > 0) {
			select = selects.get(0);
		}
		return select;
	}

	public String getSelectName() {
		return select.getName();
	}

	public List<String> getSelectNames() {
		List<String> names = new ArrayList<>(selects.size());
		for (T item : selects) {
			names.add(item.getName());
		}
		return names;
	}

	public void setSelect(String fiName) {
		for (T item : selects) {
			if (item.getName().equals(fiName)) {
				this.select = item;
				return;
			}

		}
	}

	public String getPath(String fidelityName) {
		for (T select : selects) {
			if (select.getName().equals(fidelityName)) {
				if (select instanceof ServiceFidelity) {
					return ((ServiceFidelity) select).getPath();
				}
			}
		}
		return null;
	}

	@Override
	public Object exec(Arg... args) throws ServiceException, RemoteException {
		if (select instanceof Service) {
			return ((Service)select).exec(args);
		} else return select;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof ServiceFidelity) &&
				fiName.equals(((ServiceFidelity)obj).getName()) &&
				path.equals(((ServiceFidelity)obj).getPath())) {
			return true;
		} else {
			return false;
		}
	}

	public void clear() {
		selects.clear();
	}

	public void removeSelect(T select) {
		this.selects.remove(select);
	}

	public void addSelect(T select) {
		selects.add(select);
	}

	public T get(int index) {
		return selects.get(index);
	}

	public List<T> getSelects() {
		return selects;
	}

	public List getSelects(Context scope) throws ContextException {
		if (selects.get(0) instanceof Reference) {
			List<Object> ss = new ArrayList();
			for (Object s : selects) {
				try {
					((Reference)s).setScope(scope);
					ss.add(((Reference)s).getValue());
				} catch (Exception e) {
					throw new ContextException(e);
				}
			}
			return ss;
		}
		return selects;
	}

	public Signature getProcessSignature() {
		if (selects.size() > 0) {
			for (T item : selects) {
				if (item instanceof Signature
						&& ((Signature) item).getType().equals(Signature.Type.PROC)) {
					return (Signature) item;
				}
			}
		}
		return null;
	}

	public void setSelects(List<T> selects) {
		this.selects = selects;
	}

	protected boolean selectsFidelityTypeOnly() {
		for (Object fi : selects) {
			if (fi.getClass()!=Fidelity.class) {
				return false;
			}
		}
		return true;
	}

	public List<Fidelity> selectFidelities(List<Fidelity> fidelities) {
		List<Fidelity> fis = new ArrayList();
		for (Object fi : fidelities) {
			if (fi.getClass()==Fidelity.class) {
				fis.add((Fidelity)fi);
			}
		}
		return fis;
	}

	protected List<Fidelity> selectFidelities() {
		List<Fidelity> fis = new ArrayList();
		for (Object fi : selects) {
			if (fi.getClass()==Fidelity.class) {
				fis.add((Fidelity)fi);
			}
		}
		return fis;
	}
	
	@Override
	public String toString() {
		return (path != null ? fiName + "@" + path : fiName )
				+ (selects != null && selects.size() > 0 ? ":" + selects : "");
	}

	public int size() {
		return selects.size();
	}


}
