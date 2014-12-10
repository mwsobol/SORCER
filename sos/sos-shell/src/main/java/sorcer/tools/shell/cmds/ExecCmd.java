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
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;

/**
 * Handles system commands
 */
public class ExecCmd extends ShellCmd {

	{
		COMMAND_NAME = "exec";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "exec <cmd>";

		COMMAND_HELP = "Handles the uderlying OS shell commands";
	}

	private String input;

	private PrintStream out;

	private Thread execThread;
	
	public void execute() throws Throwable {
		NetworkShell shell = NetworkShell.getInstance();
		out = NetworkShell.getShellOutputStream();
		String cmd = "";
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");
		StringTokenizer tok = new StringTokenizer(input);
		String token = tok.nextToken();
		String arg = "";
		if (token.equals("exec")) {
			//CmdResult result = null;
			token = tok.nextToken();
			if (token.equals("more") || token.equals("less")
					|| token.equals("cat")) {
				arg = tok.nextToken();
				if (!(new File(arg).isAbsolute())) {
					arg = arg.replace("." + File.separator,
							shell.getCurrentDir() + File.separator);
					if (!(new File(arg).isAbsolute())) {
						arg = shell.getCurrentDir() + File.separator + arg;
					}
				}
				cmd = token + " " + arg;
			} else {
				cmd = cmd.replace("." + File.separator,
						shell.getCurrentDir() + File.separator);
				cmd = input.substring(5);
			}
			
//			run(cmd);
//			t.join();
			
			CmdResult result = ExecUtils.execCommand(cmd);
			Thread.sleep(400);
			out.println(result.getOut());   	
			out.flush();
			
//			out.println(result.getExitValue());
//			out.flush();
//			out.println(result.getErr());
//			out.flush();
		} else {
			out.println(COMMAND_USAGE);
		}
	}
	
	
	private void run(final String cmd) {
		 execThread = new Thread(new Runnable() {
			public void run() {
				CmdResult result = null;
				try {
					result = ExecUtils.execCommand(cmd);
					Thread.sleep(400);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				out.println(result.getOut());
			}
		});
		execThread.start();
	}
}
