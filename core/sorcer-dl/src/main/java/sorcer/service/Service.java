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

import java.rmi.RemoteException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

/**
 * An top-level common interface for all service peers in
 * SORCER. Each servicer accepts a request for {@link Exertion} to
 * exert the federation of collaborating services.
 * 
 * @author Mike Sobolewski
 */
public interface Service<T> extends Evaluation<T> {
	
	/**
	 * A generic service request as specified by an exertion - a generic service
	 * message. It can be carried out dynamically and indirectly by any
	 * <code>Service</code> peer and directly by a <code>Service</code>
	 * matching the exertion's method {@link Signature}.
	 * 
	 * @param exertion
	 *            an input exertion
	 * @param txn
	 *            The transaction (if any) under which to provide service.
	 * @return a resulting exertion
	 * @throws TransactionException
	 *             if a transaction error occurs
	 * @throws ExertionException
	 *             if an exertion invocation failed for any reason
	 * @throws RemoteException
	 */
	public Exertion service(Exertion exertion, Transaction txn)
			throws TransactionException, ExertionException, RemoteException;
	
	
	public Exertion service(Exertion exertion)
			throws TransactionException, ExertionException, RemoteException;

}
