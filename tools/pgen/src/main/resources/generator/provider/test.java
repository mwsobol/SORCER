/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package $providerPackage;

import $contextClass;
import $apiPackageAndClass;
import mil.afrl.mstc.open.task.TaskFactory;
import mil.afrl.mstc.open.test.EngTestRunner;
import mil.afrl.mstc.open.test.EngTester;
import mil.afrl.mstc.open.test.ProjectContext;
import mil.afrl.mstc.open.utils.URLs;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import sorcer.core.exertion.NetTask;
import sorcer.data.DataService;
import sorcer.service.Exertion;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tester for the provider
 *
 * @author MSTC Engineering Project Generator
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("$relativePath/provider")
public class $className extends SorcerTester {
    private File dataDir = new File(System.getProperty("data.dir"));
    private DataService dataService;
    private static Logger logger = $logger;

    @Before
    public void setup() throws IOException {
        /* Get and start the data service */
        dataService = new DataService(dataDir.getPath()).start();
    }

    @Test
    public void testRun1() throws Exception {
        File inputFile = new File(dataDir, "input/input.txt");
        URL inputUrl = dataService.getDataURL(inputFile);

        $contextName context = new $contextName("$capName");
        context.setInput(inputUrl);

//        TaskFactory taskFactory = new TaskFactory(Sorcer.getActualName("$engProviderName"),
//                                                  $apiClass,
//                                                  System.getProperty("deploy.config"));
//        NetTask $taskName = taskFactory.createNetTask("execute", context);
//
//        Exertion result = $exert();
//        logger.info("Returned Task after exert: {}", result);
//        URL outputUrl = (($contextName) result.getContext()).getOutput();
//        List<String> resultContent = URLs.readLines(outputUrl);
//        logger.info("Context output = {}", resultContent);
//        Assert.assertTrue(resultContent.get(0).startsWith("Hello"));
    }
}
