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

//Main Dispatcher/Listener for all UI components

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.Provider;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.ContextManagement;
import sorcer.service.Servicer;
import sorcer.service.Task;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the SignatureDispatcherInterface to provide support for
 * using provider as a data source. This implementation calls provider directly
 * to obtain most of the information that it needs. This version is utilized for
 * in the built in editors.
 * 
 * @author Greg McChesney
 * 
 */
public class SignatureDispatcherForProvider implements SignatureDispatchment {

	protected static final Logger logger = LoggerFactory.getLogger(SignatureDispatcherForProvider.class.getName());
	/**
	 * The Cataloger service object. Passed in by constructor from CatalogerUI
	 * class
	 */
	
	Provider provider;
	/**
	 * BrowserModel to update
	 */
	BrowserModel model;
	/**
	 * Listeners for provider/interface/method lists, respectively. They are
	 * Singleton-type listeners.
	 */
	ListSelectionListener providerListener, interfaceListener, methodListener;

	/**
	 * Provider error specifies list is empty;
	 */
	Boolean providerError;

	/**
	 * Used to prevent certain interfaces from appearing on the data list.
	 */
	private String[] interfaceIgnoreList;

	/**
	 * Constructor for the Dispatcher
	 * 
	 * @param model
	 *            BrowserModel that holds the current data
	 * @param provider
	 *            Object representing the current provider
	 */
	public SignatureDispatcherForProvider(BrowserModel model, Object provider) {
		this.model = model;
		this.provider = (Provider)provider;

		interfaceIgnoreList = new String[6];
		interfaceIgnoreList[0] = "sorcer.core.Provider";
		interfaceIgnoreList[1] = "sorcer.core.AdministratableProvider";
		interfaceIgnoreList[2] = "java.rmi.Remote";
		interfaceIgnoreList[3] = "net.jini.core.constraint.RemoteMethodControl";
		interfaceIgnoreList[4] = "net.jini.security.proxytrust.TrustEquivalence";
		interfaceIgnoreList[5] = "sorcer.service.RemoteTasker";

	}

	/**
	 * Creates a ListSelectionListener to update based on provider list changes
	 * Singleton-type creation and return
	 * 
	 * @return The Listener for providerList
	 */
	public ListSelectionListener getProviderListener() {
		if (providerListener == null)
			providerListener = new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					JList list = (JList) e.getSource();
					if (!e.getValueIsAdjusting()) // make it only run on the
					// final event
					{

						String selProv = (String) list.getSelectedValue();
						model.setInterfaces(selProv,
								SignatureDispatcherForProvider.this
										.getInterfaces(selProv));
					}
				}
			};
		return providerListener;
	}

	/**
	 * Creates a ListSelectionListener to update based on interface list changes
	 * Singleton-type creation and return
	 * 
	 * @return The Listener for interfaceList
	 */
	public ListSelectionListener getInterfaceListener() {
		if (interfaceListener == null)
			interfaceListener = new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					JList list = (JList) e.getSource();
					if (!e.getValueIsAdjusting()) // make it only run on the
					// final event
					{
						String selProv = (String) list.getSelectedValue();
						model.setMethods(selProv,
								SignatureDispatcherForProvider.this
										.getMethods(selProv));
					}

				}
			};
		return interfaceListener;
	}

	/**
	 * Creates a ListSelectionListener to update based on method list changes
	 * Singleton-type creation and return
	 * 
	 * @return The Listener for methodList
	 */
	public ListSelectionListener getMethodListener() {
		if (methodListener == null)
			methodListener = new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					JList list = (JList) e.getSource();
					if (!e.getValueIsAdjusting()) // make it only run on the
					// final event
					{
						String selMethod = (String) list.getSelectedValue();
						model.setContext(selMethod,
								SignatureDispatcherForProvider.this
										.getContext(selMethod));
						model.setSelectedMethod(selMethod);
					}
				}
			};
		return methodListener;
	}

	/**
	 * This method handles search requests made by the users
	 * 
	 * @param searchString
	 *            String of the data to find
	 * @param listToSearch
	 *            String array of the items to search
	 * @return String array of the items matching the search query
	 */
	private String[] processSearch(String searchString, String[] listToSearch) {
		searchString = searchString.toLowerCase();
		if (searchString.length() != 0) // handle a real search
		{
			int count = 0;
			for (int i = 0; i < listToSearch.length; i++)
				if (listToSearch[i].toLowerCase().indexOf(searchString) != -1)
					count++;
			String[] shortlist = new String[count];
			count = 0;
			for (int i = 0; i < listToSearch.length; i++)
				if (listToSearch[i].toLowerCase().indexOf(searchString) != -1) {
					shortlist[count] = listToSearch[i];
					count++;
				}
			return shortlist;
		}
		return listToSearch;// reset the list
	}

	/**
	 * Over-ridden actionPerformed function to deal with searching, view changes
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		String comm = e.getActionCommand();

		if (comm == SignatureView.PROVIDER_SEARCH) {
			String searchedProvider = ((JTextField) (e.getSource())).getText();
			model.setProviders(processSearch(searchedProvider, model
					.getBackupProviders()), true);
		}
		if (comm == SignatureView.INTERFACE_SEARCH) {
			String searchedInterface = ((JTextField) (e.getSource())).getText();
			model.setInterfaces(processSearch(searchedInterface, model
					.getBackupInterfaces()), true);
		}
		if (comm == SignatureView.METHOD_SEARCH) {
			String searchedMethod = ((JTextField) (e.getSource())).getText();
			model.setMethods(processSearch(searchedMethod, model
					.getBackupMethods()), true);
		}
	}

	/**
	 * Used to start the MVC cycle. Called by CatalogerUI once at beginning.
	 * Fills model with initial list of Provider names.
	 * 
	 */
	public void fillModel() {
		model.setProviders(getProviders());
	}

	/**
	 * Gets the list of providers, since we are on a provider this returns one
	 * item, and is ignored in the view.
	 * 
	 * @return String array of the provider names
	 */
	public String[] getProviders() {
		String provList[] = new String[1];
		provList[0] = "provider 1 ";

		return provList;
	}

	/**
	 * Gets the list of interfaces for the given provider
	 * 
	 * @param providerName
	 *            String representing the provider to get the interface list for
	 * @return String array of the interface names
	 */
	public String[] getInterfaces(String providerName) {

		// offline mode
		/*
		 * Random randomGenerator = new Random();
		 * 
		 * 
		 * int num=randomGenerator.nextInt(10)+1;
		 * 
		 * String inters[]=new String[num]; for(int i=0;i<num;i++){
		 * inters[i]="Interface " + i; }
		 * 
		 * return inters;
		 */

		Class[] interfaceList = provider.getClass().getInterfaces();

		int count = 0;
		for (int i = 0; i < interfaceList.length; i++) {
			// logger.info("interface "+interfaceList[i].toString());

			String currentInterface = interfaceList[i].toString().substring(10); // remove
			// the
			// interface
			// part!
			boolean onList = false;
			for (int j = 0; j < interfaceIgnoreList.length; j++) {
				if (currentInterface.equals(interfaceIgnoreList[j])) {
					onList = true;
					break;
				}
			}
			if (!onList)
				count++;

		}
		String[] toReturn = new String[count];
		count = 0;
		for (int i = 0; i < interfaceList.length; i++) {
			// logger.info("interface "+interfaceList[i].toString());

			String currentInterface = interfaceList[i].toString().substring(10); // remove
			// the
			// interface
			// part!
			boolean onList = false;
			for (int j = 0; j < interfaceIgnoreList.length; j++) {
				if (currentInterface.equals(interfaceIgnoreList[j])) {
					onList = true;
					break;
				}
			}
			if (!onList) {
				toReturn[count] = currentInterface;
				count++;
			}

		}
		java.util.Arrays.sort(toReturn);
		return toReturn;

	}

	/**
	 * Gets the list of methods for the given interface and the currently
	 * selected provider
	 * 
	 * @param interfaceName
	 *            String representing the currently selected interface
	 * @return String array of the method names
	 */
	public String[] getMethods(String interfaceName) {
		// offline mode
		/*
		 * Random randomGenerator = new Random();
		 * 
		 * int num=randomGenerator.nextInt(10)+1;
		 * 
		 * String meths[]=new String[num]; for(int i=0;i<num;i++){
		 * 
		 * meths[i]="Method " + i; }
		 * 
		 * return meths;
		 */
		Class[] interfaceList = provider.getClass().getInterfaces();

		interfaceName = "interface " + interfaceName;
		for (int i = 0; i < interfaceList.length; i++) {
			if (interfaceList[i].toString().equals(interfaceName)) {
				// System.out.println("found interface list!");
				// logger.info("Found interface" + interfaceName);
				Method methods[] = interfaceList[i].getMethods();
				// logger.info("Methods Found: " + methods.length);
				String meths[] = new String[methods.length];
				for (int j = 0; j < methods.length; j++) {
					meths[j] = methods[j].getName();
					// System.out.println("new method named "+meths[j]);
				}
				Set setTemp = new HashSet(Arrays.asList(meths));
				String[] array2 = (String[]) (setTemp
						.toArray(new String[setTemp.size()]));
				java.util.Arrays.sort(array2);
				return array2;
			}
		}
		return new String[0];

	}

	/**
	 * Obtains the context for the specified method name from the network.
	 * 
	 * @param methodName
	 *            String representing the method to obtain the context from
	 * @return the service context for the method
	 */
	public Context getContext(String methodName) {
		// offline mode!
		/*
		 * Context cxt=new ServiceContext(model.getSelectedProvider()); try {
		 * cxt.putValue("deposit/amount",100);
		 * cxt.putValue("deposit/amount",50);
		 * cxt.putValue("withdrawal/amount",32);
		 * cxt.putValue("balance/amount",0); cxt.putValue("test3/Slacker",new
		 * ContextNode("master3","value")); cxt.putValue("test3/HEHE",new
		 * ContextNode("master5","value")); }catch (ContextException e) {
		 * e.printStackTrace();} System.out.println("context"+cxt); return cxt;
		 */

		try {
			return ((ContextManagement)provider).getMethodContext(model.getSelectedInterfaceName(),
					methodName);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("failed to get context for: " + model.getSelectedInterfaceName() + ":" +  methodName);
		}
		return new ServiceContext("Failed");

	}

	/**
	 * Save a context back to the network, saves the context as the currently
	 * selected method name.
	 * 
	 * @param theContext
	 *            Context to be saved.
	 * @return Boolean indicating if the operation was successful.
	 */
	public Boolean saveContext(Context theContext) {

		try {
			return ((ContextManagement)provider).saveMethodContext(model.getSelectedInterfaceName(), model
					.getSelectedMethod(), theContext);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("failed to save context: " + theContext);
		}

		return false;
	}

	/**
	 * Save the context to the network, this stores the context under the name
	 * provided in newName.
	 * 
	 * @param newName
	 *            String representing the name the context should be saved as
	 * @param theContext
	 *            Context to be saved.
	 * @return Boolean indicating if the operation was successful.
	 */
	public Boolean saveContext(String newName, Context theContext) {

		try {
			return ((ContextManagement)provider).saveMethodContext(model.getSelectedInterfaceName(),
					newName, theContext);
		} catch (Exception e) {
			System.out.println("failed to save");
		}
		return false;
	}

	/**
	 * This method creates an exertion using the context provided by the user.
	 * The results of the exertion are returned to the user.
	 * 
	 * @param theContext
	 *            Context to be sent with the exertion
	 * @return Context returned from the exertion.
	 */
	public Context exertService(Context theContext) {

		try {
			NetSignature method = new NetSignature(model
					.getSelectedMethod(), model.getSelectedInterface());
			Task task = new NetTask(model.getSelectedInterfaceName()
					+ model.getSelectedMethod(), method);
			task.setContext(theContext);
			NetTask task2 = (NetTask) ((Servicer) provider).service(task,
					null);
			return task2.getContext();
		} catch (Exception e) {
			System.out.println("failed to exert!");
		}
		return new ServiceContext("Failed");
		// Context cxt=new ServiceContext(model.getSelectedProvider());
		// if(theproxy!=null)
		// {
		/*
		 * try { returncatalog.exertService(model.getSelectedProvider(),model.
		 * getSelectedInterface(),model.getSelectedMethod(),theContext);
		 * 
		 * }catch (RemoteException e) {
		 * 
		 * e.printStackTrace(); System.out.println("failed here"); }
		 */
		// }
		// return new ServiceContext();
	}

	/**
	 * Gets the list of contexts currently stored on the provider.
	 * 
	 * @return String array of the currently stored context names
	 */
	public String[] getSavedContextList() {
		try {
			return ((ContextManagement)provider).currentContextList(model.getSelectedInterfaceName());
		} catch (Exception e) {
			System.out.println("failed to exert!" + e.getMessage());
		}
		return new String[0];
	}

	/**
	 * Delete a context from the network, the context to be deleted is defined
	 * by the String methodName.
	 * 
	 * @param methodName
	 *            String with the name of the context to delete.
	 * @return Boolean indicating if the operation was successful.
	 */
	public Boolean deleteContext(String methodName) {
		try {
			return ((ContextManagement)provider).deleteContext(model.getSelectedInterfaceName(), methodName);
		} catch (Exception e) {
			System.out.println("failed to save");
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.provider.cataloger.ui.SignatureDispatchment#getProvider()
	 */
	@Override
	public Provider getProvider() {
		return provider;
	}

}
