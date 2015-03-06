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
import net.jini.core.lookup.ServiceItem;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.DatabaseStorer.Store;
import sorcer.core.provider.Provider;
import sorcer.core.provider.StorageManagement;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.*;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.url.sos.SdbUtil;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.co.operator.list;
import static sorcer.co.operator.store;

public class DataStorageCmd extends ShellCmd {

	{
		COMMAND_NAME = "ds";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "ds [-s | -s <storage index> | -v | -x]"
			+ "\n\t\t\t  | -l [<store index>] | -r | -s"
			+ "\n\t\t\t  | (-e | -c | -cc | -ccc) [<exertion index>] [-s <filename>]";

		COMMAND_HELP = "Support for inspecting SORCER data storage;"
				+ "\n  -s   show data storage services"
				+ "\n  -s   <storage index>   select the data storage given <storage index>"
				+ "\n  -v   print the selected storage service"
				+ "\n  -x   clear the selection of data storage service"
				+ "\n  -l   [<store type>] list all storage records or of a given type"
				+ "\n  -r   print the selected record"
				+ "\n  -s   save the selected record in a given file ";

	}

	static private ServiceItem[] dataStorers;
    private int selectedDataStorer = -1;

	public DataStorageCmd() {
	}

    @Override
    public Options getOptions() {
        Option s = new Option("s", "show data storage services or select the data storage");
        s.setArgs(1);
        s.setOptionalArg(true);
        Option list = new Option("l", "list all storage records or of a given type");
        list.setArgs(1);
        list.setArgName("store type");
        list.setOptionalArg(true);
        return super.getOptions()
                .addOption("v", false, "print the selected storage service")
                .addOption(s)
                .addOption("x", false, "clear the selection of data storage service")
                .addOption(list)
                .addOption("r", false, "print the selected record");
    }

    public void execute(String command, CommandLine cmd) throws RemoteException, MonitorException {

        if(cmd.hasOption('l')){
            printRecords();
        }

        if(cmd.hasOption('s')){
            String selectString = cmd.getOptionValue('s');
            if (selectString != null) {
                try {
                    selectedDataStorer = Integer.parseInt(selectString);
                } catch (NumberFormatException e) {
                    selectedDataStorer = selectMonitorByName(selectString);
                    if (selectedDataStorer < 0)
                        out.println("No such data storage for: " + selectString);
                }
                if (selectedDataStorer >= 0) {
                    describeStorer(selectedDataStorer, "SELECTED", out);
                } else {
                    out.println("No such data storage for: " + selectedDataStorer);
                }
                return;

            }
            else
                showStorageServices();
        }

        if(cmd.hasOption('v')){
            if (selectedDataStorer >= 0) {
                describeStorer(selectedDataStorer, out);
            } else {
                out.println("No selected data storage");
                return;
            }
        }

        if (cmd.hasOption('x'))
            selectedDataStorer = -1;

        if(cmd.getArgs().length!=0){
            printUsage();
        }
	}

    private void printRecords()
			throws RemoteException, MonitorException {
		if (dataStorers == null || dataStorers.length == 0) {
			findStorers();
		}
		if (selectedDataStorer >= 0) {
			out.println("From Data Storage: "
					+ AttributesUtil
							.getProviderName(dataStorers[selectedDataStorer].attributeSets)
					+ " at: "
					+ AttributesUtil
							.getHostName(dataStorers[selectedDataStorer].attributeSets));

			Context cxt = null;
			 try {
				 store("test-only");
				 out.println("XXXXXXXXXXXXX service item: " + dataStorers[selectedDataStorer]);
				 out.println("XXXXXXXXXXXXX service: " + dataStorers[selectedDataStorer].service);
				 out.println("XXXXXXXXXXXXX interfaces: " + Arrays.toString(dataStorers[selectedDataStorer].service.getClass().getInterfaces()));
				 out.println("XXXXXXXXXXXXX name: " + ((Provider) dataStorers[selectedDataStorer].service).getProviderName());
				 try {
				 cxt = ((DatabaseStorer) dataStorers[selectedDataStorer].service).contextList(SdbUtil.getListContext(Store.object));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				out.println("XXXXXXXXXXXXX context: " + cxt);
				try {
					store("test-only");
					List<String>  records = list(Store.object);
					out.println("XXXXXXXXXXXXX records; " + records);
				} catch (ExertionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				out.println(cxt.getValue(StorageManagement.store_content_list));
			} catch (ContextException e) {
				e.printStackTrace();
			}
		} else {
			for (int i = 0; i < dataStorers.length; i++) {
				out.println("From Data Storage "
						+ AttributesUtil.getProviderName(dataStorers[i].attributeSets)
						+ " at: "
						+ AttributesUtil.getHostName(dataStorers[i].attributeSets));
			}
		}
	}

	private void showStorageServices() throws RemoteException {
		findStorers();
		printStorageServices();
	}

	private void printStorageServices() {
		if ((dataStorers != null) && (dataStorers.length > 0)) {
			for (int i = 0; i < dataStorers.length; i++) {
				describeStorer(i, out);
			}
		} else
			System.out.println("Sorry, no fetched Data Storage services.");
	}

	static private void describeStorer(int index, PrintWriter out) {
		describeStorer(index, null, out);
	}
	
	public static void printCurrentStorer(PrintWriter out, int selectedDataStorer) {
		if (selectedDataStorer >= 0) {
            out.println("Current data storage service: ");
			describeStorer(selectedDataStorer, out);
        } else {
            out.println("No selected data storage, use 'ds -s' to list and select with 'ds #'");
		}
	}

	static private void describeStorer(int index, String msg, PrintWriter out) {
		out.println("---------" + (msg != null ? " " + msg : "")
				+ " DATA STORAGE SERVICE # " + index + " ---------");
		out.println("EMX: " + dataStorers[index].serviceID + " at: "
				+ AttributesUtil.getHostName(dataStorers[index].attributeSets));
		out.println("Home: "
				+ AttributesUtil.getUserDir(dataStorers[index].attributeSets));
		String groups = AttributesUtil
				.getGroups(dataStorers[index].attributeSets);
		out.println("Provider name: "
				+ AttributesUtil
						.getProviderName(dataStorers[index].attributeSets));
		out.println("Groups supported: " + groups);
	}

	private int selectMonitorByName(String name) {
		for (int i = 0; i < dataStorers.length; i++) {
			if (AttributesUtil.getProviderName(dataStorers[i].attributeSets)
					.equals(name))
				return i;
		}
		return -1;
	}

	private int selectRecordByName(String name) {
		for (int i = 0; i < dataStorers.length; i++) {
			if (AttributesUtil.getProviderName(dataStorers[i].attributeSets)
					.equals(name))
				return i;
		}
		return -1;
	}


    ServiceItem[] findStorers() throws RemoteException {
		dataStorers = shell.lookup(new Class[]{StorageManagement.class});
		return dataStorers;
	}

}
