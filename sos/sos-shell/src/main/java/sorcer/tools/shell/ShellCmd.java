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

package sorcer.tools.shell;

import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

import net.jini.config.Configuration;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import org.apache.commons.cli.*;
import sorcer.util.ServiceAccessor;

@SuppressWarnings("rawtypes")
abstract public class ShellCmd {

    private final HelpFormatter helpFormatter = new HelpFormatter();
    protected String COMMAND_NAME;

	protected String NOT_LOADED_MSG;

	protected String COMMAND_USAGE;

	protected String COMMAND_HELP;

	protected NetworkShell shell;

	protected Configuration config;

    protected Parser parser = new BasicParser();

    protected Options options;

    protected PrintWriter out;

    public ShellCmd() {
        options = getOptions();
    }

    public void execute(String command, String[] argv) throws ExecutionException, ParseException {
        try {
            execute(command, parser.parse(getOptions(), argv));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    public void execute(String command, CommandLine cmd) throws Exception{}

    public Options getOptions() {
        return new Options();
    }

    public void printUsage() {
        helpFormatter.printUsage(new PrintWriter(out), 120, COMMAND_NAME, options);
    }

    public String getCommandWord() {
		return COMMAND_NAME;
	}

	public String getUsage(String subCmd) {
		return COMMAND_USAGE;
	}

	public String getShortHelp() {
		return COMMAND_HELP;
	}

	public String getLongDescription(String subCmd) {
		return COMMAND_HELP;
	}

	public String nameConflictDetected(Class<?> conflictClass) {
		return NOT_LOADED_MSG;
	}

	public void initializeSubsystem() {
	}

	public void endSubsystem() {
	}

	public void setNetworkShell(NetworkShell shell){
		this.shell = shell;
        out = shell.getOutputStream();
    }

	public void setConfiguration(Configuration config){
		this.config = config;
	}

	public String toString() {
		return getClass().getName() + ": " + COMMAND_NAME;
	}

    public static ServiceItem[] serviceLookup(
			Class[] serviceTypes) throws RemoteException {
		ServiceTemplate st = new ServiceTemplate(null, serviceTypes, null);
		ServiceItem[] serviceItems = ServiceAccessor.getServiceItems(st, null,
				NetworkShell.getGroups());
		return serviceItems;
	}
	
	static ServiceItem[] serviceLookup(Class[] serviceTypes, String[] groups) {
		ServiceTemplate st = new ServiceTemplate(null, serviceTypes, null);
		ServiceItem[] serviceItems = ServiceAccessor.getServiceItems(st, null,
				groups);
		return serviceItems;
	}

}
