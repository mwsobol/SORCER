package sorcer.tools.shell.cmds;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.webster.Webster;
import sorcer.util.TimeUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Handle stats command
 */
public class InfoCmd extends ShellCmd {

    {
        COMMAND_NAME = "about";

        NOT_LOADED_MSG = "***command not loaded due to conflict";

        COMMAND_USAGE = "about [-a]";

        COMMAND_HELP = "Show properties of this 'nsh' shell;"
                + "  -a   list available external applications";
    }

    @Override
    public Options getOptions() {
        return super.getOptions()
                .addOption("a", false, "all")
                .addOption("s", false, "short");
    }

    public void execute(String command, CommandLine cmd) throws IOException, ClassNotFoundException {
        PrintStream out = shell.getOutputStream();
        assert out != null;

        out.println("SORCER Network Shell (nsh " + NetworkShell.CUR_VERSION + ", JVM: " + System.getProperty("java.version"));

        if (!cmd.hasOption('s'))
            printDetails();

        if (cmd.hasOption('a'))
            printAllDetails(out);

        out.println("Type 'quit' to terminate the shell");
        out.println("Type 'help' for command help");

    }

    private void printAllDetails(PrintStream out) {
        out.println("Available applications: ");
        Iterator<Map.Entry<String, String>> mi = NetworkShell.appMap.entrySet()
                .iterator();
        Map.Entry<String, String> e;
        while (mi.hasNext()) {
            e = mi.next();
            out.println("  " + e.getKey() + "  at: " + e.getValue());
        }
    }

    private void printDetails() throws IOException, ClassNotFoundException {
        PrintStream out = shell.getOutputStream();
        long currentTime = System.currentTimeMillis();

        out.println("  User: " + System.getProperty("user.name"));
        out.println("  Home directory: " + shell.getHomeDir());
        out.println("  Current directory: " + NetworkShell.currentDir);

        out.println("  Login time: " + new Date(NetworkShell.startTime).toString());
        out.println("  Time logged in: "
                + TimeUtil.format(currentTime - NetworkShell.startTime));
        if (shell.getShellLog() != null)
            out.println("  Log file : "
                    + shell.getShellLog().getAbsolutePath());

        out.println();
        Webster web = shell.getWebster();
        if (web == null) {
            out.println("Class server: No HTTP server started");
        } else {
            out.println("Webster URL: \n  URL: http://"
                    + web.getAddress() + ":" + web.getPort()
                    + "\n  Roots: " + web.getRoots());
            out.println("  Codebase: " + System.getProperty("java.rmi.server.codebase"));
        }

        out.println();
        out.println("Lookup groups: "
                + (NetworkShell.groups == null ? "all groups" : Arrays
                .toString(NetworkShell.groups)));

        DiscoCmd.printCurrentLus(shell);
        EmxCmd.printCurrentMonitor(shell);
//			VarModelCmd.printCurrentModel();
        DataStorageCmd.printCurrentStorer(shell);
        LookupCmd.printCurrentService(shell);
    }
}
