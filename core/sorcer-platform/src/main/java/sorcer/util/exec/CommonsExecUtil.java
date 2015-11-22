package sorcer.util.exec;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute helper using apache commons exed
 *
 *  add this dependency to your pom.xml:
 <dependency>
 <groupId>org.apache.commons</groupId>
 <artifactId>commons-exec</artifactId>
 <version>1.2</version>
 </dependency>

 * @author wf
 *
 */
public class CommonsExecUtil {

    private final static Logger logger = LoggerFactory.getLogger(CommonsExecUtil.class);

    protected final static boolean debug = true;

    /**
     * LogOutputStream
     * http://stackoverflow.com/questions/7340452/process-output-from
     * -apache-commons-exec
     *
     * @author wf
     *
     */
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
                sb.append(s);
            return sb.toString();
        }
    }

    /**
     * execute the given command
     * @param cmd - the command
     * @param exitValue - the expected exit Value
     * @return the output as lines and exit Code
     * @throws Exception
     */
    public static ExecUtils.CmdResult execCmd(String cmd, String[] args) throws Exception {
        if (debug)
            logger.info("running "+cmd);
        CommandLine commandLine = CommandLine.parse(cmd);
        if (args!=null) commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        ExecResult out = new ExecResult();
        ExecResult err = new ExecResult();
        executor.setStreamHandler(new PumpStreamHandler(out, err));
        ExecUtils.CmdResult cmdResult = new ExecUtils.CmdResult(executor.execute(commandLine), out.toString(), err.toString());
        return cmdResult;
    }

}