package sorcer.core.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.AveragerImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Task;
import sorcer.util.Sorcer;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/arithmetic-tester")
public class ArithmeticNoNetBlockTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNoNetTest.class.getName());

	static {
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-arithmetic-beans.jar",
				"sorcer-dl.jar" });
		System.out.println("CLASSPATH :"
				+ System.getProperty("java.class.path"));
	}
	
	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("block/result")));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class), 
				context(ent("y1", 100), ent("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4), 
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, ent("y1", 200.0), ent("y2", 100.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);
	}
	
	@Test
	public void taskAltBlocTest() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", AveragerImpl.class), 
				context("average", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result")));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class), t4, t5, alt(
				opt(condition("{ t4, t5 -> t4 > t5 }", "t4", "t5"), t3), 
				opt(condition("{ t4, t5 -> t4 <= t5 }", "t4", "t5"), t6)));
		
//		logger.info("block: " + block);
//		logger.info("exertions: " + exertions(block));
//		logger.info("block context: " + context(block));

		block = exert(block);
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 400.00);
		
		block = exert(block, ent("block/t5/arg/x1", 200.0), ent("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 750.00);
	}
	
	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("out")));
		
		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("out")));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class), t4,
				opt(condition("{ out -> out > 600 }", "out"), t5));
		
		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);
		
		block = exert(block, ent("block/t4/arg/x1", 200.0), ent("block/t4/arg/x2", 800.0));
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}
}
