package sorcer.provider.adder;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.tools.shell.NetworkShell;
import sorcer.util.Sorcer;
import sorcer.util.StringUtils;
import sorcer.util.exec.ExecUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static sorcer.util.exec.ExecUtils.*;
import static sorcer.util.StringUtils.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class NetletTest {

    private final static Logger logger = LoggerFactory.getLogger(NetletTest.class);
    private static final String EXCEPTION = "Exception";
    private static String baseCmd;
    private static String netletDir;
    private String[] nshCmd;

    @BeforeClass
    public static void init() throws IOException {
        baseCmd = new StringBuilder(new java.io.File(Sorcer.getHomeDir(),
                "bin"+ java.io.File.separator + "nsh").getCanonicalPath()).toString();

        netletDir = new File("").getAbsolutePath() + "/src/main/netlets";
    }

    @Test
    public void evalNetletCmdTest() throws Exception {
        nshCmd = new String[] { baseCmd, "-c",  "eval", netletDir + "/adder-local.ntl"};

        ExecUtils.CmdResult result = execCommand(nshCmd);
        String out =  sysOut(result);
        String err =  sysErr(result);
        logger.info("Result running: " + join(nshCmd, " ") +":\n" + out);
        if (!sysErr(result).isEmpty())
            logger.info("batchCmdTest Result ERROR: " + err);
        assertFalse(err.contains(EXCEPTION));
        assertTrue(out.contains("300.0"));
    }

    @Test
    public void evalNetletCmdTestXXX() throws Exception {
        nshCmd = new String[] { "-c",  "eval", netletDir + "/adder-local.ntl"};
        NetworkShell.main(nshCmd);
        ExecUtils.CmdResult result = execCommand(nshCmd);
        String out =  sysOut(result);
        String err =  sysErr(result);
        logger.info("Result running: " + join(nshCmd, " ") +":\n" + out);
        if (!sysErr(result).isEmpty())
            logger.info("batchCmdTest Result ERROR: " + err);
        assertTrue(out.contains("300.0"));
    }

    @Test
    public void exertNetletCmdTest() throws Exception {
        nshCmd = new String[] { baseCmd, "-c",  "exert", netletDir + "/adder-local.ntl"};

        ExecUtils.CmdResult result = execCommand(nshCmd);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(nshCmd, " ") +":\n" + res);
        if (!result.getErr().isEmpty())
            logger.info("batchCmdTest Result ERROR: " + result.getErr());
        assertFalse(result.getErr().contains(EXCEPTION));
        assertTrue(res.contains("out/y = 300.0"));
    }

    @Test
    public void batchCmdTest() throws Exception {
        nshCmd = new String[] { baseCmd, "-b", getNshDir() + "/batch.nsh"};

        ExecUtils.CmdResult result = execCommand(nshCmd);
        String res =  result.getOut();
//        logger.info("Result running: " + StringUtils.join(nshCmd, " ") +":\n" + res);
        if (!result.getErr().isEmpty())
            logger.info("batchCmdTest Result ERROR: " + result.getErr());
        assertFalse(result.getErr().contains(EXCEPTION));
        assertTrue(res.contains("out/y = 300.0"));
    }

    static  String getNshDir() {
        return String.format("%s/nsh", System.getProperty("user.dir"));
    }


    @Test
    public void evalNetletCmdTestTmp() throws Exception {
        nshCmd = new String[] {"-c",  "eval", netletDir + "/adder-local.ntl"};
        NetworkShell.main(nshCmd);

//        ExecUtils.CmdResult result = execCommand(nshCmd);
//        String out =  sysOut(result);
//        String err =  sysErr(result);
//        logger.info("Result running: " + join(nshCmd, " ") +":\n" + out);
//        if (!sysErr(result).isEmpty())
//            logger.info("batchCmdTest Result ERROR: " + err);
//        assertFalse(err.contains(EXCEPTION));
//        assertTrue(out.contains("300.0"));
    }
}
