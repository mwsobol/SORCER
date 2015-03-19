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
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.rioproject.impl.util.FileUtils;
import sorcer.core.context.Contexts;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.node.ContextNode;
import sorcer.netlet.ScriptExerter;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.*;
import sorcer.tools.shell.*;
import sorcer.tools.shell.ShellCmd;

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

    private String script;

	private static StringBuilder staticImports;

	public ExertCmd() {
		if (staticImports == null) {
			staticImports = readTextFromJar("static-imports.txt");
		}
	}

	public void execute(String command, CommandLine cmd) {
        ScriptExerter scriptExerter = new ScriptExerter((ClassLoader) null);
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");

		File d = NetworkShell.getInstance().getCurrentDir();
		String scriptFilename = null;
		boolean outputControlContext = false;
		boolean commandLine = NetworkShell.isCommandLine();

        if (cmd.hasOption("cc"))
            outputControlContext = true;
        else if (cmd.hasOption('t')) {
            if (script == null || script.length() == 0) {
                throw new NullPointerException("Must have not empty script");
            }
        } else if (cmd.hasOption('f'))
            scriptFilename = cmd.getOptionValue('f');
        else
            printUsage();


        try {
            if (script != null) {
                scriptExerter.readScriptWithHeaders(script);
            } else if (scriptFilename != null) {
                File scriptFile;
                if ((new File(scriptFilename)).isAbsolute()) {
                    scriptFile = NetworkShell.huntForTheScriptFile(scriptFilename);
                } else {
                    scriptFile = NetworkShell.huntForTheScriptFile("" + d
                            + File.separator + scriptFilename);
                }
                try {
                    scriptExerter.readFile(scriptFile);
                } catch (IOException e) {
                    out.append("File: " + scriptFile.getAbsolutePath() + " could not be found or read: " + e.getMessage());
                }
            } else {
                out.println("Missing exertion input filename!");
                return;
            }
            Object target = scriptExerter.parse();
/*
        LoggerRemoteEventClient lrec = null;

        // Starting RemoteLoggerListener
        if (shell.isRemoteLogging() & target instanceof Exertion) {
            List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
            for (String exId : getAllExertionIdFromExertion((Exertion)target)) {
                Map<String, String> map = new HashMap<String, String>();
                map.put(RemoteLogger.KEY_EXERTION_ID, exId);
                filterMapList.add(map);
            }
            if (!filterMapList.isEmpty()) {
                try {
                    lrec = new LoggerRemoteEventClient();
                    lrec.register(filterMapList, new ConsoleLoggerListener(out));
                } catch (LoggerRemoteException lre) {
                    out.append("Remote logging disabled: " + lre.getMessage());
                    lrec = null;
                }
            }
        }
*/

            Object result = scriptExerter.execute();
            // System.out.println(">>>>>>>>>>> result: " + result);
            if (result != null) {
                if (!(result instanceof Exertion)) {
                    out.println("\n---> EVALUATION RESULT --->");
                    out.println(result);
                    return;
                }
                Exertion xrt = (Exertion) result;
                if (!xrt.getAllExceptions().isEmpty()) {
                    if (commandLine) {
                        out.println("Exceptions: ");
                        out.println(xrt.getAllExceptions());
                    } else {
                        List<ThrowableTrace> ets = xrt.getAllExceptions();
                        out.println("Exceptions: ");
                        for (ThrowableTrace t : ets) {
                            out.println(t.message);
                            out.println(t.describe());
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
                    saveFilesFromContext(xrt, out);
                if (outputControlContext) {
                    out.println("\n---> OUTPUT CONTROL CONTEXT --->");
                    out.println(xrt.getControlContext());
                }
            } else {
                if (target != null) {
                    out.println("\n--- Failed to excute exertion ---");
                    out.println(((ServiceExertion) target).describe());
                    out.println(((ServiceExertion) target).getDataContext());
                    if (!commandLine) {
                        out.println("Script failed: " + scriptFilename);
                        out.println(script);
                    }
                }
                // System.out.println(">>> executing script: \n" + sb.toString());
            }

/*
        if (lrec != null) lrec.destroy();
*/
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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

    private void saveFilesFromContext(Exertion xrt, PrintWriter out) {
        try {
            ContextNode[] cns = (xrt.isJob() ? Contexts.getTaskContextNodes(xrt)
                    : Contexts.getTaskContextNodes(xrt));
            for (ContextNode cn : cns) {

                if (cn.isOut() && cn.getData()!=null && cn.getData() instanceof byte[]) {
                    File f = new File(cn.getName());
                    FileUtils.writeByteArrayToFile(f, (byte[]) cn.getData());
                    out.println("A file was extracted and saved from context to: " + f.getAbsolutePath());
                }
            }
        } catch (ContextException e) {
            out.println(e.getMessage());
        } catch (IOException e) {
            out.println(e.getMessage());
        }
    }
}
