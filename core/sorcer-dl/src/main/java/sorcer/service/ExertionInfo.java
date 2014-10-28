/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
import java.util.List;

import net.jini.id.Uuid;
import sorcer.service.Exec.State;

/**
 * @author Mike Sobolewski
 * 
 */
public class ExertionInfo implements Serializable {

	static final long serialVersionUID = -2197284663002185050L;
	
	private String name;

	private Uuid id;

	private Integer status = Exec.INITIAL;

	private List<String> trace;
	
	private Uuid storeId;

	private Signature signature;

	public ExertionInfo(String name) {
		this.name = name;
	}
	
	public ExertionInfo(Exertion exertion) {
		name = exertion.getName();
		id = exertion.getId();
		status = exertion.getStatus();
		trace = exertion.getTrace();
		signature = exertion.getProcessSignature();
	}

	public ExertionInfo(Exertion exertion, Uuid storeId) {
		this(exertion);
		this.storeId = storeId;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Uuid getId() {
		return id;
	}

	public void setId(Uuid id) {
		this.id = id;
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public List<String> getTrace() {
		return trace;
	}

	public void setTrace(List<String> trace) {
		this.trace = trace;
	}

	public Uuid getStoreId() {
		return storeId;
	}

	public void setStoreId(Uuid storeId) {
		this.storeId = storeId;
	}
	
	public Signature getSignature() {
		return signature;
	}

	public void setSignature(Signature signature) {
		this.signature = signature;
	}

	public String describe() {
		StringBuilder info = new StringBuilder().append("name: ").append(name);
		info.append("  ID: ").append(id);
		info.append("  state: ").append(State.name(status));
		info.append("\nsignature: ").append(signature);
		info.append("\ntrace: ").append(trace);
		return info.toString();
	}
	
	public String toString() {
		StringBuilder info = new StringBuilder().append(
				this.getClass().getName()).append(": " + name);
		info.append("\n\tstatus: ").append(status);
		info.append(", \n\tsignature: ").append(signature);
		info.append(", \n\ttrace: ").append(trace);
		return info.toString();
	}
}
