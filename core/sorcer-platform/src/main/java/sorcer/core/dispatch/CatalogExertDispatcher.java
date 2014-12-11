/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.dispatch;

import java.rmi.RemoteException;
import java.util.Set;

import sorcer.core.Dispatcher;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.signature.NetSignature;
import sorcer.service.Accessor;
import sorcer.service.Block;
import sorcer.service.CompoundExertion;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.Service;
import sorcer.service.ServiceExertion;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.Task;

@SuppressWarnings("rawtypes")
abstract public class CatalogExertDispatcher extends ExertDispatcher
		implements SorcerConstants {

	private final static int SLEEP_TIME = 20;
	
	public CatalogExertDispatcher(Exertion job, 
            Set<Context> sharedContext,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) throws Throwable {
		super(job, sharedContext, isSpawned, provider, provisionManager);
		dThread = new DispatchThread();
		try {
			dThread.start();
			dThread.join();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			state = FAILED;
		}
	}
	
	protected void preExecExertion(Exertion exertion) throws ExertionException,
	SignatureException {
		// If Job, new dispatcher will update inputs for it's Exertion
		// in catalog dispatchers, if it is a job, then new dispatcher is
		// spawned
		// and the shared contexts are passed. So the new dispatcher will update
		// inputs
		// of tasks inside the jobExertion. But in space, all inputs to a new
		// job are
		// to be updated before dropping.
		try {
			exertion.getControlContext().appendTrace(provider.getProviderName() 
					+ " dispatcher: " + getClass().getName());
		} catch (RemoteException e) {
			// ignore it, local call		
		}
		logger.finest("preExecExertions>>>...UPDATING INPUTS...");
		try {
			if (exertion.isTask()) {
				updateInputs(exertion);
			}
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		((ServiceExertion) exertion).startExecTime();
		((ServiceExertion) exertion).setStatus(RUNNING);
	}

	// Parallel
	protected ExertionThread runExertion(ServiceExertion ex) {
		ExertionThread eThread = new ExertionThread(ex, this);
		eThread.start();
		return eThread;
	}

	// Sequential
	protected Exertion execExertion(Exertion ex) throws SignatureException,
			ExertionException {
		// set subject before task goes out.
		// ex.setSubject(subject);
		ServiceExertion result = null;
		try {
			preExecExertion(ex);
			if (ex.isTask()) {
				result = execTask((Task) ex);
			} else if (ex.isJob()) {
				result = execJob((Job) ex);
			} else if (ex.isBlock()) {
				result = execBlock((Block) ex);
			} else {
				logger.warning("Unknown ServiceExertion: " + ex);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// return original exertion with exception
			result = (ServiceExertion) ex;
			result.getControlContext().addException(e);
			result.setStatus(FAILED);
			setState(Exec.FAILED);
			return result;
		}
		// set subject after result is received
		// result.setSubject(subject);
		postExecExertion(ex, result);
		return result;
	}

	protected void postExecExertion(Exertion ex, Exertion result)
			throws SignatureException, ExertionException {
		ServiceExertion ser = (ServiceExertion) result;
		
		((CompoundExertion)xrt).setExertionAt(result, ((ServiceExertion) ex).getIndex());
		if (ser.getStatus() > FAILED && ser.getStatus() != SUSPENDED) {
			ser.setStatus(DONE);
			// update all outputs from shared context only for tasks. For jobs,
			// spawned dispatcher does it.
			try {
				if (((ServiceExertion) result).isTask()) {
					collectOutputs(result);
				}
				notifyExertionExecution(ex, result);
			} catch (ContextException e) {
				throw new ExertionException(e);
			}
		}
	}

	protected Task execTask(Task task) throws ExertionException,
			SignatureException, RemoteException {		
			Signature sig = task.getProcessSignature();
			if (sig != null && sig.getVariability() != null && xrt.isBlock()) {
				return task.doTask(xrt, null);
			} else if (task instanceof NetTask) {
				return execServiceTask(task);
			} else {
				return task.doTask(null);
			}
	}

	protected Task execServiceTask(Task task) throws ExertionException,
			SignatureException {
		Task result = null;
		try {
			if (((NetSignature) task.getProcessSignature())
					.getService() == provider) {
				logger.finer("\n*** getting result from delegate of "
						+ provider.getProviderName() + "... ***\n");
				result = ((ServiceProvider) provider).getDelegate().doTask(
						task, null);
				result.getControlContext().appendTrace(
						"delegate of: " + this.provider.getProviderName()
								+ "=>" + this.getClass().getName());
			} else {
				NetSignature sig = (NetSignature) task.getProcessSignature();
				// Catalog lookup or use Lookup Service for the particular
				// service
				Service service = Accessor.getService(sig);
				if (service == null) {
					String msg = null;
					// get the PROCESS Method and grab provider name + interface
					msg = "No Provider Available\n" + "Provider Name:      "
							+ sig.getProviderName() + "\n"
							+ "Provider Interface: " + sig.getServiceType();

					logger.info(msg);
					throw new ExertionException(msg, task);
				} else {
					// setTaskProvider(task, provider.getProviderName());
					task.setService(service);
					// client security
					/*					
					 * ClientSubject cs = null;
					 * try{ // //cs =
					 * (ClientSubject)ServerContext.getServerContextElement
					 * (ClientSubject.class); }catch (Exception ex){
					 * Util.debug(this, ">>>No Subject in the server call");
					 * cs=null; } Subject client = null; if(cs!=null){
					 * client=cs.getClientSubject(); Util.debug(this,
					 * "Abhijit::>>>>> CS was not null"); if(client!=null){
					 * Util.debug(this,"Abhijit::>>>>> Client Subject was not
					 * null"+client); }else{ Util.debug(this,"Abhijit::>>>>>>
					 * CLIENT SUBJECT WAS
					 * NULL!!"); } }else{ Util.debug(this, "OOPS! NULL CS"); }
					 * if(client!=null&&task.getPrincipal()!=null){
					 * Util.debug(this,"Abhijit:: >>>>>--------------Inside
					 * Client!=null, PRINCIPAL != NULL, subject="+client);
					 * result = (RemoteServiceTask)provider.service(task);
					 * }else{ Util.debug(this,"Abhijit::
					 * >>>>>--------------Inside null Subject"); result =
					 * (RemoteServiceTask)provider.service(task); }
					 */
					logger.finer("\n*** getting result from provider... ***\n");
					result = (NetTask) service.service(task, null);
					result.getControlContext().appendTrace(
							 ((Provider)service).getProviderName() + " dispatcher: "
									+ getClass().getName());
				}
			}
			logger.finer("\n*** got result: ***\n" + result);
		} catch (Exception re) {
			task.reportException(re);
			throw new ExertionException("Dispatcher failed for task: "
					+ xrt.getName(), re);
		}
		return result;
	}
	
	private Job execJob(Job job)
			throws DispatcherException, InterruptedException,
			ClassNotFoundException, ExertionException, RemoteException {

		try {
			/*
			 * Create a new dispatcher thread for the inner job, if no available
			 * Jobber is found in the network
			 */
			Dispatcher dispatcher = null;
			runningExertionIDs.addElement(job.getId());

			// create a new instance of a dispatcher
			dispatcher = ExertDispatcherFactory.getFactory()
					.createDispatcher(job, sharedContexts, true, provider);
			// wait until serviceJob is done by dispatcher
			while (dispatcher.getState() != DONE
					&& dispatcher.getState() != FAILED) {
				Thread.sleep(SLEEP_TIME);
			}
			Job out = (Job) dispatcher.getExertion();
			out.getControlContext().appendTrace(provider.getProviderName() 
					+ " dispatcher: " + getClass().getName());
			return out;
		} catch (RemoteException re) {
			re.printStackTrace();
			throw re;
		} catch (DispatcherException de) {
			de.printStackTrace();
			throw de;
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			throw ie;
		} 
	}

	private Block execBlock(Block block)
			throws DispatcherException, InterruptedException,
			ClassNotFoundException, ExertionException, RemoteException {

		try {
			Dispatcher dispatcher = null;
			runningExertionIDs.addElement(block.getId());

			// create a new instance of a dispatcher
			dispatcher = ExertDispatcherFactory.getFactory()
					.createDispatcher(block, sharedContexts, true, provider);
			// wait until serviceJob is done by dispatcher
			while (dispatcher.getState() != DONE
					&& dispatcher.getState() != FAILED) {
				Thread.sleep(SLEEP_TIME);
			}
			Block out = (Block) dispatcher.getExertion();
			out.getControlContext().appendTrace(provider.getProviderName() 
					+ " dispatcher: " + getClass().getName());
			return out;
		} catch (RemoteException re) {
			re.printStackTrace();
			throw re;
		} catch (DispatcherException de) {
			de.printStackTrace();
			throw de;
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			throw ie;
		} 
	}
	
	protected class ExertionThread extends Thread {

		private Exertion ex;

		private Exertion result;

		private ExertDispatcher dispatcher;

		public ExertionThread(ServiceExertion exertion,
				ExertDispatcher dispatcher) {
			ex = exertion;
			this.dispatcher = dispatcher;
			if (isMonitored)
				dispatchers.put(xrt.getId(), dispatcher);
		}

		public void run() {
			try {
				result = execExertion(ex);
			} catch (ExertionException ee) {
				ee.printStackTrace();
				result = ex;
				((ServiceExertion) result).setStatus(FAILED);
			} catch (SignatureException eme) {
				eme.printStackTrace();
				result = ex;
				((ServiceExertion) result).setStatus(FAILED);
			}
			dispatchers.remove(xrt.getId());
		}

		public Exertion getExertion() {
			return ex;
		}

		public Exertion getResult() {
			return result;
		}

	}

}
