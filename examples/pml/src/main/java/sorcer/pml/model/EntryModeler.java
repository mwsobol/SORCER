package sorcer.pml.model;

import sorcer.core.context.model.ent.EntryModel;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;

import java.rmi.RemoteException;

import static sorcer.co.operator.val;
import static sorcer.eo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.mo.operator.*;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public class EntryModeler {

	public static EntryModel getEntryModel() throws EvaluationException,
			RemoteException, ContextException {
		EntryModel em = entModel("ent-model");
		add(em, val("x", 10.0), val("y", 20.0));
		add(em, invoker("expr", "x + y + 30", args("x", "y")));
		return em;
	}

}
