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

import java.util.ArrayList;

/**
 * @author Mike Sobolewski
 *
 */
public class ServiceFidelity extends ArrayList<Signature> {
	
	private static final long serialVersionUID = -8756290111397908620L;
	
	private static int count = 0;
	
	private String name;

	public ServiceFidelity() {
		super();
		name = "unknown" + count++;
	}
	
	public ServiceFidelity(String name) {
		this.name = name;
	}
	
	public ServiceFidelity(String name, int initialCapacity) {
		super(initialCapacity);
		this.name = name;
	}
	
	public ServiceFidelity(int initialCapacity) {
		super(initialCapacity);
		name = "unknown" + count++;
	}
	
	public ServiceFidelity(Signature... signatures) {
		name = "unknown" + count++;
		for (Signature s : signatures)
			add(s);
	}
	
	public ServiceFidelity(String name, Signature... signatures) {
		this.name = name;
		for (Signature s : signatures)
			add(s);
	}
	
	public ServiceFidelity(ServiceFidelity fidelity) {
		for (Signature s : fidelity)
			add(s);
		name = "unknown" + count++;
	}
	
	public ServiceFidelity(ServiceFidelity fidelity, String[] selectors) {
		for (String s : selectors) {
			for (Signature sig : fidelity) {
				if (sig.getSelector().equals(s))
					add(sig);
			}
		}
		name = fidelity.getName();
	}
	
	public ServiceFidelity(String name, ServiceFidelity fidelity) {
		for (Signature s : fidelity)
			add(s);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Fi: " + name + " " + super.toString();
	}
	
}
