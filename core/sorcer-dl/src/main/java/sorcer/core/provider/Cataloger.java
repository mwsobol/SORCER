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

import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import sorcer.service.Context;
import sorcer.service.Exerter;
import sorcer.service.Service;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Defines the interface for the SORCER catalog service. It is implemented by
 * {@link sorcer.core.provider.cataloger.ServiceCataloger}.
 * 
 */

public interface Cataloger extends Service, Remote {

	/**
	 * Returns a SORCER service provider identified by its primary service type.
	 * 
	 * @param primaryInterfaces
	 *            - the interface of a SORCER provider
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	public Exerter lookup(Class... primaryInterfaces) throws RemoteException;

	/**
	 * * Returns a SORCER service provider identified by its primary service
	 * type and the provider's name/
	 * 
	 * @param providerName
	 *            - a provider name, a friendly provider's ID.
	 * @param primaryInterfaces
	 *            - interface of a SORCER provider
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	public Exerter lookup(String providerName, Class... primaryInterfaces)
			throws RemoteException;

	/**
	 * * Returns a JINI ServiceItem containing SORCER service provider
	 * identified by its primary service type and the provider's name/
	 * 
	 * @param providerName
	 *            - a provider name, a friendy provider's ID.
	 * @param serviceTypes
	 *            - the interface of a SORCER provider
	 * @return ServiceItem
	 * @throws RemoteException
	 */
	public ServiceItem lookupItem(String providerName, Class... serviceTypes)
			throws RemoteException;

	/**
	 * Returns a SORCER service provider identified by its service ID.
	 * 
	 * @param sid
	 *            - provider's ID
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	public Exerter lookup(ServiceID sid) throws RemoteException;


	/**
	 * Returns at most maxMatches items matching the template, plus the total
	 * number of items that match the template. The return execute is never null,
	 * and the returned items array is only null if maxMatches is zero. For each
	 * returned impl, if the service object cannot be deserialized, the service
	 * field of the impl is set to null and no exception is thrown. Similarly,
	 * if an attribute set cannot be deserialized, that element of the
	 * attributeSets array is set to null and no exception is thrown.
	 * 
	 * @param tmpl
	 *            - template to match
	 * @param maxMatches
	 * @return a ServiceMatches instance that contains at most maxMatches items
	 *         matching the template, plus the total number of items that match
	 *         the template. The return execute is never null, and the returned
	 *         items array is only null if maxMatches is zero.
	 * @throws RemoteException
	 */
	public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches)
			throws RemoteException;

	/**
	 * Returns a map with a key as service interface (those interfaces
	 * which are placed under package sorcer.provider or sorcer.core.provider)
	 * and its execute is a list of interface's method names.
	 * 
	 * @return a hash map of all SORCER provider interfaces with corresponding
	 *         method names.
	 * @throws RemoteException
	 */
	public Map<String, String> getProviderMethods() throws RemoteException;

	/**
	 * Returns a map with a key as service name (only those services that implement sorcer.service.Service interface)
	 * and a list of Codebase URLs for that service
	 *
	 * @return a hash map of all SORCER provider names with corresponding
	 *         codebase URLs.
	 * @throws RemoteException
	 */
	public Map<String, URL[]>  getProviderCodebaseURLs() throws RemoteException;

	/**
	 * Returns a String array of the providers currently on the network.
	 * 
	 * @return a String array of the current provider names
	 * @throws RemoteException
	 */
	public String[] getProviderList() throws RemoteException;

	/**
	 * Returns a String array of the interfaces defined by the given provider
	 * name. The interface list excludes the common interface names.
	 * 
	 * @param serviceType
	 *            Service type of the currently selected provider
	 * @return a String array of the interfaces defined by the provider
	 * @throws RemoteException
	 */
	public String[] getInterfaceList(String serviceType)
			throws RemoteException;

	/**
	 * Returns the methods defined on the interface of the provider specified.
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param serviceType
	 *            the currently selected interface
	 * @return a String array of the methods implemented by the interface.
	 * @throws RemoteException
	 */
	public String[] getMethodsList(String providerName, String serviceType)
			throws RemoteException;

	/**
	 * Gets the LookupLocator from Cataloger
	 * 
	 * @return array of LookupLocators cataloger is using.
	 * @throws RemoteException
	 */
	public LookupLocator[] getLL() throws RemoteException;

	/**
	 * Get the groups Cataloger is registered in.
	 * 
	 * @return String array of the Groups cataloger is registered in
	 * @throws RemoteException
	 */
	public String[] getGroups() throws RemoteException;

	/**
	 * Get a template for a Service, this is used create a local lookup service
	 * listener
	 * 
	 * @return ServiceTemplate used by cataloger
	 * @throws RemoteException
	 */
	public ServiceTemplate getTemplate() throws RemoteException;
	/**
	 * Create an exertion on the provider specified, sending the context to the
	 * method specified.
	 * 
	 * @param providerName
	 *            String of the currently selected provider
	 * @param serviceType
	 *           the currently selected interface
	 * @param methodName
	 *            String of the currently selected method name
	 * @return Context with the results of the exertion
	 * @throws RemoteException
	 */
	public Context exertService(String providerName, Class serviceType,
			String methodName, Context theContext) throws RemoteException;

	/**
	 * Returns service information about this provider.
	 * 
	 * @return Cataloger service information
	 * @throws RemoteException
	 */
	public String getServiceInfo() throws RemoteException;
}
