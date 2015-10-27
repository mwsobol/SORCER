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

import sorcer.core.Name;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Sobolewski
 *
 */
public class Fidelity<T extends Arg> implements Arg, Serializable {
	
	private static final long serialVersionUID = -875629011139790420L;

	public enum Type implements Arg {
		EMPTY, NAME, SYS, SIG, EXERT, CONTEXT, COMPONENT, COMPOSITE, MULTI, VAR;

		public String getName() {
			return toString();
		}
	}

	private static int count = 0;

	protected String name;

	protected List<T> selects = new ArrayList<T>();

	// component exertion path
	protected String path = "";

	protected T selection;

	public Type type = Type.NAME;

	public Fidelity() {
		super();
		name = "fidelity" + count++;
	}
	
	public Fidelity(String name) {
		this.name = name;
	}

	public Fidelity(Arg name) {
		this.name = name.getName();
	}
	public Fidelity(T[] selects) {
		name = "fidelity" + count++;
		for (T s : selects)
			this.selects.add(s);
	}

	public T getSelect(String name) {
		for (T s : selects) {
			if (s.getName().equals(name)) {
				return s;
			}
			break;
		}
		return null;
	}

	public Fidelity(String... selects) {
		this.name = "";
		for (String s : selects)
			this.selects.add((T) new Name(s));
	}

	public Fidelity(String name, String... selects) {
		this.name = name;
		for (String s : selects)
			this.selects.add((T) new Name(s));
	}

	public Fidelity(String name, T... selects) {
		this.name = name;
		for (T s : selects)
			this.selects.add(s);
	}

	public Fidelity(Fidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.type = fidelity.type;
		if (fidelity.name != null)
			this.name = fidelity.name;
		else
			this.name = "fidelity" + count++;
	}

	public Fidelity(String name, Fidelity<T> fidelity) {
		for (T s : fidelity.selects)
			selects.add(s);
		this.path = fidelity.path;
		this.type = fidelity.type;
		this.name = name;
	}

	public Fidelity(String name, List<T> selectors) {
		for (T s : selectors)
			selects.add(s);
		this.name = name;
	}

	public Fidelity(String name, T selector) {
		selects.add(selector);
		this.name = name;
	}

	public T getSelection() {
		return selection;
	}

	public void setSelection(T selection) {
		this.selection = selection;
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
