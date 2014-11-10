package sorcer.worker.provider.impl;

import java.net.InetAddress;
import java.rmi.RemoteException;

import sorcer.core.context.ServiceContext;
import sorcer.core.provider.ServiceTasker;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.Worker;

import com.sun.jini.start.LifeCycle;

@SuppressWarnings("rawtypes")
public class WorkerProvider extends ServiceTasker implements Worker {
	
	private String hostName;
	
	public WorkerProvider() throws Exception {
		hostName = InetAddress.getLocalHost().getHostName();
	}
	
	public WorkerProvider(String[] args, LifeCycle lifeCycle) throws Exception {
		super(args, lifeCycle);
		hostName = InetAddress.getLocalHost().getHostName();
	}

	public Context sayHi(Context context) throws RemoteException,
			ContextException {
		context.putValue("provider/host/name", hostName);
		String reply = "Hi" + " " + context.getValue("requestor/name") + "!";
		setMessage(context, reply);
		return context;
	}

	public Context sayBye(Context context) throws RemoteException,
			ContextException {
		context.putValue("provider/host/name", hostName);
		String reply = "Bye" + " " + context.getValue("requestor/name") + "!";
		setMessage(context, reply);
		return context;
	}

	public Context doWork(Context context) throws InvalidWork, RemoteException,
			ContextException {
        context.putValue("provider/host/name", hostName);
        String sigPrefix = ((ServiceContext)context).getCurrentPrefix();
        String path = "requestor/work";
        if (sigPrefix != null && sigPrefix.length() > 0)
        	path = sigPrefix + "/" + path;
        Object workToDo = context.getValue(path);
        if (workToDo != null && (workToDo instanceof Work)) {
            // requestor's work to be done
            Context out = ((Work)workToDo).exec(context);
            context.putValue(((ServiceContext)out).getReturnPath().path, out.getReturnValue());
        } else {
            throw new InvalidWork("No Work found to do at path requestor/work'!");
        }
		String reply = "Done work by: "
                + (getProviderName() == null ? getClass() : getProviderName());
		setMessage(context, reply);

		// simulate longer execution time based on the value in
		// configs/worker-prv.properties
		String sleep = getProperty("provider.sleep.time");
		logger.info("sleep=" + sleep);
		if (sleep != null)
			try {
				context.putValue("provider/slept/ms", sleep);
				Thread.sleep(Integer.parseInt(sleep));
			} catch (Exception e) {
				throw new ContextException(e);
			}
		return context;
	}
	
	private String setMessage(Context context, String reply)
			throws ContextException {
		String previous = (String) context.getValue("provider/message");
		String message = "";
		if (previous != null && previous.length() > 0)
			message = previous + "; " + reply;
		else
			message = reply;
		context.putValue("provider/message", message);
		return message;
	}
	
}
