/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.co.tuple;

/**
 * @author Mike Sobolewski
 */

import sorcer.core.context.model.ent.Entry;
import sorcer.service.ServiceFidelity;

import java.util.HashMap;
import java.util.Map;

public class FidelityEntry extends Entry<ServiceFidelity> {

	private static final long serialVersionUID = -1L;

	// fidelities for this entry
	protected Map<String, ServiceFidelity> fidelities;

	// the current fidelity alias, as it is named in 'fidelities'
	// its original key might be different if aliasing is used
	// for already existing names
	protected String fidelitySelector;

	protected ServiceFidelity fidelity;

	public FidelityEntry(String name) {
		super(name);
	}

	public FidelityEntry(String name, ServiceFidelity... fidelities) {
		super(name);
		this.fidelities = new HashMap<String, ServiceFidelity>();
		for (ServiceFidelity f : fidelities)  {
			this.fidelities.put(f.getName(), f);
		}
		this.fidelity = fidelities[0];
		fidelitySelector =  fidelity.getName();
	}
	
	public ServiceFidelity getFidelity() {
		return fidelity;
	}
	
	public void setFidelity(ServiceFidelity fidelity) {
		this.fidelity = fidelity;
	}


	public Map<String, ServiceFidelity> getFidelities() {
		return fidelities;
	}

	public void setFidelities(Map<String, ServiceFidelity> fidelities) {
		this.fidelities = fidelities;
	}

}