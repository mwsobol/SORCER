package sorcer.sml.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.impl.*;
import sorcer.service.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class LocalTaskExertions {
	private final static Logger logger = LoggerFactory.getLogger(LocalTaskExertions.class);
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = task("t5", sig("add", AdderImpl.class),
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
		assertTrue(context(ent("result/value", 100.0), ent("arg/x1", 20.0)).equals(
				value(cxt, outPaths("result/value", "arg/x1"))));

	}

	@Test
	public void evaluateTask() throws SignatureException, ExertionException, ContextException  {

		Task t6 = task("t6", sig("average", AveragerImpl.class),
				cxt("average", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

		// get the result value
		assertEquals(50.0, value(t6));

		// get the subcontext output from the exertion
		assertTrue(context(ent("result/y", 50.0), ent("arg/x1", 20.0)).equals(
				value(t6, outPaths("result/y", "arg/x1"))));

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

        logger.info("task getSelects:" + batch3.getFidelity());

        batch3 = exert(batch3);
		//logger.info("task result/y: " + get(batch3, "result/y"));
		assertEquals("Wrong value for 400.0", get(batch3, "result/y"), 400.0);
    }

	@Test
	public void batchFiTask() throws Exception {

		Task t4 = task("t4",
				sFi("object", sig("multiply", MultiplierImpl.class), sig("add", AdderImpl.class)),
				sFi("net", sig("multiply", Multiplier.class), sig("add", Adder.class)),
				context("shared", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		t4 = exert(t4, fi("object"));
		logger.info("task context: " + context(t4));

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
		assertTrue(get(batch3, "result/y").equals(400.0));

    }

	@Test
	public void objectTaskFidelity() throws Exception {

		Task t4 = task("t4",
				sFi("object1", sig("multiply", MultiplierImpl.class)),
				sFi("object2", sig("add", AdderImpl.class)),
				context("shared", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Context out = context(exert(t4, fi("object1")));
		logger.info("task context: " + context(t4));
		assertTrue(value(out, "result/y").equals(500.0));

		out = context(exert(t4, fi("object2")));
		logger.info("task context: " + context(t4));
		assertTrue(value(out, "result/y").equals(60.0));
	}

	@Test
	public void multiFiObjectTaskTest() throws Exception {
		ServiceExertion.debug = true;

		Task task = task("add",
				sFi("net", sig("add", Adder.class)),
				sFi("object", sig("add", AdderImpl.class)),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));

		logger.info("task fi: " + fi(task));
		assertTrue(fis(task).size() == 2);
		logger.info("selected Fi: " + fiName(task));
		assertTrue(fiName(task).equals("net"));

		task = exert(task, fi("object"));
		logger.info("exerted: " + context(task));
		assertTrue(fiName(task).equals("object"));
		assertTrue(get(task).equals(100.0));
	}


	@Test
	public void argTaskTest() throws Exception {
		Task t4 = task("t4", sig("multiply", new Multiply()),
				context(
						parameterTypes( double[].class),
						args(new double[]{10.0, 50.0}),
						result("result/y")));

		//logger.info("t4: " + value(t4));
		assertTrue(value(t4).equals(500.0));
	}
}
	
	
