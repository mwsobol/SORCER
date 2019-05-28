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
import java.util.List;
import java.util.Map;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.*;
import sorcer.service.Exec.State;
import sorcer.service.ServiceRoutine;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

/**
 * @author Mike Sobolewski
 * prc the 'help sp' command at the nsh prompt
 */
public class SpaceCmd extends ShellCmd {

	{
		COMMAND_NAME = "sp";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "sp [-xrt | -sp | <Space/exertion index> | -t | -ta | -v | -x ]"
			+ "\n\t\t\t  | [ -a | -d | -f | -r | <exertion index>] "
			+ "\n\t\t\t  | (-e | -c | -cc | -ccc) [<exertion index>] [-s <filename>]";

		COMMAND_HELP = "Support for exertion spaces; 'space' mode"
				+ "\n  'no option'; show Routine Spaces, set the 'space' mode"
				+ "\n  <Space index>   select the Space given <Space index>"
				+ "\n  -sp set the 'space' mode" 
				+ "\n  -a   show all space mograms, set the 'exertion' mode"
				+ "\n  -d   show done space mograms, set the 'exertion' mode"
				+ "\n  -f   show failed space mograms, set the 'exertion' mode"
				+ "\n  -r   show running space exertion, set the 'exertion' modes"
				+ "\n  <exertion index>   select the exertion given <exertion index>"
				+ "\n  -xrt set the 'exertion' mode"
				+ "\n  -v   print the selected Space service/exertion"
				+ "\n  -x   clearSessions the selection of Space service/exertion"
				+ "\n  -t   take the selected exertion from the Space"
				+ "\n  -ta   take all mograms from the Space"
				+ "\n  -e   show the selected exertion"
				+ "\n  -c   print the data context of selected exertion"
				+ "\n  -cc   print the control context of selected exertion"
				+ "\n  -ccc   print both data and control contexts of selected exertion"
				+ "\n  -s   save the selected exertion in a given file ";
	}

	static private PrintStream out;
	private boolean isSpaceMode = true;
	static private ServiceItem[] spaces;
	static private int selectedSpace = -1;
	private int selectedExertion = -1;
	static private Map<Uuid, ServiceItem> monitorMap = new HashMap<Uuid, ServiceItem>();

	static private List<Entry> instanceList = new ArrayList<Entry>();
	static private JavaSpace05 javaSpace;
	
	public SpaceCmd() {
	}

	public void execute(String... args) throws RemoteException, MonitorException, ContextException {
		out = NetworkShell.getShellOutputStream();
		WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();
//		out.println("numTokens: " + numTokens);
		int myIdx = 0;
		String next = null;
		State xrtType = null;

		if (numTokens == 0) {
			showSpaces();
			if (spaces != null) {
				javaSpace = (JavaSpace05)spaces[0].service;
				selectedSpace = 0;
			}
			isSpaceMode = true;
		} else if (numTokens == 1) {
			next = myTk.nextToken();
			if (next.equals("-xrt")) {
				isSpaceMode = false;
				out.println("you are in 'xrt' mode");
				return;
			} if (next.equals("-sp")) {
				isSpaceMode = true;
				out.println("you are in 'sp' mode");
				return;
			} if (next.equals("-mode")) {
				if (isSpaceMode)
					out.println("you are in 'sp' mode");
				else
					out.println("you are in 'xrt' mode");
			} else if (next.equals("-v")) {
				if (isSpaceMode) {
					if (selectedSpace >= 0) 
						describeSpace(selectedSpace);
					else
						out.println("No selected Space");
				} else {
					if (selectedExertion >= 0) 
						printExertion(selectedExertion, true, true);
					else
						out.println("No selected exertion");
				}
			} else if (next.equals("-d") || next.equals("-r")
					|| next.equals("-f") || next.equals("-f")
					|| next.equals("-a") || next.equals("-i")) {
				isSpaceMode = false;
				xrtType = getStatus(next);
				printSpaceExertions(xrtType);
			} else if (next.equals("-t")) {
				takeSelectedExertion();
			} else if (next.equals("-ta")) {
				takeAll();
			} else if (next.equals("-x")) {
				if (isSpaceMode)
					selectedSpace = -1;
				else
					selectedExertion = -1;
			} else if (next.equals("-e")) {
				isSpaceMode = false;
				if (selectedExertion >= 0) {
					printExertion(selectedExertion, false, false);
				}
			} else if (next.equals("-c")) {
				isSpaceMode = false;
				if (selectedExertion >= 0) {
					printExertion(selectedExertion, true, false);
				}
			} else if (next.equals("-cc")) {
				isSpaceMode = false;
				if (selectedExertion >= 0) {
					printExertion(selectedExertion, false, true);
				}
			} else if (next.equals("-ccc")) {
				isSpaceMode = false;
				if (selectedExertion >= 0) {
					printExertion(selectedExertion, true, true);
				}
			} else {
				if (isSpaceMode) {
					try {
						myIdx = Integer.parseInt(next);
						selectedSpace = myIdx;
					} catch (NumberFormatException e) {
						selectedSpace = selectSpaceByName(next);
						if (selectedSpace < 0)
							out.println("No such Space for: " + next);
					}
					if (selectedSpace >= 0) {
						describeSpace(selectedSpace, "SELECTED");
					} else {
						out.println("No such Space for: " + selectedSpace);
					}
				} else {
					try {
						myIdx = Integer.parseInt(next);
						selectedExertion = myIdx;
					} catch (NumberFormatException e) {
						out.println("wrong exertion index");
						return;
					}
					if (selectedExertion < 0
							|| selectedExertion >= instanceList.size()) {
						out.println("No such Routine for: " + next);
					}
					else
						printExertion(selectedExertion, true, true);
				}
			}
		} else if (numTokens == 2) {
			next = myTk.nextToken();
			if (next.equals("-e") || next.equals("-c")
					|| next.equals("-cc") || next.equals("-ccc")) {
				isSpaceMode = false;
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
					out.println("wrong exertion index");
					return;
				}
				if (selectedExertion >= 0
						&& selectedExertion < instanceList.size()) {
					printExertion(selectedExertion, isContext,
							isControlContext);
				} else
					out.println("No such Routine for: " + selectedExertion);
			}
		} else {
			out.println(COMMAND_USAGE);
		}
	}

	private void takeSelectedExertion() {
		if (selectedSpace >= 0) {
			javaSpace = (JavaSpace05)spaces[0].service;
		} else {
			out.print("no Space selected");
			return;
		}
		if (selectedExertion >= 0 && selectedExertion < instanceList.size()) {
			ExertionEnvelop ee = new ExertionEnvelop();
			ee.exertionID = ((ExertionEnvelop)instanceList.get(selectedExertion)).exertionID;
			try {
				javaSpace.take(ee, null, JavaSpace.NO_WAIT);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			instanceList.clear();
			selectedExertion = -1;
		}
	}
	
	private void takeAll() {
		if (selectedSpace >= 0) {
			javaSpace = (JavaSpace05) spaces[0].service;
		} else {
			out.print("no Space selected");
			return;
		}
		ExertionEnvelop ee = new ExertionEnvelop();
		for (Entry e : instanceList) {
			ee.exertionID = ((ExertionEnvelop) e).exertionID;
			try {
				javaSpace.take(ee, null, JavaSpace.NO_WAIT);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		instanceList.clear();
		selectedExertion = -1;
	}

	private void printExertion(int index, boolean isContext, boolean isControlContext) throws ContextException {
		Routine xrt = ((ExertionEnvelop)instanceList.get(index)).exertion;
		out.println("--------- EXERTION # " + index + " ---------");
		out.println(((ServiceRoutine) xrt).describe());
		if (isContext) {
			out.println("\nData Context:");
			out.println(xrt.getContext());
		}
		if (isControlContext) {
			out.print("\nControl Context:");
			out.println(xrt.getControlContext());
		}
	}

	private void getSpaceEntries(Entry entry) {
		if (selectedSpace >= 0) {
			javaSpace = (JavaSpace05)spaces[0].service;
		} else {
			out.print("no Space selected");
			return;
		}
		ArrayList<Entry> templateList = new ArrayList<Entry>();
		templateList.add(entry);
		int tally = 0;
		int ueCount = 0;
		Entry e = null;

		try {
			MatchSet iter = javaSpace.contents(templateList, null, 5L * 60000L, Integer.MAX_VALUE);
			while (iter != null) {
				try {
					e = iter.next();
					if (e == null) 
						break;
					else {
						instanceList.add(e);
						tally++;
					}
				} catch (UnusableEntryException uee) {
					// add an UnusableEnrty
					uee.printStackTrace();
					ueCount++;
				}
			}
			if (iter != null) {
				Lease lease = iter.getLease();
				if (lease != null) {
					lease.cancel();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		out.print("number of instances: " + tally);
		out.print("\nnumber of unusable args: " + ueCount);
	}
	
	private void printSpaceExertions(State xrtType) throws ContextException {
//		out.println("xrtType: " + xrtType);
		instanceList.clear();
		ExertionEnvelop ee = new ExertionEnvelop();
		if (xrtType == State.NULL) {
			ee.state = null;
		} else if (xrtType == State.INITIAL) {
			ee.state = new Integer(Exec.INITIAL);
		} else if (xrtType == State.DONE) {
			ee.state = new Integer(Exec.DONE);
		} else if (xrtType == State.FAILED) {
			ee.state = new Integer(Exec.FAILED);
		} else if (xrtType == State.RUNNING) {
			ee.state = new Integer(Exec.RUNNING);
		}
		
		getSpaceEntries(ee);
		if (instanceList.size() > 0) {
			out.print("\n");
			for (int i = 0; i < instanceList.size(); i++) {
				printExertion(i, false, false);
			}
		} else 
			out.print("\n");
	}

	private void showSpaces() throws RemoteException {
		findSpaces();
		printSpaces();
	}
	
	private void printSpaces() {
		if ((spaces != null) && (spaces.length > 0)) {
			for (int i = 0; i < spaces.length; i++) {
				describeSpace(i);
			}
		} else
			System.out.println("Sorry, no fetched JavaSpace services.");
	}
	
	static ServiceItem[] findSpaces() throws RemoteException {
		spaces = ShellCmd.lookup(new Class[] { JavaSpace05.class });
		return spaces;
	}
	
	static private void describeSpace(int index) {
		describeSpace(index, null);
	}
	
	static private void describeSpace(int index, String msg) {
		out.println("---------" + (msg != null ? " " + msg : "")
				+ " SPACE # " + index + " ---------");
		out.println("ID: " + spaces[index].serviceID);
		printServiceName(spaces[index]);
		out.println("Proxy class: "
				+ spaces[index].service.getClass());
	}    
	
	static private void printServiceName(ServiceItem item) {
		Entry[] attributeSets = item.attributeSets;
		for (int i = 0; i < attributeSets.length; i++) {
			if (attributeSets[i] instanceof Name) {
				out.println("Tag: " + ((Name) attributeSets[i]).name);
				return;
			}
		}
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

		return State.NULL;
	}
	
	private int selectSpaceByName(String name) {
		for (int i = 0; i < spaces.length; i++) {
			String pn = AttributesUtil.getProviderName(spaces[i].attributeSets);
			if (pn == null)
				out.println("no such space: " +  name);
			else if (pn.equals(name))
				return i;
		}
		return -1;
	}
		
	public static void printCurrentSpace() {
		if (selectedSpace >= 0) {
			NetworkShell.shellOutput.println("Current Space: ");
			describeSpace(selectedSpace);
		}
		else {
			NetworkShell.shellOutput.println("No selected Space, use 'sp' command");
		}
	}

}
