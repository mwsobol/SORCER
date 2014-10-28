/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core;

import java.rmi.RemoteException;
import java.util.Vector;

import sorcer.core.provider.Provider;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

public interface SorcerNotifierProtocol extends Provider {

	public Integer register(RemoteEventListener listener, Object handback,
			Integer regFor, String userId, Vector sessionJobs, String sessionID)
			throws RemoteException;

	public Integer register(RemoteEventListener listener, Integer regFor,
			String userId, Vector sessionJobs, String sessionID)
			throws RemoteException;

	public Integer register(RemoteEventListener listener, Integer regFor,
			String userId, Vector sessionJobs) throws RemoteException;

	public void deleteListener(Integer id, Integer regFor)
			throws RemoteException;

	public void notify(RemoteEvent ev) throws RemoteException;

	public void appendJobToSession(String ownerID, String jobID,
			int sessionType, String sessionID) throws RemoteException;

	// Need to be implemented by subclass
	/*
	 * void failureNotify(RemoteEvent ev);
	 * 
	 * void exceptionNotify(RemoteEvent ev);
	 * 
	 * void successNotify(RemoteEvent ev);
	 * 
	 * void startNotify(RemoteEvent ev);
	 * 
	 * void makeMsgPersistant();
	 */
}
