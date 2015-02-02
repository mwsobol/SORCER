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

import com.sun.jini.thread.TaskManager;
import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.UuidFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.Contexts;
import sorcer.core.context.ControlContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.ObjectBlock;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.provider.ControlFlowManager;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ProviderDelegate;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.*;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

import javax.security.auth.Subject;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * ServiceBean - The SORCER superclass of service components of ServiceProvider.
 * 
 * @author Mike Sobolewski
 */
abstract public class RendezvousBean implements Service, Executor {
	private Logger logger = Logger.getLogger(RendezvousBean.class.getName());

	protected ServiceProvider provider;

	protected ProviderDelegate delegate;
	
	protected TaskManager threadManager;
	
	public RendezvousBean() throws RemoteException {
		// do nothing
	}
	
	public void init(Provider provider) {
		this.provider = (ServiceProvider)provider;
		this.delegate = ((ServiceProvider)provider).getDelegate();
		this.threadManager = ((ServiceProvider)provider).getThreadManager();
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}

	public String getProviderName()  {
		try {
			return provider.getProviderName();
		} catch (RemoteException e) {
			// ignore local call
			return null;
		}
	}
	
	public TaskManager getThreadManager() {
		return provider.getThreadManager();
	}
		
	private void initLogger() {
		Handler h = null;
		try {
			logger = Logger.getLogger("local." + provider.getClass().getName() + "."
					+ provider.getProviderName());
			h = new FileHandler(System.getProperty(SorcerConstants.SORCER_HOME)
					+ "/logs/remote/local-Jobber-" + provider.getDelegate().getHostName() + "-" + provider.getProviderName()
					+ "%g.log", 20000, 8, true);
			if (h != null) {
				h.setFormatter(new SimpleFormatter());
				logger.addHandler(h);
			}
			logger.setUseParentHandlers(false);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** {@inheritDoc} */
	public boolean isAuthorized(Subject subject, Signature signature) {
		return true;
	}
	
	protected void replaceNullExertionIDs(Exertion ex) {
		if (ex != null && ((ServiceExertion) ex).getId() == null) {
			((ServiceExertion) ex)
					.setId(UuidFactory.generate());
			if (((ServiceExertion) ex).isJob()) {
				for (int i = 0; i < ((Job) ex).size(); i++)
					replaceNullExertionIDs(((Job) ex).get(i));
			}
		}
	}

	protected void notifyViaEmail(Exertion ex) throws ContextException {
		if (ex == null || ((ServiceExertion) ex).isTask())
			return;
		Job job = (Job) ex;
		Vector recipents = null;
		String notifyees = ((ControlContext) ((NetJob)job).getControlContext())
				.getNotifyList();
		if (notifyees != null) {
			String[] list = SorcerUtil.tokenize(notifyees, SorcerConstants.MAIL_SEP);
			recipents = new Vector(list.length);
			for (int i = 0; i < list.length; i++)
				recipents.addElement(list[i]);
		}
		String to = "", admin = Sorcer.getProperty("sorcer.admin");
		if (recipents == null) {
			if (admin != null) {
				recipents = new Vector();
				recipents.addElement(admin);
			}
		} else if (admin != null && !recipents.contains(admin))
			recipents.addElement(admin);

		if (recipents == null)
			to = to + "No e-mail notifications will be sent for this job.";
		else {
			to = to + "e-mail notification will be sent to\n";
			for (int i = 0; i < recipents.size(); i++)
				to = to + "  " + recipents.elementAt(i) + "\n";
		}
		String comment = "Your job '" + job.getName()
				+ "' has been submitted.\n" + to;
		((ControlContext) ((NetJob)job).getControlContext()).setFeedback(comment);
		if (job.getMasterExertion() != null
				&& ((ServiceExertion) job.getMasterExertion()).isTask()) {
			((ServiceExertion) (job.getMasterExertion())).getContext()
					.putValue(Context.JOB_COMMENTS, comment);

			Contexts.markOut(((ServiceExertion) (job.getMasterExertion()))
					.getContext(), Context.JOB_COMMENTS);

		}
	}
	
	public void setServiceID(Mogram ex) {
		if (provider == null) {
			try {
				provider = new ServiceProvider();
				init (provider);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		try {
			ServiceID id = provider.getProviderID();
			if (id != null) {
				logger.finest(id.getLeastSignificantBits() + ":"
						+ id.getMostSignificantBits());
				((ServiceExertion) ex).setLsbId(id.getLeastSignificantBits());
				((ServiceExertion) ex).setMsbId(id.getMostSignificantBits());
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private String getDataURL(String filename) {
		return delegate.getProviderConfig().getProperty(
				"provider.dataURL")
				+ filename;
	}

	private String getDataFilename(String filename) {
		return delegate.getProviderConfig().getDataDir() + "/"
				+ filename;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.ServiceBean#service(sorcer.service.Exertion, net.jini.core.transaction.Transaction)
	 */
	@Override
	public Mogram service(Mogram mogram, Transaction transaction) throws RemoteException, ExertionException {
		try {
			Exertion exertion = (Exertion) mogram;
			setServiceID((Exertion)exertion);
			if (exertion instanceof ObjectJob || exertion instanceof ObjectBlock)
                return execute((Exertion)exertion, transaction);
            else {
            	ControlFlowManager cm = new ControlFlowManager((Exertion)exertion, delegate);
            	return cm.process(threadManager); 
            }
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new ExertionException();
		}
	}

	public Mogram service(Mogram mogram) throws RemoteException, ExertionException, TransactionException {
		return service(mogram, null);
	}
		
	abstract public Mogram execute(Mogram mogram, Transaction txn)
			throws TransactionException, ExertionException, RemoteException;
	
	public Mogram execute(Mogram mogram)
			throws TransactionException, ExertionException, RemoteException {
		return execute(mogram, null);
	}

}
