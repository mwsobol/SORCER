package sorcer.eol.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.service.*;

import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/eol")
public class LocalExertionTasks {
	private final static Logger logger = Logger.getLogger(LocalExertionTasks.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

		Exertion out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));

		// get a single context argument
		assertEquals(100.0, value(cxt, "result/y"));

		// get subcontext output
		assertTrue(context(ent("arg/x1", 20.0), ent("result/y", 100.0)).equals(
				value(cxt, result("result/context", from("arg/x1", "result/y")))));

	}


	@Test
	public void evaluateTask() throws SignatureException, ExertionException, ContextException  {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

//		logger.info("ZZZZZZZZZZ 1: " +   value(t5));
//		Object out = value(t5);
//		logger.info("out value: " + out);
//		assertEquals(100.0, out);

		logger.info("ZZZZZZZZZZ 2: " + value(t5, result("result/context", from("arg/x1", "result/y"))));
	}

}
	
	