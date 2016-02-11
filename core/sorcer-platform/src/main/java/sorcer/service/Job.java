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

package sorcer.service;

import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ControlContext;
import sorcer.core.context.FidelityContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.Spacer;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.security.util.Auth;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Signature.ReturnPath;
import sorcer.service.Strategy.Access;
import sorcer.util.SorcerUtil;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;

/**
 * A job is a composite service-oriented message comprised of {@link sorcer.service.Exertion}
 * instances with its own service {@link sorcer.service.Context} and a collection of service
 * {@link sorcer.service.Signature}s. The job's signature is usually referring to a
 * {@link Jobber} and the job's context describes the composition
 * of component exertions as defined by the Interpreter programming pattern.
 * 
 * @see Exertion
 * @see Task
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class Job extends CompoundExertion {

	private static final long serialVersionUID = -6161435179772214884L;

	/* our logger */
	protected final static Logger logger = LoggerFactory.getLogger(Job.class.getName());

	protected Job delegate;

	public Integer state = new Integer(INITIAL);

	/**
	 * Constructs a job and sets all default values to it.
	 * @throws sorcer.service.SignatureException
	 */
	public Job() {
		this("job-" + count++);
		// exertions = Collections.synchronizedList(new ArrayList<Exertion>());
	}

	/**
	 * Constructs a job and sets all default values to it.
	 * 
	 * @param name
	 *            The name of the job.
	 * @throws sorcer.service.SignatureException
	 */
	public Job(String name) {
		super(name);
	}

	/**
	 * Constructs a job and sets all default values to it.
	 * 
	 * @param mogram
	 *            The first Exertion of the job.
	 * @throws ContextException 
	 */
	public Job(Mogram mogram) throws ExertionException {
		this("job-" + count++);
		addMogram(mogram);
	}

	public Job(String name, String description) {
		this(name);
		this.description = description;
	}

	public Job(String name, String description, Fidelity fidelity) {
		this(name, description);
		this.serviceFidelity = fidelity;
	}

	/**
	 * Initialize it with assigning it a new ControlContext and a defaultMethod
	 * with serviceInfo as "sorcer.core.provider.jobber.ServiceJobber" name as
	 * "service" and providerName "*"
	 * @throws sorcer.service.SignatureException
	 */
	protected void init() {
		super.init();
		NetSignature s = new NetSignature("exert", Jobber.class);
		// Needs to be RemoteJobber for Cataloger to find it
		// s.setServiceType(Jobber.class.getName());
		s.setProviderName(null);
		s.setType(Signature.Type.PROC);
		serviceFidelity.selects.add(s); // Add the signature
	}

	public Fidelity getFidelity() {
//		if (fidelity != null)
//			for (int i = 0; i < fidelity.size(); i++)
//				signatures.get(i).setProviderName(controlContext.getRendezvousName());
		return serviceFidelity;
	}

	@Override
	public boolean isJob() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#isCompound()
	 */
	@Override
	public boolean isCompound() {
		return true;
	}

	public long getLsbID() {
		return (lsbId == null) ? -1 : lsbId.longValue();
	}

	/**
	 * Returns the number of exertions in this Job.
	 * 
	 * @return the number of exertions in this Job.
	 */
	public int size() {
		return mograms.size();
	}

	public int indexOf(Exertion ex) {
		return mograms.indexOf(ex);
	}

	public void setRendezvousName(String jobberName) {
		controlContext.setRendezvousName(jobberName);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#addMogram(sorcer.service.Exertion)
	 */
	@Override
	public Mogram addMogram(Mogram ex) throws ExertionException {
		mograms.add(ex);
		((ServiceExertion) ex).setIndex(mograms.indexOf(ex));
		try {
			controlContext.registerExertion(ex);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		((ServiceExertion) ex).setParentId(getId());
		return this;
	}

	public void addExertions(List<Exertion> Mogram) {
		if (this.mograms != null)
			this.mograms.addAll(mograms);
		else {
			this.mograms = new ArrayList<Mogram>();
			this.mograms.addAll(mograms);
		}
	}

	public List<Mogram> getMograms(List<Mogram> exs) {
		for (Mogram e : mograms)
			((ServiceExertion) e).getMograms(exs);
		exs.add(this);
		return exs;
	}

	public Job doJob(Transaction txn) throws MogramException,
			SignatureException, RemoteException, TransactionException {
		if (delegate == null) {
			if (serviceFidelity != null) {
				Signature ss = null;
				if (serviceFidelity.selects.size() == 1) {
					ss = serviceFidelity.selects.get(0);
				} else if (serviceFidelity.selects.size() > 1) {
					for (Signature s : serviceFidelity.selects) {
						if (s.getType() == Signature.SRV) {
							ss = s;
							break;
						}
					}
				}
				if (ss != null) {
					if (ss instanceof NetSignature) {
						delegate = new NetJob(name);
					} else if (ss instanceof ObjectSignature) {
						delegate = new ObjectJob(ss.getSelector());
						delegate.setName(name);
					}

					delegate.setFidelities(getFidelities());
					delegate.setFidelity(getFidelity());
					delegate.setSelectedFidelitySelector(serviceFidelitySelector);
					delegate.setContext(dataContext);
					delegate.setControlContext(controlContext);
				}
			}
			if (delegate instanceof NetJob) {
				delegate.setControlContext(controlContext);
				if (controlContext.getAccessType().equals(Access.PULL)) {
					Signature procSig = delegate.getProcessSignature();
					procSig.setServiceType(Spacer.class);
					delegate.serviceFidelity.selects.clear();
					delegate.addSignature(procSig);
				}
			}
			if (mograms.size() > 0) {
				for (Mogram ex : mograms) {
					delegate.addMogram(ex);
				}
			}
		}
		return delegate.doJob(txn);
	}

	public void undoJob() throws ExertionException, SignatureException,
			RemoteException {
		throw new ExertionException("Not implemneted by this Job: " + this);
	}
	
	public void setState(int state) {
		this.state = new Integer(state);
	}

	public int getState() {
		return state.intValue();
	}

	public String getPrincipalID() {
		Set principals = subject.getPrincipals();
		Iterator iterator = principals.iterator();
		while (iterator.hasNext()) {
			Principal p = (Principal) iterator.next();
			if (p instanceof SorcerPrincipal)
				return ((SorcerPrincipal) p).getId();
		}
		return null;
	}

	public void setPrincipalID(String id) {
		Set principals = subject.getPrincipals();
		Iterator iterator = principals.iterator();
		while (iterator.hasNext()) {
			Principal p = (Principal) iterator.next();
			if (p instanceof SorcerPrincipal)
				((SorcerPrincipal) p).setId(id);
		}
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
		for (int i = 0; i < size(); i++) {
			((ServiceExertion) get(i)).setSubject(subject);
		}
	}

	public void setPrincipal(SorcerPrincipal principal) {
		setSubject(Auth.createSubject(principal));
		this.principal = principal;
	}

	public Subject getSubject() {
		return subject;
	}
	
	public ServiceID getServiceID() {
		if (lsbId == null || msbId == null)
			return null;
		else
			return new ServiceID(msbId.longValue(), lsbId.longValue());
	}
	
	/**
	 * Returns a string representation of Contexts of this Job, containing the
	 * String representation of each context in it's exertion.
	 * @throws sorcer.service.ExertionException
	 */
	public String jobContextToString() throws ExertionException {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mograms.size(); i++) {
			if (((ServiceExertion) get(i)).isJob())
				sb.append(((Job) get(i)).jobContextToString());
			else
				sb.append(((ServiceExertion) get(i)).contextToString());
		}
		return sb.toString();
	}

	public void setMasterExertion(Exertion exertion) {
		controlContext.setMasterExertion(exertion);
	}

	public void setOwnerId(String id) {
		ownerId = id;
		if (controlContext != null)
			controlContext.setOwnerId(id);
		for (int i = 0; i < mograms.size(); i++)
			(((ServiceExertion) get(i))).setOwnerId(id);
	}

	public String getContextName() {
		return Context.JOB_ + name + "[" + index + "]" + Context.ID;
	}

	public String toString() {
		StringBuffer desc = new StringBuffer(super.toString());
		desc.append("\n=== START PRINTING JOB ===\n");	
		desc.append("\n=============================\nListing Component Exertions\n=============================\n");
		for (int i = 0; i < size(); i++) {
			desc.append("\n===========\n Exertion ").append(i).append("\n===========\n").append((get(i)).describe());
		}
		desc.append("\n=== DONE PRINTING JOB ===\n");
		return desc.toString();
	}

	@Override
	public List<ThrowableTrace> getExceptions() {
		List<ThrowableTrace> exceptions = new ArrayList<ThrowableTrace>();
		for (Mogram ext : mograms) {
			try {
				exceptions.addAll(ext.getExceptions());
			} catch (RemoteException e) {
				exceptions.add(new ThrowableTrace("Problem while collecting exceptions", e));
			}
		}
		return exceptions;
	}
	
	/**
	 * Return true if this composite <code>Job</code> is a tree.
	 * 
	 * @param visited
	 *            a set of visited exertions
	 * @return true if this <code>Job</code> composite is a tree
	 * @see sorcer.service.Exertion#isTree()
	 */
	public boolean isTree(Set visited) {
		visited.add(this);
		Iterator i = mograms.iterator();
		while (i.hasNext()) {
			ServiceExertion e = (ServiceExertion) i.next();
			if (visited.contains(e) || !e.isTree(visited)) {
				return false;
			}
		}
		return true;
	}

	public Mogram getExertion(int index) {
		return mograms.get(index);
	}

	public Context finalizeOutDataContext() throws ContextException {
		if (dataContext.getMogramStrategy().getOutConnector() != null) {
			updateContextWith(dataContext.getMogramStrategy().getOutConnector());
		}
		return dataContext;
	}

	// TODO in/out/inout marking as defined in the connector
	public Context updateContextWith(Context connector) throws ContextException {
		if (connector != null) {
			Context jobContext =  getJobContext();
			Iterator it = ((ServiceContext)connector).entryIterator();
			while (it.hasNext()) {
				Map.Entry e = (Map.Entry) it.next();
				dataContext.putInValue((String) e.getKey(), jobContext.getValue((String) e.getValue()));
				dataContext.removePath((String) e.getValue());
			}
		}
		return dataContext;
	}

	public Context getContext() throws ContextException {
		 return getJobContext();
	}
	
	public Context getJobContext() throws ContextException {
		ServiceContext cxt = new ServiceContext(name);
		cxt.setSubject("job/data/context", name);
		cxt.append(dataContext);
		return linkContext(cxt, getName());
	}

	public Context getControlInfo() {
		ServiceContext cxt = new ServiceContext(name);
		cxt.setSubject("job/control/context", name);
		
		return linkControlContext(cxt,  getName());
	}

	@Override
	public Context linkContext(Context context, String path) {
		Mogram ext;
		for (int i = 0; i < size(); i++) {
			ext = mograms.get(i);
			try {
				((ServiceExertion) ext).linkContext(context, path + CPS + ext.getName());
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}
		return context;
	}
	
	@Override
	public Context linkControlContext(Context context, String path) {
		Mogram ext;
		for (int i = 0; i < size(); i++) {
			ext = mograms.get(i);
			try {
				((ServiceExertion) ext).linkControlContext(context, path + CPS
						+ ext.getName());
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}
		return context;
	}

	public Object getJobValue(String path) throws ContextException {
		String[] attributes = SorcerUtil.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		String last = attributes[0];
		Mogram exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = ((Job) exti).getChild(attribute);
				if (exti instanceof Task) {
					last = attribute;
					break;
				}
			} else {
				break;
			}
		}
		int index = path.indexOf(last);
		String contextPath = path.substring(index + last.length() + 1);

		return exti.getContext().getValue(contextPath);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public Object getValue(String path, Arg... args) throws ContextException {
		if (path.startsWith("super")) {
			return ((Exertion)parent).getContext().getValue(path.substring(6), args);
		} else {
			if (path.indexOf(name) >= 0)
				return getJobValue(path);
			else
				return dataContext.getValue(path, args);
		}
	}
	
	public Object putValue(String path, Object value) throws ContextException {
		if (path.indexOf(name) >= 0)
			putJobValue(path, value);
		else
			super.putValue(path, value);
		return value;
	}
	
	public Object putJobValue(String path, Object value) throws ContextException {
		String[] attributes = SorcerUtil.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		String last = attributes[0];
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = (Exertion)((Job) exti).getChild(attribute);
				if (exti instanceof Task) {
					last = attribute;
					break;
				}
			} else {
				break;
			}
		}
		int index = path.indexOf(last);
		String contextPath = path.substring(index + last.length() + 1);
		exti.getContext().putValue(contextPath, value);
		return value;
	}
	
	public ReturnPath getReturnPath() {
		return dataContext.getReturnPath();
	}
	
	@Override
	public Object getReturnValue(Arg... entries) throws ContextException,
			RemoteException {
		//TODO for getReturnJobPath
		//ReturnPath rp = ((ServiceContext) dataContext).getReturnJobPath();
		ReturnPath rp = ((ServiceContext) dataContext).getReturnPath();
		Object obj = null;
		if (rp != null) {
			if (rp.path == null || rp.path.equals(Signature.SELF)) {
				return this;
			} else if (rp.type != null) {
				obj = rp.type.cast(getContext().getValue(rp.path));
			} else {
				obj = getContext().getValue(rp.path);
			}
		} else {
			obj = getJobContext();
		}
		return obj;
	}
	
	public Context getComponentContext(String path) throws ContextException {
		Exertion xrt = (Exertion)getComponentMogram(path);
		return xrt.getContext();
	}
	
	public Context getComponentControlContext(String path) {
		Exertion xrt = (Exertion)getComponentMogram(path);
		return xrt.getControlContext();
	}
	
	public Mogram getComponentMogram(String path) {
		String[] attributes = SorcerUtil.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = (Exertion)((CompoundExertion) exti).getChild(attribute);
				if (exti instanceof Task) {
					break;
				}
			} else {
				break;
			}
		}
		return exti;
	}
	
	public void applyFidelityContext(FidelityContext fiContext) throws ExertionException {
		Collection<Fidelity> fidelities = fiContext.values();
		ServiceExertion se = null;
		for (Fidelity fi : fidelities) {
			if (fi instanceof Fidelity) {
				se = (ServiceExertion) getComponentMogram(fi.getPath());
				se.selectFidelity(fi.getName());
			}
		}
	}

	@Override
	public ServiceExertion substitute(Arg... entries)
			throws SetterException {
		try {
			if (entries != null) {
				for (Arg e : entries) {
					if (e instanceof Entry)
						if (((Entry) e).path().indexOf(name) >= 0)
							putJobValue(((Entry) e).path(), ((Entry) e).value());

						else
							super.putValue(((Entry) e).path(),
									((Entry) e).value());

					// check for control strategy
					else if (e instanceof ControlContext) {
						updateControlContect((ControlContext)e);
					}
				}
			}
		} catch (ContextException ex) {
			ex.printStackTrace();
			throw new SetterException(ex);
		}
		return this;
	}
}
