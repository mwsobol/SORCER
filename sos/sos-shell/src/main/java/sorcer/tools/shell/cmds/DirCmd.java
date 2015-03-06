/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
 * Copyright 2015 SorcerSoft.com
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
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.ProviderLookup;

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

    private Options lsOpts = new Options();

    {
        lsOpts.addOption("l", false, "long format");
        lsOpts.addOption("c", "c", false, "Contents of catalog");
        lsOpts.addOption("p", "p", false, "long format");
    }

    public void execute(String command, String[] cmd) throws ParseException {
        if ("ls".equals(command) || "dir".equals(command))
            list(cmd);
        else if ("pwd".equals(command))
            printUserDir();
        else if ("cd".equals(command))
            changeDir(cmd);
        else
            printUsage();
    }

    private void printUserDir() {
        try {
            out.println("\""
                    + shell.getCurrentDir().getCanonicalPath()
                    + "\" is the current working directory");
        } catch (IOException e) {
            out.println("\""
                    + shell.getCurrentDir().getPath()
                    + "\" is the current working directory");
        }
    }

    private void list(String[] input) throws ParseException {
        CommandLine cmd = parser.parse(lsOpts, input);

        if(cmd.hasOption('c'))
            catalogInfo();
        else if(cmd.hasOption('p'))
            listCatalog();
        else
            listDir(cmd);
    }

    private void listDir(CommandLine cmd){
        String[] args = cmd.getArgs();
        File d;
        if (args.length == 0)
            d = shell.getCurrentDir();
        else if (args.length == 1)
            d = new File(args[0]);
        else
            throw new IllegalArgumentException("Expecting exactly one argument to ls");

        boolean details = cmd.hasOption('l');

        listDir(d, details);
    }

    private void listDir(File d, boolean details) {
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
    }

    private void changeDir(String[] argv) throws ParseException {
        CommandLine cmd = parser.parse(lsOpts, argv);
        String[] args = cmd.getArgs();
        if (args.length != 1) {
            throw new IllegalArgumentException("cd is expecting exactly one argument");
        }
        changeDir(args[0], out);
    }

    boolean changeDir(String dirName, PrintWriter out)  {
		return (changeDir(dirName, false, out));
	}

	private void listCatalog() {
		Cataloger cataloger = (Cataloger)ProviderLookup.getService(Cataloger.class);
		if (cataloger != null) {
			try {
                String[] providers = cataloger.getProviderList();
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
		Cataloger cataloger = (Cataloger)ProviderLookup.getService(Cataloger.class);
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
			PrintWriter out) {
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
		if (subCmd.equals("ls")) {
			return "ls [-l]";
		} else if (subCmd.equals("dir")) {
			return "dir [-l]";
		} else if (subCmd.equals("pwd")) {
			return "pwd";
		} else if (subCmd.equals("cd")) {
			return "cd <directory name> | ~";
		} else {
			return COMMAND_USAGE;
		}
	}
	
	public String getLongDescription(String subCmd) {
		if (subCmd.equals("ls")) {
			return "Directory listing of the current working directory.";
		} else if (subCmd.equals("dir")) {
				return "Synonomous with the ls command.";
		} else if (subCmd.equals("pwd")) {
			return "Returns working directory path.";
		} else if (subCmd.equals("cd")) {
			return "Changes the current working directory. The \"~\" character can be used, this will change back to the nsh's home directory";
		} else {
			return COMMAND_HELP;
		}
	}
	
}
