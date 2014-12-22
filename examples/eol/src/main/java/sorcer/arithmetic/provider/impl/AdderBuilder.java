package sorcer.arithmetic.provider.impl;

import sorcer.core.context.model.par.ParModel;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;

import java.rmi.RemoteException;

import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 *
 */
public class AdderBuilder {

	@SuppressWarnings("rawtypes")
	public static ParModel getAdderModel() throws EvaluationException,
			RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("add", "x + y", pars("x", "y")));
		return pm;
	}
}
