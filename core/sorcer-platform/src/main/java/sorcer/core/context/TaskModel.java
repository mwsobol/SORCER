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
package sorcer.core.context;

import sorcer.service.Signature;
import sorcer.service.Task;
import sorcer.service.modeling.ServiceModel;

/**
 *  * The SORCER model task extending the basic task implementation {@link Task}.
 * 
 * @author Mike Sobolewski
 */
public class TaskModel  {

	private static final long serialVersionUID = 1L;

	protected Signature builder;

	protected ServiceModel model;

	protected ContextSelection modelSelector;

	public TaskModel() {
		// do nathing
	}

	public TaskModel(ServiceModel model) {
		  this(model, null);
	}

	public TaskModel(ServiceModel model, ContextSelection modelSelector) {
		this.model = model;
		this.modelSelector = modelSelector;
	}

	public TaskModel(Signature builder) {
		this(builder, null);
	}

	public TaskModel(Signature builder, ContextSelection modelSelector) {
		this.builder = builder;
		this.modelSelector = modelSelector;
	}

	public ServiceModel getModel() {
		return model;
	}

	public void setModel(ServiceModel model) {
		this.model = model;
	}

	public Signature getBuilder() {
		return builder;
	}

	public void setBuilder(Signature builder) {
		this.builder = builder;
	}

	public ContextSelection getModelSelector() {
		return modelSelector;
	}

	public void setModelSelector(ContextSelection modelSelector) {
		this.modelSelector = modelSelector;
	}

}
