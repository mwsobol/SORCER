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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

public class GroovyCmd extends ShellCmd {

	{
		COMMAND_NAME = "groovy";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "gvy <input filename> [-s <output filename>]";

		COMMAND_HELP = "Executes a Groovy script in the input filename;" 
			+ "\n  -s   save the excution reult in file <output filename>";
	}

	private final static Logger logger = Logger.getLogger(ExertCmd.class
			.getName());

	private String input;

	private PrintStream out;

	private File outputFile;

	private File scriptFile;

	private String script;

	private static StringBuilder staticImports;

	public GroovyCmd() {
		if (staticImports == null) {
			staticImports = readTextFromJar("static-imports.txt");
		}
	}

	public void execute() throws Throwable {
		BufferedReader br = NetworkShell.getShellInputStream();
		out = NetworkShell.getShellOutputStream();
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");

		File d = NetworkShell.getInstance().getCurrentDir();
		String nextToken = null;
		String scriptFilename = null;
		boolean outPersisted = false;
		boolean commandLine = true;
		StringTokenizer tok = new StringTokenizer(input);
		if (tok.countTokens() > 1) {
			while (tok.hasMoreTokens()) {
				nextToken = tok.nextToken();
				if (nextToken.equals("-s")) {
					outPersisted = true;
					outputFile = new File("" + d + File.separator + nextToken);
				} 
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
			if ((new File(scriptFilename)).isAbsolute()) {
				scriptFile = NetworkShell.huntForTheScriptFile(scriptFilename);
			} else {
				scriptFile = NetworkShell.huntForTheScriptFile("" + d
						+ File.separator + scriptFilename);
			}
			sb = new StringBuilder(staticImports.toString());
			try {
				sb.append(readFile(scriptFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println(">>> executing script: \n" + sb.toString());
		} else {
			out.println("Missing script input filename!");
			return;
		}
		NetletThread et = new NetletThread(sb.toString());
		et.start();
		et.join();
		Object outObject = et.getResult();
		// System.out.println(">>>>>>>>>>> result: " + xrt);
		if (outObject != null) {
			out.println("\n---> GROOVY OUTPUT --->");
			out.println(outObject.toString());
		} else {
			if (et.getTarget() != null) {
				out.println("\n--- Failed to execute script ---");
				out.println((et.getTarget()));
				if (!commandLine) {
					System.err.println(et.getTarget());
				}
			}
			//System.out.println(">>> executing script: \n" + sb.toString());
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

}
