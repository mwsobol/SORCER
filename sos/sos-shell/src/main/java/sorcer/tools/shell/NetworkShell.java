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

import com.sun.jini.config.Config;
import groovy.lang.GroovyRuntimeException;
import net.jini.config.*;
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
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.rioproject.config.DynamicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.netlet.util.LoaderConfiguration;
import sorcer.netlet.util.ScriptExertException;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Accessor;
import sorcer.service.EvaluationException;
import sorcer.service.ExertionInfo;
import sorcer.tools.shell.cmds.*;
import sorcer.tools.webster.Webster;
import sorcer.util.Sorcer;
import sorcer.util.SorcerEnv;
import sorcer.util.TimeUtil;
import sorcer.util.eval.PropertyEvaluator;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.fusesource.jansi.Ansi.ansi;
import static sorcer.util.StringUtils.tName;

/**
 * @author Mike Sobolewski
 * call the 'help' command at the nsh prompt
 */
@SuppressWarnings("unchecked")
public class NetworkShell implements DiscoveryListener, INetworkShell {

    public static final String NSH_HELP="SORCER Network Shell - command line options:\n" +
             "\t<file[.ext]> \t\t- eval the sorcer.netlet script provided in the specified file\n" +
             "\t-b <file[.ext]> \t- run batch file - start non-interactive shell\n" +
             "\t\t\t\tand eval commands specified in file\n" +
             "\t-c <command [args]> \t- start non-interactive shell and run <command> with arguments\n" +
             "\t\t\t\tto see the full list of available commands run 'nsh -c help'\n" +
             "\t-e <file[.ext]> \t- evaluate groovy script contained in specified file\n" +
             "\t-f <file[.ext]> \t- eval the sorcer.netlet script provided in the specified file\n"+
             "\t-help \t\t\t- show this help\n" +
             "\t-version \t\t- show NSH version info";


    //public static final String CONFIG_EXT_PATH = Sorcer.get + "/configs/shell/configs/nsh-start-ext.config";
    public static final String CONFIG_PATH = SorcerEnv.getHome() + "/bin/shell/configs/nsh-start.config";

	static private boolean debug = false;

	static private boolean isRemoteLogging = true;

	public static final String[] CONFIG_FILES = { CONFIG_PATH };

    public static int selectedRegistrar = 0;

    private static List<ServiceRegistrar> registrars = new CopyOnWriteArrayList<ServiceRegistrar>();

	static private String shellName = "nsh";

	private static String request;

	final static String DISCOVERY_TIMEOUT = "disco-timeout";

	final static String GROUPS = "groups";

	final static String LOCATORS = "locators";

	final static String SYS_PROPS = "system-props";

	final static String LIST = "list-props";

	public static final String COMPONENT = "sorcer.tools.shell";

	static final String CONFIG_COMPONENT = NetworkShell.class.getName();

	public static Logger logger = LoggerFactory.getLogger(COMPONENT);

	protected static NetworkShell instance;

	protected static ServiceShell serviceShell;

	protected static String[] groups;

	private static String userName;

	static private SorcerPrincipal principal;

	private static LoginContext loginContext;

	public final static long startTime = System.currentTimeMillis();

	private static LookupDiscovery disco;

	private static TreeMap<String, ShellCmd> commandTable = new TreeMap<String, ShellCmd>();

	static final int MAX_MATCHES = 5;

	static final String CUR_VERSION = System.getProperty("sorcer.version");

	static final String BUILTIN_QUIT_COMMAND = "quit,exit,bye";
	
	static final Ansi UNKNOWN_COMMAND_MSG = ansi().render("@|red Sorry, unknown command.|@");

	static final Ansi SYSTEM_PROMPT = ansi().render("@|bold nsh> |@");

	static final Ansi WELCOME_HEADER_1 = ansi().render("@|green SORCER Network Shell |@ @|bold nsh "
			+ CUR_VERSION + ", JVM: " + System.getProperty("java.version")+ "|@");

	static final Ansi WELCOME_HEADER_2 = ansi().render("Type '@|bold quit|@' to terminate the shell");

	static final Ansi WELCOME_HEADER_3 = ansi().render("Type '@|bold help|@' for command help");

	private final Map<String, Object> settings = new HashMap<String, Object>();

	static Configuration sysConfig;

	private String homeDir;

	static private File currentDir;

	private File shellLog;

	public static PrintStream shellOutput;

	private static BufferedReader shellInput;

    private static WhitespaceTokenizer shellTokenizer;

	private String hostName;

	private String hostAddress;

	private Webster webster;

	private String[] httpJars = new String[0];

	private String[] httpRoots;

	// true for interactive shell
	private static boolean interactive = true;

	public static String nshUrl;
	
	private NetworkShell() {
		// do nothing, see buildInstance
	}
	
	public static synchronized String[] buildInstance(boolean ensureSecurityManager, String... argv)
			throws Throwable {
		shellOutput = System.out;
		if (instance == null) {
            if(ensureSecurityManager) {
                ensureSecurityManager();
            }
			instance = new NetworkShell();
			instance.enumerateCommands();
			return initShell(argv);
		}
		return argv;
	}

	public static synchronized INetworkShell getInstance() {
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

	static public void main(String... argv) {
        AnsiConsole.systemInstall();
        //Ansi.setDetector(new AnsiDetector());
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		String curToken = null;
//		System.out.println("nsh main args: " + Arrays.toString(argv));
		if (argv.length > 0) {
			if ((argv.length == 1 && argv[0].startsWith("--"))
						|| (argv.length == 2 && (argv[0].equals("-e"))
						|| argv[0].equals("-f")
						|| argv[0].equals("-c")
						|| argv[0].equals("-b")
						|| argv[0].equals("-n")
						|| argv[0].equals("-version") || argv[0]
						.equals("-help")) || argv.length == 1) {

				interactive = false;
			} else {
				interactive = true;
			}
		}

		try {
            /* TODO: Need to provide a configuration*/
            Accessor.create();
            // default shellOutput
			shellOutput = System.out;
			if (interactive) {
                shellOutput.println(WELCOME_HEADER_1);
				shellOutput.println(WELCOME_HEADER_2);
				shellOutput.println(WELCOME_HEADER_3);
			}
			
			argv = buildInstance(true, argv);
			principal = new SorcerPrincipal(NetworkShell.getUserName());
			principal.setId(NetworkShell.getUserName());

			instance.loadExternalCommands();
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

            shellOutput.print(SYSTEM_PROMPT);
			shellOutput.flush();
			request = "";
			request = shellInput.readLine();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		nshUrl = getWebsterUrl();
		//System.out.println("main request: " + request);
		//ClassLoaderUtil.displayContextClassLoaderTree();    	

		if (request==null || request.equals("q")) {
			// Exit when CTRL+D is pressed
			System.exit(0);
		}
		while (request != null && ((request.length() > 0 && BUILTIN_QUIT_COMMAND.indexOf(request) < 0)
				|| request.length() == 0)) {
            processRequest(false);
		}
		// shellOutput.println("Thanks for using the SORCER Network Shell!");
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
				cmd.execute(curToken);
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
				cmd.execute();
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
			// for !! run the previous command
			if (in!=null && !in.equals("!!")) {
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
				shellOutput.println(ansi().render("@|red " + msg + "|@"));
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

	public boolean isDebug() {
		return debug;
	}

	public boolean isRemoteLogging() {
		return isRemoteLogging;
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
		shellOutput.println("cmd exit eval: " + result.getExitValue());

		if (result.getExitValue() != 0)
			throw new RuntimeException("Failed to start " + appPath);
	}

    static private void execNoninteractiveCommand(String args[])
            throws Throwable {
        // check for external commandsx
        if (args[0].indexOf("--") == 0) {
            String path = nishAppMap.get(args[0].substring(2));
            if (path!=null)
                startApplication(path);
            else
                shellOutput.println(ansi().render("@|red No such internal command available: " + args[0].substring(2) + "|@"));
            System.exit(0);
        }
        PropertyEvaluator propsEval = new PropertyEvaluator();
        propsEval.addDefaultSources();
        for (int i = 0; i < args.length; i++) {
            args[i] = propsEval.eval(args[i]);
        }
        request = arrayToRequest(args);
        shellTokenizer = new WhitespaceTokenizer(request);
        System.err.println("----------------------------------------------------");
        System.err.println("Starting non-interactive exec of request: " + request);

        if (debug) shellOutput.println("initializing nsh: " + (System.currentTimeMillis() - startTime) + "ms");
        try {
            if (args.length == 1) {
                if (args[0].equals("-version")) {
                    shellOutput.println(WELCOME_HEADER_1);
                } else if (args[0].equals("-help")) {
                    shellOutput.println(NSH_HELP);
                } else {
                    // Added reading the file as default first argument
                    // Check if file exists
                    shellOutput.println("exec nsh: " + (System.currentTimeMillis() - startTime) + "ms");
                    ShellCmd cmd = commandTable.get("eval");
                    waitForReggie();
                    shellOutput.println("found reggie: " + (System.currentTimeMillis() - startTime) + "ms");
                    cmd.execute();
                }
            } else if (args.length > 1) {
                if (args[0].equals("-f") || args[0].equals("-n")) {
                    // evaluate file
                    ShellCmd cmd = commandTable.get("eval");
                    waitForReggie();
                    cmd.execute();
                } else if (args[0].equals("-e")) {
                    // evaluate command line expression
                    EvalCmd cmd = (EvalCmd) commandTable.get("eval");
                    // cmd.setScript(instance.getText(args[1]));
                    cmd.setScript(EvalCmd.readFile(huntForTheScriptFile(args[1])));
                    waitForReggie();
                    cmd.execute();
                } else if (args[0].equals("-c")) {
                    ShellCmd cmd = commandTable.get(args[1]);
                    if (args.length > 2)
                        shellTokenizer = new WhitespaceTokenizer(request.substring(4 + args[1].length()));
                    else
                        shellTokenizer = new WhitespaceTokenizer(request.substring(3 + args[1].length()));
                    if (cmd != null) {
                        waitForReggie();
                        cmd.execute(args[1]);
                    } else
                        shellOutput.println(ansi().render("@|red Command: " + args[1] + " not found. |@" +
                                "Please run 'nsh -help' to see the list of available commands"));
                } else if (args[0].equals("-b")) {
                    File batchFile = huntForTheScriptFile(args[1], new String[] { "nsh", "nbat" });
                    System.err.println("Processing batch request on file: " + batchFile.getAbsolutePath());
                    String batchCmds = readScript(batchFile);
                    shellOutput.println("Executing batch file: " + batchFile.getAbsolutePath());
                    waitForReggie();
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
                        if (cmd!=null)
                            cmd.execute();
                        else
                            shellOutput.println(ansi().render("@|red Command: " + args[1] + " not found. |@" +
                                    "Please run 'nsh -help' to see the list of available commands"));
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

	private static void waitForReggie() throws InterruptedException {
		// Wait for DiscoveryListener to find Reggies
		int i=0;
		long waitTimes = (Long)instance.getSettings().get(DISCOVERY_TIMEOUT)/20;
		while (getRegistrars().isEmpty() && i<waitTimes) {
			Thread.sleep(200);
			i++;
		}
		// Needs 2 seconds to get the Rio resolver settled
		//if (i<11)
		//		Thread.sleep(3000-i*200);
	}

	protected ClassLoader getExtClassLoader() {
		String sorcerExtPath = Sorcer.getHome() + File.separator
				+ "lib" + File.separator + "sorcer" + File.separator
				+ "lib-ext";
		File extDir = new File(sorcerExtPath);
		File[] files = extDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.contains("-shell-");
			}
		});

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (files.length > 0) {
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

	protected void loadExternalCommands() {
        try {
            LoaderConfiguration lc = new LoaderConfiguration();
            for (String configFile : CONFIG_FILES) {
                File cfgFile = new File(configFile);
                if (cfgFile.exists()) {
                    lc.configure(new FileInputStream(cfgFile));
                }
            }
            final ClassLoader cl = new URLClassLoader(lc.getClassPathUrls(), NetworkShell.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(cl);
            //new Activator().activate(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs());
        } catch (Exception e) {
            e.printStackTrace(shellOutput);
            System.exit(-1);
        }

        ServiceLoader<IShellCmdFactory> loader = ServiceLoader.load(IShellCmdFactory.class, getExtClassLoader());
		for (IShellCmdFactory factory : loader) {
			factory.instantiateCommands(this);
		}
	}

	static public void setLookupDiscovery(String... ingroups) {
		disco = null;
		try {
            DynamicConfiguration config = new DynamicConfiguration();
            config.setEntry("net.jini.discovery.LookupDiscovery",
                            "multicastRequestHost",
                            String.class,
                            Sorcer.getLocalHost().getHostAddress());
			if (ingroups == null || ingroups.length == 0) {
                disco = new LookupDiscovery(LookupDiscovery.ALL_GROUPS, config);
			} else {
                disco = new LookupDiscovery(ingroups, config);
			}
            disco.addDiscoveryListener(instance);
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
        for (ServiceRegistrar reg : regs) {
            if (!registrars.contains(reg))
                registrars.add(reg);
        }
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
					&& commandTable.get(shellCommands[i])
							.getCommandWord().indexOf(shellCommands[i]) >= 0) {
				shellOutput.println("Missing basic command :  "
						+ shellCommands[i]);
				System.exit(1);
			}
	}

	@Override
	public void addToCommandTable(String cmd, ShellCmd cmdInstance) {
		cmdInstance.setNetworkShell(this);
		cmdInstance.setConfiguration(getConfiguration());
		commandTable.put(cmd, cmdInstance);
	}

	public void addToCommandTable(String cmd, Class<? extends ShellCmd> inCls) {
		try {
			// System.out.println("creating command's instance - "
			// + inCls.getName() + " for " + cmd);
			ShellCmd cmdInstance = inCls.newInstance();
			addToCommandTable(cmd, cmdInstance);
		} catch (Exception e) {
			shellOutput.println(e);
		}
	}

	/*
	 * Initialize runtime settings
	 */
	private void initSettings(String[] groups, LookupLocator[] locators,
			long discoTimeout, String args[]) throws ConfigurationException {
		settings.put(GROUPS, groups);
		settings.put(LOCATORS, locators);
		Properties props = new Properties();
		String sorcerHome = Sorcer.getHome();
		if (sorcerHome == null)
			throw new RuntimeException("SORCER_HOME must be set");
		props.put("java.protocol.handler.pkgs", "net.jini.url|sorcer.util.url|org.rioproject.url");
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
			String logDirPath = System.getProperty(COMPONENT + ".logDir",
					sorcerHome + File.separator + "bin" + File.separator
							+ "shell" + File.separator + "logs");

			File logDir = new File(logDirPath);
			if (!logDir.exists()) {
				if (!logDir.mkdirs()) {
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
			System.err.println("===============================================");
			if (addedProps.size() > 0) {
				StringBuilder buff = new StringBuilder();
				for (Map.Entry<Object, Object> entry : addedProps.entrySet()) {
					String key = (String) entry.getKey();
					String value = (String) entry.getValue();
					buff.append("\n");
					buff.append("    ").append(key).append("=").append(value);
				}
				System.err.println("Added System Properties {"+ buff.toString() + "\n}");
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

	public static PrintStream getShellOutputStream() {
		return shellOutput;
	}

	public PrintStream getOutputStream() {
		return shellOutput;
	}

	public static void setShellOutput(PrintStream outputStream) {
		shellOutput = outputStream;
	}

	public static LookupDiscovery getDisco() {
		return disco;
	}

    public static List<ServiceRegistrar> getRegistrars() {
        // Remove non-existent registrars                                                                                       T
        List<ServiceRegistrar> regsToRemove = new ArrayList<ServiceRegistrar>();
        for (ServiceRegistrar sr : registrars) {
            try {
                sr.getGroups();
            } catch (Exception e) {
                regsToRemove.add(sr);
            }
        }
        if (!regsToRemove.isEmpty())
            registrars.removeAll(regsToRemove);
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

	public Map<String, Object> getSettings() {
		return settings;
	}

	public static boolean isInteractive() {
		return interactive;
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

		public void execute(String... args) {
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
			COMMAND_NAME = "clearSessions";

			NOT_LOADED_MSG = "***command not loaded due to conflict";

			COMMAND_USAGE = "clearSessions -a | -p | -m | -e";

			COMMAND_HELP = "Clear fetched from the network resorces;"
					+ "  -a   clearSessions all cached resources"
					+ "  -p   clearSessions service providers"
					+ "  -m   clearSessions EMX providers"
					+ "  -e   clearSessions monitored exertion infos";
		}
		
		public void execute(String... args) throws IOException, InterruptedException {
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

		public void execute(String... args) throws IOException, InterruptedException {
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
			}, tName("exec-" + cmd));
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

		public void execute(String... args) throws IOException, ClassNotFoundException {
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
			} else if (debug) {
				shellOutput.println("Webster URL: \n  URL: http://"
						+ web.getAddress() + ":" + web.getPort()
						+ "\n  Roots: " + web.getRoots());
				shellOutput.println("  Codebase: " +  System.getProperty("java.rmi.server.codebase"));
			}
			shellOutput.println();
			
			shellOutput.println("Lookup groups: "+ (groups == null ? "all groups" : Arrays.toString(groups)));
			
			DiscoCmd.printCurrentLus();
			EmxCmd.printCurrentMonitor();
//			VarModelCmd.printCurrentModel();
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

			COMMAND_HELP = "Start and stop the nsh shell's code server;"
					+ "  <roots> is semicolon separated list of directories.\n"
					+ "  If not provided the root directory will be:\n"
					+ "  [" + debugGetDefaultRoots() + "]";
		}

		public void execute(String... args) {
			if (shellOutput == null)
				throw new NullPointerException(
						"Must have an output PrintStream");
			StringTokenizer tok = new StringTokenizer(request);
			if (tok.countTokens() < 1)
				shellOutput.print(getUsage("http"));
			int port = 0;
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
					/* Next token must be the port eval */
					String sPort = tok1.nextToken();
					try {
						port = Integer.parseInt(sPort);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						shellOutput.print("Bad port-number eval : " + sPort
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
		 * @param roots
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
				
                if (debug)
                    System.setProperty("webster.debug", "true");

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
			
			if (debug) {
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
			if (jars.length>1) sb.append("http://").append(localIPAddress).append(":")
					.append(port).append("/").append(jars[jars.length - 1]);
			codebase = sb.toString();
			System.setProperty("java.rmi.server.codebase", codebase);
			if (logger.isDebugEnabled())
				logger.debug("Setting nsh 'java.rmi.server.codebase': "
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
        if (instance!=null && instance.webster!=null)
		    return "http://" + instance.webster.getAddress() + ":"
				+ instance.webster.getPort();
        else
            return null;
	}

    public String getNshWebsterUrl() {
        return nshUrl;
    }

	private void listCommands() {
		shellOutput
				.println("You can manage the environment and interact with the service network using the following commands:");
		StringBuilder buffer = new StringBuilder();
		for (Map.Entry<String, ShellCmd> e : commandTable.entrySet()) {
			buffer.append("\n\t" + e.getKey());
			if (e.getKey().length() > 5)
				buffer.append(": \t" + e.getValue().getUsage(e.getKey()));
			else
				buffer.append(": \t\t" + e.getValue().getUsage(e.getKey()));
		}
		shellOutput.println(buffer.toString());
		shellOutput
				.println("\nFor help on any of these commands fiType 'help [<command>]'."
						+ "\nTo leave this program fiType 'quit'");
	}
	

	/**
	 * Initialize the nsh, parsing arguments, loading configuration.
	 * 
	 * @param args
	 *            Command line arguments to evaluate, must not be null
	 * 
	 * @return Array of string args to be parsed
	 * 
	 * @throws Throwable
	 *             if anything at all happens that is not supposed to happen
	 */
	public static String[] initShell(final String[] args) throws Throwable {
		if (args == null)
			throw new NullPointerException("  args is null");

		Properties props = new Properties();
		String fn = Sorcer.getHome() + File.separatorChar +
				"configs"+File.separatorChar + "versions.properties";
		props.load((new FileInputStream(new File(fn))));
		Enumeration e = props.keys();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			System.setProperty(key, props.getProperty(key));
		}

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
					.getInstance(new String[] { configFilename });
		}

		if (sysConfig == null)
			sysConfig = EmptyConfiguration.INSTANCE;

		userName = (String) sysConfig.getEntry(CONFIG_COMPONENT, "userName",
				String.class, null);
		loginContext = null;
		try {
			loginContext = (LoginContext) Config.getNonNullEntry(sysConfig,
					CONFIG_COMPONENT, "loginContext", LoginContext.class);
		} catch (NoSuchEntryException ex) {
			// leave null
		}
		isRemoteLogging = (Boolean) sysConfig.getEntry(CONFIG_COMPONENT,
				"remoteLogging", boolean.class, Boolean.TRUE);
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
			} catch (PrivilegedActionException ex) {
				throw ex.getCause();
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
				String[].class, Sorcer.getLookupGroups());
		String[] apps = (String[]) sysConfig.getEntry(
				CONFIG_COMPONENT, "applications", String[].class,
				new String[0]);
		if (apps != null && apps.length >= 2) {
			appendApps(apps);
		}

		LookupLocator[] localLocators = new LookupLocator[] { new LookupLocator("jini://localhost") };
		LookupLocator[] locators = (LookupLocator[]) sysConfig.getEntry(
				CONFIG_COMPONENT, "locators", LookupLocator[].class, localLocators);

        debug = (Boolean) sysConfig.getEntry(CONFIG_COMPONENT,
                "debug", boolean.class, Boolean.FALSE);

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
			DirCmd.changeDir(homeDir, false, shellOutput);
		}

		InetAddress inetAddress = Sorcer.getLocalHost();
		instance.hostName = inetAddress.getHostName();
		instance.hostAddress = inetAddress.getHostAddress();
		instance.httpJars = jars;
		instance.httpRoots = roots;

		if (!noHttp) {
			if (instance.interactive)
				HttpCmd.createWebster(httpPort, roots, false, instance.httpJars, shellOutput);
			else
				HttpCmd.createWebster(0, roots, false, instance.httpJars, shellOutput);
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
			else {
                if (obj.toString().contains(" "))
                    buffer.append("'").append(obj).append("'");
                else
                    buffer.append(obj);
            }

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


    static public File huntForTheScriptFile(String input) throws IOException {
        String[] standardExtensions = { ".ntl", ".xrt", ".exertion", ".mod", ".model", ".sorcer.netlet", ".net",
                ".groovy", ".gvy", ".gy", ".gsh" };
        return huntForTheScriptFile(input, standardExtensions);
    }


	/**
	 * Hunt for the script file, doesn't bother if it is named precisely.
	 * 
	 * Tries in this order: - actual supplied name - name.ex - name.exertlet -
	 * name.sorcer.netlet - name.net - name.groovy - name.gvy - name.gy - name.gsh
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
        PropertyEvaluator propsEval = new PropertyEvaluator();
        propsEval.addDefaultSources();
        String lineSep = "\n";
        BufferedReader br = new BufferedReader(new FileReader(file));
        String nextLine;
        StringBuilder sb = new StringBuilder();
        while ((nextLine = br.readLine()) != null) {
            if (!nextLine.startsWith("#")) {
                nextLine = propsEval.eval(nextLine);
                sb.append(nextLine);
                sb.append(lineSep);
            }
        }
        return sb.toString();
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
		aliases.put("cls", "clearSessions");
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

	public ServiceShell getServiceShell() {
		return serviceShell;
	}

	public void setServiceShell(ServiceShell serviceShell) {
		this.serviceShell = serviceShell;
	}

    public void addAlias(String alias, String command) {
        if (aliases.containsKey(alias)) throw new IllegalArgumentException("Alias exists");
        aliases.put(alias, command);
    }

    static final String[] shellCommands = { "start", "disco", "ls", "chgrp",
			"groups", "lup", "chgrp", "chport", "help", "eval", "exert", "http", "emx",
			"gvy", "edit", "clearSessions", "exec", "about", "sos", "ds", "sp" };

	static final Class[] shellCmdClasses = { StartStopCmd.class, DiscoCmd.class,
			DirCmd.class, ChgrpCmd.class, GroupsCmd.class, LookupCmd.class,
			ChgrpCmd.class, SetPortCmd.class, HelpCmd.class, EvalCmd.class, EvalCmd.class,
			HttpCmd.class, EmxCmd.class, GroovyCmd.class, EditCmd.class,
			ClearCmd.class, ExecCmd.class, InfoCmd.class, SosCmd.class, DataStorageCmd.class,
			SpaceCmd.class };

	// a map of application name/ filename
	static private Map<String, String> appMap = new TreeMap<String, String>();
	// non interactive shell apps - used with nsh --<app name>
	static private Map<String, String> nishAppMap = new TreeMap<String, String>();

	static { 
		String sorcerHome = System.getenv("SORCER_HOME");
		String[] apps = new String[] { 
				"browser", 
				sorcerHome + "/bin/browser/bin/sorcer-browser-spawn.xml",
				"webster", 
				sorcerHome + "/bin/webster/bin/webster-run.xml",
				"iGrid", 
				sorcerHome + "/bin/iGrid-boot-http-spawn.xml",
				"sos", 
				sorcerHome + "/bin/sorcer/bin/sorcer-boot-spawn.xml",
				"jobber",
				sorcerHome + "/bin/sorcer/jobber/bin/jeri-jobber-boot-spawn.xml",
				"spacer",
				sorcerHome + "/bin/sorcer/jobber/bin/jeri-spacer-boot-spawn.xml",
				"cataloger",
				sorcerHome + "/bin/sorcer/cataloger/bin/jeri-cataloger-boot-spawn.xml",
				"logger",
				sorcerHome + "/bin/sorcer/logger/bin/jeri-logger-boot-spawn.xml",
				"locker", 
				sorcerHome + "/bin/sorcer/blitz/bin/locker-boot-spawn.xml",
				"blitz",
				sorcerHome + "/bin/sorcer/blitz/bin/blitz-boot-spawn.xml" };
		
			appendApps(apps);
			
			String[] nishApps = new String[] { 
					"browser", 
					sorcerHome + "/bin/browser/bin/sorcer-browser.xml",
					"webster", 
					sorcerHome + "/bin/webster/bin/webster-run.xml",
					"iGrid", 
					sorcerHome + "/bin/iGrid-boot-http.xml",
					"sos", 
					sorcerHome + "/bin/iGrid-boot-http.xml",
					"jobber",
					sorcerHome + "/bin/sorcer/jobber/bin/jeri-jobber-boot.xml",
					"spacer",
					sorcerHome + "/bin/sorcer/jobber/bin/jeri-spacer-boot.xml",
					"cataloger",
					sorcerHome + "/bin/sorcer/cataloger/bin/jeri-cataloger-boot.xml",
					"logger",
					sorcerHome + "/bin/sorcer/logger/bin/jeri-logger-boot.xml",
					"locker", 
					sorcerHome + "/bin/sorcer/blitz/bin/locker-boot.xml",
					"blitz",
					sorcerHome + "/bin/sorcer/blitz/bin/blitz-boot.xml" };
			
				appendNishApps(nishApps);
	}


}
