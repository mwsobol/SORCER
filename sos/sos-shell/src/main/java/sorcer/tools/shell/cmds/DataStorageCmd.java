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
import sorcer.core.provider.DatabaseStorer;
import sorcer.core.provider.DatabaseStorer.Store;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.*;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;
import sorcer.util.bdb.objects.ObjectInfo;
import sorcer.util.url.sos.SdbUtil;

import java.io.PrintStream;
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
				+ "\n  -x   clearSessions the selection of data storage service"
				+ "\n  -l   [<store fiType>] list all storage records or of a given fiType"
				+ "\n  -r   print the selected record"
				+ "\n  -s   save the selected record in a given file ";

	}

	static private PrintStream out;
	static private ServiceItem[] dataStorers;
	static private ObjectInfo[] recordInfos;
	static private int selectedDataStorer = -1;
	private Store selectedStore;
	private int selectedRecord = -1;
	static private Map<Uuid, ServiceItem> dataStorerMap = new HashMap<Uuid, ServiceItem>();

	public DataStorageCmd() {
	}

	public void execute(String... args) throws RemoteException, MonitorException, ServiceException {
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
		int myIdx = 0;
		String next = null;
		Store storeType = null;

		if (numTokens == 0) {
			printStorageServices();
			return;
		} else if (numTokens == 1) {
			next = myTk.nextToken();
			if (next.equals("-v")) {
				if (selectedDataStorer >= 0) {
					describeStorer(selectedDataStorer);
				} else
					out.println("No selected data storage");
				return;
			} else if (next.equals("-l")) {
				printRecords(Store.all);
			} else if (next.equals("-s")) {
				showStorageServices();
				selectedDataStorer = -1;
				// remove storage select
			} else if (next.equals("-x")) {
				selectedDataStorer = -1;
			} else if (next.equals("-r")) {
				if (selectedRecord >= 0) {
					ObjectInfo recordInfo = recordInfos[selectedRecord];
//					printRecord(recordInfo.uuid, recordInfo.fiType);
				}
			} else {
				try {
					myIdx = Integer.parseInt(next);
					selectedRecord = myIdx;
				} catch (NumberFormatException e) {
					selectedRecord = selectRecordByName(next);
				}
				if (selectedRecord < 0
						|| selectedRecord >= recordInfos.length)
					out.println("No such REcord for: " + next);
				else
					out.println(recordInfos[selectedRecord]);
			}
		} else if (numTokens == 2) {
			next = myTk.nextToken();
			if (next.equals("-s")) {
				try {
					next = myTk.nextToken();
					myIdx = Integer.parseInt(next);
					selectedDataStorer = myIdx;
				} catch (NumberFormatException e) {
					selectedDataStorer = selectMonitorByName(next);
					if (selectedDataStorer < 0)
						out.println("No such data storage for: " + next);
				}
				if (selectedDataStorer >= 0) {
					describeStorer(selectedDataStorer, "SELECTED");
				} else {
					out.println("No such data storage for: " + selectedDataStorer);
				}
				return;
			} else if (next.equals("-l")) {
				try {
					next = myTk.nextToken();
					myIdx = Integer.parseInt(next);
					selectedRecord = myIdx;
				} catch (NumberFormatException e) {
					selectedRecord = selectRecordByName(next);
					if (selectedRecord < 0)
						out.println("No such Record for: " + next);
				}
				if (selectedRecord >= 0
						&& selectedRecord < recordInfos.length) {
					ObjectInfo recordInfo = recordInfos[selectedRecord];
//					printRecord(recordInfo.uuid, recordInfo.fiType);
				} else
					out.println("No such Record for: " + selectedRecord);
			}
		} else {
			out.println(COMMAND_USAGE);
			return;
		}
	}

	private void printRecord(Uuid id, Store type) throws RemoteException, MonitorException {
		Routine xrt = null;
		if (selectedDataStorer >= 0) {
			xrt = ((MonitorUIManagement) dataStorers[selectedDataStorer].service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		} else {
			xrt = ((MonitorUIManagement) dataStorerMap.get(id).service)
					.getMonitorableExertion(id, NetworkShell.getPrincipal());
		}

		out.println("--------- STORAGE RECORD # " + selectedRecord + " ---------");
		out.println(((ServiceRoutine) xrt).describe());
	}

	private void printRecords(Store type) throws  ServiceException {
		try {
			if (dataStorers == null || dataStorers.length == 0) {
				findStorers();
			}
			Map<Uuid, ObjectInfo> all;
			if (selectedDataStorer >= 0) {
				out.println("From Data Storage: "
						+ AttributesUtil
						.getProviderName(dataStorers[selectedDataStorer].attributeSets)
						+ " at: "
						+ AttributesUtil
						.getHostName(dataStorers[selectedDataStorer].attributeSets));
//			all = ((StorageManagement) dataStorers[selectedDataStorer].service)
//					.getMonitorableExertionInfo(fiType,
//							NetworkShell.getPrincipal());

				Context cxt = null;

				store("test-only");
//				out.println("XXXXXXXXXXXXX service impl: " + dataStorers[selectedDataStorer]);
//				out.println("XXXXXXXXXXXXX service: " + (DatabaseStorer) dataStorers[selectedDataStorer].service);
//				out.println("XXXXXXXXXXXXX interfaces: " + Arrays.toString(dataStorers[selectedDataStorer].service.getClass().getInterfaces()));
//				out.println("XXXXXXXXXXXXX key: " + ((Provider) dataStorers[selectedDataStorer].service).getProviderName());
				cxt = ((DatabaseStorer) dataStorers[selectedDataStorer].service).contextList(SdbUtil.getListContext(Store.object));
//				out.println("XXXXXXXXXXXXX context: " + cxt);

				store("test-only");
				List<String> records = list(Store.object);
//				out.println("XXXXXXXXXXXXX records; " + records);
				out.println(cxt.getValue(DatabaseStorer.store_content_list));
			} else {
				Map<Uuid, ObjectInfo> ri = null;
				all = new HashMap<Uuid, ObjectInfo>();
				for (int i = 0; i < dataStorers.length; i++) {
					out.println("From Data Storage "
							+ AttributesUtil
							.getProviderName(dataStorers[i].attributeSets)
							+ " at: "
							+ AttributesUtil
							.getHostName(dataStorers[i].attributeSets));

					DatabaseStorer emx = (DatabaseStorer) dataStorers[i].service;
//					ri = emx.getMonitorableExertionInfo(fiType,
//						NetworkShell.getPrincipal());
					if (ri != null && ri.size() > 0) {
						all.putAll(ri);
					}
					// populate exertion/EMX map
					dataStorerMap.clear();
					for (ObjectInfo ei : ri.values()) {
						dataStorerMap.put(ei.uuid, dataStorers[i]);
					}
				}
			}
		} catch (ContextException | RemoteException | SignatureException | MalformedURLException e) {
			throw new MogramException(e);
		}
//		if (all.size() == 0) {
//			out.println("No monitored mograms at this time.");
//			return;
//		}
//		recordInfos = new RecordInfo[all.size()];
//		all.values().toArray(recordInfos);
//		printRecordInfos(recordInfos);
	}

	private void printRecordInfos(ObjectInfo[] recordInfos) {
		for (int i = 0; i < recordInfos.length; i++) {
			out.println("--------- RECORD # " + i + " ---------");
			out.println(recordInfos[i].describe());
		}
	}

	private void showStorageServices() throws RemoteException {
		findStorers();
		printStorageServices();
	}

	private void printStorageServices() {
		if (dataStorers==null || dataStorers.length==0) {
			try {
				findStorers();
			} catch (RemoteException re) {
				out.println("Problem retrieving data storers: " + re.getMessage());
			}
		}
		if ((dataStorers != null) && (dataStorers.length > 0)) {
			for (int i = 0; i < dataStorers.length; i++) {
				describeStorer(i);
			}
		} else
			System.out.println("Sorry, no fetched Data Storage services.");
	}

	static private void describeStorer(int index) {
		describeStorer(index, null);
	}
	
	public static void printCurrentStorer() {
		if (selectedDataStorer >= 0) {
			NetworkShell.shellOutput.println("Current data storage service: ");
			describeStorer(selectedDataStorer);
		}
		else {
			NetworkShell.shellOutput.println("No selected data storage, use 'ds -s' to list and select with 'ds #'");
		}
	}

	static private void describeStorer(int index, String msg) {
		out.println("---------" + (msg != null ? " " + msg : "")
				+ " DATA STORAGE SERVICE # " + index + " ---------");
		out.println("EMX: " + dataStorers[index].serviceID + " at: "
				+ AttributesUtil.getHostName(dataStorers[index].attributeSets));
		out.println("Home: "
				+ AttributesUtil.getUserDir(dataStorers[index].attributeSets));
		String groups = AttributesUtil
				.getGroups(dataStorers[index].attributeSets);
		out.println("Provider key: "
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


	public static void setDataStorers(ServiceItem[] storers) {
		dataStorers = storers;
	}

	public static Map<Uuid, ServiceItem> getDataStorerMap() {
		return dataStorerMap;
	}

	static ServiceItem[] findStorers() throws RemoteException {
		dataStorers = ShellCmd.lookup(new Class[] { DatabaseStorer.class });
		return dataStorers;
	}
	
//	static ServiceItem[] findStorers() {
//		return findStorers(false);
//	}
//
//	static ServiceItem[] findStorers(boolean newDiscovery) {
//		ServiceTemplate st = new ServiceTemplate(null,
//				new Class[] { StorageManagement.class }, null);
//		dataStorers = ServiceAccessor.getServiceItems(st, null,
//		// DiscoveryGroupManagement.ALL_GROUPS);
//				NetworkShell.getGroups());
//		return dataStorers;
//	}

	public static ArrayList<DatabaseStorer> getDataStorers() {
		ArrayList<DatabaseStorer> dataStorerList = new ArrayList<DatabaseStorer>();
		for (int i = 0; i < dataStorers.length; i++) {
			dataStorerList.add((DatabaseStorer) dataStorers[i].service);
		}
		return dataStorerList;
	}

	private Store getStoreType(String type) {
		Store storeType = Store.all;
		String option = type.toLowerCase();
		if (option == null)
			storeType = Store.all;
		else if (option.startsWith("c"))
			storeType = Store.context;
		else if (option.startsWith("a"))
			storeType = Store.all;
		else if (option.startsWith("e"))
			storeType = Store.exertion;
		else if (option.startsWith("t"))
			storeType = Store.table;
		else if (option.startsWith("v"))
			storeType = Store.var;
		else if (option.startsWith("m"))
			storeType = Store.varmodel;
		else if (option.startsWith("o"))
			storeType = Store.object;

		selectedStore = storeType;
		return storeType;
	}
	
}
