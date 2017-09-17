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
package sorcer.core.signature;

import sorcer.core.provider.ProviderName;
import sorcer.service.Arg;
import sorcer.service.Fidelity;
import sorcer.service.ServiceFidelity;
import sorcer.service.Signature;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.sig;

public class ModelSignature extends ServiceSignature implements sig {

	static final long serialVersionUID = 5482425096559139341L;

	private Functionality var;

	private Fidelity fidelity;

	private Signature innerSignature;
	
	private Model model;

	public ModelSignature(String selector, Class serviceType, String providerName, Arg... parameters) {
		super(selector, selector);
		this.serviceType.providerType = serviceType;
		if (providerName == null || providerName.length() == 0)
			this.providerName = new ProviderName(ANY);
		else
			this.providerName = new ProviderName(providerName);
		for (Arg p : parameters) {
			if (p instanceof ReturnPath) {
				this.returnPath = (ReturnPath)p;
			}
		}
	}

	public ModelSignature(String selector, Model model) {
		super(selector, selector);
		this.model = model;
	}

	public ModelSignature(String selector, Class serviceType, Arg... paramters) {
		this(selector, serviceType, null, paramters);
	}
	
	public ModelSignature(String selector, Signature innerSignature, Arg... paramters) {
		super(selector, selector);
		this.innerSignature = innerSignature;
		if (innerSignature instanceof NetSignature)
			providerName = innerSignature.getProviderName();
		for (Arg p : paramters) {
			if (p instanceof ReturnPath) {
				this.returnPath = (ReturnPath)p;
			}
		}
	}

	public ModelSignature(String selector, Fidelity fidelity,
						  Signature targetSignature, Arg... paramters) {
		this(selector, targetSignature);
		this.fidelity = fidelity;
		for (Arg p : paramters) {
			if (p instanceof ReturnPath) {
				this.returnPath = (ReturnPath)p;
			}
		}
	}

	/**
	 * <p>
	 * Returns the var for this signature.
	 * </p>
	 * 
	 * @return the evaluation
	 */
	public Functionality<?> getVar() {
		return var;
	}

	public Signature getInnerSignature() {
		return innerSignature;
	}

	public void setInnerSignature(Signature innerSignature) {
		this.innerSignature = innerSignature;
	}

	/**
	 * <p>
	 * Returns the vFi of a var of this signature.
	 * </p>
	 * 
	 * @return the fidelity
	 */
	public Fidelity getFidelity() {
		return fidelity;
	}

	/**
	 * <p>
	 * Assigns the evaluation of a var of this signature.
	 * </p>
	 * 
	 * @param fidelity
	 *            the fidelity to setValue
	 */
	public void setFidelity(ServiceFidelity fidelity) {
		this.fidelity = fidelity;
	}

	public Model getModel() {
		return model;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	public String toString() {
		return this.getClass() + ":" + operation.selector + ":" + providerName + ";" + execType + ";"
				+ isActive + ";" + var + ";" + fidelity + ";/n" + "inner: " + innerSignature;
	}
}
