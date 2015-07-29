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

package sorcer.core.provider.exerter;

import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionManager;
import org.dancres.blitz.jini.lockmgr.LockResult;
import org.dancres.blitz.jini.lockmgr.MutualExclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.dispatch.*;
import sorcer.core.provider.*;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.ext.ProvisioningException;
import sorcer.jini.lookup.ProviderID;
import sorcer.service.*;
import sorcer.service.Exec.State;
import sorcer.service.Strategy.Access;
import sorcer.service.modeling.ModelingTask;
import sorcer.service.txmgr.TransactionManagerAccessor;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ServiceShell implements Shell, Service, Exerter, Callable {
	protected final static Logger logger = LoggerFactory.getLogger(ServiceShell.class
			.getName());

	private ServiceMogram mogram;
	private Transaction transaction;
	private static MutualExclusion locker;

	public ServiceShell() {
	}

	public ServiceShell(Mogram mogram) {
		this.mogram = (ServiceMogram) mogram;
	}

	public ServiceShell(Mogram mogram, Transaction txn) {
		this.mogram = (ServiceExertion) mogram;
		transaction = txn;

	}

	// a refrence to a provider running this service bean
	private Provider provider;

	public void init(Provider provider) {
		this.provider = provider;
	}
	
	public <T extends Mogram> T  exert(Arg... entries) throws TransactionException,
			MogramException, RemoteException {
		return exert((Transaction) null, (String) null, entries);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Exerter#exert(sorcer.service.Exertion, sorcer.service.Parameter[])
	 */
	@Override
	public Mogram exert(Mogram mogram, Arg... entries)
			throws TransactionException, MogramException, RemoteException {
		try {
			mogram.substitute(entries);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return exert(mogram, (Transaction) null, (String) null);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exerter#exert(sorcer.service.Exertion, net.jini.core.transaction.Transaction, sorcer.service.Parameter[])
	 */
	@Override
	public Mogram exert(Mogram mogram, Transaction txn, Arg... entries)
			throws TransactionException, MogramException, RemoteException {
		try {
			mogram.substitute(entries);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		transaction = txn;
		return exert(mogram, txn, (String) null);
	}

	public  <T extends Mogram> T exert(String providerName) throws TransactionException,
			MogramException, RemoteException {
		return exert(null, providerName);
	}

	@Override
	public Mogram service(Mogram exertion, Transaction txn) throws TransactionException, MogramException, RemoteException {
		return exert((Exertion)exertion, txn);
	}

	@Override
	public Mogram service(Mogram exertion) throws TransactionException, MogramException, RemoteException {
		return exert((Exertion)exertion);
	}

	public  <T extends Mogram> T exert(T xrt, Transaction txn, String providerName)
			throws TransactionException, MogramException, RemoteException {
		this.mogram = (ServiceExertion) xrt;
		transaction = txn;
		return exert(txn, providerName);
	}


	public <T extends Mogram> T  exert(Transaction txn, String providerName, Arg... entries)
			throws TransactionException, MogramException, RemoteException {
		try {
			if (mogram instanceof Exertion) {
				ServiceExertion exertion = (ServiceExertion)mogram;
				mogram.selectFidelity(entries);
				Exertion xrt = postProcessExertion((Exertion) exert0(txn, providerName,
						entries));
				if (exertion.isProxy()) {
					exertion.setContext(xrt.getDataContext());
					exertion.setControlContext((ControlContext)xrt.getControlContext());
					if (exertion.isCompound()) {
						((CompoundExertion) mogram)
								.setMograms(xrt.getMograms());
					}
					return (T) mogram;
				} else {
					return (T) xrt;
				}
			}
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		return null;
	}
	
	private void initExecState(Arg... entries) throws MogramException, RemoteException {
		Context argCxt = null;
		if (entries!=null) {
			for (Arg arg : entries) {
				if (arg instanceof Context && ((Context)arg).size() > 0) {
					argCxt = (Context)arg;
				}
			}
		}
//		if (mogram instanceof Block) {
//			resetScope(mogram, argCxt, entries);
//		}
//		else if (mogram.getScope() != null) {
//			mogram.getDataContext().append((Context)mogram.getScope());
//		}
		Exec.State state = mogram.getExecState();
		if (state == State.INITIAL) {
			if(mogram instanceof Exertion) {
				mogram.getExceptions().clear();
				mogram.getTrace().clear();
			}
			for (Mogram e : mogram.getAllMograms()) {
				if (e instanceof Exertion) {
					if (((ControlContext) ((Exertion)e).getControlContext()).getExecState() == State.INITIAL) {
						e.setStatus(Exec.INITIAL);
						e.getExceptions().clear();
						e.getTrace().clear();
					}
				}
				if (e instanceof Block) {
					resetScope((Exertion)e, argCxt);
				} else {
					e.clearScope();
				}
			}
		}
	}

	private void resetScope(Exertion exertion, Context context, Arg... entries) throws MogramException, RemoteException {
		((ServiceContext)exertion.getDataContext()).clearScope();
		exertion.getDataContext().append(((ServiceContext)exertion.getDataContext()).getInitContext());
		if (entries != null) {
			for (Arg a : entries) {
				if (a instanceof Entry) {
					exertion.getContext().putValue(
							((Entry) a).path(), ((Entry) a).value());
				}
			}
		}
		if (context != null) {
			exertion.getDataContext().append(context);
		}
		for (Mogram mogram : exertion.getMograms()) {
			mogram.clearScope();
		}
	}

	private void realizeDependencies(Arg... entries) throws RemoteException,
			MogramException {
		if (mogram instanceof Exertion) {
			Exertion exertion = (ServiceExertion)mogram;
			List<Evaluation> dependers = exertion.getDependers();
			if (dependers != null && dependers.size() > 0) {
				for (Evaluation<Object> depender : dependers) {
					try {
						((Invocation) depender).invoke(mogram.getScope(), entries);
					} catch (InvocationException e) {
						throw new ExertionException(e);
					}
				}
			}
		}
	}

	public Mogram exert0(Transaction txn, String providerName, Arg... entries)
			throws TransactionException, MogramException, RemoteException {
		ServiceExertion exertion = null;
		if (mogram instanceof Exertion)
			exertion = (ServiceExertion) mogram;
		try {
			if (entries != null && entries.length > 0) {
				mogram.substitute(entries);
			}
			// check if the mogram has to be initialized (to original state)
			// or used as is after resuming from suspension or failure
			if (mogram.isInitializable()) {
				initExecState(entries);
			}
			realizeDependencies(entries);
			if (mogram instanceof Exertion && exertion.isTask() && exertion.isProvisionable()) {
				try {
					List<ServiceDeployment> deploymnets = exertion.getDeploymnets();
					if (deploymnets.size() > 0) {
						ProvisionManager provisionManager = new ProvisionManager(exertion);
						provisionManager.deployServices();
					}
				} catch (DispatcherException e) {
					throw new ExertionException(
							"Unable to deploy services for: "
									+ mogram.getName(), e);
				}
			}

			//TODO disabled due to problem with monitoring. Needs to be fixed to run with monitoring
			/*if (mogram instanceof Job && ((Job) mogram).size() == 1) {
				return processAsTask();
			} */
			transaction = txn;
			Context<?> cxt = exertion.getDataContext();
			if (cxt != null)
				cxt.setExertion(exertion);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ExertionException(ex);
		}
		Signature signature = mogram.getProcessSignature();
		Service provider = null;
		try {
			// If the mogram is a job rearrange the inner exertions to make sure the
			// dependencies are not broken
			if (exertion.isJob()) {
				ExertionSorter es = new ExertionSorter(exertion);
				mogram = (ServiceExertion) es.getSortedJob();
			}
//			 execute modeling tasks
			if (mogram instanceof ModelingTask && mogram.getFidelity().getSelects().size() == 1) {
				return ((Task) mogram).doTask(txn);
			}

			// execute object tasks and jobs
			if (!(signature instanceof NetSignature)) {
				if (mogram instanceof Task) {
					if (mogram.getFidelity().getSelects().size() == 1) {
						return ((Task) mogram).doTask(txn);
					} else {
						try {
							return new ControlFlowManager().doTask((Task) mogram);
						} catch (ContextException e) {
							e.printStackTrace();
							throw new ExertionException(e);
						}
					}
				} else if (mogram instanceof Job) {
					return ((Job) mogram).doJob(txn);
				} else if (mogram instanceof Block) {
					return ((Block) mogram).doBlock(txn);
				}
			}
			// check for missing signature of inconsistent PULL/PUSH cases
			logger.info("signature (before) = " + signature);
			signature = correctProcessSignature();
			logger.info("signature (after) = " + signature);

			if (!((ServiceSignature) signature).isSelectable()) {
				exertion.reportException(new ExertionException(
						"No such operation in the requested signature: "
								+ signature));
				logger.warn("Not selectable mogram operation: " + signature);
				return exertion;
			}

			if (providerName != null && providerName.length() > 0) {
				signature.setProviderName(providerName);
			}
			if (logger.isDebugEnabled())
				logger.debug("* ExertProcessor's servicer accessor: "
						+ Accessor.getAccessorType());
			provider = ((NetSignature) signature).getService();
		} catch (SignatureException e) {
			e.printStackTrace();
			new ExertionException(e);
		} catch (ContextException e) {
			e.printStackTrace();
			new ExertionException(e);
		} catch (SortingException e) {
			e.printStackTrace();
			new ExertionException(e);
		}
		if (provider == null) {
			// handle signatures for PULL tasks
			if (!exertion.isJob()
					&& exertion.getControlContext().getAccessType() == Access.PULL) {
				signature = new NetSignature("service", Spacer.class, Sorcer.getActualSpacerName());
				provider = (Service) Accessor.getService(signature);
			} else {
				provider = (Service) Accessor.getService(signature);
				if (provider == null && exertion.isProvisionable() && signature instanceof NetSignature) {
					try {
						logger.debug("Provisioning: " + signature);
						provider = ServiceDirectoryProvisioner.getProvisioner().provision(signature);
					} catch (ProvisioningException pe) {
						logger.warn("Provider not available and not provisioned: " + pe.getMessage());
						mogram.setStatus(Exec.FAILED);
						exertion.reportException(new RuntimeException(
								"Cannot find provider and provisioning returned error: " + pe.getMessage()));
						return exertion;
					}
				}
			}
		}

		// Provider tasker = ProviderLookup.getProvider(mogram.getProcessSignature());
		// provider = ProviderAccessor.getProvider(null, signature
		// .getServiceInfo());
		if (provider == null) {
			logger.warn("* Provider not available for: " + signature);
			mogram.setStatus(Exec.FAILED);
			exertion.reportException(new RuntimeException(
					"Cannot find provider for: " + signature));
			return mogram;
		}
		mogram.trimAllNotSerializableSignatures();
		exertion.getControlContext().appendTrace(
				"shell: " + ((Provider) provider).getProviderName()
						+ ":" + ((Provider) provider).getProviderID());
		((NetSignature) signature).setProvider(provider);
		logger.info("Provider found for: " + signature + "\n\t" + provider);
		if (((Provider) provider).mutualExclusion()) {
			return serviceMutualExclusion((Provider) provider, exertion,
					transaction);
		} else {
//			 test mogram for serialization
//						 try {
//							 logger.info("ExertProcessor.exert0(): going to serialize mogram for testing!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//							 ObjectLogger.persistMarshalled("exertionfile", mogram);
//						 } catch (Exception e) {
//							 e.printStackTrace();
//						 }
			Mogram result = provider.service(mogram, transaction);
			if (result != null && result.getExceptions().size() > 0) {
				for (ThrowableTrace et : result.getExceptions()) {
					Throwable t = et.getThrowable();
					logger.error("Got exception running: " + mogram.getName() + " " + t.getMessage());
					logger.debug("Exception details: " + t.getMessage());
					if (t instanceof Error)
						result.setStatus(Exec.ERROR);
				}
				result.setStatus(Exec.FAILED);
			} else if (result == null) {
				exertion.reportException(new ExertionException("ExertionDispatcher failed calling: "
						+ mogram.getProcessSignature()));
				mogram.setStatus(Exec.FAILED);
				result = exertion;
			}
			return result;
		}
	}
	
	private Mogram processAsTask() throws RemoteException,
			TransactionException, MogramException {
		ServiceExertion exertion = null;
		if (mogram instanceof ServiceExertion)
				exertion = (ServiceExertion)mogram;
		Task task = (Task) exertion.getMograms().get(0);
		task = task.exert();
		exertion.getMograms().set(0, task);
		mogram.setStatus(task.getStatus());
		return mogram;
	}

	private Exertion serviceMutualExclusion(Provider provider,
			Exertion exertion, Transaction transaction) throws RemoteException,
			TransactionException, MogramException {
		ServiceID mutexId = provider.getProviderID();
		if (locker == null) {
			locker = (MutualExclusion) ProviderLookup
					.getService(MutualExclusion.class);
		}
		TransactionManager transactionManager = TransactionManagerAccessor
				.getTransactionManager();
		Transaction txn = null;

		LockResult lr = locker.getLock(""
				+ exertion.getProcessSignature().getServiceType(),
				new ProviderID(mutexId), txn,
				((ServiceExertion) exertion).getId());
		if (lr.didSucceed()) {
			((ControlContext)exertion.getControlContext()).setMutexId(provider.getProviderID());
			Exertion xrt = provider.service(exertion, transaction);
			txn.commit();
			return xrt;
		} else {
			// try continue to get lock, if failed abort the transaction txn
			txn.abort();
		}
		((ControlContext)exertion.getControlContext()).addException(
				new ExertionException("no lock avaialable for: "
						+ provider.getProviderName() + ":"
						+ provider.getProviderID()));
		return exertion;
	}

	/**
	 * Depending on provider access type correct inconsistent signatures for
	 * composite exertions only. Tasks go either to its provider directly or
	 * Spacer depending on their provider access type (PUSH or PULL).
	 * 
	 * @return the corrected signature
	 */
	public Signature correctProcessSignature() {
		ServiceExertion exertion = null;
		if (mogram instanceof ServiceExertion)
			exertion = (ServiceExertion)mogram;
		if (!exertion.isJob())
			return mogram.getProcessSignature();
		Signature sig = mogram.getProcessSignature();
		if (sig != null) {
			Access access = exertion.getControlContext().getAccessType();
			if (Access.PULL == access
					&& !mogram.getProcessSignature().getServiceType()
							.isAssignableFrom(Spacer.class)) {
				sig.setServiceType(Spacer.class);
				((NetSignature) sig).setSelector("service");
				sig.setProviderName(SorcerConstants.ANY);
				sig.setType(Signature.Type.PROC);
				exertion.getControlContext().setAccessType(access);
			} else if (Access.PUSH == access
					&& !sig.getServiceType()
							.isAssignableFrom(Jobber.class)) {
				if (sig.getServiceType().isAssignableFrom(Spacer.class)) {
					sig.setServiceType(Jobber.class);
					((NetSignature) sig).setSelector("service");
					sig.setProviderName(SorcerConstants.ANY);
					sig.setType(Signature.Type.PROC);
					exertion.getControlContext().setAccessType(access);
				}
			}
		} else {
			sig = new NetSignature("service", Jobber.class);
		}
		return sig;
	}
	
	public static Exertion postProcessExertion(Exertion exertion)
			throws ContextException, RemoteException {
		List<Mogram> mograms = exertion.getAllMograms();
		for (Mogram mogram : mograms) {
			if (mogram instanceof Exertion) {
				List<Setter> ps = ((ServiceExertion) mogram).getPersisters();
				if (ps != null) {
					for (Setter p : ps) {
						if (p != null && (p instanceof Par) && ((Par) p).isMappable()) {
							String from = (String) ((Par) p).getName();
							Object obj = null;
							if (mogram instanceof Job)
								obj = ((Job) mogram).getJobContext().getValue(from);
							else {
								obj = ((Exertion) mogram).getContext().getValue(from);
							}

							if (obj != null)
								p.setValue(obj);
						}
					}
				}
			}
		}
		return exertion;
	}

	private boolean isShellRemote() {
		return provider != null;
	}
	
	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public String toString() {
		if (mogram == null)
			return "Exerter";
		else
			return "Exerter for: " + mogram.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Object call() throws Exception {
		return mogram.exert(transaction);
	}

}
