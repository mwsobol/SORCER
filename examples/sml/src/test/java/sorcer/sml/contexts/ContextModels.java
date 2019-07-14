package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.Copier;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.ent.Function;
import sorcer.core.context.model.ent.Prc;
import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.ContextDomain;
import sorcer.service.Invocation;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.result;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ContextModels {
	private final static Logger logger = LoggerFactory.getLogger(ContextModels.class);

	@Test
	public void entryModel() throws Exception {

		// use entModel to create an EntModel the same way as a regular context
		// or convert any context to entModel(<context>)
		EntModel mdl = entModel(val("arg/x1", 1.0), val("arg/x2", 2.0),
				val("arg/x3", 3.0), val("arg/x4", 4.0), val("arg/x5", 5.0));

		setValues(mdl, val("arg/x6", 6.0));
		assertTrue(exec(mdl, "arg/x6").equals(6.0));

		// prc is of the Evaluation multitype
		// args in models are evaluated
		setValues(mdl, val("arg/x6", val("overwrite", 20.0)));
		assertTrue(exec(mdl, "arg/x6").equals(20.0));

		// invoker is of the Invocation multitype
		put(mdl, prc("arg/x7", invoker("x1 + x3", args("x1", "x3"))));

		assertTrue(exec(mdl, "arg/x7").equals(4.0));
		assertTrue(get(mdl, "arg/x7") instanceof Function);
		assertTrue(get(mdl, "arg/x7") instanceof Prc);
		assertTrue(get(mdl, "arg/x7") instanceof Invocation);
	}

	@Test
	public void modelingInputsResponses() throws Exception {

		Model mdl = entModel(val("arg/x1", 1.0), val("arg/x2", 2.0),
				val("arg/x3", 3.0), val("arg/x4", 4.0), val("arg/x5", 5.0));

		add(mdl, prc("invoke", invoker("x1 + x3", args("x1", "x3"))));

		// declare the modeling responses
		responseUp(mdl, "invoke");
		// evaluate the model
		assertTrue(value(eval(mdl), "invoke").equals(4.0));

		// evaluate the model with overwritten inputs
		Arg inCxt = context(val("arg/x1", 2.0), val("arg/x2", 3.0));
		Double result = (Double) value(eval(mdl, inCxt), "invoke");
		assertTrue(result.equals(5.0));

		// compute the model with new inputs
		add(mdl, prc("invoke", invoker("x6 * x7 + x1", args("x1", "x6", "x7"))));
		result = (Double) value(eval(mdl, ent("arg/x6", 6.0), ent("arg/x7", 7.0)), "invoke");
		assertTrue(result.equals(44.0));
	}

	@Test
	public void modelDependencies() throws Exception {

		Model mdl1 = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));
		add(mdl1, ent("y1", invoker("x1 + x3", 	ents("x1", "x3"))));
		add(mdl1, ent("y2", invoker("x4 * x5", ents("x1", "x3"))));

		// mdl2 depends on values y1 and y2 calculated in cxt1
		Model mdl2 = entModel(ent("arg/y3", 8.0), ent("arg/y4", 9.0), ent("arg/y5", 10.0));
		add(mdl2, ent("invoke", invoker("y1 + y2 + y4 + y5", ents("y1", "y2", "y4", "y5"))));
		responseUp(mdl2, "invoke");

		// created dependency of mdl2 on mdl1 via a context copier
		Copier cp = copier(mdl1, ents("arg/x1", "arg/x2"), mdl2, ents("y1", "y2"));
		dependsOn(mdl2, mdlDep(cp));

		Double result = (Double) exec(mdl2);
//		logger.info("result: " + result);
		assertTrue(result.equals(22.0));
	}

	@Test
	public void evaluateMultiEntryResponseModel() throws Exception {
		
		Model mdl = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(mdl, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

		add(mdl, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

		// two respnse paths declared for the result
		responseUp(mdl, "add", "multiply");

		// compute the model
		Context result = response(mdl);

		logger.info("result: " + result);
		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
	}

	@Test
	public void exertEntModel() throws Exception {

		Model mdl = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(mdl, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

		add(mdl, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

		// two response paths declared
		responseUp(mdl, "add", "multiply");

		// exert the model
		Model model = exert(mdl);
		Context result = response(model);

		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
	}

	@Test
	public void contextEntryService() throws Exception {

		Context cxt = context(
				inVal("x1", 20.0),
				inVal("x2", 80.0));

		Entry e = ent("x2", 100.0);
		assertEquals(100.0, exec(e, cxt));
	}

	@Test
	public void invokerEntryService() throws Exception {

		ContextDomain em = model(
				inVal("x1", 20.0),
				inVal("x2", 80.0),
				result("result/y"));

		Entry ie = ent("multiply", invoker("x1 * x2", ents("x1", "x2")));
		Object result = exec(ie, em);
		assertEquals(1600.0, result);
	}

	@Test
	public void srvEntryLocalService() throws Exception {

		ContextDomain sm = model(
				inVal("y1", 20.0),
				inVal("y2", 80.0));

		Function se = ent(sig("add", AdderImpl.class, result("add", inPaths("y1", "y2"))));
		Context result = (Context) exec(se, sm);
        assertEquals(100.0, value(result, "add"));
	}

	@Test
	public void srvEntryRemoteService() throws Exception {

		ContextDomain sm = model(
				inVal("y1", 20.0),
				inVal("y2", 80.0));

		Function se = ent(sig("add", Adder.class, result("add", inPaths("y1", "y2"))));
		Context result = (Context) exec(se, sm);
		assertEquals(100.0, value(result, "add"));
	}

}
