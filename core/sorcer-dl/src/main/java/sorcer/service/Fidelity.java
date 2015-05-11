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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Sobolewski
 *
 */
public class Fidelity<S> implements Arg, Serializable {
	
	private static final long serialVersionUID = -875629011139790420L;
	
	private static int count = 0;

	public void setSelects(List<S> selects) {
		this.selects = selects;
	}

	protected String name;

	protected List<S> selects = new ArrayList<S>();

	public Fidelity() {
		super();
		name = "fidelity" + count++;
	}
	
	public Fidelity(String name) {
		this.name = name;
	}
	
	public Fidelity(S[] selects) {
		name = "fidelity" + count++;
		for (S s : selects)
			this.selects.add(s);
	}
	
	public Fidelity(String name, S[] selects) {
		this.name = name;
		for (S s : selects)
			this.selects.add(s);
	}
	
	public Fidelity(Fidelity<S> fidelity) {
		for (S s : fidelity.selects)
			selects.add(s);
		name = "fidelity" + count++;
	}

	public Fidelity(Fidelity<S> fidelity, List<String> selectors) {
		for (String selector : selectors) {
			for (S s : fidelity.selects) {
				if (s instanceof Signature && ((Signature)s).getName().equals(selector))
					selects.add(s);
			}
		}
	}

	public Fidelity(String name, Fidelity<S> fidelity) {
		for (S s : fidelity.selects)
			selects.add(s);
		this.name = name;
	}

	public Fidelity(String name, List<S> selectors) {
		for (S s : selectors)
			selects.add(s);
		this.name = name;
	}

	public List<S> getSelects() {
		return selects;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Fi: " + name + " " + selects;
	}
	
}
