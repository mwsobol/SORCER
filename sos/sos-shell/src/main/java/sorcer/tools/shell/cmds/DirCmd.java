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

import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.service.Accessor;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Handles directory commands
 */
public class DirCmd extends ShellCmd {

	{
		COMMAND_NAME = "ls, pwd, dir, cd";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "ls [-l] | pwd | cd <directory name> | ls --c | --p ";

		COMMAND_HELP = "Handles directory commands: ls, pwd, cd; Cataloger contents or providers: ls --c | --p.";
	}

    private PrintStream out;

	public void execute(String... args) throws Throwable {
		BufferedReader br = NetworkShell.getShellInputStream();
		out = NetworkShell.getShellOutputStream();
        String input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");
		if (input.startsWith("ls") || input.startsWith("dir")) {
			File d = NetworkShell.getInstance().getCurrentDir();
			boolean details = false;
			StringTokenizer tok = new StringTokenizer(input);
			if (tok.countTokens() > 1) {
				/* First token is "ls" */
				tok.nextToken();
				String option = tok.nextToken();
				
				//list the content of the CatalogercatalogInfo
				if (option.equals("--c")) {
					catalogInfo();
					return;
				}
				else if (option.equals("--p")) {
					listCatalog();
					return;
				}
				
				if (option.equals("-l"))
					details = true;
				else {
					File temp = new File(d + File.separator + option);
					if (temp.isDirectory()) {
						d = temp;
					} else {
						out.println("Bad option " + option);
						return;
					}
				}
			}
			File[] files = d.listFiles();
			if (files == null) {
				String path = NetworkShell.getInstance().getCurrentDir()
						.getAbsolutePath();
				try {
					path = NetworkShell.getInstance().getCurrentDir()
							.getCanonicalPath();
				} catch (IOException e) {
					/* ignore */
				}
				out.println("No files for current working directory \"" + path
						+ "\"");
				return;
			}
			int sum = 0;
			for (File file : files) {
				sum += file.length();
			}

			out.println("total " + sum);
			File parent = d.getParentFile();
			if (parent != null && details) {
				Date fileDate = new Date(parent.lastModified());
				out.println(getPerms(parent) + "   " + parent.length() + "\t"
						+ fileDate.toString() + "\t" + "core/sorcer-ui/src/main");
			}
			for (File file : files) {
				if (details) {
					String tabs = "\t";
					if (file.length() < 10)
						tabs = tabs + "\t";
					String perms = getPerms(file);
					Date fileDate = new Date(file.lastModified());
					out.println(perms + "   " + file.length() + tabs
							+ fileDate.toString() + "\t" + file.getName());
				} else {
					out.println(file.getName());
				}
			}
		} else if (input.equals("pwd")) {
			try {
				out.println("\""
						+ NetworkShell.getInstance().getCurrentDir()
								.getCanonicalPath() + "\" "
						+ "is the current working directory");
			} catch (IOException e) {
				out.println("\""
						+ NetworkShell.getInstance().getCurrentDir()
								.getAbsolutePath() + "\" "
						+ "is the current working directory");
			}
		} else {
			StringTokenizer tok = new StringTokenizer(input);
			if (tok.countTokens() > 1) {
				/* First token is "cd" */
				tok.nextToken();
				String value = tok.nextToken();
				if (!value.endsWith("*"))
					changeDir(value, out);
			} else {
				out.print("(act a directory to change to) ");
				try {
					String response = br.readLine();
					if (response.length() == 0) {
						out.println("usage: cd directory");
					} else {
						changeDir(response, out);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	boolean changeDir(String dirName, PrintStream out) throws Throwable {
		return (changeDir(dirName, false, out));
	}

	private void listCatalog() {
		Cataloger cataloger = Accessor.get().getService(null, Cataloger.class);
		if (cataloger != null) {
            String[] providers;
            try {
                providers = cataloger.getProviderList();
                out.println("Providers in the " + ((Provider)cataloger).getProviderName() + ": ");
                for (String provider : providers) {
                    out.println("  " + provider);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
	}
	
	private void catalogInfo() {
		Cataloger cataloger = Accessor.get().getService(null, Cataloger.class);
		if (cataloger != null) {
			try {
			out.println(((Provider)cataloger).getProviderName() + " Contents");
			out.println(cataloger.getServiceInfo());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static boolean changeDir(String dirName, boolean echoSuccess,
			PrintStream out) throws Throwable {
		boolean changed = false;
		if (dirName.startsWith("core/sorcer-ui/src/main")) {
			dirName = NetworkShell.getInstance().getCurrentDir()
					.getAbsolutePath()
					+ File.separator + dirName;
		}
		if (dirName.equals("~")) {
			dirName = NetworkShell.getInstance().getHomeDir();
		}
		/* See if the passed in property is a complete directory */
		File dir = new File(dirName);
		/* If its not, it may be a relative path */
		if (!dir.exists()) {
			dir = new File(NetworkShell.getInstance().getCurrentDir()
					.getAbsolutePath()
					+ File.separator + dirName);
			if (!dir.exists()) {
				out.println(dirName + ": No such file or directory");
			}
		}
		if (dir.isDirectory()) {
			try {
				NetworkShell.getInstance()
						.setCurrentDir(dir.getCanonicalFile());
				if (echoSuccess) {
					out.println("Command successful " + dir.getCanonicalPath());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			changed = true;
		} else {
			out.println(dirName + ": Not a directory");
		}
		return (changed);
	}

	String getPerms(File file) {
		String perms;
		if (file.isDirectory())
			perms = "d";
		else
			perms = "-";
		if (file.canRead())
			perms = perms + "r";
		else
			perms = perms + "-";
		if (file.canWrite())
			perms = perms + "w";
		else
			perms = perms + "-";
		return (perms);
	}

	public String getUsage(String subCmd) {
        switch (subCmd) {
            case "ls":
                return "ls [-l]";
            case "dir":
                return "dir [-l]";
            case "pwd":
                return "pwd";
            case "cd":
                return "cd <directory name> | ~";
            default:
                return COMMAND_USAGE;
        }
	}
	
	public String getLongDescription(String subCmd) {
        switch (subCmd) {
            case "ls":
                return "Directory listing of the current working directory.";
            case "dir":
                return "Synonymous with the ls command.";
            case "pwd":
                return "Returns working directory path.";
            case "cd":
                return "Changes the current working directory. The \"~\" character can be used, this will change back to the nsh's home directory";
            default:
                return COMMAND_HELP;
        }
	}
	
}
