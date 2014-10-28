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

import groovy.lang.GroovyRuntimeException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.EmptyConfiguration;
import net.jini.config.NoSuchEntryException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.entry.AbstractEntry;
import net.jini.lookup.entry.UIDescriptor;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.EvaluationException;
import sorcer.service.ExertionInfo;
import sorcer.tools.shell.cmds.ChgrpCmd;
import sorcer.tools.shell.cmds.DataStorageCmd;
import sorcer.tools.shell.cmds.DirCmd;
import sorcer.tools.shell.cmds.DiscoCmd;
import sorcer.tools.shell.cmds.EmxCmd;
import sorcer.tools.shell.cmds.ExecCmd;
import sorcer.tools.shell.cmds.ExertCmd;
import sorcer.tools.shell.cmds.GroovyCmd;
import sorcer.tools.shell.cmds.GroupsCmd;
import sorcer.tools.shell.cmds.LookupCmd;
import sorcer.tools.shell.cmds.SetPortCmd;
import sorcer.tools.shell.cmds.SpaceCmd;
import sorcer.tools.shell.cmds.StartStopCmd;
import sorcer.tools.shell.cmds.VarModelCmd;
import sorcer.tools.shell.cmds.iGridCmd;
import sorcer.tools.webster.Webster;
import sorcer.util.Sorcer;
import sorcer.util.TimeUtil;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

import com.sun.jini.config.Config;

/**
 * @author Mike Sobolewski
 * call the 'help' command at the nsh prompt
 */
public class NetworkShell implements DiscoveryListener {

	private static ArrayList<ServiceRegistrar> registrars = new ArrayList<ServiceRegistrar>();

	static private String shellName = "nsh";

	private static String request;

	final static String DISCOVERY_TIMEOUT = "disco-timeout";

	final static String GROUPS = "groups";

	final static String LOCATORS = "locators";

	final static String SYS_PROPS = "system-props";

	final static String LIST = "list-props";

	public static final String COMPONENT = "sorcer.tools.shell";

	static final String CONFIG_COMPONENT = NetworkShell.class.getName();

	public static Logger logger = Logger.getLogger(COMPONENT);

	protected static NetworkShell instance;

	protected static String[] groups;

	private static String userName;

	static private SorcerPrincipal principal;

	private static LoginContext loginContext;

	final static long startTime = System.currentTimeMillis();

	private static LookupDiscovery disco;

	private static TreeMap<String, ShellCmd> commandTable = new TreeMap<String, ShellCmd>();

	static final int MAX_MATCHES = 5;

	static final String CUR_VERSION = "1.2";

	static final String BUILTIN_QUIT_COMMAND = "quit,exit,bye";
	
	static final String UNKNOWN_COMMAND_MSG = "Sorry, unknown command.";

	static final String SYSTEM_PROMPT = "nsh> ";

	private final Map<String, Object> settings = new HashMap<String, Object>();

	static Configuration sysConfig;

	private String homeDir;

	static private File currentDir;

	private File shellLog;

	public static PrintStream shellOutput;

	private static BufferedReader shellInput;

	private static StringTokenizer shellTokenizer;

	private String hostName;

	private String hostAddress;

	private Webster webster;

	private String[] httpJars;
	
	private String[] httpRoots;
	
	// true for interactive shell
	private static boolean interactive = true;

	public static String nshUrl;
	
	private NetworkShell() {
		// do nothing, see buildInstance
	}
	
	public static synchronized String[] buildInstance(String... argv)
			throws Throwable {
		shellOutput = System.out;
		if (instance == null) {
			ensureSecurityManager();
			instance = new NetworkShell();
			instance.enumerateCommands();
			return initShell(argv);
		}
		return argv;
	}

	public static synchronized NetworkShell getInstance() {
		return instance;
	}

	/**
	 * Utility routine that sets a security manager if one isn't already
	 * present.
	 */
	protected synchronized static void ensureSecurityManager() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
	}

	static public void main(String argv[]) {
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		String curToken = null;
		//System.out.println("nsh main args: " + Arrays.toString(argv));	
		if (argv.length > 0) {
			if ((argv.length == 1 && argv[0].startsWith("--"))
					|| (argv.length == 2 && (argv[0].equals("-e"))
							|| argv[0].equals("-f") || argv[0].equals("-n")
							|| argv[0].equals("-version") || argv[0]
							.equals("-help"))) {
				interactive = false;
			} else {
				interactive = true;
			}
		}

		try {
			// default shellOutput
			shellOutput = System.out;
			if (interactive) {
				shellOutput.println("SORCER Network Shell (nsh " + CUR_VERSION
						+ ", JVM: " + System.getProperty("java.version"));
				shellOutput.println("Type 'quit' to terminate the shell");
				shellOutput.println("Type 'help' for command help");
			}
			
			argv = buildInstance(argv);
			if (!instance.interactive) {
				// System.out.println("main appMap: " + appMap);
				execNoninteractiveCommand(argv);
				shellOutput.println();
				System.exit(0);
			}
			if (argv.length == 1 && argv[0].indexOf("-") == 0) {
				shellOutput.println(UNKNOWN_COMMAND_MSG);
				shellOutput.flush();
				System.exit(1);
			}

			principal = new SorcerPrincipal(NetworkShell.getUserName());
			principal.setId(NetworkShell.getUserName());

			shellOutput.print(SYSTEM_PROMPT);
			shellOutput.flush();
			request = "";
			request = shellInput.readLine();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		ShellCmd cmd = null;
		nshUrl = getWebsterUrl();
		//System.out.println("main request: " + request);
		//ClassLoaderUtil.displayContextClassLoaderTree();    	
		
		while ((request.length() > 0 && BUILTIN_QUIT_COMMAND.indexOf(request) < 0)
				|| request.length() == 0) {
			shellTokenizer = new StringTokenizer(request);
			curToken = "";
			if (shellTokenizer.hasMoreTokens()) {
				curToken = shellTokenizer.nextToken();
			}
			try {
				if (commandTable.containsKey(curToken)) {
					cmd = (ShellCmd) commandTable.get(curToken);
					cmd.execute();
				}
				// admissible shortcuts in the 'synonyms' map
				else if (aliases.containsKey(curToken)) {
					String cmdName = aliases.get(curToken);
					int i = cmdName.indexOf(" -");
					if (i > 0) {
						request = cmdName;
						cmdName = cmdName.substring(0, i);
						shellTokenizer = new StringTokenizer(request);
					} else {
						request = cmdName + " " + request;
						cmdName = new StringTokenizer(cmdName).nextToken();
						shellTokenizer = new StringTokenizer(request);
					}
					cmd = (ShellCmd) commandTable.get(cmdName);
					cmd.execute();
				} else if (request.length() > 0) {
					if (request.equals("?")) {
						instance.listCommands();

					} else {
						shellOutput.println(UNKNOWN_COMMAND_MSG);
					}
				}
				shellOutput.print(SYSTEM_PROMPT);
				shellOutput.flush();
				String in = shellInput.readLine();
				// fore !! run the previous command
				if (!in.equals("!!")) {
					instance.request = in;
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
				try {
					request = shellInput.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		// shellOutput.println("Thanks for using the SORCER Network Shell!");
	}

	public static String getUserName() {
		return userName;
	}

	public static SorcerPrincipal getPrincipal() {
		return principal;
	}

	static public void startApplication(String appPath)
			throws EvaluationException, IOException, InterruptedException {
		shellOutput.println("nsh executing: " + appPath);
		CmdResult result = null;
		if (appPath.endsWith(".xml")) {
			result = ExecUtils.execCommand("ant -f " + appPath);
			shellOutput.println("cmd result out: " + result.getOut());
		} else {
			result = ExecUtils.execCommand(appPath);
			shellOutput.println("cmd result out: " + result.getOut());
		}
		if (result.getErr() != null && result.getErr().length() > 0)
			shellOutput.println("cmd result err: " + result.getErr());
		shellOutput.println("cmd exit value: " + result.getExitValue());

		if (result.getExitValue() != 0)
			throw new RuntimeException("Failed to start " + appPath);
	}

	static private void execNoninteractiveCommand(String args[])
			throws Throwable {
		// check for external commands
		System.out.println("nonintercative nsh: " + Arrays.toString(args));

		if (args[0].indexOf("--") == 0) {
			String path = nishAppMap.get(args[0].substring(2));
			startApplication(path);
			System.exit(0);
		}
		request = arrayToRequest(args);
		if (args.length == 1) {
			if (args[0].equals("-version")) {
				shellOutput.println("SORCER Network Shell (nsh " + CUR_VERSION
						+ ", JVM: " + System.getProperty("java.version"));
			} else if (args[0].equals("-help")) {
				ShellCmd cmd = (HelpCmd) commandTable.get("help");
				cmd.execute();
			}
		} else if (args.length == 2) {
			if (args[0].equals("-f") || args[0].equals("-n")) {
				// evaluate file
				ShellCmd cmd = (ShellCmd) commandTable.get("exert");
				cmd.execute();
			} else if (args[0].equals("-e")) {
				// evaluate command line expression
				ExertCmd cmd = (ExertCmd) commandTable.get("exert");
				// cmd.setScript(instance.getText(args[1]));
				cmd.setScript(ExertCmd.readFile(huntForTheScriptFile(args[1])));
				cmd.execute();
			}
		}
	}

	static public void setLookupDiscovery(String... ingroups) {
		disco = null;
		try {
			disco = new LookupDiscovery(LookupDiscovery.NO_GROUPS);
			disco.addDiscoveryListener(instance);
			if (ingroups == null || ingroups.length == 0) {
				// System.out.println("SORCER groups: " +
				// Arrays.toString(Sorcer.getGroups()));
				// disco.setGroups(Sorcer.getGroups());
				// disco.setGroups(LookupDiscovery.ALL_GROUPS);
				disco.setGroups(LookupDiscovery.ALL_GROUPS);
			} else {
				disco.setGroups(ingroups);
			}
		} catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void discovered(DiscoveryEvent evt) {
		ServiceRegistrar[] regs = evt.getRegistrars();
		// shellOutput.println("NOTICE: discovery made");
		// shellOutput.print(SYSTEM_PROMPT);
		// shellOutput.flush();
		for (int i = 0; i < regs.length; i++)
			if (!registrars.contains(regs[i]))
				registrars.add(regs[i]);
	}

	public void discarded(DiscoveryEvent evt) {
		ServiceRegistrar[] discardedReg = evt.getRegistrars();
		shellOutput.println("NOTICE: Discarded registrar:");

		for (int i = 0; i < discardedReg.length; i++) {
			int tpInt = registrars.indexOf(discardedReg[i]);
			shellOutput.println("\tindex: " + tpInt);
			registrars.remove(tpInt);
		}
		shellOutput.print(SYSTEM_PROMPT);
		shellOutput.flush();
	}

	public void enumerateCommands() {
		for (int i = 0; i < shellCommands.length; i++) {
			addToCommandTable(shellCommands[i], shellCmdClasses[i]);
			ShellCmd cmdInstance = commandTable.get(shellCommands[i]);
			String[] subCmds = toArray(cmdInstance.getCommandWord(), ", ");
			if (subCmds.length > 1) {
				for (String subCmd : subCmds) {
					commandTable.put(subCmd, cmdInstance);
					// System.out.println("subCmd " + subCmd);
				}
			}
		}
		// verify that the basic minimal commands are available
		for (int i = 0; i < shellCommands.length; i++)
			if (!commandTable.containsKey(shellCommands[i])
					&& ((ShellCmd) commandTable.get(shellCommands[i]))
							.getCommandWord().indexOf(shellCommands[i]) >= 0) {
				shellOutput.println("Missing basic command :  "
						+ shellCommands[i]);
				System.exit(1);
			}
	}

	public void addToCommandTable(String cmd, Class<?> inCls) {
		try {
			// System.out.println("creating command's instance - "
			// + inCls.getName() + " for " + cmd);
			ShellCmd cmdInstance = (ShellCmd) inCls.newInstance();
			commandTable.put(cmd, cmdInstance);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/*
	 * Initialize runtime settings
	 */
	private void initSettings(String[] groups, LookupLocator[] locators,
			long discoTimeout) throws ConfigurationException {
		settings.put(GROUPS, groups);
		settings.put(LOCATORS, locators);
		Properties props = new Properties();
		String iGridHome = Sorcer.getHome();
		if (iGridHome == null)
			throw new RuntimeException("IGRID_HOME must be set");
		props.put("java.protocol.handler.pkgs", "net.jini.url|sorcer.util.bdb.sos");
		Properties addedProps = getConfiguredSystemProperties();
		props.putAll(addedProps);
		Properties sysProps = System.getProperties();
		sysProps.putAll(props);
		System.setProperties(sysProps);
		settings.put(SYS_PROPS, props);
		settings.put(DISCOVERY_TIMEOUT, discoTimeout);
		if (homeDir == null)
			homeDir = System.getProperty("user.dir");
		currentDir = new File(homeDir);

		if (!interactive) {
			String logDirPath = System.getProperty(COMPONENT + "logDir",
					iGridHome + File.separator + "bin" + File.separator
							+ "shell" + File.separator + "logs");
		
			File logDir = new File(logDirPath);
			if (!logDir.exists()) {
				if (!logDir.mkdir()) {
					System.err.println("Unable to create log directory at "
							+ logDir.getAbsolutePath());
				}
			}

			shellLog = new File(logDir, "nsh.log");
			System.out.println("Created the nsh shel log file: " + shellLog.getAbsolutePath());
			if (shellLog.exists()) {
				shellLog.delete();
			}
			if (logDir.exists()) {
				try {
					System.setErr(new PrintStream(
							new FileOutputStream(shellLog)));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			System.err
					.println("===============================================");
			System.err.println(shellName
					+ " non-interactive network shell\n"
					+ "Log creation: " + new Date(startTime).toString() + "\n"
					+ "Operator: " + System.getProperty("user.name"));
			System.err
					.println("===============================================");
			if (addedProps.size() > 0) {
				StringBuilder buff = new StringBuilder();
				for (Map.Entry<Object, Object> entry : addedProps.entrySet()) {
					String key = (String) entry.getKey();
					String value = (String) entry.getValue();
					buff.append("\n");
					buff.append("    ").append(key).append("=").append(value);
				}
				System.err.println("Added System Properties {"
						+ buff.toString() + "\n}");
			}
		}

		setShellOutput((PrintStream) sysConfig.getEntry(CONFIG_COMPONENT,
				"output", PrintStream.class, System.out));
	}

	/*
	 * Set system properties from configuration
	 */
	private Properties getConfiguredSystemProperties()
			throws ConfigurationException {
		Configuration config = getConfiguration();
		Properties sysProps = new Properties();
		String[] systemProperties = (String[]) config.getEntry(
				"sorcer.tools.shell", "systemProperties", String[].class,
				new String[0]);
		if (systemProperties.length > 0) {
			if (systemProperties.length % 2 != 0) {
				System.err
						.println("systemProperties elements has odd length : "
								+ systemProperties.length);
			} else {
				for (int i = 0; i < systemProperties.length; i += 2) {
					String name = systemProperties[i];
					String value = systemProperties[i + 1];
					sysProps.setProperty(name, value);
				}
			}
		}
		return sysProps;
	}

	public String getCmd() {
		return request;
	}

	public static StringTokenizer getShellTokenizer() {
		return shellTokenizer;
	}

	public static BufferedReader getShellInputStream() {
		return shellInput;
	}

	public static PrintStream getShellOutputStream() {
		return shellOutput;
	}

	public static void setShellOutput(PrintStream outputStream) {
		shellOutput = outputStream;
	}

	public static LookupDiscovery getDisco() {
		return disco;
	}

	public static ArrayList<ServiceRegistrar> getRegistrars() {
		return registrars;
	}

	/**
	 * Get the Configuration
	 * 
	 * @return The SystemConfiguration
	 */
	public static synchronized Configuration getConfiguration() {
		if (sysConfig == null)
			sysConfig = EmptyConfiguration.INSTANCE;
		return sysConfig;
	}

	public String getHomeDir() {
		return homeDir;
	}

	public void setHomeDir(String homeDir) {
		this.homeDir = homeDir;
	}

	public File getCurrentDir() {
		return currentDir;
	}

	public void setCurrentDir(File currentDir) {
		this.currentDir = currentDir;
	}

	public File getShellLog() {
		return shellLog;
	}

	public void setShellLog(File shellLog) {
		this.shellLog = shellLog;
	}

	public static boolean isCommandLine() {
		return interactive;
	}

	protected static class HelpCmd extends ShellCmd {

		{
			COMMAND_NAME = "help";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "help <command> | ? ";

			COMMAND_HELP = "Describes the Network Shell (nsh) commands";
		}

		public void execute() {		
			// noninteractive shell
			if (shellTokenizer == null) {
				instance.listCommands();
				return;
			}
	
			if (shellTokenizer.countTokens() > 0) {
				String option = shellTokenizer.nextToken();
				if (commandTable.get(option) != null) {
					shellOutput.println("Usage: "
							+ commandTable.get(option).getUsage(option) + "\n");
					shellOutput.println(commandTable.get(option)
							.getLongDescription(option));
				} else
					shellOutput.print("unknown command for " + option + "\n");
			} else {
				instance.listCommands();
			}
		}

	}

	protected static class ClearCmd extends ShellCmd {

		{
			COMMAND_NAME = "clear";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "clear -a | -p | -m | -e";

			COMMAND_HELP = "Clear fetched from the network resorces;"
					+ "  -a   clear all cached resources"
					+ "  -p   clear service providers"
					+ "  -m   clear EMX providers"
					+ "  -e   clear monitored exertion infos";
		}
		
		public void execute() throws IOException, InterruptedException {
			if (shellOutput == null)
				throw new NullPointerException(
						"Must have an output PrintStream");
			if (shellTokenizer.countTokens() == 1) {
				String option = shellTokenizer.nextToken();
				if (option.equals("-a")) {
					LookupCmd.getServiceItems().clear();
					EmxCmd.setEmxMonitors(new ServiceItem[0]);
					EmxCmd.getMonitorMap().clear();
					EmxCmd.setExertionInfos(new ExertionInfo[0]);
				} else if (option.equals("-p")) {
					LookupCmd.getServiceItems().clear();
				} else if (option.equals("-m")) {
					EmxCmd.setEmxMonitors(new ServiceItem[0]);
					EmxCmd.getMonitorMap().clear();
				} else if (option.equals("-e")) {
					EmxCmd.setExertionInfos(new ExertionInfo[0]);
				}
			} else {
				shellOutput.println(COMMAND_USAGE);
			}
		}
	}

	protected static class EditCmd extends ShellCmd {

		{
			COMMAND_NAME = "edit";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "edit [<filename>]";

			COMMAND_HELP = "Open the default editor (NSH_EDITOR) or a file <filename>, "
					+ "on Mac /Applications/TextEdit.app/Contents/MacOS/TextEdit can be used.";
		}

		public void execute() throws IOException, InterruptedException {
			final String cmd = getEditorCmd();
			Thread edt = new Thread(new Runnable() {
				public void run() {
					try {
						ExecUtils.execCommand(cmd);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			edt.setDaemon(true);
			edt.start();
		}

		static private String getEditorCmd() {
			String cmd = System.getenv("NSH_EDITOR");
			if (cmd == null) {
				cmd = appMap.get("EDITOR");
			}
			if (cmd == null) {
				throw new NullPointerException(
						"No editor specified for this shell!");
			}
			if (shellTokenizer.countTokens() > 0) {
				String option = shellTokenizer.nextToken();
				if (option != null && option.length() > 0) {
					try {
						cmd = System.getenv("NSH_EDITOR") + " "
								+ currentDir.getCanonicalPath()
								+ File.separator + option;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return cmd;
		}
	}

	/**
	 * Handle stats command
	 */
	protected static class InfoCmd extends ShellCmd {

		{
			COMMAND_NAME = "about";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "about [-a]";

			COMMAND_HELP = "Show properties of this 'nsh' shell;"
					+ "  -a   list available external applications";
		}

		public void execute() throws IOException, ClassNotFoundException {
			if (shellOutput == null)
				throw new NullPointerException(
						"Must have an output PrintStream");
			long currentTime = System.currentTimeMillis();
			shellOutput.println("Network Shell  nsh " + CUR_VERSION);
			shellOutput.println("  User: " + System.getProperty("user.name"));
			shellOutput.println("  Home directory: " + instance.getHomeDir());
			shellOutput.println("  Current directory: " + currentDir);

			shellOutput
					.println("  Login time: " + new Date(startTime).toString());
			shellOutput.println("  Time logged in: "
					+ TimeUtil.format(currentTime - startTime));
			if (instance.getShellLog() != null)
				shellOutput.println("  Log file : "
						+ instance.getShellLog().getAbsolutePath());
			
			shellOutput.println();
			Webster web = instance.getWebster();
			if (web == null) {
				shellOutput.println("Class server: No HTTP server started");
			} else {
				shellOutput.println("Webster URL: \n  URL: http://"
						+ web.getAddress() + ":" + web.getPort()
						+ "\n  Roots: " + web.getRoots());
				shellOutput.println("  Codebase: " +  System.getProperty("java.rmi.server.codebase"));
			}
			shellOutput.println();
			
			shellOutput
					.println("Lookup groups: "
							+ (groups == null ? "all groups" : Arrays
									.toString(groups)));
			
			DiscoCmd.printCurrentLus();
			EmxCmd.printCurrentMonitor();
			VarModelCmd.printCurrentModel();
			DataStorageCmd.printCurrentStorer();
			LookupCmd.printCurrentService();

			if (shellTokenizer.countTokens() == 1) {
				String option = shellTokenizer.nextToken();
				if (option.equals("-a")) {
					shellOutput.println("Available applications: ");
					Iterator<Map.Entry<String, String>> mi = appMap.entrySet()
							.iterator();
					Map.Entry<String, String> e;
					while (mi.hasNext()) {
						e = mi.next();
						shellOutput.println("  " + e.getKey() + "  at: "
								+ e.getValue());
					}
				}
				// } else {
				// shellOutput.println(COMMAND_USAGE);
			}
		}
	}

	/**
	 * Handle http command
	 */
	public static class HttpCmd extends ShellCmd {

		{
			COMMAND_NAME = "http";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "http [port=<port-num>] [roots=<roots>] [jars=<codebase jars>] | stop";

			COMMAND_HELP = "Start and stop the nsf shell's code server;" 
					+ "  <roots> is semicolon separated list of directories.\n"
					+ "  If not provided the root directory will be:\n"
					+ "  [" + debugGetDefaultRoots() + "]";
		}

		public void execute() {
			if (shellOutput == null)
				throw new NullPointerException(
						"Must have an output PrintStream");
			StringTokenizer tok = new StringTokenizer(request);
			if (tok.countTokens() < 1)
				shellOutput.print(getUsage("http"));
			int port = 0;
			//String roots = null;
			String[] jars = null;
			/* The first token is the "http" token */
			tok.nextToken();
			while (tok.hasMoreTokens()) {
				String value = tok.nextToken();
				if (value.equals("stop")) {
					if (instance.webster == null) {
						shellOutput.print("No HTTP server running\n");
					} else {
						stopWebster();
						shellOutput.print("Command successful\n");
					}
				}
				if (value.startsWith("port")) {
					StringTokenizer tok1 = new StringTokenizer(value, " =");
					if (tok1.countTokens() < 2)
						shellOutput.print(getUsage("http"));
					/* First token will be "port" */
					tok1.nextToken();
					/* Next token must be the port value */
					String sPort = tok1.nextToken();
					try {
						port = Integer.parseInt(sPort);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						shellOutput.print("Bad port-number value : " + sPort
								+ "\n");
					}
				}
				if (value.startsWith("roots")) {
					String[] values = value.split("=");
					String rootArg = values[1].trim();
					instance.httpRoots = toArray(rootArg, " \t\n\r\f,;");
				}
				if (value.startsWith("jars")) {
						String[] values = value.split("=");
						String jarsArg = values[1].trim();
						instance.httpJars = toArray(jarsArg, " \t\n\r\f,;");
				}
			}
			if (instance.webster != null) {
				shellOutput.print("An HTTP server is already running on port "
						+ "[" + instance.webster.getPort() + "], "
						+ "serving [" + instance.webster.getRoots()
						+ "], stop this " + "and continue [y/n]? ");
				if (shellInput == null)
					shellInput = new BufferedReader(new InputStreamReader(
							System.in));
				try {
					String response = shellInput.readLine();
					if (response != null) {
						if (response.startsWith("y")
								|| response.startsWith("Y")) {
							stopWebster();
							if (createWebster(port, instance.httpRoots, instance.httpJars, shellOutput))
								shellOutput.println("Command successful\n");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					shellOutput.print("Problem reading user input, "
							+ "Exception :" + e.getClass().getName() + ": "
							+ e.getLocalizedMessage() + "\n");
				}
			} else {
				if (createWebster(port, instance.httpRoots, instance.httpJars, shellOutput))
					shellOutput.println("Command successful\n");
			}
		}

		/**
		 * Create a Webster instance
		 * 
		 * @param port
		 *            The port to use
		 * @param roots
		 *            Webster's roots
		 * @param out
		 *            A print stream for output
		 * 
		 * @return True if created
		 */
		public boolean createWebster(final int port, final String[] roots,
				String[] jars, PrintStream out) {
			return (createWebster(port, roots, false, jars, out));
		}

		public static String debugGetDefaultRoots() {
			String sorcerLibDir = Sorcer.getHome() + File.separator
					+ "lib" + File.separator + "sorcer" + File.separator
					+ "lib";
			String sorcerLibDLDir = Sorcer.getHome()
					+ File.separator + "lib" + File.separator + "sorcer"
					+ File.separator + "lib-dl";
			String sorcerExtDir = Sorcer.getHome() + File.separator
					+ "lib" + File.separator + "sorcer" + File.separator
					+ "lib-ext";
			return (sorcerLibDir + ";" + sorcerLibDLDir + ";" + sorcerExtDir);
		}

		/**
		 * Create a Webster instance
		 * 
		 * @param port
		 *            The port to use
		 * @param  
		 *            Webster's roots
		 * @param quiet
		 *            Run without output
		 * @param out
		 *            A print stream for output
		 * 
		 * @return True if created
		 */
		public static boolean createWebster(final int port, final String[] roots,
				boolean quiet, String[] jars, PrintStream out) {
			if (out == null)
				throw new NullPointerException(
						"Must have an output PrintStream");
			try {
				String sorcerLibDir = Sorcer.getHome()
						+ File.separator + "lib" + File.separator + "sorcer"
						+ File.separator + "lib";
				String sorcerLibDLDir = Sorcer.getHome()
						+ File.separator + "lib" + File.separator + "sorcer"
						+ File.separator + "lib-dl";
				String sorcerExtDir = Sorcer.getHome()
						+ File.separator + "lib" + File.separator + "sorcer"
						+ File.separator + "lib-ext";

				String[] systemRoots = { sorcerLibDir, sorcerLibDLDir, sorcerExtDir };
				String[] realRoots = (roots == null ? systemRoots : roots);
				
				instance.webster = new Webster(port, realRoots,
						instance.hostAddress, true);

				//System.out.println("webster: " + instance.hostAddress + ":"
						//+ port);
				//System.out.println("webster roots: " + realRoots);
			} catch (Exception e) {
				e.printStackTrace();
				out.println("Problem creating HTTP server, " + "Exception :"
						+ e.getClass().getName() + ": "
						+ e.getLocalizedMessage() + "\n");
				
				return (false);
			}

			setCodeBase(jars);
			
			if (!quiet) {
				out.println("Webster URL: http://" + instance.webster.getAddress()
						+ ":" + instance.webster.getPort());
				out.println("  Roots: " + instance.webster.getRoots());
			}
			return (true);
		}
		
		public static void setCodeBase(String[] jars) {
			int port = instance.webster.getPort();
			String localIPAddress = instance.webster.getAddress();
			String codebase = "";
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < jars.length - 1; i++) {
				sb.append("http://").append(localIPAddress).append(":")
						.append(port).append("/").append(jars[i]).append(" ");
			}
			sb.append("http://").append(localIPAddress).append(":")
					.append(port).append("/").append(jars[jars.length - 1]);
			codebase = sb.toString();
			System.setProperty("java.rmi.server.codebase", codebase);
			if (logger.isLoggable(Level.FINE))
				logger.fine("Setting nsh 'java.rmi.server.codebase': "
						+ codebase);
		}

		/**
		 * Stop the webster instance
		 */
		public static void stopWebster() {
			if (instance.webster != null)
				instance.webster.terminate();
			instance.webster = null;
		}
	}

	public static String getWebsterUrl() {
		return "http://" + instance.webster.getAddress() + ":"
				+ instance.webster.getPort();
	}
	
	private void listCommands() {
		shellOutput
				.println("You can manage the environment and interact with the service network using the following commands:");
		StringBuilder buffer = new StringBuilder();
		for (Map.Entry<String, ShellCmd> e : commandTable.entrySet()) {
			buffer.append("\n\t" + e.getKey());
			if (((String) e.getKey()).length() > 5)
				buffer.append(": \t" + e.getValue().getUsage(e.getKey()));
			else
				buffer.append(": \t\t" + e.getValue().getUsage(e.getKey()));
		}
		shellOutput.println(buffer.toString());
		shellOutput
				.println("\nFor help on any of these commands type 'help [<command>]'." 
						+ "\nTo leave this program type 'quit'");
	}
	

	/**
	 * Initialize the nsh, parsing arguments, loading configuration.
	 * 
	 * @param args
	 *            Command line arguments to parse, must not be null
	 * 
	 * @return Array of string args to be parsed
	 * 
	 * @throws Throwable
	 *             if anything at all happens that is not supposed to happen
	 */
	public static String[] initShell(final String[] args) throws Throwable {
		if (args == null)
			throw new NullPointerException("  args is null");
		final LinkedList<String> commandArgs = new LinkedList<String>();
		commandArgs.addAll(Arrays.asList(args));
		// System.out.println("initShell args: " + Arrays.toString(args));
		// default initialization file
		String nshConfigDir = Sorcer.getHome() + File.separator
				+ "bin" + File.separator + "shell" + File.separator + "configs";
		String configFilename = nshConfigDir + File.separator
				+ "nsh-init.config";
		// if initialization file not found look at the user home
		if (configFilename == null || configFilename.length() == 0) {
			configFilename = System.getenv("HOME") + File.separator + ".nsh"
					+ File.separator + "configs" + File.separator
					+ "nsh-init.config";
		}	
		if (args.length >= 1) {
			if (args[0].endsWith(".config") || args[0].endsWith(".groovy")) {
				configFilename = args[0];
				commandArgs.removeFirst();
			}
		}
		// System.out.println("initShell configFilename: " + configFilename);

		File configf = new File(configFilename);
		if (!configf.exists()) {
			System.err.println("No nsh configuration file: " + configf
					+ " in: " + new File(".").getCanonicalPath());
		} else {
			sysConfig = ConfigurationProvider
					.getInstance(new String[] { configFilename }, Thread.currentThread().getContextClassLoader().getParent());
		}

		if (sysConfig == null)
			sysConfig = EmptyConfiguration.INSTANCE;

		userName = (String) sysConfig.getEntry(CONFIG_COMPONENT, "userName",
				String.class, null);
		// System.out.println("userName: " + userName);
		loginContext = null;
		try {
			loginContext = (LoginContext) Config.getNonNullEntry(sysConfig,
					CONFIG_COMPONENT, "loginContext", LoginContext.class);
		} catch (NoSuchEntryException e) {
			// leave null
		}
		String[] result = null;
		if (loginContext != null) {
			loginContext.login();
			try {
				result = Subject.doAsPrivileged(loginContext.getSubject(),
						new PrivilegedExceptionAction<String[]>() {
							public String[] run() throws Exception {
								try {
									return (initialize(args, commandArgs));
								} catch (Throwable e) {
									throw new RuntimeException(e);
								}
							}
						}, null);
			} catch (PrivilegedActionException e) {
				throw e.getCause();
			}
		} else {
			result = initialize(args, commandArgs);
		}
		return result;
	}

	static String[] initialize(String[] args, LinkedList<String> commandArgs)
			throws Throwable {
		shellOutput = (PrintStream) sysConfig.getEntry(CONFIG_COMPONENT,
				"output", PrintStream.class, System.out);
		shellInput = new BufferedReader(new InputStreamReader(System.in));
		groups = (String[]) sysConfig.getEntry(CONFIG_COMPONENT, "groups",
				String[].class, DiscoveryGroupManagement.ALL_GROUPS);
		String[] apps = (String[]) sysConfig.getEntry(
				CONFIG_COMPONENT, "applications", String[].class,
				new String[0]);
		if (apps != null && apps.length >= 2) {
			appendApps(apps);
		}

		LookupLocator[] locators = (LookupLocator[]) sysConfig.getEntry(
				CONFIG_COMPONENT, "locators", LookupLocator[].class, null);

		long discoveryTimeout = (Long) sysConfig.getEntry(CONFIG_COMPONENT,
				"discoveryTimeout", long.class, (long) 1000 * 5);

		int httpPort = (Integer) sysConfig.getEntry(CONFIG_COMPONENT,
				"httpPort", int.class, 0);

		boolean noHttp = (Boolean) sysConfig.getEntry(CONFIG_COMPONENT,
				"noHttp", boolean.class, Boolean.FALSE);
		
		String[] roots = (String[]) sysConfig.getEntry(CONFIG_COMPONENT,
				"httpRoots", String[].class, new String[] { });
		
		String[] jars = (String[]) sysConfig.getEntry(CONFIG_COMPONENT,
				"httpJars", String[].class, new String[] { });
		/*
		 * Look to see if the user has provided a starting directory, groups,
		 * locators, discovery timeout, httpPort or ignore http
		 */
		String homeDir = null;

		for (String arg : args) {
			if (arg.startsWith("homeDir")) {
				String[] values = arg.split("=");
				homeDir = values[1].trim();
				commandArgs.remove(arg);
			} else if (arg.startsWith("groups")) {
				String[] values = arg.split("=");
				String groupsArg = values[1].trim();
				groups = toArray(groupsArg, " \t\n\r\f,");
				for (int j = 0; j < groups.length; j++) {
					if (groups[j].equalsIgnoreCase("all")) {
						groups = DiscoveryGroupManagement.ALL_GROUPS;
						break;
					}
				}
				commandArgs.remove(arg);
			} else if (arg.startsWith("jars")) {
				String[] values = arg.split("=");
				String jarsArg = values[1].trim();
				jars = toArray(jarsArg, " ,;");
				commandArgs.remove(arg);
			} else if (arg.startsWith("roots")) {
				String[] values = arg.split("=");
				String rootArg = values[1].trim();
				roots = toArray(rootArg, " ,;");
				commandArgs.remove(arg);
			} else if (arg.startsWith("locators")) {
				String[] values = arg.split("=");
				String locatorsArg = values[1].trim();
				String[] locatorArray = toArray(locatorsArg, " \t\n\r\f,");
				List<LookupLocator> list = new ArrayList<LookupLocator>();
				if (locators != null) {
					list.addAll(Arrays.asList(locators));
				}
				for (String aLocatorArray : locatorArray) {
					list.add(new LookupLocator(aLocatorArray));
				}
				locators = list.toArray(new LookupLocator[list.size()]);
				commandArgs.remove(arg);
			} else if (arg.startsWith("discoveryTimeout")) {
				String[] values = arg.split("=");
				String timeoutArg = values[1].trim();
				discoveryTimeout = Long.parseLong(timeoutArg);
				commandArgs.remove(arg);
			} else if (arg.startsWith("httpPort")) {
				String[] values = arg.split("=");
				String httpPortArg = values[1].trim();
				httpPort = Integer.parseInt(httpPortArg);
				commandArgs.remove(arg);
			} else if (arg.startsWith("-noHttp")) {
				noHttp = true;
				commandArgs.remove(arg);
			}
		}

		if (instance.homeDir == null)
			instance.homeDir = System.getProperty("user.dir");
		if (instance.currentDir == null)
			instance.currentDir = new File(instance.homeDir);

		/* Reset the args parameter, removing the config parameter */
		args = commandArgs.toArray(new String[commandArgs.size()]);
		instance.initSettings(groups, locators, discoveryTimeout);
		setLookupDiscovery(groups);

		if (homeDir != null) {
			DirCmd.changeDir(homeDir, false, shellOutput);
		}

		InetAddress inetAddress = InetAddress.getLocalHost();
		instance.hostName = inetAddress.getHostName();
		instance.hostAddress = inetAddress.getHostAddress();
		instance.httpJars = jars;
		instance.httpRoots = roots;

		if (!noHttp && instance.interactive) {
			HttpCmd.createWebster(httpPort, roots, false, instance.httpJars, shellOutput);
		}

		return args;
	}

	public static String[] getGroups() {
		return groups;
	}

	public static void setGroups(String[] groups) {
		NetworkShell.groups = groups;
	}

	private static Object[] toArray(String s) {
		return toArray(s, null);
	}

	public static String[] toArray(String s, String delim) {
		StringTokenizer tok;
		if (delim == null)
			tok = new StringTokenizer(s);
		else
			tok = new StringTokenizer(s, delim);
		String[] array = new String[tok.countTokens()];
		int i = 0;
		while (tok.hasMoreTokens()) {
			array[i++] = tok.nextToken();
		}
		return array;
	}

	public static String arrayToRequest(Object array) {
		if (array == null)
			return "null";
		else if (!array.getClass().isArray()) {
			return array.toString();
		}
		int length = Array.getLength(array);
		if (length == 0)
			return "";

		StringBuffer buffer = new StringBuffer();
		int last = length - 1;
		Object obj;
		for (int i = 0; i < length; i++) {
			obj = Array.get(array, i);
			if (obj == null)
				buffer.append("null");
			else if (obj.getClass().isArray())
				buffer.append(arrayToRequest(obj));
			else
				buffer.append(obj);

			if (i == last)
				buffer.append("");
			else
				buffer.append(" ");
		}
		return buffer.toString();
	}

	public String getText(String urlOrFilename) throws IOException {
		if (isScriptUrl(urlOrFilename)) {
			try {
				return DefaultGroovyMethods.getText(new URL(urlOrFilename));
			} catch (Exception e) {
				throw new GroovyRuntimeException(
						"Unable to get script from URL: ", e);
			}
		}
		return DefaultGroovyMethods
				.getText(huntForTheScriptFile(urlOrFilename));
	}

	private boolean isScriptUrl(String urlOrFilename) {
		return urlOrFilename.startsWith("http://")
				|| urlOrFilename.startsWith("https://")
				|| urlOrFilename.startsWith("file:");
	}

	/**
	 * Hunt for the script file, doesn't bother if it is named precisely.
	 * 
	 * Tries in this order: - actual supplied name - name.ex - name.exertlet -
	 * name.netlet - name.net - name.groovy - name.gvy - name.gy - name.gsh
	 * 
	 * @throws IOException
	 */
	static public File huntForTheScriptFile(String input) throws IOException {
		String scriptFileName = input.trim();
		File scriptFile = new File(scriptFileName);
		String[] standardExtensions = { ".xrt", ".exertlet", ".netlet", ".net",
				".groovy", ".gvy", ".gy", ".gsh" };
		int i = 0;
		while (i < standardExtensions.length && !scriptFile.exists()) {
			scriptFile = new File(scriptFileName + standardExtensions[i]);
			i++;
		}
		// if we still haven't found the file, point back to the originally
		// specified filename
		if (!scriptFile.exists()) {
			scriptFile = new File(scriptFileName);
			shellOutput.println("No such file: "
					+ new File(scriptFileName).getCanonicalPath());
		}
		return scriptFile;
	}

	public static TreeMap<String, ShellCmd> getCommandTable() {
		return commandTable;
	}

	public static String getRequest() {
		return request;
	}

	public static void setRequest(String request) {
		NetworkShell.request = request;
	}

	static public void printLookupAttributes(Entry[] attributeSets)
			throws IOException, ClassNotFoundException {
		if (attributeSets.length > 0) {
			PrintStream out = NetworkShell.getShellOutputStream();
			out.println("Lookup attributes:");
			for (int k = 0; k < attributeSets.length; k++) {
				if (attributeSets[k] instanceof UIDescriptor) {
					out.println("  "
					// + attributeSets[k].getClass()
					// + "UIDescriptor: "
							+ ((MarshalledObject) ((UIDescriptor) attributeSets[k]).factory)
									.get());
				} else if (attributeSets[k] instanceof SorcerServiceInfo) {
					printSorcerServiceInfo((SorcerServiceInfo) attributeSets[k]);
				} else {
					if (attributeSets[k] instanceof AbstractEntry) {
						out.println("  "
								+ toShortString((AbstractEntry) attributeSets[k]));
					} else {
						out.println("  " + attributeSets[k]);
					}
				}
			}
		}
	}

	public static Map<String, String> getAppMap() {
		return appMap;
	}
	
	public static void appendApps(String[] apps) {
		for (int i=0; i<apps.length; i=i+2) {
			appMap.put(apps[i], apps[i+1]);
		}
	}
	
	public static void appendNishApps(String[] apps) {
		for (int i=0; i<apps.length; i=i+2) {
			nishAppMap.put(apps[i], apps[i+1]);
		}
	}
	
	public Webster getWebster() {
		return webster;
	}

	public static String toShortString(AbstractEntry entry) {
		String str = entry.toString();
		int start = str.indexOf("(");
		return str.substring(start + 1, str.length() - 1);
	}

	public static void printSorcerServiceInfo(SorcerServiceInfo serviceInfo) {
		PrintStream out = NetworkShell.getShellOutputStream();
		out.println("  description: " + serviceInfo.shortDescription);
		out.println("  published services: "
				+ Arrays.toString(serviceInfo.publishedServices));
		out.println("  Space enabled: " + serviceInfo.puller);
		out.println("  EMX enabled: " + serviceInfo.monitorable);
		out.println("  location: " + serviceInfo.location);
		out.println("  repository: " + serviceInfo.serviceHome);
		out.println("  host name: " + serviceInfo.hostName);
		out.println("  host address: " + serviceInfo.hostAddress);
		out.println("  provider groups: " + serviceInfo.groups);
		out.println("  space group: " + serviceInfo.spaceGroup);
	}
	
	static Map<String, String> aliases;

	{
		aliases = new HashMap<String, String>();
		aliases.put("xrt", "exert");
		aliases.put("cls", "clear");
		aliases.put("ed", "edit");
		aliases.put("igh", "ig -h");
		aliases.put("ign", "ig -n");
		aliases.put("igd", "ig -d");
		aliases.put("more", "exec");
		aliases.put("cat", "exec");
		aliases.put("less", "exec");
	}

	static final String[] shellCommands = { "stop", "disco", "ls", "chgrp",
			"groups", "lup", "chgrp", "chport", "help", "exert", "http", "emx",
			"gvy", "edit", "clear", "exec", "about", "ig", "ds", "vm", "sp" };

	static final Class[] shellCmdClasses = { StartStopCmd.class, DiscoCmd.class,
			DirCmd.class, ChgrpCmd.class, GroupsCmd.class, LookupCmd.class,
			ChgrpCmd.class, SetPortCmd.class, HelpCmd.class, ExertCmd.class,
			HttpCmd.class, EmxCmd.class, GroovyCmd.class, EditCmd.class,
			ClearCmd.class, ExecCmd.class, InfoCmd.class, iGridCmd.class, DataStorageCmd.class, 
			VarModelCmd.class, SpaceCmd.class };

	// a map of application name/ filename
	static private Map<String, String> appMap = new TreeMap<String, String>();
	// non interactive shell apps - used with nsh --<app name>
	static private Map<String, String> nishAppMap = new TreeMap<String, String>();

	static { 
		String iGridHome = System.getenv("IGRID_HOME");
		String[] apps = new String[] { 
				"browser", 
				iGridHome + "/bin/browser/bin/sorcer-browser-spawn.xml",
				"webster", 
				iGridHome + "/bin/webster/bin/webster-run.xml",
				"iGrid", 
				iGridHome + "/bin/iGrid-boot-http-spawn.xml",
				"sos", 
				iGridHome + "/bin/sorcer/bin/sorcer-boot-spawn.xml",
				"jobber",
				iGridHome + "/bin/sorcer/jobber/bin/jeri-jobber-boot-spawn.xml",
				"spacer",
				iGridHome + "/bin/sorcer/jobber/bin/jeri-spacer-boot-spawn.xml",
				"cataloger",
				iGridHome + "/bin/sorcer/cataloger/bin/jeri-cataloger-boot-spawn.xml",
				"logger",
				iGridHome + "/bin/sorcer/logger/bin/jeri-logger-boot-spawn.xml",
				"locker", 
				iGridHome + "/bin/sorcer/blitz/bin/locker-boot-spawn.xml",
				"blitz",
				iGridHome + "/bin/sorcer/blitz/bin/blitz-boot-spawn.xml" };
		
			appendApps(apps);
			
			String[] nishApps = new String[] { 
					"browser", 
					iGridHome + "/bin/browser/bin/sorcer-browser.xml",
					"webster", 
					iGridHome + "/bin/webster/bin/webster-run.xml",
					"iGrid", 
					iGridHome + "/bin/iGrid-boot-http.xml",
					"sos", 
					iGridHome + "/bin/iGrid-boot-http.xml",
					"jobber",
					iGridHome + "/bin/sorcer/jobber/bin/jeri-jobber-boot.xml",
					"spacer",
					iGridHome + "/bin/sorcer/jobber/bin/jeri-spacer-boot.xml",
					"cataloger",
					iGridHome + "/bin/sorcer/cataloger/bin/jeri-cataloger-boot.xml",
					"logger",
					iGridHome + "/bin/sorcer/logger/bin/jeri-logger-boot.xml",
					"locker", 
					iGridHome + "/bin/sorcer/blitz/bin/locker-boot.xml",
					"blitz",
					iGridHome + "/bin/sorcer/blitz/bin/blitz-boot.xml" };
			
				appendNishApps(nishApps);
	}


}
