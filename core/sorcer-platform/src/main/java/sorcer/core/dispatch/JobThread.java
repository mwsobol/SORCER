package sorcer.core.dispatch;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.config.ConfigurationException;

import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.ContextException;
import sorcer.service.Exec;
import sorcer.service.Job;

public class JobThread extends Thread {
	private final static Logger logger = Logger.getLogger(JobThread.class
			.getName());

	private static final int SLEEP_TIME = 250;
	// doJob method calls internally
	private Job job;

	private Job result;

	Provider provider;

	public JobThread(Job job, Provider provider) {
		this.job = job;
		this.provider = provider;
	}

	public void run() {
		logger.finer("*** Exertion dispatcher started with control context ***\n"
				+ job.getControlContext());
		Dispatcher dispatcher = null;
		try {
			dispatcher = ExertDispatcherFactory.getFactory().createDispatcher(job, provider);
			try {
				job.getControlContext().appendTrace(provider.getProviderName() +
						" dispatcher: " + dispatcher.getClass().getName());
			} catch (RemoteException e) {
				// ignore it, locall call
			}
			 int COUNT = 1000;
			 int count = COUNT;
			while (dispatcher.getState() != Exec.DONE
					&& dispatcher.getState() != Exec.FAILED
					&& dispatcher.getState() != Exec.SUSPENDED) {
				 count--;
				 if (count < 0) {
				 logger.finer("*** Jobber's Exertion Dispatcher waiting in state: "
				 + dispatcher.getState());
				 count = COUNT;
				 }
				Thread.sleep(SLEEP_TIME);
			}
			logger.finer("*** Dispatcher exit state = " + dispatcher.getClass().getName()  + " state: " + dispatcher.getState()
					+ " for job***\n" + job.getControlContext());
		} catch (DispatcherException de) {
			de.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		result = (Job) dispatcher.getExertion();
	}

	public Job getJob() {
		return job;
	}

	public Job getResult() throws ContextException {
		return result;
	}
}
