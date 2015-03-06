package sorcer.tools.shell.cmds;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.WhitespaceTokenizer;

public class HelpCmd extends ShellCmd {

    {
        COMMAND_NAME = "help";

        NOT_LOADED_MSG = "***command not loaded due to conflict";

        COMMAND_USAGE = "help <command> | ? ";

        COMMAND_HELP = "Describes the Network Shell (nsh) commands";
    }

    public void execute(String command, String[] cmd) {
        // noninteractive shell
        WhitespaceTokenizer tokenizer = NetworkShell.getShellTokenizer();
        if (tokenizer == null) {
            shell.listCommands();
            return;
        }

        if (tokenizer.countTokens() > 0) {
            String option = tokenizer.nextToken();
            if (NetworkShell.getCommandTable().get(option) != null) {
                NetworkShell.shellOutput.println("Usage: "
                        + NetworkShell.getCommandTable().get(option).getUsage(option) + "\n");
                NetworkShell.shellOutput.println(NetworkShell.getCommandTable().get(option)
                        .getLongDescription(option));
            } else
                NetworkShell.shellOutput.print("unknown command for " + option + "\n");
        } else {
            shell.listCommands();
        }
    }

}
