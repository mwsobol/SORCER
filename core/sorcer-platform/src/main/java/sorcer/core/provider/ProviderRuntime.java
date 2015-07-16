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
package sorcer.core.provider;

import java.io.Serializable;
import java.rmi.RemoteException;

import net.jini.core.lookup.ServiceID;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

/**
 * @author Mike Sobolewski
 */
public class ProviderRuntime implements Serializable {
	
	private Uuid runtimeId;
	
	private String name;
	
	private ServiceID providerId;
	
	protected String[] groupsToDiscover;

	protected String spaceGroup;

	protected String spaceName;
	
	protected Class[] publishedServiceTypes;
	
	public ProviderRuntime() {
		runtimeId =  UuidFactory.generate();
	}
	
	public Uuid getRuntimeId() {
		return runtimeId;
	}
	
	public void setRuntimeId(Uuid runtimeId) {
		 this.runtimeId = runtimeId;
	}
	
	public String getProviderName() {
		return name;
	}
	
	public ServiceID getProviderID() {
		return providerId;
	}
	
	public String[] getGroupsToDiscover() {
		return groupsToDiscover;
	}

	public String getSpaceGroup() {
		return spaceGroup;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public Class[] getPublishedServiceTypes() {
		return publishedServiceTypes;
	}
}
