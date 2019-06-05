package sorcer.worker.requestor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Routine;
import sorcer.service.Task;
import sorcer.util.Sorcer;
import sorcer.util.SorcerEnv;
import sorcer.worker.provider.Worker;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class WorkerTaskApplication {

	private final static Logger logger = LoggerFactory.getLogger(WorkerTaskApplication.class);

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();

		// getValue the queried provider key from the command line
		String pn = null;
		if (args.length == 1)
			pn = args[0];

		logger.info("Provider key: " + pn);

		Routine exertion = new WorkerTaskApplication().getExertion(pn);
		Routine result = exertion.exert();
		logger.info("Output context: \n" + result.getContext());
	}

	private Routine getExertion(String pn) throws Exception {
		String hostname = SorcerEnv.getHostName();

        if (pn!=null) pn = Sorcer.getActualName(pn);
        logger.info("Suffixed Provider key: " + pn);

        Context context = new ServiceContext("work");
        context.putValue("requstor/key", hostname);
        context.putValue("consumer/operand/1", 4);
        context.putValue("consumer/operand/2", 4);
        context.putValue("to/provider/key", pn);
        context.putValue("consumer/work", Works.work2);


        NetSignature signature = new NetSignature("doWork", Worker.class, pn);

		Task task = new NetTask("work", signature, context);

		return task;
	}
}
