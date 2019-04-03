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

package sorcer.core.context.model.proc;

import java.util.ArrayList;

import sorcer.core.context.model.ent.Subroutine;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class ContextFidelity extends ArrayList<Subroutine> {
	
	private static final long serialVersionUID = -1L;
	
	private static int count = 0;
	
	private String name;

	public ContextFidelity() {
		super();
		name = "unknown" + count++;
	}
	
	public ContextFidelity(String name) {
		this.name = name;
	}
	
	public ContextFidelity(String name, int initialCapacity) {
		super(initialCapacity);
		this.name = name;
	}
	
	public ContextFidelity(int initialCapacity) {
		super(initialCapacity);
		name = "unknown" + count++;
	}
	
	public ContextFidelity(Subroutine... entries) {
		name = "unknown" + count++;
		for (Subroutine e : entries)
			add(e);
	}
	
	public ContextFidelity(String name, Subroutine... entries) {
		this.name = name;
		for (Subroutine e : entries)
			add(e);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "procFi: " + name + " " + super.toString();
	}
	
}
