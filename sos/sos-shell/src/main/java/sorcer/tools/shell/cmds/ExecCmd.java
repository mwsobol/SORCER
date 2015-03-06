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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import sorcer.tools.shell.ShellCmd;
import sorcer.util.StringUtils;
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

		COMMAND_HELP = "Handles the underlying OS shell commands";
	}

    final static String[] viewers = {"cat", "less", "more"};

    public void execute(String command, String[] argv) throws ExecutionException{

        if (Arrays.binarySearch(viewers, argv[0]) > -1) {
            adjustPath(argv);
        }

        String cmd = StringUtils.join(argv,' ');

        CmdResult result;
				try {
					result = ExecUtils.execCommand(cmd);
					Thread.sleep(400);
				} catch (IOException e) {
            throw new ExecutionException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        out.println(result.getOut());
        out.flush();
    }

    private void adjustPath(String[] argv) {
        String arg = argv[1];
        if (!(new File(arg).isAbsolute())) {
            arg = arg.replace("." + File.separator,
                    shell.getCurrentDir() + File.separator);
            if (!(new File(arg).isAbsolute())) {
                arg = shell.getCurrentDir() + File.separator + arg;
            }
        }
        argv[1] = arg;
    }
}
