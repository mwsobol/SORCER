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

package sorcer.service;

import java.rmi.RemoteException;

import javax.security.auth.Subject;

import net.jini.id.Uuid;

/**
 * All providers that need to be monitored implement this interface. If a
 * provider is a monitorable, then the provider has to have the capability to
 * keep track of the providerExertionID's and the task corresponding to it. It
 * has to also provide functionality to stop, suspend any running exertion or
 * resume/step any suspended or finished tasks.
 * 
 * All states of the executing exertions are maintained by the MonitorManager.
 */

public interface Monitorable {

	/**
	 * MonitorManagers call stop on a MonitorableService. Once stop is called,
	 * the monitorables must return the context and then initiate the cleanup.
	 * 
	 * @throws UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */

	public void stop(Uuid ref, Subject subject) throws RemoteException,
			UnknownExertionException, AccessDeniedException;

	/**
	 * MonitorManagers call suspend a MonitorableService. Once suspend is
	 * called, the monitorables must suspend immediatly and return the suspended
	 * state of the context.
	 * 
	 * @throws UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */

	public void suspend(Uuid ref, Subject subject) throws RemoteException,
			UnknownExertionException, AccessDeniedException;

	/**
	 * Resume if the resume functionality is supported by the monitorables. Else
	 * start from the begining.
	 * 
	 * @throws UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public void resume(Routine ex) throws RemoteException, RoutineException;

	/**
	 * Step if the step functionality is supported by the monitorables. Else
	 * start from the begining.
	 * 
	 * @throws UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 * 
	 * @throws RemoteException
	 *             if there is a communication error
	 * 
	 */
	public void step(Routine ex) throws RemoteException, RoutineException;

}
