package sorcer.worker.requestor;

import java.util.logging.Logger;

import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Task;
import sorcer.util.Log;
import sorcer.util.Sorcer;
import sorcer.util.SorcerEnv;
import sorcer.worker.provider.Worker;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class WorkerTaskRequestor {

	private static Logger logger = Log.getTestLog();

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();

		// get the queried provider name from the command line
		String pn = null;
		if (args.length == 1)
			pn = args[0];

		logger.info("Provider name: " + pn);

		Exertion exertion = new WorkerTaskRequestor().getExertion(pn);
		Exertion result = exertion.exert();
		logger.info("Output context: \n" + result.getContext());
	}

	private Exertion getExertion(String pn) throws Exception {
		String hostname = SorcerEnv.getHostName();

        if (pn!=null) pn = Sorcer.getActualName(pn);
        logger.info("Suffixed Provider name: " + pn);

        Context context = new ServiceContext("work");
        context.putValue("requstor/name", hostname);
        context.putValue("requestor/operand/1", 4);
        context.putValue("requestor/operand/2", 4);
        context.putValue("to/provider/name", pn);
        context.putValue("requestor/work", Works.work2);


        NetSignature signature = new NetSignature("doWork", Worker.class, pn);

		Task task = new NetTask("work", signature, context);

		return task;
	}
}
