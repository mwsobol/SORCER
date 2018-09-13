package sorcer.util.exec;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Rubach
 * based on ExecUtils
 * @author wf
 *
 */
public class CommonsExecUtil {

    private final static Logger logger = LoggerFactory.getLogger(CommonsExecUtil.class);

    protected final static boolean debug = true;

    public static class ExecResult extends LogOutputStream {
        private final List<String> lines = new LinkedList<String>();

        @Override
        protected void processLine(String line, int level) {
            lines.add(line);
        }

        public List<String> getLines() {
            return lines;
        }

        public String toString()  {
            StringBuilder sb = new StringBuilder();
            for (String s : getLines())
                sb.append(s+"\n");
            return sb.toString();
        }
    }

    /**
     * execEnt the given command
     * @param cmd - the command
     * @return CmdResult
     * @throws IOException
     */
    public static ExecUtils.CmdResult execCommand(String cmd) throws IOException {
        return execCommand(cmd, null);
    }

    /**
     * execEnt the given command
     * @param cmd - the command
     * @param args - String array of arguments
     * @return CmdResult
     * @throws IOException
     */public static ExecUtils.CmdResult execCommand(String cmd, String[] args) throws IOException {
        return execCommand(cmd, args, null);
    }

    /**
     * execEnt the given command
     * @param cmd - the command
     * @param args - String array of arguments
     * @param inputStream - stdin
     * @return CmdResult
     * @throws IOException
     */
    public static ExecUtils.CmdResult execCommand(String cmd, String[] args, InputStream inputStream) throws IOException {
        return execCommand(cmd, args, inputStream, false);
    }


    /**
     * execEnt the given command
     * @param cmd - the command
     * @param args - String array of arguments
     * @param inputStream - stdin
     * @param handleQuotes
     * @return CmdResult
     * @throws IOException
     */
    public static ExecUtils.CmdResult execCommand(String cmd, String[] args, InputStream inputStream, boolean handleQuotes) throws IOException {
        if (debug)
            logger.info("running "+cmd);
        ExecResult out = new ExecResult();
        ExecResult err = new ExecResult();
        ExecUtils.CmdResult cmdResult;
        int exitValue = 0;
        try {
            CommandLine commandLine = CommandLine.parse(cmd);
            if (args != null) commandLine.addArguments(args, handleQuotes);
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(out, err, inputStream));
            exitValue = executor.execute(commandLine);
        } catch (ExecuteException ee) {
            exitValue = ee.getExitValue();
        }
        cmdResult = new ExecUtils.CmdResult(exitValue, out.toString(), err.toString());
        logger.info("Got OUT: " + out.toString());
        logger.info("Got ERR: " + err.toString());
        return cmdResult;
    }

}