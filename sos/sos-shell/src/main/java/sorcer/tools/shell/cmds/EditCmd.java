package sorcer.tools.shell.cmds;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.util.exec.ExecUtils;

import java.io.File;
import java.io.IOException;

import static sorcer.util.StringUtils.tName;

/**
* @author Rafał Krupiński
*/
public class EditCmd extends ShellCmd {

    {
        COMMAND_NAME = "edit";

        NOT_LOADED_MSG = "***command not loaded due to conflict";

        COMMAND_USAGE = "edit [<filename>]";

        COMMAND_HELP = "Open the default editor (NSH_EDITOR) or a file <filename>, "
                + "on Mac /Applications/TextEdit.app/Contents/MacOS/TextEdit can be used.";
    }

    public void execute(String command, String[] request) {
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
            cmd = NetworkShell.appMap.get("EDITOR");
        }
        if (cmd == null) {
            throw new NullPointerException(
                    "No editor specified for this shell!");
        }
        if (NetworkShell.getShellTokenizer().countTokens() > 0) {
            String option = NetworkShell.getShellTokenizer().nextToken();
            if (option != null && option.length() > 0) {
                try {
                    cmd = System.getenv("NSH_EDITOR") + " "
                            + NetworkShell.currentDir.getCanonicalPath()
                            + File.separator + option;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cmd;
    }
}
