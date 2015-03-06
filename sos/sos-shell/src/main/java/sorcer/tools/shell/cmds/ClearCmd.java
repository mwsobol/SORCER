package sorcer.tools.shell.cmds;

import net.jini.core.lookup.ServiceItem;
import sorcer.service.ExertionInfo;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

import java.io.IOException;

/**
* @author Rafał Krupiński
*/
public class ClearCmd extends ShellCmd {

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

    public void execute(String command, String[] cmd) throws IOException, InterruptedException {
        if (NetworkShell.shellOutput == null)
            throw new NullPointerException(
                    "Must have an output PrintStream");
        WhitespaceTokenizer tokenizer = NetworkShell.getShellTokenizer();
        if (tokenizer.countTokens() == 1) {
            String option = tokenizer.nextToken();
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
            NetworkShell.shellOutput.println(COMMAND_USAGE);
        }
    }
}
