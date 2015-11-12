package sorcer.pml.model;

import sorcer.core.context.model.par.ParModel;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;

import java.rmi.RemoteException;

import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class ParModeler {

	public static ParModel getParModel() throws EvaluationException,
			RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", args("x", "y")));
		return pm;
	}

}
