package sorcer.provider.adder;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.util.Sorcer;
import sorcer.util.StringUtils;
import sorcer.util.exec.ExecUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    private String[] cmds;

    @BeforeClass
    public static void init() throws IOException {
        baseCmd = new StringBuilder(new java.io.File(Sorcer.getHomeDir(),
                "bin"+ java.io.File.separator + "nsh").getCanonicalPath()).toString();

        netletDir = new File("").getAbsolutePath() + "/src/main/netlets";
    }

    @Test
    public void evalNetletCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-c", "eval", netletDir + "/adder-local.ntl"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
//        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);
        if (!result.getErr().isEmpty())
            logger.info("batchCmdTest Result ERROR: " + result.getErr());
        assertFalse(result.getErr().contains(EXCEPTION));
        assertTrue(res.contains("out/y = 300.0"));
    }

    @Test
    public void batchCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-b", getNshDir() + "/batch.nsh"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
//        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);
        if (!result.getErr().isEmpty())
            logger.info("batchCmdTest Result ERROR: " + result.getErr());
        assertFalse(result.getErr().contains(EXCEPTION));
        assertTrue(res.contains("out/y = 300.0"));
    }

    static  String getNshDir() {
        return String.format("%s/nsh", System.getProperty("user.dir"));
    }

}
