/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.core.provider.rendezvous;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.Dispatcher;
import sorcer.core.context.ControlContext;
import sorcer.core.dispatch.DispatcherException;
import sorcer.core.dispatch.ExertDispatcherFactory;
import sorcer.core.dispatch.SpaceTaskDispatcher;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Spacer;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * ServiceSpacer - The SORCER rendezvous service provider that provides
 * coordination for executing exertions using JavaSpace from which provides PULL
 * exertions to be executed.
 * 
 * @author Mike Sobolewski
 */
public class ServiceSpacer extends RendezvousBean implements Spacer {
	private Logger logger = Logger.getLogger(ServiceSpacer.class.getName());
	private LokiMemberUtil myMemberUtil;

	/**
	 * ServiceSpacer - Default constructor
	 * 
	 * @throws RemoteException
	 */
	public ServiceSpacer() throws RemoteException {
		myMemberUtil = new LokiMemberUtil(ServiceSpacer.class.getName());
	}

	public Mogram execute(Mogram mogram, Transaction txn)
			throws TransactionException, RemoteException {
		Exertion exertion = (Exertion) mogram;
		if (exertion.isJob())
			return doJob(exertion);
		else
			return doTask(exertion);
	}

	public Exertion doJob(Exertion job) {
		setServiceID(job);
		try {
			if (((ControlContext)job.getControlContext()).isMonitorable()
					&& !((ControlContext)job.getControlContext()).isWaitable()) {
				replaceNullExertionIDs(job);
				notifyViaEmail(job);
				new JobThread((Job) job, provider).start();
				return job;
			} else {
				JobThread jobThread = new JobThread((Job) job, provider);
				jobThread.start();
				jobThread.join();
				Job result = jobThread.getResult();
				logger.finest("Result: " + result);
				return result;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	protected class JobThread extends Thread {

		// doJob method calls this internally
		private Job job;

		private Job result;

		private String jobID;

		private Provider provider;
		
		public JobThread(Job job, Provider provider) {
			this.job = job;
			this.provider = provider;
		}

		public void run() {
			logger.finest("*** JobThread Started ***");
			Dispatcher dispatcher = null;

			try {
				dispatcher = ExertDispatcherFactory.getFactory()
						.createDispatcher((NetJob) job,
								new HashSet<Context>(), false, myMemberUtil, provider);
				while (dispatcher.getState() != Exec.DONE
						&& dispatcher.getState() != Exec.FAILED
						&& dispatcher.getState() != Exec.SUSPENDED) {
					logger.fine("Dispatcher waiting for exertions... Sleeping for 250 milliseconds.");
					Thread.sleep(250);
				}
				logger.fine("Dispatcher State: " + dispatcher.getState());
			} catch (Exception e) {
				e.printStackTrace();
				 logger.warning("Error while executing space task: " +  result.getName());
				 result.reportException(e);
			} 
			result = (NetJob) dispatcher.getExertion();
			try {
				job.getControlContext().appendTrace(provider.getProviderName()  + " dispatcher: " 
						+ dispatcher.getClass().getName());
			} catch (RemoteException e) {
				// ignore it
			}
		}

		public Job getJob() {
			return job;
		}

		public Job getResult() throws ContextException {
			return result;
		}

		public String getJobID() {
			return jobID;
		}
	}

	protected class TaskThread extends Thread {

		// doJob method calls this internally
		private Task task;

		private Task result;

		private String taskID;
		
		private Provider provider;

		public TaskThread(Task task, Provider provider) {
			this.task = task;
			this.provider = provider;
		}

		public void run() {
			logger.info("*** TaskThread Started ***");
			SpaceTaskDispatcher dispatcher = null;

			try {
				dispatcher = (SpaceTaskDispatcher) ExertDispatcherFactory
						.getFactory().createDispatcher((NetTask) task,
								new HashSet<Context>(), false, myMemberUtil, provider);
				try {
					task.getControlContext().appendTrace(provider.getProviderName() + " dispatcher: " 
							+ dispatcher.getClass().getName());
				} catch (RemoteException e) {
					//ignore it, local call
				}
				while (dispatcher.getState() != Exec.DONE
						&& dispatcher.getState() != Exec.FAILED
						&& dispatcher.getState() != Exec.SUSPENDED) {
					logger.fine("Dispatcher waiting for a space task... Sleeping for 250 milliseconds.");
					Thread.sleep(250);
				}
				logger.fine("Dispatcher State: " + dispatcher.getState());
			} catch (DispatcherException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			result = (NetTask) dispatcher.getExertion();
		}

		public Task getTask() {
			return task;
		}

		public Task getResult() throws ContextException {
			return result;
		}

		public String getTaskID() {
			return taskID;
		}
	}
	
	public Exertion doTask(Exertion task) throws RemoteException {
		setServiceID(task);
		try {
			if (task.isMonitorable()
					&& !task.isWaitable()) {
				replaceNullExertionIDs(task);
				notifyViaEmail(task);
				new TaskThread((Task) task, provider).start();
				return task;
			} else {
				TaskThread taskThread = new TaskThread((Task) task, provider);
				taskThread.start();
				taskThread.join();
				Task result = taskThread.getResult();
				logger.finest("Spacer result: " + result);
				return result;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

}
