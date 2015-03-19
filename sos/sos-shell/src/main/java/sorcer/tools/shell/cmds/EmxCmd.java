/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
 * Copyright 2015 SorcerSoft.com
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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import sorcer.core.provider.MonitorUIManagement;
import sorcer.core.provider.MonitoringManagement;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.ContextException;
import sorcer.service.Exec.State;
import sorcer.service.Exertion;
import sorcer.service.ExertionInfo;
import sorcer.service.MonitorException;
import sorcer.service.ServiceExertion;
import sorcer.tools.shell.IStatusCommand;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

public class EmxCmd extends ShellCmd implements IStatusCommand {

	{
		COMMAND_NAME = "emx";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "emx [-xrt | -emx | <EMX/exertion index> | -v | -x]"
			+ "\n\t\t\t  | [ -a | -d | -f | -r | -y | <exertion index>] "
			+ "\n\t\t\t  | (-e | -c | -cc | -ccc) [<exertion index>] [-s <filename>]";

		COMMAND_HELP = "Support for monitoring runtime exertions;"
				+ "\n  ('emx' mode) show EMX services or ('xrt' mode) print the selected exertion"
				+ "\n  <EMX index>   select the EMX given <EMX index>"
				+ "\n  -v   print the selected EMX service/exertion"
				+ "\n  -x   clear the selection of MX service/exertion"
				+ "\n  -a   show all monitored exertions"
				+ "\n  -d   show done monitored exertions"
				+ "\n  -f   show failed monitored exertions"
				+ "\n  -r   show running monitored exertions"
				+ "\n  -y   show asynchronous monitored exertions"
				+ "\n  -e   print the selected exertion"
				+ "\n  <exertion index>   select the exertion given <exertion index>"
				+ "\n  -c   print the data context of selected exertion"
				+ "\n  -cc   print the control context of selected exertion"
				+ "\n  -ccc   print both data and control contexts of selected exertion"
				+ "\n  -s   save the selected exertion in a given file ";

	}

	boolean isEmxMode = true;
	private ServiceItem[] emxMonitors;
	private int selectedMonitor = -1;
	private ExertionInfo[] exertionInfos = new ExertionInfo[0];
	int selectedExertion = -1;
	private Map<Uuid, ServiceItem> monitorMap = new HashMap<Uuid, ServiceItem>();

	public EmxCmd() {
	}

    @Override
    public Options getOptions() {
        Option e = new Option("e", false, "print selected exertion");
        e.setOptionalArg(true);
        Option c = new Option("c", false, "print the data context of selected exertion");
        c.setOptionalArg(true);
        Option cc = new Option(null, "cc", false, "print the control context of selected exertion");
        cc.setOptionalArg(true);
        Option ccc = new Option(null, "ccc", false, "print both data and control contexts of selected exertion");
        ccc.setOptionalArg(true);

        return super.getOptions()
                .addOption(null, "xrt", false, "set xrt mode")
                .addOption(null, "emx", false, "set emx mode")
                .addOption(null, "mode", false, "show mode")
                .addOption("v", false, "print exertions")
                .addOption("d", false, "show done monitored exertions")
                .addOption("r", false, "show running monitored exertions")
                .addOption("f", false, "show failed monitored exertions")
                .addOption("a", false, "show all monitored exertions")
                .addOption("y", false, "show asynchronous monitored exertions")
                .addOption("x", false, "clear selection")
                .addOption(e)
                .addOption(c)
                .addOption(cc)
                .addOption(ccc)
                ;
    }

    public void execute(String command, CommandLine cmd) throws RemoteException, MonitorException, ContextException, ExecutionException {
        if (cmd.hasOption("xrt")) {
            isEmxMode = false;
            out.println("you are in 'xrt' mode");
        } else if (cmd.hasOption("emx")) {
            isEmxMode = true;
            out.println("you are in 'emx' mode");
        } else if (cmd.hasOption("mode")) {
            if (isEmxMode)
                out.println("you are in 'emx' mode");
            else
                out.println("you are in 'xrt' mode");
        } else if (cmd.hasOption('v')) {
            if (selectedMonitor >= 0) {
                describeMonitor(selectedMonitor);
            } else
                out.println("No selected EMX");
        } else if (cmd.hasOption('d'))
            printMonitoredExertions(State.DONE);
        else if (cmd.hasOption('r'))
            printMonitoredExertions(State.RUNNING);
        else if (cmd.hasOption('f'))
            printMonitoredExertions(State.FAILED);
        else if (cmd.hasOption('a'))
            printMonitoredExertions(State.NULL);
        else if (cmd.hasOption('y'))
            printMonitoredExertions(State.ASYNC);
        else if (cmd.hasOption('x'))
            selectedMonitor = -1;
        else if (cmd.hasOption('e')) {
            isEmxMode = false;
            String selectionStr = cmd.getOptionValue('e');
            ExertionInfo xrtInfo = getSelectedExertion(selectionStr);
            printExertionInfo(xrtInfo, false, false, selectionStr);
        } else if (cmd.hasOption('c')) {
            isEmxMode = false;
            String selectionStr = cmd.getOptionValue('c');
            ExertionInfo xrtInfo = getSelectedExertion(selectionStr);
            printExertionInfo(xrtInfo, true, false, selectionStr);
        } else if (cmd.hasOption("cc")) {
            isEmxMode = false;
            String selectionStr = cmd.getOptionValue("cc");
            ExertionInfo xrtInfo = getSelectedExertion(selectionStr);
            printExertionInfo(xrtInfo, false, true, selectionStr);
        } else if (cmd.hasOption("ccc")) {
            isEmxMode = false;
            String selectionStr = cmd.getOptionValue('e');
            ExertionInfo xrtInfo = getSelectedExertion(selectionStr);
            printExertionInfo(xrtInfo, true, true, selectionStr);
        } else {
            String[] args = cmd.getArgs();
            if (args.length > 0)
                try {
                    selectedMonitor = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    selectedMonitor = selectMonitorByName(args[0]);
                }

            if (isEmxMode) {
                if (selectedMonitor >= 0 && selectedMonitor < emxMonitors.length) {
                    describeMonitor(selectedMonitor, args[0]);
                } else {
                    out.println("No such EMX for: " + args[0]);
                }
            } else {
                if (selectedExertion < 0 || selectedExertion >= exertionInfos.length)
                    out.println("No such Exertion for: " + args[0]);
                else
                    out.println(exertionInfos[selectedExertion]);
            }
        }
    }

    private void printExertionInfo(ExertionInfo xrtInfo, boolean isContext, boolean isControlContext, String arg) throws ExecutionException {
        if (xrtInfo != null)
            try {
                printExertion(xrtInfo.getStoreId(), isContext, isControlContext);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        else
            out.println("No such Exertion for: " + arg);
    }

    private ExertionInfo getSelectedExertion(String selectionStr) {
        if (selectionStr != null) {
            try{
                selectedExertion = Integer.parseInt(selectionStr);
            }catch (NumberFormatException e){
                selectedExertion = selectExertionByName(selectionStr);
            }
        }
        return selectedExertion >= 0 && selectedExertion < exertionInfos.length ? exertionInfos[selectedExertion] : null;
    }

    private void printExertion(Uuid id, boolean isContext,
			boolean isControlContext) throws RemoteException, MonitorException, ContextException {
		Exertion xrt;
		if (selectedMonitor >= 0) {
			xrt = ((MonitorUIManagement) emxMonitors[selectedMonitor].service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		} else {
			xrt = ((MonitorUIManagement) monitorMap.get(id).service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		}

		out.println("--------- EXERTION # " + selectedExertion + " ---------");
		out.println(((ServiceExertion) xrt).describe());
		if (isContext) {
			out.println("\nData Context:");
			out.println(xrt.getContext());
		}
		if (isControlContext) {
			out.print("\nControl Context:");
			out.println(xrt.getControlContext());
		}
	}

	private void printMonitoredExertions(State xetType)
			throws RemoteException, MonitorException {
		if (emxMonitors == null || emxMonitors.length == 0) {
			findMonitors();
		}
		Map<Uuid, ExertionInfo> all;
		if (selectedMonitor >= 0) {
			out.println("From EMX "
					+ AttributesUtil
							.getProviderName(emxMonitors[selectedMonitor].attributeSets)
					+ " at: "
					+ AttributesUtil
							.getHostName(emxMonitors[selectedMonitor].attributeSets));
			all = ((MonitorUIManagement) emxMonitors[selectedMonitor].service)
					.getMonitorableExertionInfo(xetType,
							NetworkShell.getPrincipal());
		} else {
			Map<Uuid, ExertionInfo> hm;
			all = new HashMap<Uuid, ExertionInfo>();
			for (int i = 0; i < emxMonitors.length; i++) {
				out.println("From EMX "
						+ AttributesUtil
								.getProviderName(emxMonitors[i].attributeSets)
						+ " at: "
						+ AttributesUtil
								.getHostName(emxMonitors[i].attributeSets));

				MonitorUIManagement emx = (MonitorUIManagement) emxMonitors[i].service;
				hm = emx.getMonitorableExertionInfo(xetType,
						NetworkShell.getPrincipal());
				if (hm != null && hm.size() > 0) {
					all.putAll(hm);
				}
				// populate exertion/EMX map
				monitorMap.clear();
				for (ExertionInfo ei : hm.values()) {
					monitorMap.put(ei.getStoreId(), emxMonitors[i]);
				}
			}
		}
		if (all.size() == 0) {
			out.println("No monitored exertions at this time.");
			return;
		}
		exertionInfos = new ExertionInfo[all.size()];
		all.values().toArray(exertionInfos);
		printExerionInfos(exertionInfos);
	}

	private void printExerionInfos(ExertionInfo[] exertionInfos) {
		for (int i = 0; i < exertionInfos.length; i++) {
			out.println("--------- EXERTION # " + i + " ---------");
			out.println(exertionInfos[i].describe());
		}
	}

    private void describeMonitor(int index) {
		describeMonitor(index, null);
	}

    public void printStatus() throws Exception {
		if (selectedMonitor >= 0) {
            shell.getOutputStream().println("Current exertion monitoring service: ");
			describeMonitor(selectedMonitor);
		}
		else {
            shell.getOutputStream().println("No selected EMX, use 'mxe' command");
		}
	}

	private void describeMonitor(int index, String msg) {
		out.println("---------" + (msg != null ? " " + msg : "")
				+ " EMX SERVICE # " + index + " ---------");
		out.println("EMX: " + emxMonitors[index].serviceID + " at: "
				+ AttributesUtil.getHostName(emxMonitors[index].attributeSets));
		out.println("Home: "
				+ AttributesUtil.getUserDir(emxMonitors[index].attributeSets));
		String groups = AttributesUtil
				.getGroups(emxMonitors[index].attributeSets);
		out.println("Provider name: "
				+ AttributesUtil
						.getProviderName(emxMonitors[index].attributeSets));
		out.println("Groups supported: " + groups);
	}

	private int selectMonitorByName(String name) {
		for (int i = 0; i < emxMonitors.length; i++) {
			if (AttributesUtil.getProviderName(emxMonitors[i].attributeSets)
					.equals(name))
				return i;
		}
		return -1;
	}

	private int selectExertionByName(String name) {
		for (int i = 0; i < emxMonitors.length; i++) {
			if (AttributesUtil.getProviderName(emxMonitors[i].attributeSets)
					.equals(name))
				return i;
		}
		return -1;
	}

	public void setExertionInfos(ExertionInfo[] exertionInfos) {
		this.exertionInfos = exertionInfos;
	}

	public void setEmxMonitors(ServiceItem[] monitors) {
		emxMonitors = monitors;
	}

	public Map<Uuid, ServiceItem> getMonitorMap() {
		return monitorMap;
	}

	ServiceItem[] findMonitors() throws RemoteException {
		emxMonitors = shell.lookup(new Class[]{MonitoringManagement.class});
		return emxMonitors;
	}
}
