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

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.PrintStream;

public class SosCmd extends ShellCmd {
	{
		COMMAND_NAME = "sos";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "sos -h | -n | -d | -v";

		COMMAND_HELP = "Display SORCER_HOME \n  -h  cd to SORCER_HOME\n"
				+"  -n  cd to the netlets directory\n  -d  cd to http data root directory\n  -version  SOS version";
	}

	private PrintStream out;

	public SosCmd() {
	}

	public void execute(String... args) throws Throwable {
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		if (numTokens == 0) {
			out.println("SORCER_HOME: " + Sorcer.getHome());
			return;
		}
		String option = myTk.nextToken();
		if (option.equals("sos")) {
			option = myTk.nextToken();
		}

		if (option.equals("-h")) {
			NetworkShell.setRequest("cd " + Sorcer.getHome());
			ShellCmd cmd = NetworkShell.getCommandTable().get("ls");
			cmd.execute();
		} else if (option.equals("-r")) {
			NetworkShell.setRequest("cd " + getSosRootDir());
			ShellCmd cmd = NetworkShell.getCommandTable().get("ls");
			cmd.execute();
		} else if (option.equals("-n")) {
			NetworkShell.setRequest("cd " + getSosRootDir()  + File.separator + "examples"
					+ File.separator + "sml" + File.separator + "src" + File.separator + "main"
					+ File.separator + "netlets");
			ShellCmd cmd = NetworkShell.getCommandTable().get("ls");
			cmd.execute();
		} else if (option.equals("-d")) {
			NetworkShell.setRequest("cd " + Sorcer.getHome()
					+ File.separator + "data");
			ShellCmd cmd = NetworkShell.getCommandTable().get("ls");
			cmd.execute();
		} else if (option.equals("~")) {
			NetworkShell.setRequest("cd " + System.getProperty("user.home"));
			ShellCmd cmd = NetworkShell.getCommandTable().get("ls");
			cmd.execute();
		} else if (option.equals("-v") || option.equals("-version")) {
			NetworkShell.shellOutput.println("SOS version: " + getSosVersion());
		}
	}

	public static String getSosRootDir() {
		String str = Sorcer.getHome();
		int index = str.indexOf("distribution");
		return str.substring(0, index-1);
	}

	public static String getSosVersion() {
		String str = Sorcer.getHome();
		int index = str.indexOf("sorcer-");
		return str.substring(index+7);
	}
}
