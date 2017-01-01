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
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.po.operator;
import sorcer.service.Context;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ServiceModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.ent;
import static sorcer.po.operator.invoker;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ContextModels {
	private final static Logger logger = LoggerFactory.getLogger(ContextModels.class);

	@Test
	public void entryModel() throws Exception {

		// use procModel to create an ProcModel the same way as a regular context
		// or convert any context to procModel(<context>)
		ServiceModel mdl = procModel(val("arg/x1", 1.0), val("arg/x2", 2.0),
				val("arg/x3", 3.0), val("arg/x4", 4.0), val("arg/x5", 5.0));

		add(mdl, ent("arg/x6", 6.0));
		assertTrue(eval(mdl, "arg/x6").equals(6.0));

		// proc is of the Evaluation type
		// args in models are evaluated
		put(mdl, ent("arg/x6", ent("overwrite", 20.0)));
		assertTrue(eval(mdl, "arg/x6").equals(20.0));

		// invoker is of the Invocation type
		add(mdl, ent("arg/x7", invoker("x1 + x3", operator.ents("x1", "x3"))));

		assertTrue(eval(mdl, "arg/x7").equals(4.0));
		assertTrue(asis(mdl, "arg/x7") instanceof Entry);
		assertTrue(asis(mdl, "arg/x7") instanceof Proc);
		assertTrue(asis(asis(mdl, "arg/x7")) instanceof ServiceInvoker);
	}

	@Test
	public void modelingTarget() throws Exception {

		ServiceModel mdl = procModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(mdl, ent("invoke", invoker("x1 + x3", operator.ents("x1", "x3"))));

		// declare the modeling target
		responseUp(mdl, "invoke");
		// evaluate the model
		eval(mdl);
		assertTrue(eval(mdl).equals(4.0));

		// evaluate the model with overwritten inputs
		Double result = (Double) eval(mdl, ent("arg/x1", 2.0), ent("arg/x2", 3.0));
		assertTrue(result.equals(5.0));

		// evaluate the model with new inputs
		add(mdl, ent("invoke", invoker("x6 * x7 + x1", operator.ents("x1", "x6", "x7"))));
		result = (Double) eval(mdl, ent("arg/x6", 6.0), ent("arg/x7", 7.0));
		assertTrue(result.equals(44.0));
	}

	@Test
	public void modelDependencies() throws Exception {

		ServiceModel mdl1 = procModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));
		add(mdl1, ent("y1", invoker("x1 + x3", operator.ents("x1", "x3"))));
		add(mdl1, ent("y2", invoker("x4 * x5", operator.ents("x1", "x3"))));

		// mdl2 depends on values y1 and y2 calculated in cxt1
		ServiceModel mdl2 = procModel(ent("arg/y3", 8.0), ent("arg/y4", 9.0), ent("arg/y5", 10.0));
		add(mdl2, ent("invoke", invoker("y1 + y2 + y4 + y5", operator.ents("y1", "y2", "y4", "y5"))));
		responseUp(mdl2, "invoke");

		// created dependency of mdl2 on mdl1 via a context copier
		Copier cp = copier(mdl1, operator.ents("arg/x1", "arg/x2"), mdl2, operator.ents("y1", "y2"));
		dependsOn(mdl2, cp);

		Double result = (Double) eval(mdl2);
//		logger.info("result: " + result);
		assertTrue(result.equals(22.0));
	}

	@Test
	public void evaluateMultiEntryResponseModel() throws Exception {
		ServiceModel mdl = procModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(mdl, ent("add", invoker("x1 + x3", operator.ents("x1", "x3"))));

		add(mdl, ent("multiply", invoker("x4 * x5", operator.ents("x4", "x5"))));

		// two respnse paths declared for the result
		responseUp(mdl, "add", "multiply");

		// evaluate the model
		Context result = response(mdl);

		logger.info("result: " + result);
		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
	}

	@Test
	public void exertEntModel() throws Exception {

		ServiceModel mdl = procModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(mdl, ent("add", invoker("x1 + x3", operator.ents("x1", "x3"))));

		add(mdl, ent("multiply", invoker("x4 * x5", operator.ents("x4", "x5"))));

		// two response paths declared
		responseUp(mdl, "add", "multiply");

		// exert the model
		ServiceModel model = exert(mdl);
		Context result = response(model);

		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
	}

	@Test
	public void contextEntryService() throws Exception {

		Context cxt = context(
				inVal("x1", 20.0),
				inVal("x2", 80.0));

		Entry e = ent("x2", 100.0);
		assertEquals(100.0, value((Context)exec(e, cxt), "x2"));
	}

	@Test
	public void invokerEntryService() throws Exception {

		Model em = model(
				inVal("x1", 20.0),
				inVal("x2", 80.0),
				result("result/y"));

		Entry ie = ent("multiply", invoker("x1 * x2", operator.ents("x1", "x2")));
		Object result = exec(ie, em);
		assertEquals(1600.0, result);
	}

	@Test
	public void srvEntryLocalService() throws Exception {

		Model sm = model(
				inVal("y1", 20.0),
				inVal("y2", 80.0));

		Entry se = ent(sig("add", AdderImpl.class, result("add", inPaths("y1", "y2"))));
		Context result = (Context) exec(se, sm);
		assertEquals(100.0, value(result, "add"));
	}

	@Test
	public void srvEntryRemoteService() throws Exception {

		Model sm = model(
				inVal("y1", 20.0),
				inVal("y2", 80.0));

		Entry se = ent(sig("add", Adder.class, result("add", inPaths("y1", "y2"))));
		Context result = (Context) exec(se, sm);
		assertEquals(100.0, value(result, "add"));
	}

}
