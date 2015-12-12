/*
 * Distribution Statement
 * 
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 * 
 * Disclaimer
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.service;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.ModelSignature;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.modeling.Modeling;
import sorcer.service.modeling.ModelingTask;

import java.rmi.RemoteException;

/**
 *  * The SORCER var-oriented model task extending the basic task implementation {@link Task}.
 * 
 * @author Mike Sobolewski
 */
abstract public class ModelTask extends Task implements ModelingTask {
	
	private static final long serialVersionUID = 7755033700015872647L;
	
	protected Modeling model;

	protected ServiceContext modelContext;

	public ModelTask() {
		// do nathing
	}

	public ModelTask(String name) {
		super(name);
	}

	abstract public Task doTask(Transaction txn, Arg... args) throws ExertionException,
			SignatureException, RemoteException;

	abstract protected ServiceContext createModelContext(ServiceContext context, Signature targetSignature)
	throws ContextException;
	
	public ServiceContext getModelContext() throws ContextException,
			SignatureException {
		if (model != null) {
			return createModelContext(dataContext, new ObjectSignature(
					getProcessSignature().getSelector(),
					model.getClass()));
		} else {
			Object model = instance((ObjectSignature) ((ModelSignature) getProcessSignature())
					.getInnerSignature());
			String selector = getProcessSignature().getSelector();
			Signature sig = null;
			if (model instanceof Class && ((Class) model).isInterface()) {
				sig = new NetSignature(selector, (Class) model);
			} else {
				try {
					sig = new ObjectSignature(selector, model, null);
				} catch (Exception e) {
					throw new SignatureException(e);
				}
			}
			return createModelContext(dataContext, sig);
		}
	}
	
	private  Object instance(ObjectSignature signature)
			throws SignatureException {
		
		if (signature.getSelector() == null
				|| signature.getSelector().equals("new"))
			return signature.newInstance();
		else
			return signature.initInstance();
	}
	
	public Modeling getModel() {
		return model;
	}
	
	public void setModel(Modeling model) {
		this.model = model;
	}

}
