/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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
import net.jini.core.transaction.TransactionException;
import sorcer.core.Name;

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
		name = "fidelity" + count++;
	}

	public ServiceFidelity(Type type) {
		this();
		this.type = type;
	}
	public ServiceFidelity(String name) {
		this.name = name;
	}

	public ServiceFidelity(Arg name) {
		this.name = name.getName();
	}

	public ServiceFidelity(Fidelity<T> fi) {
		this.name = fi.getName();
		this.path = fi.getPath();
		this.select = fi.getSelect();
	}

	public ServiceFidelity(T[] selects) {
		name = "fidelity" + count++;
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
		this.name = "";
		for (String s : selects)
			this.selects.add((T) new Name(s));
	}

	public ServiceFidelity(String name, String... selects) {
		this.name = name;
		for (String s : selects)
			this.selects.add((T) new Name(s));
	}

	public ServiceFidelity(String name, T... selects) {
		this.name = name;
		for (T s : selects)
			this.selects.add(s);
	}

	public ServiceFidelity(ServiceFidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.type = fidelity.type;
		if (fidelity.name != null)
			this.name = fidelity.name;
		else
			this.name = "fidelity" + count++;
	}

	public ServiceFidelity(String name, ServiceFidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.type = fidelity.type;
		this.name = name;
	}

	public ServiceFidelity(String name, List<T> selectors) {
		for (T s : selectors)
			selects.add(s);
		this.name = name;
	}

	public ServiceFidelity(String name, T selector) {
		selects.add(selector);
		this.name = name;
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
	public Object exec(Arg... args) throws ServiceException, RemoteException, TransactionException {
		if (select instanceof Service) {
			return ((Service)select).exec(args);
		} else return select;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof ServiceFidelity) &&
				name.equals(((ServiceFidelity)obj).getName()) &&
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

	public Fidelity createFidelity() {
		return new Fidelity(getName(), getPath());
	}

	@Override
	public String toString() {
		return (path != null ? path + "@" + name : name )
				+ (selects != null && selects.size() > 0 ? ":" + selects : "");
	}

	public int size() {
		return selects.size();
	}
}
