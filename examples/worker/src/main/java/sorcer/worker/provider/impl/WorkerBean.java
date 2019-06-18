package sorcer.worker.provider.impl;

import sorcer.core.context.ServiceContext;
import sorcer.service.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.Worker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class WorkerBean implements Worker {

	private Logger logger = LoggerFactory.getLogger(WorkerBean.class.getName());

	private Provider provider;

	private String hostName;
	
	public void init(Provider provider) {
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.provider = provider;
	}

	public Context sayHi(Context context) throws RemoteException,
			ContextException {
		context.putValue("prv/host/key", hostName);
		String reply = "Hi" + " " + context.getValue("req/key") + "!";
		setMessage(context, reply);
		return context;
	}

	public Context sayBye(Context context) throws RemoteException,
			ContextException {
		context.putValue("prv/host/key", hostName);
		String reply = "Bye" + " " + context.getValue("req/key") + "!";
		setMessage(context, reply);
		return context;
	}

	public Context doWork(Context context) throws InvalidWork, RemoteException,
			ContextException {
		context.putValue("prv/host/key", hostName);
		String sigPrefix = ((ServiceContext) context).getCurrentPrefix();
		String path = "req/work";
		if (sigPrefix != null && sigPrefix.length() > 0)
			path = sigPrefix + "/" + path;
		Object workToDo = context.getValue(path);
		if (workToDo != null && (workToDo instanceof Work)) {
			// consumer's work to be done
			Context out = ((Work) workToDo).exec(context);
			context.putValue(((ServiceContext) out).getRequestReturn().returnPath, out.getReturnValue());
		} else {
			throw new InvalidWork("No Work found to do at requestReturn consumer/work'!");
		}

		String reply = "Done work by: "
				+ (provider == null ? getClass().getName() : provider.getProviderName());
		setMessage(context, reply);

		return context;
	}
	
	private String setMessage(Context context, String reply) throws ContextException {
		String previous = null;
		try {
			previous = (String) context.getValue("prv/message");
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		String message = "";
		if (previous != null && previous.length() > 0)
			message = previous + "; " + reply;
		else
			message = reply;
		context.putValue("prv/message", message);
		return message;
	}
	
}
