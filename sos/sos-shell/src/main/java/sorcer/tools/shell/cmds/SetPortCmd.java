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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.jini.admin.Administrable;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.lookup.DiscoveryAdmin;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

public class SetPortCmd extends ShellCmd {
	{
		COMMAND_NAME = "chport";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "chport <lookup service index> port";

		COMMAND_HELP = "Change the unicast discovery port used by a lookup service.";
	}

	private PrintStream out;

	public SetPortCmd() {
	}

	public void execute() {
		out = NetworkShell.getShellOutputStream();
		StringTokenizer myTk = NetworkShell.getShellTokenizer();
		// pass in a clone of list - command may modify it
		ArrayList registrars = (ArrayList) NetworkShell.getRegistrars().clone();
		int numTokens = myTk.countTokens();
		if (numTokens < 2) {
			out.println(COMMAND_USAGE);
			return;
		}

		int myIdx = Integer.parseInt(myTk.nextToken());
		int myPort = Integer.parseInt(myTk.nextToken());

		if ((myIdx < registrars.size()) && (myIdx >= 0)) {
			ServiceRegistrar myReg = (ServiceRegistrar) registrars.get(myIdx);

			if (myReg != null)
				setPort(myReg, myIdx, myPort);

		}
	}

	private void setPort(ServiceRegistrar registrar, int Idx, int port) {
		try {
			Administrable admin = null;
			if (registrar instanceof Administrable) {
				admin = (Administrable) registrar;
				DiscoveryAdmin discoveryAdmin = (DiscoveryAdmin) admin
						.getAdmin();

				out.println("  Changing port for lookup service # " + Idx
						+ " now!");

				discoveryAdmin.setUnicastPort(port);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
