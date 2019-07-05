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
import net.jini.id.Uuid;
import sorcer.core.monitor.MonitorUIManagement;
import sorcer.core.monitor.MonitoringManagement;
import sorcer.service.Exerter;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.*;
import sorcer.service.Exec.State;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;

@SuppressWarnings("unchecked")
public class EmxCmd extends ShellCmd {

	{
		COMMAND_NAME = "emx";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "emx [-xrt | -emx | -mode | <EMX/exertion index> | -v | -x]"
			+ "\n\t\t\t  | [ -a | -d | -f | -r | -y | <exertion index>] "
			+ "\n\t\t\t  | (-e | -c | -cc | -ccc) [<exertion index>] [-s <filename>]";

		COMMAND_HELP = "Support for monitoring runtime mograms;"
				+ "\n  -mode   show current mode"
				+ "\n  ('emx' mode) show EMX services or ('xrt' mode) print the selected exertion"
				+ "\n  <EMX index>   select the EMX given <EMX index>"
				+ "\n  -v   print the selected EMX service/exertion"
				+ "\n  -x   clearSessions the selection of MX service/exertion"
				+ "\n  -a   show all monitored mograms"
				+ "\n  -d   show done monitored mograms"
				+ "\n  -f   show failed monitored mograms"
				+ "\n  -r   show running monitored mograms"
				+ "\n  -y   show asynchronous monitored mograms"
				+ "\n  -e   print the selected exertion"
				+ "\n  <exertion index>   select the exertion given <exertion index>"
				+ "\n  -c   print the data context of selected exertion"
				+ "\n  -cc   print the control context of selected exertion"
				+ "\n  -ccc   print both data and control contexts of selected exertion"
				+ "\n  -s   save the selected exertion in a given file ";

	}

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static private PrintStream out;
	private boolean isEmxMode = true;
	static private ServiceItem[] emxMonitors;
	static private int selectedMonitor = -1;
	static private ExertionInfo[] exertionInfos = new ExertionInfo[0];
	private int selectedExertion = -1;
	static private Map<Uuid, ServiceItem> monitorMap = new HashMap<Uuid, ServiceItem>();

	public EmxCmd() {
	}

	public void execute(String... args) throws RemoteException, MonitorException, ContextException {
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
				// clearSessions monitor select
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
//						String oldNext = next;
//						next = myTk.nextToken();
//						if (next.length() == 0) {
//							out.println("Invalid command option: " + oldNext);
//							return;
//						}
						myIdx = Integer.parseInt(next);
						selectedMonitor = myIdx;
					} catch (NumberFormatException e) {
						selectedMonitor = selectMonitorByName(next);
						if (selectedMonitor < 0)
							out.println("No such EMX for: " + next);
						return;
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
						out.println("No such Subroutine for: " + next);
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
						out.println("No such Subroutine for: " + next);
				}
				if (selectedExertion >= 0
						&& selectedExertion < exertionInfos.length) {
					ExertionInfo xrtInfo = exertionInfos[selectedExertion];
					printExertion(xrtInfo.getStoreId(), isContext,
							isControlContext);
				} else
					out.println("No such Subroutine for: " + selectedExertion);
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}

	private void printExertion(Uuid id, boolean isContext,
			boolean isControlContext) throws RemoteException, MonitorException, ContextException {
        Subroutine xrt = null;
        if (emxMonitors == null || emxMonitors.length == 0) {
            findMonitors();
        }
        try {
            ((Exerter)emxMonitors[selectedMonitor]).getProviderName();
        } catch (Exception e) {
            findMonitors();
        }
		if (selectedMonitor >= 0) {
			xrt = ((MonitorUIManagement) emxMonitors[selectedMonitor].service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		} else {
            if (monitorMap.size()>0) xrt = ((MonitorUIManagement) monitorMap.get(id).service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		}

        out.println(ansi().render("@|blue ---------"
                + "|@ @|bold,blue EXERTION # " + selectedExertion + "|@ @|blue ---------|@"));
		out.println(ansi().render("@|bold " + ((ServiceRoutine) xrt).describe() + "|@"));
        if (isContext) {
			out.println(ansi().render("@|yellow " + "\nData Context:" + "|@"));
			out.println(ansi().render("@|bold,yellow " + xrt.getContext() + "|@"));
		}
		if (isControlContext) {
            out.println(ansi().render("@|green " + "\nControl Context:" + "|@"));
            out.println(ansi().render("@|bold,greeb " + xrt.getControlContext() + "|@"));
		}
	}

	private void printMonitoredExertions(State xetType)
			throws RemoteException, MonitorException {
		if (emxMonitors == null || emxMonitors.length == 0) {
			findMonitors();
		}
		try {
			((Exerter)emxMonitors[selectedMonitor]).getProviderName();
		} catch (Exception e) {
			findMonitors();
		}
		Map<Uuid, ExertionInfo> all;
		if (selectedMonitor >= 0) {


            out.println(ansi().render("From EMX @|bold " + AttributesUtil
                    .getProviderName(emxMonitors[selectedMonitor].attributeSets) + "|@ at: @|bold "
                    + AttributesUtil.getHostName(emxMonitors[selectedMonitor].attributeSets) + "|@"));

			all = ((MonitorUIManagement) emxMonitors[selectedMonitor].service)
					.getMonitorableExertionInfo(xetType,
							NetworkShell.getPrincipal());
		} else {
			Map<Uuid, ExertionInfo> hm;
			all = new HashMap<Uuid, ExertionInfo>();
            if (emxMonitors!=null) {
				monitorMap.clear();
				for (int i = 0; i < emxMonitors.length; i++) {

					out.println(ansi().render("From EMX @|bold " + AttributesUtil
							.getProviderName(emxMonitors[i].attributeSets) + "|@ at: @|bold "
							+ AttributesUtil.getHostName(emxMonitors[i].attributeSets) + "|@"));

					MonitorUIManagement emx = (MonitorUIManagement) emxMonitors[i].service;
					hm = emx.getMonitorableExertionInfo(xetType,
							NetworkShell.getPrincipal());
					if (hm != null && hm.size() > 0) {
						all.putAll(hm);
					}
					// populate exertion/EMX map
					for (ExertionInfo ei : hm.values()) {
						monitorMap.put(ei.getStoreId(), emxMonitors[i]);
					}
				}
			}
		}
		if (all.size() == 0) {
			out.println("No monitored mograms at this time.");
			return;
		}

        ArrayList<ExertionInfo> eInfos =  new ArrayList<ExertionInfo>(all.values());
        Collections.sort(eInfos);
        exertionInfos = new ExertionInfo[all.size()];
        eInfos.toArray(exertionInfos);
		printExerionInfos(exertionInfos);
	}

	private void printExerionInfos(ExertionInfo[] exertionInfos) {

		for (int i = 0; i < exertionInfos.length; i++) {
			out.println(ansi().render("@|blue ---------"
                    + "|@ @|bold,blue EXERTION # " + i + "|@ @|blue ---------|@"));

            String color = "bold ";
            switch (Exec.State.state(exertionInfos[i].getStatus())) {
                case INITIAL : color = "yellow ";
                    break;
                case INSPACE : color = "bold,yellow ";
                    break;
                case FAILED : color = "bold,red ";
                    break;
                case RUNNING: color = "red ";
                    break;
                case DONE: color = "green ";
                    break;
            }

            StringBuilder info = new StringBuilder().append("key: ").append("@|bold,green ").append(exertionInfos[i].getName()).append("|@");
            info.append("  ID: ").append("@|bold ").append(exertionInfos[i].getId()).append("|@");
            info.append("  state: ").append("@|").append(color).append(Exec.State.name(exertionInfos[i].getStatus())).append("|@");
            info.append("\ncreated at: ").append("@|yellow ").append((exertionInfos[i].getCreationDate() != null) ? sdf.format(exertionInfos[i].getCreationDate()) : "").append("|@");
            info.append(",  last updated at: ").append(exertionInfos[i].getLastUpdateDate());
            info.append("\nsignature: ").append("@|bold ").append(exertionInfos[i].getSignature()).append("|@");
            if (exertionInfos[i].getTrace().size()>0) info.append("\ntrace: ").append("@|bold,red ").append(exertionInfos[i].getTrace()).append("|@");
            out.println(ansi().render(info.toString()));
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
        out.println(ansi().render("@|blue ---------" + (msg != null ? " " + msg : "")
                + "|@ @|bold,blue EMX SERVICE # " +index + "|@ @|blue ---------|@"));
      	out.println(ansi().render("EMX: @|bold " + emxMonitors[index].serviceID + "|@ at: @|bold "
				+ AttributesUtil.getHostName(emxMonitors[index].attributeSets) + "|@"));
		out.println("Home: "
				+ AttributesUtil.getUserDir(emxMonitors[index].attributeSets));
		String groups = AttributesUtil
				.getGroups(emxMonitors[index].attributeSets);
		out.println(ansi().render("Provider key: @|bold "
				+ AttributesUtil
						.getProviderName(emxMonitors[index].attributeSets) + "|@"));
		out.println(ansi().render("Groups supported: @|bold " + groups + "|@"));
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
