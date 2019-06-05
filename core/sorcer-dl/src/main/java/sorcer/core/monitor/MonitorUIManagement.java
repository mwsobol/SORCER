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

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.Uuid;
import sorcer.core.UEID;
import sorcer.service.Exec;
import sorcer.service.Routine;
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
	 * key -> ExertionReferenceID execute -> Some info regarding this exertion
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	Map<Uuid, ExertionInfo> getMonitorableExertionInfo(Exec.State aspect, Principal principal)
		throws RemoteException, MonitorException;

	/**
	 * For this reference ID, which references a exertion in a monitor, getValue the
	 * exertion if the principal has enough credentials.
	 * 
	 * @throws MonitorException
	 *             if the client does not have enough credentials.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
    Routine getMonitorableExertion(UEID cookie, Principal credentials)
        throws RemoteException, MonitorException;

	/**
	 * For this reference ID, which references a exertion in a datastore, getValue the
	 * exertion if the client has enough credentials.
	 * 
	 * @throws MonitorException
	 *             if the client does not have enough credentials.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	Routine getMonitorableExertion(Uuid id, Principal credentials)
			throws RemoteException, MonitorException;

    /**
     * The register method creates a leased
     * {@link net.jini.core.event.EventRegistration} for the notification of
     * sorcer.core.monitor.MonitorEvent events that correspond to exertions
     * being monitored for a provided Principal.
     * type passed in based on the requested lease duration. The implied
     * semantics of notification are dependant on
     * {@code org.rioproject.event.EventHandler} specializations.
     *
     * @param principal The Principal identifying events that match the provided
     *                  Principal. If null, all Exertions will be matched.
     * @param listener A RemoteEventListener.
     * @param duration Requested EventRegistration lease duration
     *
     * @return An EventRegistration
     *
     * @throws IllegalArgumentException if any of the parameters are null
     * @throws LeaseDeniedException if the duration parameter is not accepted
     * @throws RemoteException if communication errors occur
     */
    EventRegistration register(Principal principal, RemoteEventListener listener, long duration) throws LeaseDeniedException, RemoteException;
}
