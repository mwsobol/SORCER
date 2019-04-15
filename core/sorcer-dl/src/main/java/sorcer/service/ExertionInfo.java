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

import net.jini.id.Uuid;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Mike Sobolewski
 * 
 */
public class ExertionInfo implements Comparable, Serializable {

	static final long serialVersionUID = -2197284663002185050L;
	
	private String name;

	private Uuid id;

	private Integer status = Exec.INITIAL;

	private List<String> trace;
	
	private Uuid storeId;

	private Signature signature;

    private Date creationDate;

    private Date lastUpdateDate;

    private Routine exertion;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ExertionInfo(String name) {
		this.name = name;
	}
	
	public ExertionInfo(Routine exertion) throws RemoteException {
		name = exertion.getName();
		id = exertion.getId();
		status = exertion.getStatus();
		trace = exertion.getTrace();
        creationDate = exertion.getCreationDate();
        lastUpdateDate = new Date();
		signature = exertion.getProcessSignature();
        this.exertion = exertion;
	}

	public ExertionInfo(Routine exertion, Uuid storeId) throws RemoteException {
		this(exertion);
		this.storeId = storeId;
	}

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
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

    public Routine getExertion() {
        return exertion;
    }

    public String describe() {
		StringBuilder info = new StringBuilder().append("name: ").append(name);
		info.append("  ID: ").append(id);
		info.append("  state: ").append(Exec.State.name(status));
        info.append("\ncreated at: ").append((creationDate!=null) ? sdf.format(creationDate) : "");
        info.append(",  last updated at: ").append(lastUpdateDate);
        info.append("\nsignature: ").append(signature);
		info.append("\ntrace: ").append(trace);
		return info.toString();
	}
	
	public String toString() {
		StringBuilder info = new StringBuilder().append(
				this.getClass().getName()).append(": " + name);
		info.append("\n\tstatus: ").append(Exec.State.name(status));
        info.append(", \n\tcreated at: ").append((creationDate!=null) ? sdf.format(creationDate) : "");
        info.append(", \n\tlast updated at: ").append(lastUpdateDate);
        info.append(", \n\tsignature: ").append(signature);
		info.append(", \n\ttrace: ").append(trace);
		return info.toString();
	}

    @Override
    public int compareTo(Object o) {
        return getCreationDate().compareTo(((ExertionInfo)o).getCreationDate());
    }
}
