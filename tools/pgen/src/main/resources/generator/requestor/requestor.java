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
package $requestorPackage;

$imports

//import mil.afrl.mstc.open.provider.ProviderStrategy;
import sorcer.requestor.ServiceRequestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.data.DataService;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.util.Sorcer;

import java.io.File;
import java.util.Properties;

/**
 * A requestor that uses the following providers:
 *
 * <ul>
$providerList
 * </ul>
 *
 * @author MSTC Engineering Project Generator
 */
public class $reqName {
    private static Logger logger = $logger;
    private File dataDir = new File(System.getProperty("data.dir"));
    private DataService dataService;
    private Properties properties;
    private EngineeringRequestor engineeringRequestor = new EngineeringRequestor();

    /**
     * Run the requestor
     *
     * @param args Arguments to process
     * @throws Exception If there are problems running the requestor
     */
    public void run(String... args) throws Exception {
        if (args.length == 0)
            throw new IllegalArgumentException("The requestor must be provided with arguments");

        /* Get and start the data service */
        dataService = engineeringRequestor.getDataService(dataDir.getPath());
        dataService.start();

        /* Load properties */
        properties = engineeringRequestor.getProperties("$props");

        int arg = Integer.parseInt(args[0]);
        if(arg==1) {
            run1();
        } else {
            logger.warn("Unknown option {}", arg);
        }
    }

    Exertion run1() throws Exception {
        /*
         * Uncomment the following to make your data file accessible
         * as an input to provider(s)
         */
        /*
        File dataFile = new File(dataDir, "");
        URL dataURL = dataService.getDataURL(dataFile);
        */

$netTaskGeneration
        Exertion result = null;

        /* Add additional logic to perform the exertions created */

        logger.info("Returned Task after exert: {}", result);

        return result;
    }


    public static void main(String[] args) throws Exception {
        new $reqName().run(args);
    }
}
    
