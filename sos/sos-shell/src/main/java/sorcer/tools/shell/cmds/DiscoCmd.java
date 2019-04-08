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
import java.util.*;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.lookup.ServiceRegistrar;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

import static org.fusesource.jansi.Ansi.ansi;

public class DiscoCmd extends ShellCmd {

	{
		COMMAND_NAME = "disco";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "disco [<registrar index> | -v | -x]";

		COMMAND_HELP = "List all lookup services discovered for provided groups;"
			+ "\n\twith <registrar index> select the specified registrar as a default one"
			+ "\n\t-v   print the default registrar info"
			+ "\n\t-x   clearSessions the selected registrar and start discovery";
	}

	static private List<ServiceRegistrar> registrars;
	static int selectedRegistrar = 0;
	static private PrintStream out;

	public DiscoCmd() {
	}

	public void execute(String... args) throws IOException, ClassNotFoundException {
		// create a clone of list - command may modify it
		registrars = new ArrayList<ServiceRegistrar>(NetworkShell.getRegistrars());
		// outGovernance.println("registrars: " + registrars);
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		int index = 0;
		String next = null;
		if (numTokens == 1) {
			next = myTk.nextToken();
			if (next.equals("-x")) {
				NetworkShell.getRegistrars().clear();
				registrars.clear();
				// default index
				selectedRegistrar = 0;
				NetworkShell.getDisco().terminate();
				// start new lookup discovery
				NetworkShell.setLookupDiscovery(NetworkShell.getGroups());
			}
			else if (next.equals("-v")) {
				if (selectedRegistrar >= 0
						&& selectedRegistrar < registrars.size()) {
					describeServiceRegistrar(registrars.get(selectedRegistrar),
							true);
				}
				else {
					out.println("No selected registrar!");
				}
			} else if (next != null) {
				index = Integer.parseInt(next);
				if (index >= 0 && index < registrars.size())
					selectedRegistrar = index;
				describeServiceRegistrar(registrars.get(selectedRegistrar),
						true);
			} else {
				out.println("Wrong argument for selected registrar!");
			}
			return;
		}
		if ((registrars != null) && (registrars.size() > 0)) {
			Iterator it = registrars.iterator();
			while (it.hasNext()) {
				ServiceRegistrar myReg = (ServiceRegistrar) it.next();
				describeServiceRegistrar(myReg, false);
			}
		} else
			System.out.println("Sorry, no lookup services located");
	}

	static public void describeServiceRegistrar(ServiceRegistrar myReg,
			boolean withDetails) throws IOException, ClassNotFoundException {
		String[] groups;
		String msg = "";
		if (out == null) {
			out = NetworkShell.getShellOutputStream();
		}

        out.println(ansi().render("@|green ---------" + (msg != null ? " " + msg : "")
                + " LOOKUP SERVICE # |@ @|bold,green " + registrars.indexOf(myReg) + "|@ @|green ---------|@"));
		out.println(ansi().render("ID: @|bold " + myReg.getServiceID()+"|@"));
		groups = myReg.getGroups();
		if (groups.length > 0)
			for (int o = 0; o < groups.length; o++) {
				msg += "\'" + groups[o] + "\' ";
			}
		out.println(ansi().render("Groups supported: @|green " + msg +"|@"));
		out.println(ansi().render("Lookup locator: @|bold " + myReg.getLocator().getHost() + ":"
				+ myReg.getLocator().getPort() + "|@"));
		if (withDetails)
			printDetails(myReg);
	}

	static public void printCurrentLus() throws IOException,
			ClassNotFoundException {
		if (registrars == null)
			registrars = NetworkShell.getRegistrars();
		if (selectedRegistrar >= 0) {
			NetworkShell.shellOutput.println("Current lookup service: ");
			if (registrars.size() > 0)
				describeServiceRegistrar(registrars.get(selectedRegistrar), false);
		} else {
			NetworkShell.shellOutput
					.println("No selected LUS; use 'disco' cmd");
		}
	}
	
	static private void printDetails(ServiceRegistrar myReg) throws IOException,
			ClassNotFoundException {
		Class myCls = myReg.getClass();
		out.println("Proxy class: " + myCls);
		Class[] allIntf = myCls.getInterfaces();
		out.println("Interfaces: ");
		for (int k = 0; k < allIntf.length; k++)
			out.println("  " + allIntf[k].getName());
		
		Administrable admin = null;
		JoinAdmin jAdmin = null;
		if (myReg instanceof Administrable) {
			admin = (Administrable) myReg;
			jAdmin = (JoinAdmin) admin.getAdmin();
			out.println("Lookup locators: "
					+ Arrays.toString(jAdmin.getLookupLocators()));
			ClassLoader cl = myReg.getClass().getClassLoader();
			if (cl instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader) cl).getURLs();
				if (urls.length > 0) {
					out.println("Codebase URLs:");
					for (int l = 1; l < urls.length; l++) {
						out.println("  " + urls[l]);
					}
				}
			}
			NetworkShell.printLookupAttributes(jAdmin.getLookupAttributes());
		}
	}

	public static ServiceRegistrar getSelectedRegistrar() {
        if (registrars==null)
            registrars = new ArrayList<ServiceRegistrar>(NetworkShell.getRegistrars());
		if (registrars != null && registrars.size() > 0
				&& selectedRegistrar >= 0)
			return registrars.get(selectedRegistrar);
		else if (selectedRegistrar < 0 && registrars.size() > 0) {
                return registrars.get(0);
            } else
                return null;
        }
    }
