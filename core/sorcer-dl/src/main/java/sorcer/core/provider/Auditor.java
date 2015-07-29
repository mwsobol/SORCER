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

package sorcer.core.provider;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sorcer.service.Context;

/**
 * <p>
 * All implementation of this interface are Service providers in SORCER and are
 * used to save ServiceContext sent to it. In SCAF the ServiceContext contains a
 * SignedServiceTask and a Subject that contains principal name of the
 * user/requestor from the card.
 */

public interface Auditor extends Remote {
	
	// public ServiceContext audit(ProviderContext context) throws
	// RemoteException;
	/**
	 * Audits the information sent in context in a persistent storage.
	 */
	public void audit(Context context) throws RemoteException;
}
