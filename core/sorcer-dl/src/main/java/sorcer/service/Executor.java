/* 
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

package sorcer.service;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import javax.security.auth.Subject;
import java.rmi.RemoteException;

/**
 * An top-level common local interface for all service-to-service (S2S)
 * providers in SORCER. Each service accepts a service-oriented mogram
 * {@link sorcer.service.Mogram} to be processed locally as the follow up of
 * {@link Exertion#exert(Mogram, Transaction)} and returns the mogram after
 * executing it locally.
 * 
 * @author Mike Sobolewski
 */
public interface Executor {

	/**
	 * A generic service request as specified by an exertion - a generic service
	 * message. It can be carried out dynamically and indirectly by any
	 * <code>Service</code> peer and directly by a <code>Service</code>
	 * matching the exertion's method {@link Signature}.
	 * 
	 * @param mogram
	 *            an input mogram
	 * @param txn
	 *            The transaction (if any) under which to provide service.
	 * @return a resulting exertion
	 * @throws TransactionException
	 *             if a transaction error occurs
	 * @throws RoutineException
	 *             if an exertion invocation failed for any reason
	 */
	
	public Mogram execute(Mogram mogram, Transaction txn)
			throws TransactionException, RoutineException, RemoteException;
	
	/**
	 * Returns true if the <code>subject</code> is authorized to execute the
	 * <code>exertion</code>.
	 * 
	 * @param subject
	 *            <code>subject</code> invoking the <code>exertion</code>
	 * @param signature
	 *            a signature
	 * @return true if authorized, otherwise false
	 * @throws RemoteException
	 */
	public boolean isAuthorized(Subject subject, Signature signature)throws RemoteException;

}
