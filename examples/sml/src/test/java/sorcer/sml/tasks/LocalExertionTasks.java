package sorcer.sml.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.service.*;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class LocalExertionTasks {
	private final static Logger logger = Logger.getLogger(LocalExertionTasks.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0)));

		Exertion out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/value: " + value(cxt, "result/value"));

		// get a single context argument
		assertEquals(100.0, value(cxt, "result/value"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("result/value", 100.0)).equals(
				value(cxt, result("result/value", outPaths("arg/x1", "result/value")))));

	}


	@Test
	public void evaluateTask() throws SignatureException, ExertionException, ContextException  {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

//		// get the result value
//		assertEquals(100.0, value(t5));

		// get the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
				value(t5, result("result/z", outPaths("arg/x1", "result/z")))));

	}

    @Test
    public void batchTask() throws Exception {
        // batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
        // shared context with named paths
        Task batch3 = task("batch3",
                type(sig("multiply", MultiplierImpl.class, result("subtract/x1", Signature.Direction.IN)), Signature.PRE),
                type(sig("add", AdderImpl.class, result("subtract/x2", Signature.Direction.IN)), Signature.PRE),
                sig("subtract", SubtractorImpl.class, result("result/y", inPaths("subtract/x1", "subtract/x2"))),
                context(inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                        inEnt("add/x1", 20.0), inEnt("add/x2", 80.0)));

        logger.info("task getSignatures:" + batch3.getFidelity());

        batch3 = exert(batch3);
//		//logger.info("task result/y: " + get(batch3, "result/y"));
//		assertEquals("Wrong value for 400.0", get(batch3, "result/y"), 400.0);
    }


    @Test
    public void prefixedBatchTask() throws Exception {

        // batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
        // shared context with prefixed paths
        Task batch3 = task("batch3",
                type(sig("multiply#op1", MultiplierImpl.class, result("op3/x1", Signature.Direction.IN)), Signature.PRE),
                type(sig("add#op2", AdderImpl.class, result("op3/x2", Signature.Direction.IN)), Signature.PRE),
                sig("subtract", SubtractorImpl.class, result("result/y", inPaths("op3/x1", "op3/x2"))),
                context(inEnt("op1/x1", 10.0), inEnt("op1/x2", 50.0),
                        inEnt("op2/x1", 20.0), inEnt("op2/x2", 80.0)));

        batch3 = exert(batch3);
        assertEquals(get(batch3, "result/y"), 400.0);

    }
}
	
	