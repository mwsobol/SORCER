/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
 * Copyright 2013 Sorcersoft.com S.A.
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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.Contexts;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.node.ContextNode;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.logger.LoggerRemoteException;
import sorcer.core.provider.logger.RemoteLoggerListener;
import sorcer.netlet.ScriptExerter;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.tools.shell.INetworkShell;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalCmd extends ShellCmd {

	{
		COMMAND_NAME = "eval";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "eval [[-s | --s | --m] <output filename>] <input filename>";

		COMMAND_HELP = "Evaluate a netlet specified by the <input filename>;"
				+ "\n  -db   save the command output in a DB"
				+ "\n  -s   save the command output in a file"
				+ "\n  --s   serialize the command output in a file"
				+ "\n  --m   marshal the the command output in a file";
	}

	private final static Logger logger = LoggerFactory.getLogger(EvalCmd.class
			.getName());

    private ScriptExerter scriptExerter;

	private String input;

	private PrintStream out;

	private File outputFile;

	private File scriptFile;

	private String script;

    private INetworkShell shell;

	public EvalCmd() {
	}

	public void execute() throws Throwable {
		out = NetworkShell.getShellOutputStream();
		shell = NetworkShell.getInstance();
		scriptExerter = new ScriptExerter(out, null, NetworkShell.getWebsterUrl(), shell.isDebug());
		shell.setServiceShell(scriptExerter.getServiceShell());
		scriptExerter.setConfig(config);
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");

		File cdir = NetworkShell.getInstance().getCurrentDir();
		String scriptFilename = null;
		boolean ifEvaluation = false;
		boolean ifOutPersisted = false;
		boolean ifMogramControl = false;
		boolean ifMarshalled = false;
		boolean commandLine = NetworkShell.isInteractive();

		List<String> argsList = WhitespaceTokenizer.tokenize(input);

//       Pattern p = Pattern.compile("(\"[^\"]*\"|[^\"^\\s]+)(\\s+|$)", Pattern.MULTILINE);
//       Matcher m = p.matcher(input);
		if (argsList.isEmpty()) {
			out.println(COMMAND_USAGE);
			return;
		}

		try {
			for (int i = 0; i < argsList.size(); i++) {
				String nextToken = argsList.get(i);
				if (nextToken.startsWith("\"") || nextToken.startsWith("'"))
					nextToken = nextToken.substring(1, nextToken.length() - 1);
				if (nextToken.equals("-s")) {
					ifOutPersisted = true;
					outputFile = new File("" + cdir + File.separator + argsList.get(i + 1));
				} else if (nextToken.equals("-eval") || nextToken.equals("-e")) {
					ifEvaluation = true;
					scriptExerter.setIsExerted(false);
				} else if (nextToken.equals("-stgy")) {
					ifMogramControl = true;
				} else if (nextToken.equals("-m")) {
					ifMarshalled = true;
					// evaluate text
				} else if (nextToken.equals("-t")) {
					if (script == null || script.length() == 0) {
						throw new NullPointerException("Must have not empty script");
					}
				}
				// evaluate file script
				else if (nextToken.equals("-f")) {
					scriptFilename = argsList.get(i + 1);
				} else {
					scriptFilename = nextToken;
				}
			}
		} catch (IndexOutOfBoundsException ie) {
			out.println("Wrong number of arguments");
			return;
		}

		if (script != null) {
			scriptExerter.readScriptWithHeaders(script);
		} else if (scriptFilename != null) {
			if ((new File(scriptFilename)).isAbsolute()) {
				scriptFile = NetworkShell.huntForTheScriptFile(scriptFilename);
			} else {
				scriptFile = NetworkShell.huntForTheScriptFile("" + cdir
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
		Object target = scriptExerter.evaluate();
//		out.println(">>>>>>>>>>> scriptExerter.evaluate result: " + target);
		if (!scriptExerter.isExertable()) {
			if (target == null) {
				return;
			} else {
				out.println("\n---> EVALUATION RESULT --->");
				out.println(target);
			}
		}

		// Create RemoteLoggerListener
		RemoteLoggerListener listener = null;
		if (shell.isRemoteLogging() && target instanceof Mogram) {
			List<Map<String, String>> filterMapList = new ArrayList<Map<String, String>>();
			for (String exId : ((ServiceMogram)target).getAllMogramIds()) {
				Map<String, String> map = new HashMap<String, String>();
				map.put(RemoteLogger.KEY_MOGRAM_ID, exId);
				filterMapList.add(map);
			}
			if (!filterMapList.isEmpty()) {
				try {
					listener = new RemoteLoggerListener(filterMapList);
					//listener.register(filterMapList);
				} catch (LoggerRemoteException lre) {
					out.append("Remote logging disabled: " + lre.getMessage());
					listener = null;
				}
			}
		}

//		if (NetworkShell.getInstance().isDebug()) out.println("Starting exert netlet!");
		Object result = scriptExerter.execute();
//		out.println(">>>>>>>>>>> scriptExerter.execute result: " + result);
		if (result != null) {
			if (ifEvaluation) {
				if (target instanceof Model) {
					out.println("\n---> MODEL RESPONSE --->");
					out.println(result);
				} else {
					out.println("\n---> EVALUATION RESULT --->");
					out.println(result);
				}
				return;
			} else if ((result instanceof Mogram)) {
				Mogram mog = (Mogram) result;
				if (!mog.getAllExceptions().isEmpty()) {
					if (commandLine) {
						out.println("Exceptions: ");
						out.println(mog.getAllExceptions());
					} else {
						List<ThrowableTrace> ets = mog.getAllExceptions();
						out.println("Exceptions: ");
						for (ThrowableTrace t : ets) {
							out.println(t.message);
							out.println(t.describe());
						}
					}
				}

				out.println("\n---> OUTPUT MOGRAM --->");
				out.println(mog.describe());
				if (mog instanceof Exertion) {
					Exertion xrt = (Exertion)mog;
					out.println("\n---> OUTPUT DATA CONTEXT --->");
					out.println(xrt.getContext());
					saveFilesFromContext(xrt, out);
					if (ifMogramControl) {
						out.println("\n---> OUTPUT STRATEGY  --->");
						out.println(xrt.getControlContext());
					}
				} else {
					out.println("\n---> MODEL RESPONSE --->");
					out.println(((Model)mog).getResult());
//					out.println(((Model)target).getResponse());
				}
			}
			if (ifMogramControl) {
				out.println("\n---> OUTPUT STRATEGY --->");
				out.println(((Model) out).getMogramStrategy());
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
//		if (listener != null) listener.destroy();
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

	private void saveFilesFromContext(Exertion xrt, PrintStream out) {
		try {
			ContextNode[] cns = (xrt.isJob() ? Contexts.getTaskContextNodes((ServiceExertion)xrt)
					: Contexts.getTaskContextNodes((ServiceExertion)xrt));
			for (ContextNode cn : cns) {

				if (cn.isOut() && cn.getData()!=null && cn.getData() instanceof byte[]) {
					File f = new File(cn.getName());
					FileUtils.writeByteArrayToFile(f, (byte[])cn.getData());
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
