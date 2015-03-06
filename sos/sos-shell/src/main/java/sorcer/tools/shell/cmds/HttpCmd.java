package sorcer.tools.shell.cmds;

import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.webster.Webster;
import sorcer.util.Sorcer;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Handle http command
 */
public class HttpCmd extends ShellCmd {

    {
        COMMAND_NAME = "http";

        NOT_LOADED_MSG = "***command not loaded due to conflict";

        COMMAND_USAGE = "http [port=<port-num>] [roots=<roots>] [jars=<codebase jars>] | stop";

        COMMAND_HELP = "Start and stop the nsf shell's code server;"
                + "  <roots> is semicolon separated list of directories.\n"
                + "  If not provided the root directory will be:\n"
                + "  [" + debugGetDefaultRoots() + "]";
    }

    public void execute(String command, String[] cmd) {
        if (NetworkShell.shellOutput == null)
            throw new NullPointerException(
                    "Must have an output PrintStream");
        StringTokenizer tok = new StringTokenizer(NetworkShell.getRequest());
        if (tok.countTokens() < 1)
            NetworkShell.shellOutput.print(getUsage("http"));
        int port = 0;
        /* The first token is the "http" token */
        tok.nextToken();
        while (tok.hasMoreTokens()) {
            String value = tok.nextToken();
            if (value.equals("stop")) {
                if (shell.getWebster() == null) {
                    NetworkShell.shellOutput.print("No HTTP server running\n");
                } else {
                    stopWebster(shell);
                    NetworkShell.shellOutput.print("Command successful\n");
                }
            }
            if (value.startsWith("port")) {
                StringTokenizer tok1 = new StringTokenizer(value, " =");
                if (tok1.countTokens() < 2)
                    NetworkShell.shellOutput.print(getUsage("http"));
                /* First token will be "port" */
                tok1.nextToken();
                /* Next token must be the port value */
                String sPort = tok1.nextToken();
                try {
                    port = Integer.parseInt(sPort);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    NetworkShell.shellOutput.print("Bad port-number value : " + sPort
                            + "\n");
                }
            }
            if (value.startsWith("roots")) {
                String[] values = value.split("=");
                String rootArg = values[1].trim();
                shell.httpRoots = NetworkShell.toArray(rootArg, " \t\n\r\f,;");
            }
            if (value.startsWith("jars")) {
                    String[] values = value.split("=");
                    String jarsArg = values[1].trim();
                    shell.httpJars = NetworkShell.toArray(jarsArg, " \t\n\r\f,;");
            }
        }
        if (shell.webster != null) {
            NetworkShell.shellOutput.print("An HTTP server is already running on port "
                    + "[" + shell.webster.getPort() + "], "
                    + "serving [" + shell.webster.getRoots()
                    + "], stop this " + "and continue [y/n]? ");
            if (NetworkShell.getShellInputStream() == null)
                NetworkShell.shellInput = new BufferedReader(new InputStreamReader(
                        System.in));
            try {
                String response = NetworkShell.shellInput.readLine();
                if (response != null) {
                    if (response.startsWith("y")
                            || response.startsWith("Y")) {
                        stopWebster(shell);
                        if (createWebster(port, shell.httpRoots, shell.httpJars, NetworkShell.shellOutput))
                            NetworkShell.shellOutput.println("Command successful\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                NetworkShell.shellOutput.print("Problem reading user input, "
                        + "Exception :" + e.getClass().getName() + ": "
                        + e.getLocalizedMessage() + "\n");
            }
        } else {
            if (createWebster(port, shell.httpRoots, shell.httpJars, NetworkShell.shellOutput))
                NetworkShell.shellOutput.println("Command successful\n");
        }
    }

    /**
     * Create a Webster instance
     *
     * @param port
     *            The port to use
     * @param roots
     *            Webster's roots
     * @param out
     *            A print stream for output
     *
     * @return True if created
     */
    public boolean createWebster(final int port, final String[] roots,
            String[] jars, PrintStream out) {
        return (createWebster(port, roots, false, jars, out, shell));
    }

    public static String debugGetDefaultRoots() {
        String sorcerLibDir = Sorcer.getHome() + File.separator
                + "lib" + File.separator + "sorcer" + File.separator
                + "lib";
        String sorcerLibDLDir = Sorcer.getHome()
                + File.separator + "lib" + File.separator + "sorcer"
                + File.separator + "lib-dl";
        String sorcerExtDir = Sorcer.getHome() + File.separator
                + "lib" + File.separator + "sorcer" + File.separator
                + "lib-ext";
        return (sorcerLibDir + ";" + sorcerLibDLDir + ";" + sorcerExtDir);
    }

    /**
     * Create a Webster instance
     *
     * @param port
     *            The port to use
     * @param roots
     *            Webster's roots
     * @param quiet
     *            Run without output
     * @param out
     *            A print stream for output
     *
     * @return True if created
     */
    public static boolean createWebster(final int port, final String[] roots,
                                        boolean quiet, String[] jars, PrintStream out, NetworkShell shell) {
        if (out == null)
            throw new NullPointerException(
                    "Must have an output PrintStream");
        try {
            String sorcerLibDir = Sorcer.getHome()
                    + File.separator + "lib" + File.separator + "sorcer"
                    + File.separator + "lib";
            String sorcerLibDLDir = Sorcer.getHome()
                    + File.separator + "lib" + File.separator + "sorcer"
                    + File.separator + "lib-dl";
            String sorcerExtDir = Sorcer.getHome()
                    + File.separator + "lib" + File.separator + "sorcer"
                    + File.separator + "lib-ext";

            String[] systemRoots = { sorcerLibDir, sorcerLibDLDir, sorcerExtDir };
            String[] realRoots = (roots == null ? systemRoots : roots);

            shell.webster = new Webster(port, realRoots,
                    shell.hostAddress, true);

            //System.out.println("webster: " + instance.hostAddress + ":"
                    //+ port);
            //System.out.println("webster roots: " + realRoots);
        } catch (Exception e) {
            e.printStackTrace();
            out.println("Problem creating HTTP server, " + "Exception :"
                    + e.getClass().getName() + ": "
                    + e.getLocalizedMessage() + "\n");

            return (false);
        }

        setCodeBase(jars, shell);

        if (!quiet) {
            out.println("Webster URL: http://" + shell.webster.getAddress()
                    + ":" + shell.webster.getPort());
            out.println("  Roots: " + shell.webster.getRoots());
        }
        return (true);
    }

    public static void setCodeBase(String[] jars, NetworkShell shell) {
        int port = shell.webster.getPort();
        String localIPAddress = shell.webster.getAddress();
        String codebase = "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < jars.length - 1; i++) {
            sb.append("http://").append(localIPAddress).append(":")
                    .append(port).append("/").append(jars[i]).append(" ");
        }
        sb.append("http://").append(localIPAddress).append(":")
                .append(port).append("/").append(jars[jars.length - 1]);
        codebase = sb.toString();
        System.setProperty("java.rmi.server.codebase", codebase);
        if (NetworkShell.logger.isDebugEnabled())
            NetworkShell.logger.debug("Setting nsh 'java.rmi.server.codebase': "
                    + codebase);
    }

    /**
     * Stop the webster instance
     * @param shell
     */
    public static void stopWebster(NetworkShell shell) {
        if (shell.webster != null)
            shell.webster.terminate();
        shell.webster = null;
    }
}
