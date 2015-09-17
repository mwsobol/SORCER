package sorcer.netlet;

import org.junit.Assert;
import org.junit.Test;
import sorcer.service.Job;

import java.io.File;

/**
 * SORCER class
 * User: prubach
 * Date: 02.07.13
 */
public class ScriptExerterTest {

    final static String SCRIPT_FILE1="src/test/resources/f1.ntl";
    final static String SCRIPT_FILE2="src/test/resources/f2.ntl";
    final static String SCRIPT_FILE3="src/test/resources/f3.ntl";

    @Test
    public void testParseNetletF1() throws Throwable {
        ScriptExerter se = new ScriptExerter(new File(SCRIPT_FILE1));
        se.parse();
        Assert.assertNotNull(se.getTarget() instanceof Job);
    }

    @Test
    public void testParseNetletF2() throws Throwable {
        ScriptExerter se = new ScriptExerter(new File(SCRIPT_FILE2));
        se.parse();
        Assert.assertNotNull(se.getTarget() instanceof Job);
    }

    @Test
    public void testParseNetletF3() throws Throwable {
        ScriptExerter se = new ScriptExerter(new File(SCRIPT_FILE3));
        se.parse();
        Assert.assertNotNull(se.getTarget() instanceof Job);
    }

}
