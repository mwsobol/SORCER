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

import java.rmi.RemoteException;

/**
 * An top-level common interface for all service peers in SORCER.
 * Each service accepts a request for {@link Mogram} to exert
 * the federation of collaborating services as specified by the mogram.
 *
 * @author Mike Sobolewski
 */
public interface Service {

	/**
	 * A generic service request as specified by a mogram - a generic service
	 * mogram. It can be carried out dynamically by any
	 * <code>Service</code> peer matching the exertion's {@link Signature}.
	 *
	 * @param mogram an input mogram
	 * @param txn      The transaction (if any) under which to provide service.
	 * @return a resulting mogram
	 * @throws TransactionException if a transaction error occurs
	 * @throws ExertionException    if an exertion invocation failed for any reason
	 * @throws RemoteException
	 */
	public <T extends Mogram> T service(T mogram, Transaction txn)
			throws TransactionException, ExertionException, RemoteException;


	public <T extends Mogram> T service(T mogram)
			throws TransactionException, ExertionException, RemoteException;

}
