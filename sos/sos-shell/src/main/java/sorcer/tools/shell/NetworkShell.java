/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
 * Copyright 2015 SorcerSoft.com.
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

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.entry.Name;
import org.apache.commons.cli.*;
import org.rioproject.impl.config.DynamicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.EmptyConfiguration;
import net.jini.config.NoSuchEntryException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.entry.AbstractEntry;
import net.jini.lookup.entry.UIDescriptor;

import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.netlet.util.ScriptExertException;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.EvaluationException;
import sorcer.tools.shell.cmds.*;
import sorcer.tools.webster.Webster;
import sorcer.util.*;
import sorcer.util.eval.PropertyEvaluator;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

import com.sun.jini.config.Config;

import static sorcer.util.StringUtils.arrayToRequest;
import static sorcer.util.StringUtils.toArray;

/**
 * @author Mike Sobolewski
 * call the 'help' command at the nsh prompt
 */
public class NetworkShell implements DiscoveryListener {

    public static final String NSH_HELP="SORCER Network Shell - command line options:\n" +
             "\t<file[.ext]> \t\t- exert the netlet script provided in the specified file\n" +
             "\t-b <file[.ext]> \t- run batch file - start non-interactive shell\n" +
             "\t\t\t\tand execute commands specified in file\n" +
             "\t-c <command [args]> \t- start non-interactive shell and run <command> with arguments\n" +
             "\t\t\t\tto see the full list of available commands run 'nsh -c help'\n" +
             "\t-e <file[.ext]> \t- evaluate groovy script contained in specified file\n" +
             "\t-f <file[.ext]> \t- exert the netlet script provided in the specified file\n"+
             "\t-help \t\t\t- show this help\n" +
             "\t-version \t\t- show NSH version info";

    public static int selectedRegistrar = 0;

    private static List<ServiceRegistrar> registrars = java.util.Collections.synchronizedList(new ArrayList<ServiceRegistrar>());

//    static private boolean isRemoteLogging = true;

	final static private String shellName = "nsh";

	private static String request;

	final static String DISCOVERY_TIMEOUT = "disco-timeout";

	final static String GROUPS = "groups";

	final static String LOCATORS = "locators";

	final static String SYS_PROPS = "system-props";

	public static final String COMPONENT = "sorcer.tools.shell";

	static final String CONFIG_COMPONENT = NetworkShell.class.getName();

	public static Logger logger = LoggerFactory.getLogger(COMPONENT);

	protected static NetworkShell instance;

	public static String[] groups;

	private static String userName;

	static private SorcerPrincipal principal;

	private static LoginContext loginContext;

	public final static long startTime = System.currentTimeMillis();

	private static LookupDiscovery disco;

	private TreeMap<String, ShellCmd> commandTable = new TreeMap<String, ShellCmd>();

	public static final int MAX_MATCHES = 5;

	public static final String CUR_VERSION = "1.2";

	static final String BUILTIN_QUIT_COMMAND = "quit,exit,bye";
	
	static final String UNKNOWN_COMMAND_MSG = "Sorry, unknown command.";

	static final String SYSTEM_PROMPT = "nsh> ";

	private final Map<String, Object> settings = new HashMap<String, Object>();

	static Configuration sysConfig;

	private String homeDir;

	public static File currentDir;

	private File shellLog;

	public static PrintStream shellOutput;
	public PrintWriter _shellOutput;

	public static BufferedReader shellInput;
	public BufferedReader _shellInput;

    private static WhitespaceTokenizer shellTokenizer;

	private String hostName;

	public String hostAddress;

	public Webster webster;

	public String[] httpJars;

	public String[] httpRoots;

	// true for interactive shell
	private static boolean interactive = true;

	public static String nshUrl;

    private DiscoCmd discoCmd;
    private ExertCmd exertCmd;
    private InfoCmd infoCmd;

    public NetworkShell(PrintWriter out, BufferedReader in) {
        _shellOutput = out;
        _shellInput = in;

        loadBuiltinCommands();
        loadInternalCommands();
        loadExternalCommands();
	}
	
	public static synchronized String[] buildInstance(boolean ensureSecurityManager, String... argv)
			throws Throwable {
		shellOutput = System.out;
		if (instance == null) {
            if(ensureSecurityManager) {
                ensureSecurityManager();
            }
			instance = new NetworkShell(new PrintWriter(shellOutput), new BufferedReader(new InputStreamReader(System.in)));
			instance.loadBuiltinCommands();
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
			System.setSecurityManager(new SecurityManager());
		}
	}

    static Options globalOptions;

    static {
        globalOptions = new Options();
        globalOptions.addOption("f", false, "-f");
        globalOptions.addOption("c", false, "-c");
        globalOptions.addOption("b", false, "-b");
        globalOptions.addOption("n", false, "-n");
        globalOptions.addOption("version", false, "Version");
        globalOptions.addOption("help", false, "Help");
    }

    public ServiceItem[] lookup(
            Class[] serviceTypes) throws RemoteException {
        return lookup(serviceTypes, null);
    }

    public ServiceItem[] lookup(
            Class[] serviceTypes, String serviceName) throws RemoteException {
        return lookup(null, serviceTypes, serviceName);
    }

    public ServiceItem[] lookup(ServiceRegistrar registrar,
            Class[] serviceTypes, String serviceName) throws RemoteException {
        ServiceRegistrar regie = null;
        if (registrar == null) {
            regie = discoCmd.getSelectedRegistrar();
            if (regie == null)
                return null;
        } else {
            regie = registrar;
        }

        ArrayList<ServiceItem> serviceItems = new ArrayList<ServiceItem>();
        ServiceMatches matches = null;
        Entry myAttrib[] = null;
        if (serviceName != null) {
            myAttrib = new Entry[1];
            myAttrib[0] = new Name(serviceName);
        }
        ServiceTemplate myTmpl = new ServiceTemplate(null, serviceTypes,
                myAttrib);

        matches = regie.lookup(myTmpl, MAX_MATCHES);
        for (int j = 0; j < Math.min(MAX_MATCHES, matches.totalMatches); j++) {
            serviceItems.add(matches.items[j]);
        }
        ServiceItem[] sItems = new ServiceItem[serviceItems.size()];
        return serviceItems.toArray(sItems);
    }

    /**
     * Start interactive shell
     */
    public void start() throws Throwable {
        infoCmd.execute("about", new String[]{});

        String line;
        do {
            prompt();
            line = _shellInput.readLine();
            execute(line);
        } while (line != null);
    }

    private void prompt(){
        _shellOutput.print(SYSTEM_PROMPT);
        _shellOutput.flush();
    }

    static public void main(String argv[]) {
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());

        Parser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(globalOptions, argv);
            boolean interactive = argv.length == 0
                    || cmd.hasOption('f')
                    || cmd.hasOption('c')
                    || cmd.hasOption('b')
                    || cmd.hasOption('n')
                    || cmd.hasOption("version")
                    || cmd.hasOption("help")
            ;

            principal = new SorcerPrincipal(NetworkShell.getUserName());
            principal.setId(NetworkShell.getUserName());

            instance = buildInstance(true, argv) new NetworkShell(new PrintWriter(System.out, true), new BufferedReader(new InputStreamReader(System.in)));

            if(!interactive)
                instance.execute(cmd);
            else
                instance.start();

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        if(true)
            return;

        try {
            // default shellOutput
            shellOutput = System.out;

            argv = buildInstance(true, argv);
            instance.loadExternalCommands();
            if (!instance.interactive) {
                // System.out.println("main appMap: " + appMap);
                System.exit(0);
            }
            if (argv.length == 1 && argv[0].indexOf("-") == 0) {
                shellOutput.println(UNKNOWN_COMMAND_MSG);
                shellOutput.flush();
                System.exit(1);
            }

            shellOutput.print(SYSTEM_PROMPT);
            shellOutput.flush();
            request = "";
            request = shellInput.readLine();
        } catch (ScriptExertException e) {
            String msg = "Problem parsing script @ line: " + e.getLineNum() + ":\n"
                    + e.getLocalizedMessage();
            logger.error(msg);
            shellOutput.println(msg);
            System.exit(1);
        } catch (Throwable e) {
            e.printStackTrace(shellOutput);
			System.exit(1);
		}
		ShellCmd cmd = null;
		nshUrl = getWebsterUrl();
		//System.out.println("main request: " + request);
		//ClassLoaderUtil.displayContextClassLoaderTree();    	

        if (!interactive && request == null || "q".equals(request)) {
            // Exit when CTRL+D is pressed
			System.exit(0);
		}
		while ((request !=null && (request.length() > 0 && BUILTIN_QUIT_COMMAND.indexOf(request) < 0))
				|| request == null || request.length() == 0) {
            processRequest(false);
		}
		// shellOutput.println("Thanks for using the SORCER Network Shell!");
	}

    private void execute(CommandLine cmd) {
        instance.execNoninteractiveCommand(cmd);
        shellOutput.println();

    }

    /**
     * Execute unparsed command
     *
     * @param commandLine the unparsed command line
     */
    private void execute(String commandLine) {
        if (commandLine == null)
            return;
        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(commandLine);
        List<String> tokens = new LinkedList<String>();
        while (tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());
        execute(tokens);
    }

    /**
     * Execute parsed command line
     *
     * @param commandLine parsed command line
     */
    private void execute(List<String> commandLine) {
        if (commandLine.isEmpty())
            return;
        String command = commandLine.get(0);

        if (!commandTable.containsKey(command))
            command = aliases.get(command);

        ShellCmd cmd = commandTable.get(command);

        if (cmd == null) {
            missing(commandLine);
            return;
        }
        try {
            cmd.execute(command, commandLine.toArray(new String[commandLine.size()]));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void missing(List<String> commandLine) {

    }

    public static void processRequest(boolean outsideCall) {
        ShellCmd cmd;
        shellTokenizer = new WhitespaceTokenizer(request);
        String curToken = "";
			if (shellTokenizer.hasMoreTokens()) {
				curToken = shellTokenizer.nextToken();
			}
			try {
				if (commandTable.containsKey(curToken)) {
					cmd = commandTable.get(curToken);
                cmd.execute(request);
				}
				// admissible shortcuts in the 'synonyms' map
				else if (aliases.containsKey(curToken)) {
					String cmdName = aliases.get(curToken);
					int i = cmdName.indexOf(" -");
					if (i > 0) {
						request = cmdName;
						cmdName = cmdName.substring(0, i);
						shellTokenizer = new WhitespaceTokenizer(request);
					} else {
						request = cmdName + " " + request;
						cmdName = new StringTokenizer(cmdName).nextToken();
						shellTokenizer = new WhitespaceTokenizer(request);
					}
					cmd = commandTable.get(cmdName);
                    cmd.execute(request);
				} else if (request.length() > 0) {
					if (request.equals("?")) {
						instance.listCommands();

					} else {
						shellOutput.println(UNKNOWN_COMMAND_MSG);
					}
				}
            if(outsideCall) {
                return;
            }
				shellOutput.print(SYSTEM_PROMPT);
				shellOutput.flush();
				String in = shellInput.readLine();
            // Exit if CTRL+D pressed
            if (in==null || in.equals("q")) System.exit(0);
            for (String q : BUILTIN_QUIT_COMMAND.split(",")) {
                if (in != null && in.equals(q)) System.exit(0);
            }

				// fore !! run the previous command
				if (!in.equals("!!")) {
					instance.request = in;
				}
        } catch (IOException io) {
            shellOutput.println(io.getMessage());
            try {
                request = shellInput.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
			} catch (Throwable ex) {
            if (ex instanceof ScriptExertException) {
                String msg = "Problem parsing script @ line: " +
                        ((ScriptExertException)ex).getLineNum() + ":\n" + ex.getLocalizedMessage();
                logger.error(msg);
                shellOutput.println(msg);
            } else
                ex.printStackTrace(shellOutput);
				try {
					request = shellInput.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		// shellOutput.println("Thanks for using the SORCER Network Shell!");

	public static String getUserName() {
		return userName;
	}

	public static SorcerPrincipal getPrincipal() {
		return principal;
	}

	public void startApplication(String appPath)
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

    CommandLineParser parser = new BasicParser();

	private void execNoninteractiveCommand(String args[])
			throws Throwable {
		// check for external commands
		//System.out.println("nonintercative nsh: " + Arrays.toString(args));

/*
		if (args[0].indexOf("--") == 0) {
			String path = nishAppMap.get(args[0].substring(2));
            if (path!=null)
                startApplication(path);
            else
                shellOutput.println("No such internal command available: " + args[0].substring(2));
			System.exit(0);
		}
*/
        PropertyEvaluator propsEval = new PropertyEvaluator();
        propsEval.addDefaultSources();
        for (int i = 0; i < args.length; i++) {
            args[i] = propsEval.eval(args[i]);
        }
		request = arrayToRequest(args);
        shellTokenizer = new WhitespaceTokenizer(request);
        System.err.println("----------------------------------------------------");
        System.err.println("Starting non-interactive exec of request: " + request);

        // Wait for DiscoveryListener to find Reggies
        int i=0;
        while (getRegistrars().isEmpty() && i<20)
            Thread.sleep(100);
        try {

            CommandLine cmd = parser.parse(globalOptions, args);

            if (cmd.hasOption("version")) {
                printVersion();
            } else if (cmd.hasOption("help")) {
                printHelp();
            } else if (cmd.hasOption('f')){
                exertCmd.executeFile(cmd.getOptionValue('f'));
            } else if(cmd.hasOption('n')){
                exertCmd.executeFile(cmd.getOptionValue('n'));
            }

            if (args.length == 1) {
                if (args[0].equals("-version")) {
                } else if (args[0].equals("-help")) {
                } else {
                    // Added reading the file as default first argument
                    // Check if file exists
                    exertCmd.execute(request);
			}
            } else if (args.length > 1) {
			if (args[0].equals("-f") || args[0].equals("-n")) {
				// evaluate file
                exertCmd.executeFile(cmd.getOptionValue());
			} else if (args[0].equals("-e")) {
				// evaluate command line expression

				// cmd.setScript(instance.getText(args[1]));
				exertCmd.setScript(ExertCmd.readFile(huntForTheScriptFile(args[1])));
                exertCmd.execute(request);
                } else if (args[0].equals("-c")) {
                    ShellCmd cmd = commandTable.get(args[1]);
                    if (args.length > 2)
                        shellTokenizer = new WhitespaceTokenizer(request.substring(4 + args[1].length()));
                    else
                        shellTokenizer = new WhitespaceTokenizer(request.substring(3 + args[1].length()));
                    if (cmd!=null) {
                        cmd.execute(request);
                    } else
                        shellOutput.println("Command: " + args[1] + " not found. " +
                                "Please run 'nsh -help' to see the list of available commands");
                } else if (args[0].equals("-b")) {
                    File batchFile = huntForTheScriptFile(args[1], new String[] { "nsh", "nbat" });
                    System.err.println("Processing batch request on file: " + batchFile.getAbsolutePath());
                    String batchCmds = readScript(batchFile);
                    shellOutput.println("Executing batch file: " + batchFile.getAbsolutePath());
                    for (String batchCmd : batchCmds.split("\n")) {
                        WhitespaceTokenizer tok = new WhitespaceTokenizer(batchCmd);
                        List<String> argsList = new ArrayList<String>();
                        while (tok.hasMoreTokens()) {
                            argsList.add(tok.nextToken());
                        }
                        String originalRequest = request;
                        ShellCmd cmd = commandTable.get(argsList.get(0));
                        if (argsList.size() > 1)
                            shellTokenizer = new WhitespaceTokenizer(batchCmd.substring(argsList.get(0).length() +1));
                        else
                            shellTokenizer = new WhitespaceTokenizer(batchCmd.substring(argsList.get(0).length()));
                        request = batchCmd;
                        System.err.println("Starting command: '" + batchCmd + "'");
                        if (cmd!=null) {
                            cmd.execute(request);
                        }else
                            shellOutput.println("Command: " + args[1] + " not found. " +
                                    "Please run 'nsh -help' to see the list of available commands");
                        System.err.println("Execution of command: '" + batchCmd + "' finished");
                        request = originalRequest;
                    }
                }
            }
        } catch (IOException io) {
            shellOutput.println(io.getMessage());
            System.err.println(io.getMessage());
            // Do nothing since an error message was already printed out by th huntForTheScriptFile method
        }
        System.err.println("----------------------------------------------------");
    }

    private void printHelp() {
        _shellOutput.println(NSH_HELP);
    }

    private void printVersion() {
        _shellOutput.println("SORCER Network Shell (nsh " + CUR_VERSION
                + ", JVM: " + System.getProperty("java.version"));
    }

    protected ClassLoader getExtClassLoader() {
		String sorcerExtPath = Sorcer.getHome() + File.separator
				+ "lib" + File.separator + "sorcer" + File.separator
				+ "lib-ext";
		File extDir = new File(sorcerExtPath);
		File[] files = extDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("-shell.jar");
			}
		});

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (files != null && files.length > 0) {
			URL[] urls = new URL[files.length];
			for (int i = 0; i < files.length; i++) {
				try {
					urls[i] = files[i].toURI().toURL();
				} catch (MalformedURLException e) {
					throw new RuntimeException("Error parsing path " + files[i], e);
				}
			}
			cl = new URLClassLoader(urls, cl);
		}
		return cl;
	}

    protected void loadInternalCommands(){
    }

	protected void loadExternalCommands() {
		ServiceLoader<IShellCmdFactory> loader = ServiceLoader.load(IShellCmdFactory.class, getExtClassLoader());
		for (IShellCmdFactory factory : loader) {
			factory.instantiateCommands(this);
		}
	}

	static public void setLookupDiscovery(String... ingroups) {
		disco = null;
		try {
            DynamicConfiguration config = new DynamicConfiguration();
            config.setEntry("net.jini.discovery.LookupDiscovery", "multicastRequestHost",
                    String.class, InetAddress.getLocalHost().getHostAddress());
			disco = new LookupDiscovery(LookupDiscovery.NO_GROUPS, config);
			disco.addDiscoveryListener(instance);
			if (ingroups == null || ingroups.length == 0) {
				// System.out.println("SORCER groups: " +
				// Arrays.toString(Sorcer.getGroups()));
				 disco.setGroups(SorcerEnv.getLookupGroups());
				// disco.setGroups(LookupDiscovery.ALL_GROUPS);
				disco.setGroups(LookupDiscovery.ALL_GROUPS);
			} else {
				disco.setGroups(ingroups);
			}
		} catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
			System.exit(1);
		}  catch (ConfigurationException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(1);
        }
	}

    public ServiceRegistrar getSelectedRegistrar() {
        List<ServiceRegistrar> registrars = getRegistrars();
        if (registrars != null && registrars.size() > 0
                && selectedRegistrar >= 0)
            return registrars.get(selectedRegistrar);
        else if (selectedRegistrar < 0 && registrars.size() > 0) {
            return registrars.get(0);
        } else
            return null;
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

    public void loadBuiltinCommands() {
        addCommand(new StartStopCmd(), "start", "stop");
        discoCmd = new DiscoCmd();
        addCommand(discoCmd, "disco");
        addCommand(new DirCmd(), "ls");
        addCommand(new ChgrpCmd(), "chgrp");
        addCommand(new GroupsCmd(), "groups");
        addCommand(new LookupCmd(), "lup");
        addCommand(new SetPortCmd(), "chport");
        addCommand(new HelpCmd(), "help");
        exertCmd = new ExertCmd();
        addCommand(exertCmd, "exert");
        addCommand(new HttpCmd(), "http");
        addCommand(new EmxCmd(), "emx");
        addCommand(new EditCmd(), "edit");
        addCommand(new ClearCmd(), "clear");
        addCommand(new ExecCmd(), "exec");
        infoCmd = new InfoCmd();
        addCommand(infoCmd, "about");
        addCommand(new iGridCmd(), "ig");
        addCommand(new DataStorageCmd(), "ds");
        addCommand(new SpaceCmd(), "sp");
    }

	public void addToCommandTable(String cmd, ShellCmd cmdInstance) {
		cmdInstance.setNetworkShell(this);
		cmdInstance.setConfiguration(getConfiguration());
		commandTable.put(cmd, cmdInstance);
	}

    public void addToCommandTable(String cmd, Class<? extends ShellCmd> inCls) throws IllegalArgumentException {
        ShellCmd cmdInstance;
        try {
            // System.out.println("creating command's instance - "
            // + inCls.getName() + " for " + cmd);
            cmdInstance = inCls.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while instantiating " + inCls, e);
        }
         addToCommandTable(cmd, cmdInstance);
    }

	/*
	 * Initialize runtime settings
	 */
	private void initSettings(String[] groups, LookupLocator[] locators,
			long discoTimeout, String args[]) throws ConfigurationException {
		settings.put(GROUPS, groups);
		settings.put(LOCATORS, locators);
		Properties props = new Properties();
		String iGridHome = Sorcer.getHome();
		if (iGridHome == null)
			throw new RuntimeException("SORCER_HOME must be set");
		props.put("java.protocol.handler.pkgs", "net.jini.url|sorcer.util.bdb.sos|org.rioproject.url");
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

		if (!interactive && (args.length==0 || (!args[0].equals("-c")
                && !args[0].equals("-help") && !args[0].equals("-version")))) {
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
			System.out.println("Created the nsh shell log file: " + shellLog.getAbsolutePath());
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

	public static WhitespaceTokenizer getShellTokenizer() {
		return shellTokenizer;
	}

	public static BufferedReader getShellInputStream() {
		return shellInput;
	}

    public PrintWriter getOutputStream() {
		return _shellOutput;
	}

	public static void setShellOutput(PrintStream outputStream) {
		shellOutput = outputStream;
	}

	public static LookupDiscovery getDisco() {
		return disco;
	}

	public static List<ServiceRegistrar> getRegistrars() {
        // Remove non-existent registrars
        List<ServiceRegistrar> regsToRemove = new ArrayList<ServiceRegistrar>();
        synchronized (registrars) {
            for (ServiceRegistrar sr : registrars) {
                try {
                    sr.getGroups();
                } catch (Exception e) {
                    regsToRemove.add(sr);
                }
            }
            if (!regsToRemove.isEmpty()) registrars.removeAll(regsToRemove);
        }
        if (registrars.isEmpty()) {
            if (disco != null)
                disco.terminate();
            // start new lookup discovery
            NetworkShell.setLookupDiscovery(NetworkShell.getGroups());
        }
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

	public static boolean isInteractive() {
		return interactive;
	}

	public static boolean isCommandLine() {
		return interactive;
	}

	public static String getWebsterUrl() {
        if (instance!=null && instance.webster!=null)
		    return "http://" + instance.webster.getAddress() + ":"
				+ instance.webster.getPort();
        else
            return null;
	}

    public String getNshWebsterUrl() {
        return nshUrl;
    }

    public void listCommands() {
		_shellOutput
				.println("You can manage the environment and interact with the service network using the following commands:");
		StringBuilder buffer = new StringBuilder();
		for (Map.Entry<String, ShellCmd> e : commandTable.entrySet()) {
			buffer.append("\n\t" + e.getKey());
			if (e.getKey().length() > 5)
				buffer.append(": \t" + e.getValue().getUsage(e.getKey()));
			else
				buffer.append(": \t\t" + e.getValue().getUsage(e.getKey()));
		}
		_shellOutput.println(buffer.toString());
		_shellOutput
				.println("\nFor help on any of these commands type 'help [<command>]'." 
						+ "\nTo leave this program type 'quit'");
	}

    public void addCommand(ShellCmd cmd, String name, String...aliases){

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
/*
		String[] apps = (String[]) sysConfig.getEntry(
				CONFIG_COMPONENT, "applications", String[].class,
				new String[0]);
		if (apps != null && apps.length >= 2) {
			appendApps(apps);
		}
*/

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
		instance.initSettings(groups, locators, discoveryTimeout, args);
		setLookupDiscovery(groups);

		if (homeDir != null) {
			DirCmd.changeDir(homeDir, false, instance._shellOutput);
		}

		InetAddress inetAddress = InetAddress.getLocalHost();
		instance.hostName = inetAddress.getHostName();
		instance.hostAddress = inetAddress.getHostAddress();
		instance.httpJars = jars;
		instance.httpRoots = roots;

		if (!noHttp) {
			if (instance.interactive)
				HttpCmd.createWebster(httpPort, roots, false, instance.httpJars, shellOutput, instance);
			else
				HttpCmd.createWebster(0, roots, false, instance.httpJars, shellOutput, instance);
		}
		nshUrl = getWebsterUrl();
		return args;
	}

	public static String[] getGroups() {
		return groups;
	}

	public static void setGroups(String[] groups) {
		NetworkShell.groups = groups;
	}

    static public File huntForTheScriptFile(String input) throws IOException {
        String[] standardExtensions = { ".ntl", ".xrt", ".exertlet", ".netlet", ".net",
                ".groovy", ".gvy", ".gy", ".gsh" };
        return huntForTheScriptFile(input, standardExtensions);
    }


	/**
	 * Hunt for the script file, doesn't bother if it is named precisely.
	 * 
	 * Tries in this order: - actual supplied name - name.ex - name.exertlet -
	 * name.netlet - name.net - name.groovy - name.gvy - name.gy - name.gsh
	 * 
	 * @throws IOException
	 */
	static public File huntForTheScriptFile(String input, String[] standardExtensions) throws IOException {
		String scriptFileName = input.trim();
		File scriptFile = new File(scriptFileName);
		int i = 0;
		while (i < standardExtensions.length && !scriptFile.exists()) {
			scriptFile = new File(scriptFileName + standardExtensions[i]);
			i++;
		}
		// if we still haven't found the file, point back to the originally
		// specified filename
		if (!scriptFile.exists()) {
			scriptFile = new File(scriptFileName);
			//shellOutput.println("No such file: "
			//		+ new File(scriptFileName).getCanonicalPath());
            throw new IOException("No such file: " + new File(scriptFileName).getCanonicalPath());
		}
		return scriptFile;
	}

    public static String readScript(File file) throws IOException {
        String lineSep = "\n";
        BufferedReader br = new BufferedReader(new FileReader(file));
        String nextLine;
        StringBuilder sb = new StringBuilder();
        while ((nextLine = br.readLine()) != null) {
            if (!nextLine.startsWith("#")) {
                sb.append(nextLine);
                sb.append(lineSep);
            }
        }
        return sb.toString();
    }

	public TreeMap<String, ShellCmd> getCommandTable() {
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
            PrintWriter out = instance.getOutputStream();
			out.println("Lookup attributes:");
			for (int k = 0; k < attributeSets.length; k++) {
				if (attributeSets[k] instanceof UIDescriptor) {
					out.println("  "
					// + attributeSets[k].getClass()
					// + "UIDescriptor: "
							+ ((UIDescriptor) attributeSets[k]).factory
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

/*
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
*/

	public Webster getWebster() {
		return webster;
	}

	public static String toShortString(AbstractEntry entry) {
		String str = entry.toString();
		int start = str.indexOf("(");
		return str.substring(start + 1, str.length() - 1);
	}

	public static void printSorcerServiceInfo(SorcerServiceInfo serviceInfo) {
        PrintWriter out = instance.getOutputStream();
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
		aliases.put("shh", "sh -h");
		aliases.put("shn", "sh -n");
		aliases.put("shd", "sh -d");
		aliases.put("more", "exec");
		aliases.put("cat", "exec");
		aliases.put("less", "exec");
        aliases.put("sb", "boot");
        aliases.put("sorcer-boot", "boot");
	}

    public void addAlias(String alias, String command) {
        if (aliases.containsKey(alias)) throw new IllegalArgumentException("Alias exists");
        aliases.put(alias, command);
    }

/*
	// a map of application name/ filename
	public static Map<String, String> appMap = new TreeMap<String, String>();
	// non interactive shell apps - used with nsh --<app name>
	static private Map<String, String> nishAppMap = new TreeMap<String, String>();

	static { 
		String iGridHome = System.getenv("SORCER_HOME");
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
*/


}
