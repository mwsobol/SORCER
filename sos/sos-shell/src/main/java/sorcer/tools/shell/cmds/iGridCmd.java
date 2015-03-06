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

import java.io.File;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.Sorcer;
import sorcer.tools.shell.WhitespaceTokenizer;

public class iGridCmd extends ShellCmd {
	{
		COMMAND_NAME = "ig";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "ig -h | -n | -d";

		COMMAND_HELP = "Display SORCER_HOME \n  -h  cd to SORCER_HOME\n"
				+"-n  cd to the netlets directory\n  -d  cd to http data root directory.";
	}

	public iGridCmd() {
	}

	public void execute(String command, String[] request) throws Throwable {
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		if (numTokens == 0) {
			out.println("SORCER_HOME: " + Sorcer.getHome());
			return;
		}
		String option = myTk.nextToken();
		if (option.equals("ig")) {
			option = myTk.nextToken();
		}

		if (option.equals("-h")) {
			NetworkShell.setRequest("cd " + Sorcer.getHome());
			ShellCmd cmd = (ShellCmd) NetworkShell.getCommandTable().get("ls");
			cmd.execute(command, request);
		} else if (option.equals("-n")) {
			NetworkShell.setRequest("cd " + Sorcer.getHome()
					+ File.separator + "netlets");
			ShellCmd cmd = (ShellCmd) NetworkShell.getCommandTable().get("ls");
			cmd.execute(command, request);
		} else if (option.equals("-d")) {
			NetworkShell.setRequest("cd " + Sorcer.getHome()
					+ File.separator + "data");
			ShellCmd cmd = (ShellCmd) NetworkShell.getCommandTable().get("ls");
			cmd.execute(command, request);
		} else if (option.equals("~")) {
			NetworkShell.setRequest("cd " + System.getProperty("user.home"));
			ShellCmd cmd = (ShellCmd) NetworkShell.getCommandTable().get("ls");
			cmd.execute(command, request);
		}
	}

}
