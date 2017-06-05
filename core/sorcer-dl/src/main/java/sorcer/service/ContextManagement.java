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
 * Context Management provides methods for managing contexts over the network.
 * The ContextManagement interface is implemented by providers to allow for
 * remote calls to obtain, update and delete contexts.
 */
@SuppressWarnings("rawtypes")
public interface ContextManagement extends Remote {
	// used in sorcer.core.exertion.ContextTask
	public final static String CONTEXT_REQUEST_PATH = "request/context";
	public final static String CONTEXT_FILENAME = "context.cxt";

	/**
	 * Returns the default context by the defining provider.
	 * @throws RemoteException
	 */
	public Context getContext() throws RemoteException;
	
	/**
	 * Returns the default context script by the defining provider.
	 * @throws RemoteException
	 */
	public String getContextScript() throws RemoteException;
	
	/**
	 * Obtains the context from data storage used to hold the contexts. Returns
	 * an empty context in the event the requested one is not found.
	 * 
	 * @param interfaceName
	 *            String of the interface name the method is defined on
	 * @param methodName
	 *            Tag of the method the context is saved as
	 * @return Context representing the methodName
	 * @throws RemoteException
	 */
	public Context getMethodContext(String interfaceName, String methodName)
			throws RemoteException;

	/**
	 * Returns the context script or its URL.
	 * 
	 * @param interfaceName
	 *            String of the interface name the method is defined on
	 * @param methodName
	 *            Tag of the method the context is saved as
	 * @return String representing the methodName
	 * @throws RemoteException
	 */
	public String getMethodContextScript(String interfaceName, String methodName)
			throws RemoteException;
	
	/**
	 * Saves the context to the local data store and updates the file used for
	 * permanent storage
	 * 
	 * @param interfaceName
	 *            String of the interface name the method is defined on
	 * @param methodName
	 *            Tag of the method the context is saved as
	 * @param theContext
	 *            Context to be saved
	 * @return Boolean indicating if the context was stored properly
	 * @throws RemoteException
	 */
	public boolean saveMethodContext(String interfaceName, String methodName,
			Context theContext) throws RemoteException;

	/**
	 * Gets the list of methods of the defined interface which have contexts
	 * defined on in the context storage
	 * 
	 * @param interfaceName
	 *            String of the interface to lookup
	 * @return String array of the methods which have a context defined
	 * @throws RemoteException
	 */
	public String[] currentContextList(String interfaceName)
			throws RemoteException;

	/**
	 * Deletes a context from the data store, the data store is then updated so
	 * that future requests are handled properly.
	 * 
	 * @param interfaceName
	 *            String showing which interface the method is defined for
	 * @param methodName
	 *            String of the method name to remove the context for
	 * @return boolean indicating if the context has been deleted successfully
	 * @throws RemoteException
	 */
	public boolean deleteContext(String interfaceName, String methodName)
			throws RemoteException;

	/**
	 * Get the context from the provider specified for the interface and method
	 * requested
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param interfaceName
	 *            the currently selected interface
	 * @param methodName
	 *            String of the currently selected method name
	 * @return Context defined for the specified method
	 * @throws RemoteException
	 */
	public Context getContext(String providerName, String serviceType,
			String methodName) throws RemoteException;

	/**
	 * Saves the context to the provider specified for the interface and method
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param serviceType
	 *            the currently selected interface
	 * @param methodName
	 *            String of the currently selected method name
	 * @param theContext
	 *            Context to be stored
	 * @return Boolean indicating if the operation was successful
	 * @throws RemoteException
	 */
	public Boolean saveContext(String providerName, String serviceType,
			String methodName, Context theContext) throws RemoteException;

	/**
	 * Delete the context from the provider specified for the interface and
	 * method
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param serviceType
	 *            the currently selected interface
	 * @param methodName
	 *            String of the currently selected method name
	 * @return Boolean indicating if the operation was successful
	 * @throws RemoteException
	 */
	public Boolean deleteContext(String providerName, String serviceType,
			String methodName) throws RemoteException;

	/**
	 * Get a list of the contexts currently stored on the provider.
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param serviceType
	 *            the currently selected interface
	 * @return a String array of the available contexts stored on the provider
	 * @throws RemoteException
	 */
	public String[] getSavedContextList(String providerName,
			String serviceType) throws RemoteException;

}
