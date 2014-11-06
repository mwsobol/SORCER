package junit.sorcer.core.context.model.par;

import static sorcer.po.operator.add;
import static sorcer.po.operator.invoker;
import static sorcer.po.operator.model;
import static sorcer.po.operator.par;
import static sorcer.po.operator.pars;

import java.rmi.RemoteException;

import sorcer.core.context.model.par.ParModel;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class ParModeler {

	public static ParModel getParModel() throws EvaluationException,
			RemoteException, ContextException {
		ParModel pm = model("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", pars("x", "y")));
		return pm;
	}

	public static ParModel getVarParModel() {
		try {
			ParModel rm = new ParModel();
			add(rm, par("x", 10.0), par("y", 20.0));
			add(rm, invoker("expr", "x + y + 30", pars("x", "y")));
			return rm;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
