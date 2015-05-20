/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.core.monitor;

import net.jini.id.Uuid;
import sorcer.core.UEID;
import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.ExertionInfo;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Map;

/**
 * An interface for UI to interact with the monitor. Get all the refIDs and
 * monitored exertion info for a particular credential. The refID will be the
 * combination of serviceID which is globally unique and the referenceID for the
 * exertion.
 * 
 * This referenceID is also required by the monitorable to be passed back to the
 * MonitorableService (ie, the MonitorManager) to control the exertion
 * execution.
 * 
 * The spec requires that the implementation of this interface gets all the
 * monitored exertions for the particular user credentials.
 * 
 * 
 */

public interface MonitorUIManagement {

	/**
	 * The spec requires that this method gets all the monitorable exertion
	 * infos from all the monitor managers and return a Hashtable where
	 * 
	 * key -> ExertionReferenceID value -> Some info regarding this exertion
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public Map<Uuid, ExertionInfo> getMonitorableExertionInfo(
			Exec.State aspect, Principal principal)
			throws RemoteException, MonitorException;

	/**
	 * For this reference ID, which references a exertion in a monitor, get the
	 * exertion if the principal has enough credentials.
	 * 
	 * @throws MonitorException
	 *             if the client does not have enough credentials.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public Exertion getMonitorableExertion(UEID cookie, Principal credentials)
			throws RemoteException, MonitorException;

	/**
	 * For this reference ID, which references a exertion in a datastore, get the
	 * exertion if the client has enough credentials.
	 * 
	 * @throws MonitorException
	 *             if the client does not have enough credentials.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public Exertion getMonitorableExertion(Uuid id, Principal credentials)
			throws RemoteException, MonitorException;
}
