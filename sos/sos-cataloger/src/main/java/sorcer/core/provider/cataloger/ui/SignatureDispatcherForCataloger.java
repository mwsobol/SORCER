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

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sorcer.core.context.ServiceContext;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextManagement;

/**
 * Implementation of the SignatureDispatcherInterface to provide support for
 * using Cataloger as a data source. This implementation calls Cataloger to
 * obtain most of the information that it needs. This version is utilized for
 * the Cataloger UI.
 * 
 * @author Greg McChesney
 * 
 */
public class SignatureDispatcherForCataloger implements SignatureDispatchment {

	protected static final Logger logger = Logger
			.getLogger(SignatureDispatcherForCataloger.class.getName());
	/**
	 * The Cataloger service object. Passed in by constructor from CatalogerUI
	 * class
	 */
	Cataloger catalog;
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
	 * Constructor for the Dispatcher
	 * 
	 * @param m
	 *            BrowserModel that holds the current data
	 * @param c
	 *            Cataloger Service where remote queries can be made
	 */
	public SignatureDispatcherForCataloger(BrowserModel m, Cataloger c) {
		model = m;
		catalog = c;
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
								SignatureDispatcherForCataloger.this
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
								SignatureDispatcherForCataloger.this
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
						String selProv = (String) list.getSelectedValue();
						model.setContext(selProv,
								SignatureDispatcherForCataloger.this
										.getContext(selProv));
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
	 * Gets the list of providers
	 * 
	 * @return String array of the provider names
	 */
	public String[] getProviders() {
		int i = 0;
		String[] provs;
		try {

			provs = catalog.getProviderList();
			java.util.Arrays.sort(provs);
			if (provs.length == 0) {
				provs = new String[1];
				provs[0] = "No Providers Found";
				providerError = true;
			} else
				providerError = false;

		} catch (RemoteException e) {

			e.printStackTrace();
			provs = new String[1];
			provs[0] = "ERROR HAS OCCURRED";
			providerError = true;
		}

		return provs;
		/* turn back on for offline mode */
		/*
		 * String provList[]=new String[6];
		 * 
		 * 
		 * provList[0]="provider 1 "; provList[1]="cataloger 1 ";
		 * provList[2]="Cataloger 1 "; provList[3]="provider 2 ";
		 * provList[4]="Gregs Service 2 1 "; provList[5]="gregs service 1 ";
		 * 
		 * return provList;
		 */
	}

	/**
	 * Gets the list of interfaces for the given provider
	 * 
	 * @param providerName
	 *            String representing the provider to get the interface list for
	 * @return String array of the interface names
	 */
	public String[] getInterfaces(String providerName) {

		if (providerError) {
			return new String[1];

		}
		String[] interfaces;
		try {
			interfaces = catalog.getInterfaceList(providerName);
			java.util.Arrays.sort(interfaces);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			interfaces = new String[0];
		}

		return interfaces;

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
		if (model.getSelectedProvider() == null)
			return new String[0];

		try {

			// System.out.println("before get methods");
			String[] methods = catalog.getMethodsList(model
					.getSelectedProvider(), "interface " + interfaceName); // ((Provider)
			// catalog).getProxy();
			// System.out.println("after begin methods");

			try {
				// catalog.getProxy(selectedProvider,"interface "+interfaceName);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			// System.out.println("after proxy");
			java.util.Arrays.sort(methods);
			return methods;

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

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
		Context cxt = new ServiceContext(model.getSelectedProvider());
		// if(theproxy!=null)
		{
			try {
				cxt = ((ContextManagement)catalog).getContext(model.getSelectedProvider(), model
						.getSelectedInterfaceName(), methodName);

			} catch (RemoteException e) {

				e.printStackTrace();
			}
		}
		return cxt;
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
			return ((ContextManagement)catalog).saveContext(model.getSelectedProvider(), model
					.getSelectedInterfaceName(), model.getSelectedMethod(),
					theContext);

		} catch (RemoteException e) {

			e.printStackTrace();
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

		// Context cxt=new ServiceContext(model.getSelectedProvider());
		// if(theproxy!=null)
		// {
		try {
			return ((ContextManagement)catalog).saveContext(model.getSelectedProvider(), model
					.getSelectedInterfaceName(), newName, theContext);

		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Gets the list of contexts currently stored on the provider.
	 * 
	 * @return String array of the currently stored context names
	 */
	public String[] getSavedContextList() {

		try {
			return ((ContextManagement)catalog).getSavedContextList(model.getSelectedProvider(),
					model.getSelectedInterfaceName());
			// sorcer.core.Provider temp=(sorcer.core.Provider)item;
			// return temp.currentContextList(model.getSelectedInterface());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String[0];
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

		// Context cxt=new ServiceContext(model.getSelectedProvider());
		// if(theproxy!=null)
		// {
		try {
			return catalog.exertService(model.getSelectedProvider(), model
					.getSelectedInterface(), model.getSelectedMethod(),
					theContext);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return new ServiceContext();
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
			return ((ContextManagement)catalog).deleteContext(model.getSelectedProvider(), model
					.getSelectedInterfaceName(), methodName);
			// sorcer.core.Provider temp=(sorcer.core.Provider)item;
			// return temp.currentContextList(model.getSelectedInterface());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.cataloger.ui.SignatureDispatchment#getProvider()
	 */
	@Override
	public Provider getProvider() {
		return (Provider)catalog;
	}

}