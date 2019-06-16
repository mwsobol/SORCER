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

package sorcer.core.provider.cataloger.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionListener;

import sorcer.service.Provider;
import sorcer.service.Context;

/**
 * The Signature Dispatcher Interface allows us to abstract the interactions
 * with cataloger or the provider. The two implementations of this interface are @link
 * SignatureDispatcherForCataloger and @link SignatureDispatcherForCataloger
 * each implementation handles obtaining the information requested by the model.
 * 
 * @author Greg McChensey
 * 
 */
public interface SignatureDispatchment extends ActionListener {

	/**
	 * Provides a ListSelectionListener for the provider list.
	 * 
	 * @return ListSelectionListener for the provider list
	 */
	ListSelectionListener getProviderListener();

	/**
	 * Provides a ListSelectionListener for the interface list.
	 * 
	 * @return ListSelectionListener for the interface list
	 */
	ListSelectionListener getInterfaceListener();

	/**
	 * Provides a ListSelectionListener for the method list.
	 * 
	 * @return ListSelectionListener for the method list
	 */
	ListSelectionListener getMethodListener();

	/**
	 * This method is called when the user utilizes the search functionality on
	 * the bottom of each list.
	 */
	void actionPerformed(ActionEvent e);

	/**
	 * This method fills the model with the appropriate data.
	 */
	void fillModel();

	/**
	 * Gets the list of providers
	 * 
	 * @return String array of the provider names
	 */
	String[] getProviders();

	/**
	 * Gets the list of interfaces for the given provider
	 * 
	 * @param providerName
	 *            String representing the provider to getValue the interface list for
	 * @return String array of the interface names
	 */
	String[] getInterfaces(String providerName);

	/**
	 * Gets the list of methods for the given interface and the currently
	 * selected provider
	 * 
	 * @param interfaceName
	 *            String representing the currently selected interface
	 * @return String array of the method names
	 */
	String[] getMethods(String interfaceName);

	/**
	 * Gets the list of contexts currently stored on the provider.
	 * 
	 * @return String array of the currently stored context names
	 */
	String[] getSavedContextList();

	/**
	 * Obtains the context for the specified method key from the network.
	 * 
	 * @param methodName
	 *            String representing the method to obtain the context from
	 * @return the service context for the method
	 */
	Context getContext(String methodName);

	/**
	 * Save a context back to the network, saves the context as the currently
	 * selected method key.
	 * 
	 * @param theContext
	 *            Context to be saved.
	 * @return Boolean indicating if the operation was successful.
	 */
	Boolean saveContext(Context theContext);

	/**
	 * Save the context to the network, this stores the context under the key
	 * provided in newName.
	 * 
	 * @param newName
	 *            String representing the key the context should be saved as
	 * @param theContext
	 *            Context to be saved.
	 * @return Boolean indicating if the operation was successful.
	 */
	Boolean saveContext(String newName, Context theContext);

	/**
	 * Delete a context from the network, the context to be deleted is defined
	 * by the String methodName.
	 * 
	 * @param methodName
	 *            String with the key of the context to delete.
	 * @return Boolean indicating if the operation was successful.
	 */
	Boolean deleteContext(String methodName);

	/**
	 * This method creates an exertion using the context provided by the user.
	 * The results of the exertion are returned to the user.
	 * 
	 * @param theContext
	 *            Context to be sent with the exertion
	 * @return Context returned from the exertion.
	 */
	Context exertService(Context theContext);

	
	Provider getProvider();
	
}
