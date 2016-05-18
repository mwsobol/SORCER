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
public class ServiceFidelity<T extends Arg> implements Multifidelity<T>, Arg {

	private static final long serialVersionUID = -875629011139790420L;

	public enum Type implements Arg {
		EMPTY, NAME, SYS, SIG, ENTRY, EXERT, CONTEXT, COMPONENT, COMPOSITE, MULTI, VAR, SERVICE;

		public String getName() {
			return toString();
		}
	}

	protected static int count = 0;

	protected String name;

	protected List<T> selects = new ArrayList<T>();

	// component exertion path
	protected String path = "";

	protected T select;

	public Type type = Type.NAME;

	public ServiceFidelity() {
		super();
		name = "fidelity" + count++;
	}

	public ServiceFidelity(String name) {
		this.name = name;
	}

	public ServiceFidelity(Arg name) {
		this.name = name.getName();
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
		return select;
	}

	public String getSelectName() {
		return select.getName();
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

	public void clear() {
		selects.clear();
	}

	public void setSelect(T selection) {
		this.select = selection;
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

	public void setSelects(List<T> selects) {
		this.selects = selects;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String fidelityPath) {
		this.path = fidelityPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + (path != null ? "@" + path + " " : " ") + selects;
	}

	public int size() {
		return selects.size();
	}
}
