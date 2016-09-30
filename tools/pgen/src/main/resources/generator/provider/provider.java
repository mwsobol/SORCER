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

import com.sun.jini.start.LifeCycle;
import $apiClass;
import $apiContext;
import mil.afrl.mstc.open.core.provider.EngineeringProvider;
import sorcer.service.Context;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link $apiClass} interface
 *
 * @author MSTC Engineering Project Generator
 */
public class $providerImpl extends EngineeringProvider implements $apiClassName {
    private final static Logger logger = $logger;

    /**
     * Default constructor, initializes the provider with the properties file loaded as a resource.
     *
     * @throws Exception If initialization fails.
     */
    public $providerImpl() throws Exception {
        this("$name/provider.properties");
    }

    /**
     * Create the provider using a properties file
     *
     * @param propFile The properties file, must not be {@code null}
     * @throws Exception If initialization fails.
     */
    public $providerImpl(final String propFile) throws Exception {
        super(propFile);
        initialize();
    }

    /**
     * Create the provider if called from the River {@link com.sun.jini.start.ServiceStarter} approach.
     *
     * @param args      Parameters that point to configuration file and possible overrides. Never {@code null}.
     * @param lifeCycle The lifecycle container to be notified if the provider terminates.
     * @throws Exception If initialization fails.
     */
    public $providerImpl(final String[] args, final LifeCycle lifeCycle) throws Exception {
        super(args, lifeCycle);
        initialize();
    }

    /*
     * Provides provider specific initialization and environment checking
     */
    private void initialize() {

    }

    public Context execute(final Context context) {
        long startTime = System.currentTimeMillis();
        logger.info("\\n************* Running execute *****************");
        File scratchDir = null;
        URL scratchUrl = null;
        String serviceIdString = doThreadMonitor(null);

		/*
		 * You need to fill in the details here for provider implementation.
		 */
        try {
            logger.info("Obtaining scratch directory ... ");
            scratchDir = getScratchDir(context, "$scratchDir" + serviceIdString + "_");
            logger.info("Scratch directory = [{}]\\n", scratchDir.getAbsolutePath());

            $contextClassName $contextName = ($contextClassName) context;
            /* Get the input file and write it out locally */
            URL input = $getInput();
            File localInput = new File(scratchDir, "input.txt");
            getDataService().download(input, localInput);

            /* Read the input file, and write out an output file with additional content */
            List<String> lines = new ArrayList<String>();
            lines.add("Hello pre-pended in $providerImpl to input");
            lines.addAll(Files.readAllLines(localInput.toPath(), Charset.defaultCharset()));
            File output = new File(scratchDir, "output.txt");
            Files.write(output.toPath(), lines, Charset.defaultCharset());

            /* Return the output file to the client as a URL that can be read remotely */
            URL outputUrl = getScratchURL(output);
            $setOutput(outputUrl);

        } catch (Exception e) {
            StringBuilder reason = new StringBuilder();
            reason.append("*** error: $providerImpl caught exception = ").append(e.getClass().getName())
                .append("\\n\\tscratchDir = ").append(scratchDir)
                .append("\\n\\tscratchUrl = ").append(scratchUrl);

            context.reportException(reason.toString(), e, getProviderInfo("execute", scratchDir, scratchUrl));
            logger.error(reason.toString(), e);
            return context;
        } finally {
            doThreadMonitor(serviceIdString);
            doTimeKeeping((System.currentTimeMillis() - startTime) / 1000);
        }
        logger.info("\\n\\n *************** Returning from service execute ********************* \\n Output context = {}"
                    +"\\n********************************************************************\\n\\n", context);
        return context;
    }

}
