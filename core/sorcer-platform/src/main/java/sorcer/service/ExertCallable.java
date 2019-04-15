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

import net.jini.core.transaction.TransactionException;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class ExertCallable implements Callable<Routine> {
	private Routine exertion;

	public ExertCallable(Routine exertion) {
		this.exertion = exertion;
	}

	public Routine call() throws RemoteException, TransactionException,
			MogramException {
		if (exertion != null)
			return exertion.exert();

		return exertion;
	}

	public Routine getExertion() {
		return exertion;
	}
}