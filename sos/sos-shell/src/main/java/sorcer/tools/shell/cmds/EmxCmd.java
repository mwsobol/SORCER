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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import sorcer.core.provider.MonitorUIManagement;
import sorcer.core.provider.MonitoringManagement;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.ContextException;
import sorcer.service.Exec.State;
import sorcer.service.Exertion;
import sorcer.service.ExertionInfo;
import sorcer.service.MonitorException;
import sorcer.service.ServiceExertion;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.WhitespaceTokenizer;

public class EmxCmd extends ShellCmd {

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

	static private PrintStream out;
	private boolean isEmxMode = true;
	static private ServiceItem[] emxMonitors;
	static private int selectedMonitor = -1;
	static private ExertionInfo[] exertionInfos = new ExertionInfo[0];
	private int selectedExertion = -1;
	static private Map<Uuid, ServiceItem> monitorMap = new HashMap<Uuid, ServiceItem>();

	public EmxCmd() {
	}

	public void execute() throws RemoteException, MonitorException, ContextException {
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		int myIdx = 0;
		String next = null;
		State xrtType = null;

		if (numTokens == 0) {
			showEmxServices();
			selectedMonitor = -1;
			isEmxMode = true;
		} else if (numTokens == 1) {
			next = myTk.nextToken();
			if (next.equals("-xrt")) {
				isEmxMode = false;
				out.println("you are in 'xrt' mode");
			} if (next.equals("-emx")) {
				isEmxMode = true;
				out.println("you are in 'emx' mode");
			} if (next.equals("-mode")) {
				if (isEmxMode)
					out.println("you are in 'emx' mode");
				else
					out.println("you are in 'xrt' mode");
			} else if (next.equals("-v")) {
				if (selectedMonitor >= 0) {
					describeMonitor(selectedMonitor);
				} else
					out.println("No selected EMX");
			} else if (next.equals("-d") || next.equals("-r")
					|| next.equals("-f") || next.equals("-f")
					|| next.equals("-a") || next.equals("-y")) {
				xrtType = getStatus(next);
				printMonitoredExertions(xrtType);
			} else if (next.equals("-x")) {
				// clear monitor selection
				selectedMonitor = -1;
			} else if (next.equals("-e")) {
				isEmxMode = false;
				if (selectedExertion >= 0) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), false, false);
				}
			} else if (next.equals("-c")) {
				isEmxMode = false;
				if (selectedExertion >= 0) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), true, false);
				}
			} else if (next.equals("-cc")) {
				isEmxMode = false;
				if (selectedExertion >= 0) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), false, true);
				}
			} else if (next.equals("-ccc")) {
				isEmxMode = false;
				if (selectedExertion >= 0) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), true, true);
				}
			} else {
				if (isEmxMode) {
					try {
						next = myTk.nextToken();
						myIdx = Integer.parseInt(next);
						selectedMonitor = myIdx;
					} catch (NumberFormatException e) {
						selectedMonitor = selectMonitorByName(next);
						if (selectedMonitor < 0)
							out.println("No such EMX for: " + next);
					}
					if (selectedMonitor >= 0) {
						describeMonitor(selectedMonitor, "SELECTED");
					} else {
						out.println("No such EMX for: " + selectedMonitor);
					}
				} else {
					try {
						myIdx = Integer.parseInt(next);
						selectedExertion = myIdx;
					} catch (NumberFormatException e) {
						selectedExertion = selectExertionByName(next);
					}
					if (selectedExertion < 0
							|| selectedExertion >= exertionInfos.length)
						out.println("No such Exertion for: " + next);
					else
						out.println(exertionInfos[selectedExertion]);
				}
			}
		} else if (numTokens == 2) {
			next = myTk.nextToken();
			if (next.equals("-e") || next.equals("-c")
					|| next.equals("-cc") || next.equals("-ccc")) {
				isEmxMode = false;
				boolean isContext = false;
				boolean isControlContext = false;
				if (next.equals("-c"))
					isContext = true;
				if (next.equals("-cc"))
					isControlContext = true;
				if (next.equals("-ccc")) {
					isContext = true;
					isControlContext = true;
				}
				try {
					next = myTk.nextToken();
					myIdx = Integer.parseInt(next);
					selectedExertion = myIdx;
				} catch (NumberFormatException e) {
					selectedExertion = selectExertionByName(next);
					if (selectedExertion < 0)
						out.println("No such Exertion for: " + next);
				}
				if (selectedExertion >= 0
						&& selectedExertion < exertionInfos.length) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), isContext,
							isControlContext);
				} else
					out.println("No such Exertion for: " + selectedExertion);
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}

	private void printExertion(Uuid id, boolean isContext,
			boolean isControlContext) throws RemoteException, MonitorException, ContextException {
		Exertion xrt = null;
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

	private void showEmxServices() throws RemoteException {
		findMonitors();
		printEmxServices();
	}

	private void printEmxServices() {
		if ((emxMonitors != null) && (emxMonitors.length > 0)) {
			for (int i = 0; i < emxMonitors.length; i++) {
				describeMonitor(i);
			}
		} else
			System.out.println("Sorry, no fetched EMX services.");
	}

	static private void describeMonitor(int index) {
		describeMonitor(index, null);
	}
	
	public static void printCurrentMonitor() {
		if (selectedMonitor >= 0) {
			NetworkShell.shellOutput.println("Current exertion monitoring service: ");
			describeMonitor(selectedMonitor);
		}
		else {
			NetworkShell.shellOutput.println("No selected EMX, use 'mxe' command");
		}
	}

	static private void describeMonitor(int index, String msg) {
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

	public static ServiceItem[] getEmxMonitors() {
		return emxMonitors;
	}

	public static ExertionInfo[] getExertionInfos() {
		return exertionInfos;
	}

	public static void setExertionInfos(ExertionInfo[] exertionInfos) {
		EmxCmd.exertionInfos = exertionInfos;
	}

	public static void setEmxMonitors(ServiceItem[] monitors) {
		emxMonitors = monitors;
	}

	public static Map<Uuid, ServiceItem> getMonitorMap() {
		return monitorMap;
	}

	static ServiceItem[] findMonitors() throws RemoteException {
		emxMonitors = ShellCmd.lookup(new Class[] { MonitoringManagement.class });
		return emxMonitors;
	}

	public static ArrayList<MonitoringManagement> getMonitors() {
		ArrayList<MonitoringManagement> emxList = new ArrayList<MonitoringManagement>();
		for (int i = 0; i < emxMonitors.length; i++) {
			emxList.add((MonitoringManagement) emxMonitors[i].service);
		}
		return emxList;
	}

	private State getStatus(String option) {
		if (option.equals("-r"))
			return State.RUNNING;
		else if (option.equals("-a"))
			return State.NULL;
		else if (option.equals("-f"))
			return State.FAILED;
		else if (option.equals("-d"))
			return State.DONE;
		else if (option.equals("-i"))
			return State.INITIAL;
		else if (option.equals("-u"))
			return State.SUSPENDED;
		else if (option.equals("-p"))
			return State.STOPPED;
		else if (option.equals("-s"))
			return State.INSPACE;
		else if (option.equals("-y"))
			return State.ASYNC;

		return State.NULL;
	}
}
