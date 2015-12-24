package sorcer.provider.adder;

import net.jini.core.transaction.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.provider.adder.impl.AdderImpl;
import sorcer.service.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.response;
import static sorcer.mo.operator.srvModel;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class LocalMograms {
	private final static Logger logger = LoggerFactory.getLogger(LocalMograms.class);

	@Test
	public void exertTask() throws Exception {

		Service t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0)));

		Service out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/value: " + value(cxt, "result/value"));

		// get a single context argument
		assertEquals(100.0, value(cxt, "result/value"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("result/value", 100.0)).equals(
				subcontext(cxt, paths("arg/x1", "result/value"))));

	}


	@Test
	public void evaluateTask() throws SignatureException, ExertionException, ContextException {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

		// get the result value
		assertEquals(100.0, value(t5));

		// get the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
				value(t5, result("result/z", outPaths("arg/x1", "result/z")))));

	}

	@Test
	public void evalauteLocalModel() throws Exception {

		// three entry model
		Model mod = model(inEnt("arg/x1", 10.00), inEnt("arg/x2", 90.00),
				srv(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
				response("add", "arg/x1", "arg/x2"));

		Context out = response(mod);
		assertTrue(get(out, "add").equals(100.0));

		assertTrue(get(mod, "result/y").equals(100.0));

	}

}
	
	
