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

//Main Browser Domain
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.ui.exertlet.EditorViewSignature;

/**
 * The BrowserModel, in standard model-view-controller, is the model for the
 * Cataloger User Interface service. This model is also utilized by the task
 * manager ui.
 */
public class BrowserModel extends Observable implements Serializable, EditorViewSignature {
	private static final long serialVersionUID = 5311936826023311727L;
	String selectedProvider;
	String selectedInterfaceName;
	String selectedMethod;
	Class selectedInterface;
	String providerList[], interfaceList[], methodList[], bkproviderList[],
			bkinterfaceList[], bkmethodList[];
	Map<String, URL[]> codebaseURLs = new HashMap<String, URL[]>();
	Context context;
	String contextScript;
	public static final String PROVIDER_UPDATED = "ProvUp";
	public static final String PROVIDER_ADDED = "ProvAdded";
	public static final String INTERFACE_UPDATED = "InterUp";
	public static final String METHOD_UPDATED = "MethUp";
	public static final String CONTEXT_UPDATED = "ContUp";

	/**
	 * An initialization function for setting up necessary values which will be
	 * used in later function executions.<br />
	 * <br />
	 * <b>mArray</b> - An array of methods<br />
	 * <b>selectedProvider</b> - The provider selected in the UI<br />
	 * <b>selectedInterface</b> - The interface selected in the UI<br />
	 * <b>selectedMethod</b> - The method selected in the UI<br />
	 * <b>providerList</b> - An array of all providers<br />
	 * <b>interfaceList</b> - An array of all interfaces for a selected provider<br />
	 * <b>methodList</b> - An array of all methods for a selected interface<br />
	 * <b>contextList</b> - An array of all contexts for a selected method
	 */
	void initialize() {
		// mArray = new ArrayList();
		selectedProvider = null;
		selectedInterface = null;
		selectedMethod = null;
		providerList = new String[0];
		interfaceList = new String[0];
		methodList = new String[0];
		bkproviderList = new String[0];
		bkinterfaceList = new String[0];
		bkmethodList = new String[0];
		context = new ServiceContext("blank");
	}

	/**
	 * Stores the provider list into the model.<br />
	 * Updates the Observer that a change has taken place.
	 * 
	 * @param providers
	 *            - A String array of provider names
	 * @see java.util.Observable#setChanged()
	 */
	public void setProviders(String[] providers) {
		providerList = providers;
		bkproviderList = providers;
		setChanged();
		notifyObservers(PROVIDER_UPDATED);
	}

	public void setCodebaseURLs(Map<String, URL[]> codebaseURLs) {
		this.codebaseURLs = codebaseURLs;
	}

	/**
	 * Stores the provider list into the model.<br />
	 * Updates the Observer that a change has taken place. <br/>
	 * This calls observers with PROVIDER_ADDED and thus does not force the
	 * lists to be refreshed.
	 * 
	 * @param providers
	 *            - A String array of provider names
	 * @see java.util.Observable#setChanged()
	 */
	public void setProvidersAdded(String[] providers) {
		providerList = providers;
		bkproviderList = providers;
		setChanged();
		notifyObservers(PROVIDER_ADDED);
	}

	/**
	 * Stores the provider list into the model.<br />
	 * Updates the Observer that a change has taken place. <br/>
	 * If searched is true the bkproviderList is not setValue, this allows for
	 * caching providerList during searching.
	 * 
	 * @param providers
	 *            - A String array of provider names
	 * @param searched
	 *            - Boolean indicating if a search took place
	 * @see java.util.Observable#setChanged()
	 */
	public void setProviders(String[] providers, boolean searched) {
		providerList = providers;
		if (!searched)
			bkproviderList = providers;
		setChanged();
		notifyObservers(PROVIDER_UPDATED);
	}

	/**
	 * Stores the selected Provider and the list of interfaces into the Domain.<br />
	 * Erases the old methodList and contextList.<br />
	 * Erases the old selectedInterface and selectedMethod.<br />
	 * Updates the Observer that a change has taken place.
	 * 
	 * @param selProv
	 *            - The currently selected Provider in the UI
	 * @param interfaces
	 *            - A string array of Interface names for the selected Provider
	 */
	public void setInterfaces(String selProv, String[] interfaces) {
		selectedProvider = selProv;
		interfaceList = interfaces;
		bkinterfaceList = interfaces;

		methodList = new String[0];
		context = new ServiceContext("blank");
		selectedInterface = null;
		selectedMethod = null;

		setChanged();
		notifyObservers(METHOD_UPDATED);
		setChanged();
		notifyObservers(INTERFACE_UPDATED);
	}

	/**
	 * Stores the selected Provider and the list of interfaces into the Domain.<br />
	 * Erases the old methodList and contextList.<br />
	 * Erases the old selectedInterface and selectedMethod.<br />
	 * Updates the Observer that a change has taken place. If searched is true,
	 * backup list is not changed.
	 * 
	 * @param interfaces
	 *            - A string array of Interface names for the selected Provider
	 * @param searched
	 *            - boolean indicating a search was performed.
	 */
	public void setInterfaces(String[] interfaces, boolean searched) {
		interfaceList = interfaces;

		if (!searched)
			bkinterfaceList = interfaces;

		methodList = new String[0];
		context = new ServiceContext("blank");
		selectedInterface = null;
		selectedMethod = null;

		setChanged();
		notifyObservers(METHOD_UPDATED);
		setChanged();
		notifyObservers(INTERFACE_UPDATED);
	}

	/**
	 * Stores the selected Interface and the list of methods to the Domain.<br />
	 * Erases the old contextList and old selectedMethod.<br />
	 * Updates the Observer that a change has taken place.
	 * 
	 * @param selInter
	 *            - The currently selected Interface in the UI
	 * @param methods
	 *            - A string array of Method names for the selected Interface
	 */
	public void setMethods(String selInter, String[] methods) {
		selectedInterfaceName = selInter;
		methodList = methods;
		bkmethodList = methods;
		context = new ServiceContext("blank");
		selectedMethod = null;

		setChanged();
		notifyObservers(METHOD_UPDATED);
	}

	/**
	 * Stores the selected Interface and the list of methods to the Domain.<br />
	 * Erases the old contextList and old selectedMethod.<br />
	 * Updates the Observer that a change has taken place. <br />
	 * searched indicates if backup list should be changed.
	 * 
	 * @param methods
	 *            - A string array of Method names for the selected Interface
	 * @param searched
	 *            - boolean indicating a search was performed.
	 */
	public void setMethods(String[] methods, boolean searched) {

		methodList = methods;

		if (!searched)
			bkmethodList = methods;

		context = new ServiceContext("blank");
		selectedMethod = null;

		setChanged();
		notifyObservers(METHOD_UPDATED);
	}

	/**
	 * Stores the selected Method and the list of contexts to the Domain.<br />
	 * Updates the Observer that a change has taken place.
	 * 
	 * @param selMeth
	 *            - The currently selected Method in the UI
	 * @param context
	 *            - A string array of the Contexts for the seleted Method
	 */
	public void setContext(String selMeth, Context context) {
		selectedMethod = selMeth;
		this.context = context;
		setChanged();
		notifyObservers(CONTEXT_UPDATED);
	}

	/**
	 * Used to get the list of providers in the model
	 * 
	 * @return - A string array of all providers available
	 */
	public String[] getProviders() {
		return providerList;
	}

	/**
	 * Used to get the backup list of providers from the model
	 * 
	 * @return - A string array with the backup list of providers, can be used
	 *         to reload view after clearing search
	 */
	public String[] getBackupProviders() {
		return bkproviderList;
	}

	/**
	 * Used to get the selected provider from the model
	 * 
	 * @return - a string with the selected providers name
	 */
	public String getSelectedProvider() {
		return selectedProvider;
	}

	/**
	 * Used to get the list of interfaces from the model
	 * 
	 * @return - A string array of all the interfaces available for the selected
	 *         provider
	 */
	public String[] getInterfaces() {
		return interfaceList;
	}

	/**
	 * Used to get the backup list of interfaces from the model.
	 * 
	 * @return - A string array of the backup interface listed, used to revert
	 *         search back to default
	 */
	public String[] getBackupInterfaces() {
		return bkinterfaceList;
	}

	/**
	 * Used to get the selected interface name from the model
	 * 
	 * @return - A string with the selected interface name
	 */
	public String getSelectedInterfaceName() {
		return selectedInterfaceName;
	}

	public Class getSelectedInterface() {
		return selectedInterface;
	}
	
	/**
	 * Used to get the list of methods from the model
	 * 
	 * @return - A string array of all the methods for the selected interface
	 */
	public String[] getMethods() {
		return methodList;
	}

	/**
	 * Used to get the backup list of methods from the model
	 * 
	 * @return - A string array of the backup list of methods, used for
	 *         searching
	 */
	public String[] getBackupMethods() {
		return bkmethodList;
	}

	/**
	 * Used to get the selected method from the model
	 * 
	 * @return - A string with the name of the currently selected provider
	 */
	public String getSelectedMethod() {
		return selectedMethod;
	}

	/**
	 * Used to get the current @link Context from the model.
	 * 
	 * @return - A @link Context that is currently selected.
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * <p>
	 *  Returns a declarative context definition.
	 * </p>
	 * 
	 * @return the contextScript
	 */
	public String getContextScript() {
		return contextScript;
	}

	/**
	 * <p>
	 * Assigns a declarative context definition.
	 * </p>
	 * 
	 * @param contextScript
	 *            the contextScript to setValue
	 */
	public void setContextScript(String contextScript) {
		this.contextScript = contextScript;
	}

	@Override
	public String getServiceType() {
		return getSelectedInterfaceName();
	}

	public void setSelectedMethod(String selectedMethod) {
		this.selectedMethod = selectedMethod;
	}

	@Override
	public String getSelector() {
		return getSelectedMethod();
	}

	@Override
	public URL[] getCodebaseURLs() {
		return codebaseURLs.get(selectedProvider);
	}

}
