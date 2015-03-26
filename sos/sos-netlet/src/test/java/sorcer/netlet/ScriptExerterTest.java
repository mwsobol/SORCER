package sorcer.netlet;

import org.junit.Assert;
import org.junit.Test;
import sorcer.service.Job;

import java.io.*;

/**
 * SORCER class
 * User: prubach
 * Date: 02.07.13
 */
public class ScriptExerterTest {

    final static String SCRIPT_FILE="src/test/resources/f1.ntl";

    @Test
    public void testLoadNetletFromFile() throws Exception {
        ScriptExerter se = new ScriptExerter(new File(SCRIPT_FILE));
        Assert.assertNotNull(se.getScript());
    }

    @Test
    public void testParseNetlet() throws Throwable {
        ScriptExerter se = new ScriptExerter(new File(SCRIPT_FILE));
        se.parse();
        Assert.assertNotNull(se.getTarget() instanceof Job);
    }


}
