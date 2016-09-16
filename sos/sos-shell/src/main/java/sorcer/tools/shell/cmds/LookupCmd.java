/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
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

package sorcer.tools.shell.cmds;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

import net.jini.admin.Administrable;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.entry.Name;
import sorcer.core.provider.Provider;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

import com.sun.jini.admin.DestroyAdmin;
import sorcer.tools.shell.WhitespaceTokenizer;

import static org.fusesource.jansi.Ansi.ansi;

public class LookupCmd extends ShellCmd {

	{
		COMMAND_NAME = "lup";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "lup  [-s | -p | <service index> | -v | -x] | --d <service index>" 
				+ "\n\t\t\t  | ( -p | -s ) [-n <name attribute eval>] [-i <service type name>] ";

		COMMAND_HELP = "Performs lookup on a default lookup service (disco <registrar index>);"
			+ "\n  -s   show all services registered with the default lookup service" 
			+ "\n  -p   show services based on the " + Provider.class.getName() + " interface" 
			+ "\n  <service index>   show and select the fetched <service index> provider"
			+ "\n  -v   show the selected provider"
			+ "\n  -x   clear the selected provider"
			+ "\n  -n   show service based on the name attribute eval"
			+ "\n  -t   show services based on the type name"
			+ "\n  --d <service index>	  destroy the <provider index> provider";

	}

	static private ArrayList<ServiceItem> serviceItems = new ArrayList<ServiceItem>();

	static private PrintStream out;

	private boolean isProvider = false;

	static private int selectedServiceItem = -1;

	public LookupCmd() {
	}

	public void execute(String... args) throws IOException, ClassNotFoundException {
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		int index = DiscoCmd.selectedRegistrar;
		String next = null;
		String attribValue = null;
		String serviceType = null;
		String option = null;

		if (numTokens == 0) {
			printServices();
		} else if (numTokens == 1) {
			// assume an attribute is given only
			attribValue = myTk.nextToken();
			if (attribValue.charAt(0) != '-') {
				try {
					selectedServiceItem = Integer.parseInt(attribValue);
				} catch (Exception e) {
					out.println(COMMAND_USAGE);
					return;
				}
				if (selectedServiceItem >= 0
						&& selectedServiceItem < serviceItems.size()) {
					describeService(selectedServiceItem, "");
				}
			} else if (attribValue.equals("-p")) {
				option = attribValue;
				attribValue = null;
				isProvider = true;
				lookup(index, option, attribValue);
			} else if (attribValue.equals("-s")) {
				option = attribValue;
				attribValue = null;
				isProvider = false;
				lookup(index, option, attribValue);
			} else if (attribValue.equals("-v")) {
				if (selectedServiceItem >= 0
						&& selectedServiceItem < serviceItems.size()) {
					describeService(selectedServiceItem, "");
				}
				else {
					out.println("No selected service provider");
				}
			} else if (attribValue.equals("-x")) {
				selectedServiceItem = -1;
				out.println("Deselected the existing service provider");
			} else if (attribValue.equals("--d")) {
				// destroyProvider(selectedServiceItem);
				destroyAllProviders();
			} else {
				out.println(COMMAND_USAGE);
			}
		} else if (numTokens == 2) {
			next = myTk.nextToken();
			if (next.indexOf("-") == 0) {
				option = next;
				if (option.equals("--d")) {
					try {
						next = myTk.nextToken();
						if (next != null) {
							index = Integer.parseInt(next);
							destroyProvider(index);
						} else {
							out.println("Wrong argument for service index!");
						}
					} catch (Exception e) {
						// investigate a thrown exporter exception
						//e.printStackTrace();
					}
				} else if (next.equals("-pn") ||  next.equals("-np")) {
					attribValue = myTk.nextToken();
					isProvider = true;
					lookup(index, option, attribValue);
				} else if (next.equals("-sn") ||  next.equals("-ns")) {
					attribValue = myTk.nextToken();
					isProvider = false;
					lookup(index, option, attribValue);
				} else if (next.equals("-pi") || next.equals("-ip")) {
					attribValue = myTk.nextToken();
					isProvider = true;
					lookup(index, option, attribValue, serviceType);
				} else if (next.equals("-si") || next.equals("-is")) {
					serviceType = myTk.nextToken();
					isProvider = false;
					lookup(index, option, attribValue, serviceType);
				} else {
					out.println(COMMAND_USAGE);
				}
			}
		} else if (numTokens == 3) {
			next = myTk.nextToken();
			if (next.equals("-p")) {
				option = myTk.nextToken();
				if (option.equals("-n")) {
					attribValue = myTk.nextToken();
				}
				isProvider = true;
				lookup(index, option, attribValue);
			} else {
				out.println(COMMAND_USAGE);
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}

	private void lookup(int index, String option, String attribValue) {
		lookup(index, option, attribValue, null);
	}
	
	@SuppressWarnings("unchecked")
	private void lookup(int index, String option, String attributeValue, String serviceType) {
		// pass in a clone of list - command may modify it
		ArrayList<ServiceRegistrar> registrars = new ArrayList<ServiceRegistrar>(NetworkShell.getRegistrars());
		if (index < registrars.size()) {
			ServiceRegistrar myReg = (ServiceRegistrar) registrars.get(index);
			if (myReg != null) {
				lookup(myReg, index, attributeValue, serviceType);
			}
		}
	}
	
	private void lookup(ServiceRegistrar registrar, int idx, String attr) {
		lookup(registrar, idx, attr, null);
	}
	
	private void lookup(ServiceRegistrar registrar, int idx, String attr, String serviceType) {
		ServiceMatches matches = null;
		Entry myAttrib[] = null;
		if (attr != null) {
			myAttrib = new Entry[1];
			myAttrib[0] = new Name(attr);
		}
		if (idx >= 0)
			out.println("Performing lookup on lookup service # " + idx + " Name=\""
				+ attr + "\":");
		try {
			ServiceTemplate myTmpl = null;
			Class[] serviceTypes = null;
			if (isProvider) {
				if (serviceType != null) {
					serviceTypes = new Class[2];
					serviceTypes[0] = Provider.class;
					serviceTypes[1] = Class.forName(serviceType);
				}
				else {
					serviceTypes = new Class[1];
					serviceTypes[0] = Provider.class;
				}
				myTmpl = new ServiceTemplate(null, serviceTypes, myAttrib);
			} else {
				if (serviceType != null) {
					serviceTypes = new Class[1];
					serviceTypes[0] = Class.forName(serviceType);
				}
				myTmpl = new ServiceTemplate(null, serviceTypes, myAttrib);
			}

			matches = registrar.lookup(myTmpl, MAX_MATCHES);
			out.println("\t.... found " + matches.totalMatches + " services...");
			// for (int j=0; j < matches.totalMatches; j++) {
			serviceItems.clear();
			for (int j = 0; j < Math.min(MAX_MATCHES, matches.totalMatches); j++) {
				serviceItems.add(matches.items[j]);
			}
			printServices();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void printServices() {
		if (serviceItems != null && serviceItems.size() > 0) {
			for (int j = 0; j < serviceItems.size(); j++) {
				if (isProvider) {
					printProvider(j, "");
				} else {
					printService(j, "");
				}
			}
		} else {
			out.println("No fetched services yet!");
		}
	}

	static private void printServiceName(ServiceItem item) {
		Entry[] attributeSets = item.attributeSets;
		for (int i = 0; i < attributeSets.length; i++) {
			if (attributeSets[i] instanceof Name) {
				out.println(ansi().render("Name: @|green " +((Name) attributeSets[i]).name + "|@"));
				return;
			}
		}
	}
	
	private boolean isProvider(ServiceItem serviceItem) {
		Class[] allInt = serviceItem.service.getClass().getInterfaces();
		for (int k = 0; k < allInt.length; k++) {
			if (allInt[k] == Provider.class)
				return true;
		}
		return false;
	}

	static private void printService(int index, String msg) {
		if (serviceItems.get(index).service instanceof Provider) {
			printProvider(index, msg);
			return;
		}
		out.println(ansi().render("@|blue ---------" + (msg != null ? " " + msg : "")
				+ "|@ @|bold,blue SERVICE # " +index + "|@ @|blue ---------|@"));
		out.println(ansi().render("ID: @|bold " + serviceItems.get(index).serviceID + "|@"));
        printServiceName(serviceItems.get(index));
        out.println(ansi().render("Proxy class: @|bold "
                + serviceItems.get(index).service.getClass().getName() + "|@"));
	}           

	static private void describeService(int index, String msg) throws IOException,
			ClassNotFoundException {
        out.println(ansi().render("@|blue ---------" + (msg != null ? " " + msg : "")
                + "|@ @|bold,blue SERVICE # " +index + "|@ @|blue ---------|@"));
        out.println(ansi().render("ID: @|white " + serviceItems.get(index).serviceID + "|@"));
        printServiceName(serviceItems.get(index));
		Class myCls = serviceItems.get(index).service.getClass();
		out.println("Proxy class: " + myCls);
		Class[] allIntf = myCls.getInterfaces();
		out.println("Interfaces: ");
		//Class[] allIntf = myCls.printAllInterfaces();
		for (int k = 0; k < allIntf.length; k++)
			out.println("  " + allIntf[k].getName());
		printCodebaseURL(serviceItems.get(index));
		Entry[] attributeSets = serviceItems.get(index).attributeSets;
		if (attributeSets != null && attributeSets.length > 0) {
				NetworkShell.printLookupAttributes(attributeSets);
				// for (int i = 0; i < attributeSets.length - 1; i++) {
				// out.println("  - " + attributeSets[i]);
				// }
		}
	}

	static private void printProvider(int index, String msg) {
        out.println(ansi().render("@|blue ---------" + (msg != null ? " " + msg : "")
                + "|@ @|bold,blue SERVICE PROVIDER # " +index + "|@ @|blue ---------|@"));
        out.println(ansi().render("ID: " +
                "@|bold " + serviceItems.get(index).serviceID + "|@"
                + " at: "
                + "@|bold,green " + AttributesUtil.getHostName(serviceItems.get(index).attributeSets) + "|@"
        ));
		out.println(ansi().render("Home: @|bold "
				+ AttributesUtil.getUserDir(serviceItems.get(index).attributeSets) + "|@"));
		String groups = AttributesUtil
				.getGroups(serviceItems.get(index).attributeSets);
		out.println(ansi().render("Provider name: "
                + "@|bold,green " + AttributesUtil.getProviderName(serviceItems.get(index).attributeSets) + "|@"));
		out.println(ansi().render("Proxy class: @|bold "
				+ serviceItems.get(index).service.getClass().getName() + "|@"));
		out.println(ansi().render("Groups supported: @|bold " + groups + "|@"));
		out.println(ansi().render("Published services: @|bold "
				+ Arrays.toString(AttributesUtil
						.getPublishedServices(serviceItems.get(index).attributeSets))+"|@"));
	}

	public static void printCurrentService() throws IOException,
			ClassNotFoundException {
		if (selectedServiceItem >= 0) {
			NetworkShell.shellOutput.println("Selected service provider: ");
			printService(selectedServiceItem, "");
		} else {
			NetworkShell.shellOutput.println("No selected service provider, use 'lup' command");
		}
	}
	
	private void destroyAllProviders() throws RemoteException {
		for (int i = 0; i < serviceItems.size(); i++) {
			destroyProvider(i);
		}
	}

	private void destroyProvider(int index) throws RemoteException {
		ServiceItem item = serviceItems.get(index);
		Administrable admin = (Administrable) item.service;
		if (admin instanceof DestroyAdmin) {
			DestroyAdmin discoveryAdmin = (DestroyAdmin) admin.getAdmin();
			discoveryAdmin.destroy();
			out.println("Destroyed the provider # " + index + " now!");
		}
	}

	static private void printCodebaseURL(ServiceItem item) {
		ClassLoader cl = item.service.getClass().getClassLoader();
		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			if (urls.length > 0) {
				out.println("Codebase URLs:");
				for (int l = 1; l < urls.length; l++) {
					out.println("  " + urls[l]);
				}
			}
		}
	}
	
	static private void printAllInterfaces(Class cls) {
		Class[] interfaces = cls.getInterfaces();
		if (interfaces.length != 0) {
			out.println("\t" + cls);
			for (int i = 0; i < interfaces.length; i++) {
				out.println("\t\t" + interfaces[i].getName());
				printAllInterfaces(interfaces[i]);
			}
		}
	}
	
	public static ArrayList<ServiceItem> getServiceItems() {
		return serviceItems;
	}

}
