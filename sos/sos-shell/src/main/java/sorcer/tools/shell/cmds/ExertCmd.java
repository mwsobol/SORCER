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

import java.io.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import net.jini.core.transaction.TransactionException;
import org.codehaus.groovy.control.CompilationFailedException;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.ShellStarter;

public class ExertCmd extends ShellCmd {

	{
		COMMAND_NAME = "exert";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "exert [-cc] [[-s | --s | --m] <output filename>] <input filename>";

		COMMAND_HELP = "Manage and execute the federation of services specified by the <input filename>;"
				+ "\n  -cc   print the executed exertion with control context"
				+ "\n  -s   save the command output in a file"
				+ "\n  --s   serialize the command output in a file"
				+ "\n  --m   marshal the the command output in a file";
	}

	private final static Logger logger = Logger.getLogger(ExertCmd.class
			.getName());

	private String input;

	private File outputFile;

	private File scriptFile;

	private String script;

	private static StringBuilder staticImports;

    private GroovyShell gShell = new GroovyShell(ShellStarter.getLoader());

	public ExertCmd() {
		if (staticImports == null) {
			staticImports = readTextFromJar("static-imports.txt");
			// System.out.println("get staticImports: " +
			// staticImports.toString());
			// ClassLoader rootClassLoader = ShellStarter.getLoader();
			// URL[] urls = ((URLClassLoader)rootClassLoader).getURLs();
			// for(int i=0; i< urls.length; i++) {
			// System.out.println("Root" + urls[i].getFile());
			// }
		}
	}

	public void execute(String command, String[] cmd) {
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");

		File d = NetworkShell.getInstance().getCurrentDir();
		String nextToken = null;
		String scriptFilename = null;
		boolean outPersisted = false;
		boolean outputControlContext = false;
		boolean marshalled = false;
		boolean commandLine = NetworkShell.isCommandLine();
		StringTokenizer tok = new StringTokenizer(input);
		if (tok.countTokens() >= 1) {
			while (tok.hasMoreTokens()) {
				nextToken = tok.nextToken();
				if (nextToken.equals("-s")) {
					outPersisted = true;
					outputFile = new File("" + d + File.separator + nextToken);
				} else if (nextToken.equals("-cc"))
					outputControlContext = true;
				else if (nextToken.equals("-m"))
					marshalled = true;
				// evaluate text
				else if (nextToken.equals("-t")) {
					if (script == null || script.length() == 0) {
						throw new NullPointerException("Must have not empty sctipt");
					}
				}
				// evaluate file script
				else if (nextToken.equals("-f"))
					scriptFilename = nextToken;
				else
					scriptFilename = nextToken;
			}
		} else {
			out.println(COMMAND_USAGE);
			return;
		}
		StringBuilder sb = null;
		if (script != null) {
			sb = new StringBuilder(staticImports.toString());
			sb.append(script);
		} else if (scriptFilename != null) {
            try {
			if ((new File(scriptFilename)).isAbsolute()) {
				scriptFile = NetworkShell.huntForTheScriptFile(scriptFilename);
			} else {
				scriptFile = NetworkShell.huntForTheScriptFile("" + d
						+ File.separator + scriptFilename);
			}
			sb = new StringBuilder(staticImports.toString());
				sb.append(readFile(scriptFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println(">>> executing script: \n" + sb.toString());
		} else {
			out.println("Missing exertion input filename!");
			return;
		}
		NetletThread et = new NetletThread(sb.toString());
		et.start();
		et.join();
		Object result = et.getResult();
		// System.out.println(">>>>>>>>>>> result: " + result);
		if (result != null) {
			if (!(result instanceof Exertion)) {
				out.println("\n---> EVALUATION RESULT --->");
				out.println(result);
				return;
			}
			Exertion xrt = (Exertion) result;
			List<ThrowableTrace> ets = xrt.getExceptions();
			if (ets.size() > 0) {
				if (commandLine) {
					out.println("\n---> EXCEPTIONS --->");
					for (ThrowableTrace t : ets) {
						out.println(t.describe());
					}
				} else {
					out.println("\n---> EXCEPTIONS --->");
					out.println(xrt.getExceptions());
					// and into the log file
					System.err.println("\n---> EXCEPTIONS --->");
					for (ThrowableTrace t : ets) {
						System.err.println(t.describe());
					}
				}
			}

			out.println("\n---> OUTPUT EXERTION --->");
			out.println(((ServiceExertion) xrt).describe());
			out.println("\n---> OUTPUT DATA CONTEXT --->");
			if (xrt.isJob())
				out.println(((Job)xrt).getJobContext());
			else
				out.println(xrt.getContext());
			if (outputControlContext) {
				out.println("\n---> OUTPUT CONTROL CONTEXT --->");
				out.println(xrt.getControlContext());
			}
		} else {
			if (et.getTarget() != null) {
				out.println("\n--- Failed to excute exertion ---");
				out.println(((ServiceExertion) et.getTarget()).describe());
				out.println(((ServiceExertion) et.getTarget()).getContext());
				if (!commandLine) {
					out.println("Script failed: " + scriptFilename);
					out.println(script);
				}
			}
			// System.out.println(">>> executing script: \n" + sb.toString());
		}
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public static String readFile(File file) throws IOException {
		// String lineSep = System.getProperty("line.separator");
		String lineSep = "\n";
		BufferedReader br = new BufferedReader(new FileReader(file));
		String nextLine = "";
		StringBuffer sb = new StringBuffer();
		nextLine = br.readLine();
		// skip shebang line
		if (nextLine.indexOf("#!") < 0) {
			sb.append(nextLine);
			sb.append(lineSep);
		}
		while ((nextLine = br.readLine()) != null) {
			sb.append(nextLine);
			sb.append(lineSep);
		}
		return sb.toString();
	}

	private StringBuilder readTextFromJar(String filename) {
		InputStream is = null;
		BufferedReader br = null;
		String line;
		StringBuilder sb = new StringBuilder();
		;

		try {
			is = getClass().getResourceAsStream(filename);
			if (is != null) {
				br = new BufferedReader(new InputStreamReader(is));
				while (null != (line = br.readLine())) {
					sb.append(line);
					sb.append("\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb;
	}

    public Object run() throws IOException, ExecutionException {
        Object target;

        Script parse = gShell.parse(script);
        if (script != null) {
            target = parse.run();
        } else {
            target = gShell.parse(scriptFile);
        }
        try {
            parse.run();
        } catch (CompilationFailedException e) {
            throw new ExecutionException(e);
        }

        if (target instanceof Exertion) {
            ServiceShell se = new ServiceShell((Exertion) target);
            try {
                return se.exert();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (TransactionException e) {
                e.printStackTrace();
            } catch (ExertionException e) {
                e.printStackTrace();
            }
        }
        return target;
    }

}
