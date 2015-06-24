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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A service interface related to {@link sorcer.service.Context}
 * evaluation/invocation and accessing contexts of of context service providers.
 * A context provider that can federate with relevant service providers to
 * provide associative data in the network.
 */
public interface Contexter<T> extends Remote {

	public Context<T> appendContext(Context<T> context)
			throws ContextException, RemoteException;
	
	public Context<T> getContext(Context<T> contextTemplate)
			throws RemoteException, ContextException;

	/**
	 * Appends an argument context to this context for a given path.
	 * @param context a context to be appended
	 * @param path an offset path of the argument context
	 * @return an appended context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context<T> appendContext(Context<T> context, String path)
			throws ContextException, RemoteException;
	
	/**
	 * Returns a subcontext at a given path.
	 * @param path a path in this context
	 * @return a subcontext of this context at <code>path</code>
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context<T> getContext(String path) throws ContextException,
			RemoteException;

}
