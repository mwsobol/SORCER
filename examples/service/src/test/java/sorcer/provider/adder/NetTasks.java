package sorcer.provider.adder;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.outPaths;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class NetTasks {
	private final static Logger logger = Logger.getLogger(NetTasks.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = srv("t5", sig("add", Adder.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

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

        Task t5 = srv("t5", sig("add", Adder.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

        // get the result value
        assertEquals(100.0, value(t5));

        // get the subcontext output from the exertion
        assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
                value(t5, result("result/z", outPaths("arg/x1", "result/z")))));

    }

    @Test
    public void sessionTask() throws SignatureException, ExertionException, ContextException  {

        Task sum = srv("t6", sig("sum", Adder.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

        assertEquals(100.0, value(sum));
        assertEquals(200.0, value(sum));
        assertEquals(300.0, value(sum));
    }

    @Test
	public void spaceTask() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/y")),
				strategy(Access.PULL, Wait.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals(get(t5, "result/y"), 100.0);
	}

   
    @Ignore
	@Test
	public void provisionedTask() throws Exception {
        // requires SORCER provisioning support
        Task t5 = task("provisioned adder", sig("add",
						Adder.class,
						deploy(configuration("org.sorcer:adder:config:5.0"))),
				strategy(Strategy.Provision.YES),
				context(inEnt("arg/x1", 10.0), inEnt("arg/x2", 80.0), result("result/y")));

        t5 = exert(t5);
        logger.info("t5 context: " + context(t5));
        logger.info("t5 value: " + get(t5, "result/y"));
        assertEquals(get(t5, "result/y"), 100.0);
	}
}
	
	