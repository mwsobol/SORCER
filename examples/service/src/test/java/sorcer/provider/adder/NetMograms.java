package sorcer.provider.adder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;
import sorcer.service.modeling.ContextModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.response;
import static sorcer.po.operator.ent;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class NetMograms {
	private final static Logger logger = LoggerFactory.getLogger(NetMograms.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = task("t5", sig("add", Adder.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		Exertion out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));

		// get a single context argument
		assertEquals(100.0, value(cxt, "result/y"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("result/y", 100.0)).equals(
				value(cxt, result("result/context", outPaths("arg/x1", "result/y")))));
	}

    @Test
    public void valueTask() throws SignatureException, ExertionException, ContextException  {

        Task t5 = task("t5", sig("add", Adder.class),
                cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

        // get the result eval
        assertTrue(eval(t5).equals(100.0));

        // get the subcontext output from the exertion
        assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
                eval(t5, result("result/z", outPaths("arg/x1", "result/z")))));

    }

    @Test
    public void sessionTask() throws SignatureException, ExertionException, ContextException  {

        Task sum = task("t6", sig("sum", Adder.class),
                cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		assertTrue(eval(sum).equals(100.0));
		assertTrue(eval(sum).equals(200.0));
		assertTrue(eval(sum).equals(300.0));
    }

    @Test
	public void spaceTask() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), result("result/y")),
				strategy(Access.PULL, Wait.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 eval: " + get(t5, "result/y"));
		assertEquals(get(t5, "result/y"), 100.0);
	}

	@Test
	public void evalauteRemoteModel() throws Exception {

		// three entry model
		ContextModel mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
				ent(sig("add", Adder.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
				sorcer.mo.operator.response("add", "arg/x1", "arg/x2"));

		Context out = response(mod);

		logger.info("out: " +out );
		assertTrue(get(out, "add").equals(100.0));
		assertTrue(get(mod, "result/y").equals(100.0));

	}
}
	
	
