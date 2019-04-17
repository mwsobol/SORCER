package sorcer.shell;

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

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SORCER class
 * User: prubach
 * Date: 28.04.14
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class NshTest {

    private final static Logger logger = LoggerFactory.getLogger(NshTest.class);
    private static final String EXCEPTION = "Exception";
    private static String baseCmd;
    private String[] cmds;

    @BeforeClass
    public static void init() throws IOException {
        baseCmd = new StringBuilder(new java.io.File(Sorcer.getHomeDir(),
                "bin"+ java.io.File.separator + "nsh").getCanonicalPath()).toString();
    }


    @Test
    public void discoCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-c", "disco"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);
        assertTrue(res.contains("LOOKUP SERVICE"));
        assertTrue(res.contains(Sorcer.getLookupGroups()[0]));
        assertFalse(res.contains(EXCEPTION));
        if (!result.getErr().isEmpty())
            logger.info("discoCmdTest result ERROR: " + result.getErr());
        //assertFalse(result.getErr().contains(EXCEPTION));
    }

    @Test
    public void lupCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-c", "lup", "-s"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") + ":\n" + res);
        //assertTrue(res.contains(Sorcer.getActualName("Rendezvous")));
        //assertTrue(res.contains(Sorcer.getActualSpacerName()));
        assertTrue(res.contains("found"));
        assertTrue(res.contains("SERVICE PROVIDER #"));
        //assertTrue(res.contains(Sorcer.getActualDatabaseStorerName()));
        //assertTrue(res.contains(Sorcer.getLookupGroups()[0]));
        assertFalse(res.contains(EXCEPTION));
        if (!result.getErr().isEmpty())
            logger.info("lupCmdTest result ERROR: " + result.getErr());
        //assertFalse(result.getErr().contains(EXCEPTION));
    }

    @Test
    public void spCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-c", "sp"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);

        assertTrue(res.contains(Sorcer.getActualSpaceName()));
        assertFalse(res.contains(EXCEPTION));
        if (!result.getErr().isEmpty())
            logger.info("spCmdTest result ERROR: " + result.getErr());
        //assertFalse(result.getErr().contains(EXCEPTION));
    }

    @Test
    public void dsCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-c", "ds"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);

        assertTrue(res.contains(Sorcer.getActualDatabaseStorerName()));
        assertFalse(res.contains(EXCEPTION));
        if (!result.getErr().isEmpty())
            logger.info("dsCmdTest result ERROR: " + result.getErr());
        //assertFalse(result.getErr().contains(EXCEPTION));
    }

    @Test
    public void batchCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-b", getNshDir() + "/batch.nsh"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") +":\n" + res);
        if (!result.getErr().isEmpty())
            logger.info("batchCmdTest Result ERROR: " + result.getErr());
        //assertTrue(res.contains(Sorcer.getActualName("Rendezvous")));
//        assertTrue(res.contains(Sorcer.getActualSpacerName()));
//        assertTrue(res.contains(Sorcer.getActualDatabaseStorerName()));
//        assertTrue(res.contains(Sorcer.getLookupGroups()[0]));
//        assertFalse(res.contains(EXCEPTION));
//        assertFalse(result.getErr().contains(EXCEPTION));
    }

    //@Category(TestsRequiringRio.class)
    @Test(timeout = 120000)
    public void batchExertCmdTest() throws Exception {
        cmds = new String[] { baseCmd, "-b", getNshDir() + "/batchExert.nsh"};

        logger.info("Running: " + StringUtils.join(cmds, " ") + ":\n");
        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Result running: " + StringUtils.join(cmds, " ") + ":\n" + res);
        assertFalse(res.contains("RoutineException:"));
        assertTrue(res.contains("out/y = 300.0"));
        assertFalse(result.getErr().contains("RoutineException:"));
    }

    static  String getNshDir() {
        return String.format("%s/nsh", System.getProperty("user.dir"));
    }

}
