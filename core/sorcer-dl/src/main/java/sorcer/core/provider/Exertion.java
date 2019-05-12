/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * A management of a mogram execution (exerting a federation of service providers)
 * with given parameters for its actualized federation.
 *
 * In particular, a service container ServiceProvider, ServiceShell,
 * and system service beans of type SystemServiceBean are exerters.
 *
 * @author Mike Sobolewski
 */
public interface Exertion extends Service {
	/**
	 *
	 * A generic federated execution.
	 *
	 * @param mogram an input mogram
	 * @param txn The transaction (if any) under which to provide service.
	 * @return a resulting mogram
	 * @throws TransactionException if a transaction error occurs
	 * @throws MogramException    if an mogram exerting failed for any reason
	 * @throws RemoteException
	 */
	public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... entries)
			throws MogramException, RemoteException;

}
