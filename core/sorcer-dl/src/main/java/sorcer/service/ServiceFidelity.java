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

import sorcer.service.modeling.Reference;
import sorcer.service.modeling.SupportComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Sobolewski
 *
 */
public class ServiceFidelity extends Fidelity<Service> implements SupportComponent {

	private static final long serialVersionUID = -875629011139790420L;

	public ServiceFidelity() {
		super();
		fiName = "fidelity" + count++;
	}

	public ServiceFidelity(Type type) {
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

	public ServiceFidelity(Fidelity fi) {
		this.fiName = fi.getName();
		this.path = fi.getPath();
		this.select = (Service) fi.getSelect();
	}

	public ServiceFidelity(Service... selects) {
		fiName = "fidelity" + count++;
		for (Service s : selects) {
			this.selects.add(s);
		}
		select = selects[0];
	}

	public ServiceFidelity(Signature... selects) {
		fiName = "fidelity" + count++;
		for (Service s : selects) {
			this.selects.add(s);
		}
		select = selects[0];
	}

	public Service getSelect(String name) {
		for (Service s : selects) {
			if (((Identifiable)s).getName().equals(name)) {
				return s;
			}
		}
		return null;
	}

	public ServiceFidelity(String name, Service... selects) {
		this.fiName = name;
		for (Service s : selects)
			this.selects.add(s);
	}

	public ServiceFidelity(ServiceFidelity fidelity) {
		for (Service s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.fiType = fidelity.fiType;
		if (fidelity.fiName != null)
			this.fiName = fidelity.fiName;
		else
			this.fiName = "fidelity" + count++;
	}

	public ServiceFidelity(String name, ServiceFidelity fidelity) {
		for (Service s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.fiType = fidelity.fiType;
		this.fiName = name;
	}

	public ServiceFidelity(String name, List<Service> selectors) {
		for (Service s : selectors)
			selects.add(s);
		this.fiName = name;
	}

	public ServiceFidelity(String name, Service selector) {
		selects.add(selector);
		this.fiName = name;
	}

	@Override
	public Service selectSelect(String fiName) throws ConfigurationException {
		Object selected = null;
		for (Service item : selects) {
			if (((Identifiable) item).getName().equals(fiName)) {
				selected = item;
				break;
			}
		}
		if (selected != null) {
			select = (Service) selected;
			return select;
		} else {
			throw new ConfigurationException("no such fidelity: " + fiName);
		}
	}

	public String getPath(String fidelityName) {
		for (Service select : selects) {
			if (((Identifiable)select).getName().equals(fidelityName)) {
				if (select instanceof ServiceFidelity) {
					return ((ServiceFidelity) select).getPath();
				}
			}
		}
		return null;
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

	public void removeSelect(Service select) {
		this.selects.remove(select);
	}

	public Service get(int index) {
		return selects.get(index);
	}

	public List getSelects(Context scope) throws ContextException {
		if (selects.get(0) instanceof Reference) {
			List<Object> ss = new ArrayList();
			for (Object s : selects) {
				try {
					((Reference)s).setScope(scope);
					ss.add(((Reference)s).get());
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
			for (Service item : selects) {
				if (item instanceof Signature
						&& ((Signature) item).getType().equals(Signature.Type.PROC)) {
					return (Signature) item;
				}
			}
		}
		return null;
	}

	public void setSelects(List<Service> selects) {
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
