package sorcer.arithmetic.block;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.entry;
import static sorcer.co.operator.inEntry;
import static sorcer.eo.operator.alt;
import static sorcer.eo.operator.block;
import static sorcer.eo.operator.condition;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.opt;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoker;
import static sorcer.po.operator.par;
import static sorcer.po.operator.pars;

import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Task;
import sorcer.util.Sorcer;



/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticNoNetBlockTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNoNetBlockTest.class.getName());

	static {
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-dl.jar",
				"sorcer-dl.jar" });
		System.out.println("CLASSPATH :"
				+ System.getProperty("java.class.path"));
	}
	
	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("block/result")));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class), 
				context(entry("y1", 100), entry("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4), 
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, entry("y1", 200.0), entry("y2", 100.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);
	}
	
	@Test
	public void taskAltBlocTest() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inEntry("arg/t4"), inEntry("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", AveragerImpl.class), 
				context("average", inEntry("arg/t4"), inEntry("arg/t5"),
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
		
		block = exert(block, entry("block/t5/arg/x1", 200.0), entry("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 750.00);
	}
	
	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("out")));
		
		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("out")));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class), t4,
				opt(condition("{ out -> out > 600 }", "out"), t5));
		
		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);
		
		block = exert(block, entry("block/t4/arg/x1", 200.0), entry("block/t4/arg/x2", 800.0));
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}
	
	@Test
	public void parBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("block/result")));
				 
		Block block = block("block", sig("execute", ServiceConcatenator.class), 
				context(entry("x1", 4), entry("x2", 5)),
				task(par("y", invoker("x1 * x2", pars("x1", "x2")))), 
				alt(opt(condition("{ y -> y > 50 }", "y"), t4), 
				    opt(condition("{ y -> y <= 50 }", "y"), t5)));
		
//		logger.info("block: " + block);
//		logger.info("exertions: " + exertions(block));
//		logger.info("block context: " + context(block));
//
		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);
		
		block = exert(block, entry("block/x1", 10.0), entry("block/x2", 6.0));
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.00);
	}
	
	@Test
	public void loopBlockTest() throws Exception {
		Block block = block("block", sig("execute", ServiceConcatenator.class), 
				context(entry("x1", 10.0), entry("x2", 20.0), entry("z", 100.0)),
				loop(condition("{ x1, x2, z -> x1 + x2 < z }", "x1", "x2", "z"), 
						task(par("x1", invoker("x1 + 3", par("x1"))))));
		
		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "x1"));
		assertEquals(value(context(block), "x1"), 82.00);
	}
}
