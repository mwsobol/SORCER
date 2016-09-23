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

package sorcer.service;

import net.jini.core.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.provider.ControlFlowManager;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.core.signature.ServiceSignature;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A <code>Task</code> is an elementary service-oriented message
 * {@link Exertion} (with its own service {@link Context} and a collection of
 * service {@link sorcer.service.Signature}s. Signatures of four
 * {@link Signature.Type}s can be associated with each task:
 * <code>SERVICE</code>, <code>PREPROCESS</code>, <code>POSTROCESS</code>, and
 * <code>APPEND</code>. However, only a single <code>PROCESS</code> signature
 * can be associated with a task but multiple preprocessing, postprocessing, and
 * context appending methods can be added.
 * 
 * @see sorcer.service.Exertion
 * @see sorcer.service.Job
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class Task extends ServiceExertion {

	private static final long serialVersionUID = 5179772214884L;

	/** our logger */
	protected final static Logger logger = LoggerFactory.getLogger(Task.class
			.getName());

	public final static String argsPath = "method/args";

	// used for tasks with multiple signatures by CatalogSequentialDispatcher
	private boolean isContinous = false;

	protected Task delegate;
	
	public Task() {
		super("task-" + count++);
	}

	public Task(String name) {
		super(name);
	}

	public Task(Signature signature) {
		addSignature(signature);
	}
	
	public Task(String name, Signature signature) {
		this(name);
		addSignature(signature);
	}
	
	public Task(Signature signature, Context context) {
		addSignature(signature);
		if (context != null)
			dataContext = (ServiceContext)context;
	}
	
	public Task(String name, Signature signature, Context context) {
		this(name);
		addSignature(signature);
		if (context != null)
			dataContext = (ServiceContext)context;
	}
	
	public Task(String name, String description) {
		this(name);
		this.description = description;
	}

	public Task(String name, List<Signature> signatures) {
		this(name, "", signatures);
	}

	public Task(String name, String description, List<Signature> signatures) {
		this(name, description);
		this.selectedFidelity = new ServiceFidelity(name);
		this.selectedFidelity.type = ServiceFidelity.Type.SIG;
		this.selectedFidelity.selects.addAll(signatures);
		this.selectedFidelity.select = signatures.get(0);
	}

	public Task doTask(Arg... args) throws MogramException, SignatureException,
			RemoteException {
		return doTask(null, args);
	}
	
	public Task doTask(Transaction txn, Arg... args) throws ExertionException,
			SignatureException, RemoteException, MogramException {
		initDelegate();
		Task done = delegate.doTask(txn, args);
		setContext(done.getDataContext());
		setControlContext(done.getControlContext());
		return this;
	}

	public void initDelegate() throws ContextException, ExertionException, SignatureException {
		if (delegate != null && selectedFidelity != delegate.selectedFidelity) {
			delegate = null;
			dataContext.clearReturnPath();
		}

		try {
			if (delegate == null) {
				ServiceSignature ts = (ServiceSignature) selectedFidelity.select;
				if (ts.getClass() == ServiceSignature.class) {
					ts = createSignature(ts);
				}
				if (ts instanceof NetSignature) {
					delegate = new NetTask(name, ts);
				} else {
					delegate = new ObjectTask(name, ts);
				}
//				delegate.getSelectedFidelity().setSelect(ts);

				delegate.setFidelityManager(getFidelityManager());
				delegate.setFidelities(getFidelities());
//				delegate.setSelectedFidelity(getSelectedFidelity());
				delegate.setServiceMorphFidelity(getServiceMorphFidelity());
				delegate.setServiceMetafidelities(getServiceMetafidelities());
				delegate.setSelectedFidelitySelector(serviceFidelitySelector);
				delegate.setContext(dataContext);
				delegate.setControlContext(controlContext);
			}
		} catch (SignatureException e) {
			throw new ExertionException(e);
		}
	}

	private ServiceSignature createSignature(ServiceSignature signature) throws SignatureException {
		ServiceSignature sig;
		if (signature.getServiceType().isInterface()) {
			sig = new NetSignature(signature);
		} else {
			sig = new ObjectSignature(signature);
		}
		return sig;
	}

	public Task doTask(Exertion xrt, Transaction txn) throws ExertionException {
		// implemented for example by VarTask
		return null;
	}
	
	public void updateConditionalContext(Conditional condition)
			throws EvaluationException, ContextException {
		// implement is subclasses
	}

	public void undoTask() throws ExertionException, SignatureException,
			RemoteException {
		throw new ExertionException("Not implemneted by this Task: " + this);
	}

	@Override
	public boolean isTask()  {
		return true;
	}
	
	@Override
	public boolean isCmd()  {
		return (selectedFidelity.selects.size() == 1);
	}
	
	public boolean hasChild(String childName) {
		return false;
	}

	/** {@inheritDoc} */
	public boolean isJob() {
		return false;
	}

	public void setOwnerId(String oid) {
		// Util.debug("Owner ID: " +oid);
		this.ownerId = oid;
		if (selectedFidelity.selects != null)
			for (int i = 0; i < selectedFidelity.selects.size(); i++)
				((NetSignature) selectedFidelity.selects.get(i)).setOwnerId(oid);
		// Util.debug("Context : "+ context);
		if (dataContext != null)
			dataContext.setOwnerId(oid);
	}

	public ServiceContext doIt() throws ExertionException {
		throw new ExertionException("Not supported method in this class");
	}

	public boolean isNetTask() throws SignatureException {
		return getProcessSignature().getServiceType().isInterface();
	}

	// Just to remove if at all the places.
	public boolean equals(Task task) throws Exception {
		return name.equals(task.name);
	}

	public String toString() {
		if (delegate != null) {
			return delegate.toString();
		}
		StringBuilder sb = new StringBuilder(
				"\n=== START PRINTING TASK ===\nExertion Description: "
						+ getClass().getName() + ":" + name);
		sb.append("\n\tstatus: ").append(getStatus());
		sb.append(", task ID=");
		sb.append(getId());
		sb.append(", description: ");
		sb.append(description);
		sb.append(", priority: ");
		sb.append(priority);
		// .append( ", Index=" + getIndex())
		// sb.append(", AccessClass=");
		// sb.append(getAccessClass());
		sb.append(
				// ", isExportControlled=" + isExportControlled()).append(
				", providerName: ");
		if (getProcessSignature() != null)
			sb.append(getProcessSignature().getProviderName());
		sb.append(", principal: ").append(getPrincipal());
		try {
			sb.append(", serviceInfo: ").append(getServiceType());
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		sb.append(", selector: ").append(getSelector());
		sb.append(", parent ID: ").append(parentId);

		if (selectedFidelity.selects.size() == 1) {
			sb.append(getProcessSignature().getProviderName());
		} else {
			for (Signature s : selectedFidelity.selects) {
				sb.append("\n  ").append(s);
			}
		}
		String time = getControlContext().getExecTime();
		if (time != null && time.length() > 0)
			sb.append("\n\texec time=").append(time);
		sb.append("\n");
		sb.append(controlContext).append("\n");
//		sb.append(dataContext);
		sb.append(dataContext.getName() + ": ");
		sb.append(dataContext.getSubjectPath() + " = ");
		sb.append(dataContext.getSubjectValue());
		sb.append(dataContext.toString());
		sb.append("\n=== DONE PRINTING TASK ===\n");

		return sb.toString();
	}

	public String describe() {
		StringBuilder sb = new StringBuilder(this.getClass().getName() + ": "
				+ name);
		sb.append(" task ID: ").append(getId()).append("\n  process sig: ")
				.append(getProcessSignature());
		sb.append("\n  status: ").append(getStatus());
		String time = getControlContext().getExecTime();
		if (time != null && time.length() > 0)
			sb.append("\n  exec time: ").append(time);
		return sb.toString();
	}

	/**
	 * Returns true; elementary mograms are always "trees."
	 * 
	 * @param visited
	 *            ignored
	 * @return true; elementary mograms are always "trees"
	 * @see Exertion#isTree()
	 */
	public boolean isTree(Set visited) {
		visited.add(this);
		return true;
	}

	/**
	 * Returns a service task in the specified format. Some tasks can be defined
	 * for thin clients that do not use RMI or Jini.
	 * 
	 * @param type
	 *            the type of needed task format
	 * @return
	 */
	public Exertion getUpdatedExertion(int type) {
		// the previous implementation of ServiceTask (thin) and
		// RemoteServiceTask (thick) abandoned for a while.
		return this;
	}

	@Override
	public Context linkContext(Context context, String path) {
		try {
			((ServiceContext) context).putLink(path, getDataContext());
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return context;
	}

	@Override
	public Context linkControlContext(Context context, String path) {
		try {
			((ServiceContext) context).putLink(path, getControlContext());
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Exertion#addMogram(sorcer.service.Exertion)
	 */
	@Override
	public Mogram addMogram(Mogram component) {
		throw new RuntimeException("Tasks do not contain component mograms!");
	}

	public Task getDelegate() {
		return delegate;
	}

	public void setDelegate(Task delegate) {
		this.delegate = delegate;
	}
	
	/**
	 * <p>
	 * Returns <code>true</code> if this task takes its service context from the
	 * previously executed task in sequence, otherwise <code>false</code>.
	 * </p>
	 * 
	 * @return the isContinous
	 */
	public boolean isContinous() {
		return isContinous;
	}

	/**
	 * <p>
	 * Assigns <code>isContinous</code> <code>true</code> to if this task takes
	 * its service context from the previously executed task in sequence.
	 * </p>
	 * 
	 * @param isContinous
	 *            the isContinous to setValue
	 */
	public void setContinous(boolean isContinous) {
		this.isContinous = isContinous;
	}

	protected Task doBatchTask(Transaction txn) throws RemoteException,
			MogramException, SignatureException, ContextException {
		ControlFlowManager ep = new ControlFlowManager();
		return ep.doFidelityTask(this);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public Object getValue(String path, Arg... args) throws ContextException {
		Object val = dataContext.getValue(path, args);
		if (val == Context.none) {
			if (scope != null){
			val = scope.getValue(path, args);
			}
		}
		return val;
	}

	public List<Mogram> getMograms(List<Mogram> exs) {
		exs.add(this);
		return exs;
	}

	@Override
	public List<Mogram> getMograms() {
		List<Mogram> ml = new ArrayList<Mogram>();
		ml.add(this);
		return ml;
	}

	public Mogram clearScope() throws MogramException {
		if (!isContinous()) getDataContext().clearScope();
		return this;
	}

	public void correctBatchSignatures() {
		// if all signatures are of service process SRV type make all
		// except the last one of preprocess PRE type
		List<Signature> alls = selectedFidelity.selects;
		if (alls.size() > 1) {
			Signature lastSig = alls.get(alls.size() - 1);
			if (alls.size() > 1 && this.isBatch() && !(lastSig instanceof NetSignature)) {
				boolean allSrvType = true;
				for (Signature sig : alls) {
					if (!sig.getType().equals(Signature.SRV)) {
						allSrvType = false;
						break;
					}
				}
				if (allSrvType) {
					for (int i = 0; i < alls.size() - 1; i++) {
						alls.get(i).setType(Signature.PRE);
					}
				}
			}
		}
	}

	@Override
	public Object get(String component) throws ServiceException {
		return getFidelities().get(component);
	}

}
