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

import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.Contexts;
import sorcer.core.exertion.ObjectBlock;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.provider.*;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ModelingTask;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * ServiceBean - The SORCER superclass of service components of ServiceProvider.
 * 
 * @author Mike Sobolewski
 */
abstract public class RendezvousBean implements Service, Exerter {
	private Logger logger = LoggerFactory.getLogger(RendezvousBean.class.getName());

	protected ServiceProvider provider;

	protected ProviderDelegate delegate;
	
	//protected TaskManager threadManager;
	
	public RendezvousBean() throws RemoteException {
		// do nothing
	}
	
	public void init(Provider provider) {
		this.provider = (ServiceProvider)provider;
		this.delegate = ((ServiceProvider)provider).getDelegate();
		//this.threadManager = ((ServiceProvider)provider).getThreadManager();

	}

    public String getProviderName()  {
        return provider.getProviderName();
	}
	
	/** {@inheritDoc} */
	public boolean isAuthorized(Subject subject, Signature signature) {
		return true;
	}
	
	protected void replaceNullExertionIDs(Exertion ex) {
		if (ex != null && ((ServiceExertion) ex).getId() == null) {
			((ServiceExertion) ex)
					.setId(UuidFactory.generate());
			if (ex.isJob()) {
				for (int i = 0; i < ((Job) ex).size(); i++)
					replaceNullExertionIDs(((Job) ex).get(i));
			}
		}
	}

	protected void notifyViaEmail(Exertion ex) throws ContextException {
		if (ex == null || ex.isTask())
			return;
		Job job = (Job) ex;
		Vector recipents = null;
		String notifyees = job.getControlContext().getNotifyList();
		if (notifyees != null) {
			String[] list = SorcerUtil.tokenize(notifyees, ",");
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
		job.getControlContext().setFeedback(comment);
		if (job.getMasterExertion() != null
				&& job.getMasterExertion().isTask()) {
			job.getMasterExertion().getContext()
					.putValue(Context.JOB_COMMENTS, comment);

			Contexts.markOut((job.getMasterExertion()).getContext(), Context.JOB_COMMENTS);

		}
	}
	
    public void setServiceID(Mogram ex) {
        if (provider == null) {
            provider = new ServiceProvider();
            init (provider);
        }
        ServiceID id = provider.getProviderID();
        if (id != null) {
            logger.trace(id.getLeastSignificantBits() + ":"
                          + id.getMostSignificantBits());
            ((ServiceMogram) ex).setLsbId(id.getLeastSignificantBits());
            ((ServiceMogram) ex).setMsbId(id.getMostSignificantBits());
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
	public Mogram exert(Mogram mogram, Transaction transaction, Arg... args) throws RemoteException, ExertionException {
		Mogram out = null;
		try {
			setServiceID(mogram);
			mogram.appendTrace("mogram: " + mogram.getName() + " rendezvous: " +
					(provider != null ? provider.getProviderName() + " " : "")
					+ this.getClass().getName());
            if (mogram instanceof ObjectJob || mogram instanceof ObjectBlock
					|| mogram instanceof Model || mogram instanceof ModelingTask) {
				out = localExert(mogram, transaction, args);
			} else {
				out = getControlFlownManager(mogram).process();
			}

			if (mogram instanceof Exertion)
				mogram.getDataContext().setExertion(null);
        }
		catch (Exception e) {
			logger.debug("exert failed for: " + mogram.getName(), e);
			throw new ExertionException();
		}
		return out;
	}

    protected ControlFlowManager getControlFlownManager(Mogram exertion) throws ExertionException {
        try {
            if (exertion instanceof Exertion) {
                if (exertion.isMonitorable())
                    return new MonitoringControlFlowManager((Exertion)exertion, delegate, this);
                else
                    return new ControlFlowManager((Exertion)exertion, delegate, this);
            }
            else
                return null;
        } catch (Exception e) {
            ((Task) exertion).reportException(e);
            throw new ExertionException(e);
        }
    }

	abstract public Mogram localExert(Mogram mogram, Transaction txn, Arg... args)
			throws TransactionException, ExertionException, RemoteException;

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException {
		Mogram mog = Arg.getMogram(args);
		if (mog != null)
			try {
				return exert(mog, null, args);
			} catch (Exception e) {
				throw new MogramException(e);
			}
		else
			return null;
	}
}
