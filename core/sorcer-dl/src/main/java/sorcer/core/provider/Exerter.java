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

import java.rmi.RemoteException;

import sorcer.service.Arg;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.service.MogramException;

/**
 * A functionality required for running exertions with given parameters.
 * 
 * @author Mike Sobolewski
 */
public interface Exerter {
	
	public Exertion exert(Exertion xrt, Arg... entries) throws TransactionException,
			ExertionException, RemoteException, MogramException;

	public Exertion exert(Exertion xrt, Transaction txn, Arg... entries)
			throws TransactionException, MogramException, RemoteException;

}
