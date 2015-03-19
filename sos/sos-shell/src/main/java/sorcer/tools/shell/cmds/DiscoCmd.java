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
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.lookup.ServiceRegistrar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import sorcer.tools.shell.IStatusCommand;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

public class DiscoCmd extends ShellCmd implements IStatusCommand {

	{
		COMMAND_NAME = "disco";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "disco [<registrar index> | -v | -x]";

		COMMAND_HELP = "List all lookup services discovered for provided groups;"
			+ "\n\twith <registrar index> select the specified registrar as a default one"
			+ "\n\t-v   print the default registrar info"
			+ "\n\t-x   clear the selected registrar and start discovery";
	}

	private List<ServiceRegistrar> registrars;
	int selectedRegistrar = 0;

	public DiscoCmd() {
	}

    @Override
    public Options getOptions() {
        return super.getOptions()
                .addOption("x", false, "clear the selected registrar and start discovery")
                .addOption("v", false, "print the default registrar info");

    }

    @Override
    public void execute(String command, CommandLine cmd) throws Exception {
        String[] args = cmd.getArgs();
        if (args.length == 1)
            try {
                selectedRegistrar = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Expected an index of the registrar", e);
            }


        if (cmd.hasOption('x')) {
            clearSelection();
        }else if(cmd.hasOption('v'))
            describeSelectedRegistrar();
        else
            describeRegistrars();

    }

    private void clearSelection() {
        NetworkShell.getRegistrars().clear();
        registrars.clear();
        // default index
        selectedRegistrar = 0;
        NetworkShell.getDisco().terminate();
        // start new lookup discovery
        NetworkShell.setLookupDiscovery(NetworkShell.getGroups());
    }

    private void describeRegistrars() throws IOException, ClassNotFoundException {
        if ((registrars != null) && (registrars.size() > 0)) {
            Iterator it = registrars.iterator();
            while (it.hasNext()) {
                ServiceRegistrar myReg = (ServiceRegistrar) it.next();
                describeServiceRegistrar(myReg, false, shell);
            }
        } else
            System.out.println("Sorry, no lookup services located");
    }

    private void describeSelectedRegistrar() throws IOException, ClassNotFoundException {
        if (selectedRegistrar >= 0
                && selectedRegistrar < registrars.size()) {
            describeServiceRegistrar(registrars.get(selectedRegistrar),
                    true, shell);
        }
        else {
            out.println("No selected registrar!");
        }
    }

    public void describeServiceRegistrar(ServiceRegistrar myReg,
			boolean withDetails, NetworkShell shell) throws IOException, ClassNotFoundException {
		String[] groups;
		String msg = "";

        PrintWriter out = shell.getOutputStream();

		out.println("--------- LOOKUP SERVICE # " + registrars.indexOf(myReg)
				+ " ---------");
		out.println("ID: " + myReg.getServiceID());
		groups = myReg.getGroups();
		if (groups.length > 0)
			for (int o = 0; o < groups.length; o++) {
				msg += "\'" + groups[o] + "\' ";
			}
		out.println("Groups supported: " + msg);
		out.println("Lookup locator: " + myReg.getLocator().getHost() + ":"
				+ myReg.getLocator().getPort());
		if (withDetails)
			printDetails(myReg, shell);
	}

    public void printStatus() throws Exception {
		if (registrars == null)
			registrars = NetworkShell.getRegistrars();
		if (selectedRegistrar >= 0) {
			shell.getOutputStream().println("Current lookup service: ");
			if (registrars.size() > 0)
				describeServiceRegistrar(registrars.get(selectedRegistrar), false, shell);
		} else {
            shell.getOutputStream()
					.println("No selected LUS; use 'disco' cmd");
		}
	}
	
	static private void printDetails(ServiceRegistrar myReg, NetworkShell shell1) throws IOException,
			ClassNotFoundException {
		Class myCls = myReg.getClass();
        PrintWriter out = shell1.getOutputStream();
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
					out.println("Codbase URLs:");
					for (int l = 1; l < urls.length; l++) {
						out.println("  " + urls[l]);
					}
				}
			}
			NetworkShell.printLookupAttributes(jAdmin.getLookupAttributes());
		}
	}

	public ServiceRegistrar getSelectedRegistrar() {
		if (registrars != null && registrars.size() > 0
				&& selectedRegistrar >= 0)
			return registrars.get(selectedRegistrar);
		else if (selectedRegistrar < 0 && registrars.size() > 0) {
			return registrars.get(0);
		} else
			return null;
	}
}
