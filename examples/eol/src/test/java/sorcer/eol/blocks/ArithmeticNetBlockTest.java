package sorcer.eol.blocks;

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

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Averager;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.core.SorcerConstants;
import sorcer.service.Block;
import sorcer.service.ServiceExertion;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticNetBlockTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNetBlockTest.class.getName());
	
	static {
		String version = "5.0.0-SNAPSHOT";
		ServiceExertion.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + version + "-dl.jar",  "sorcer-dl-"+version +".jar" });
		System.out.println("CLASSPATH :" + System.getProperty("java.class.path"));
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");	}
	
	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("block/result")));
		
		Block block = block("block", context(entry("y1", 100), entry("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4), 
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, entry("y1", 200.0), entry("y2", 100.0));
		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);
	}
	
	@Test
	public void taskAltBlockTest() throws Exception {
		Task t3 = task("t3",  sig("subtract", Subtractor.class), 
				context("subtract", inEntry("arg/t4"), inEntry("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", Averager.class), 
				context("average", inEntry("arg/t4"), inEntry("arg/t5"),
						result("block/result")));
		
//		Block block = block("block", t4, t5);
//		block = exert(block);
//		logger.info("block context 0: " + context(block));

		
		Block block = block("block", t4, t5, alt(
			opt(condition("{ t4, t5 -> t4 > t5 }", "t4", "t5"), t3), 
			opt(condition("{ t4, t5 -> t4 <= t5 }", "t4", "t5"), t6)));

		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 400.00);
		
		block = exert(block, entry("block/t5/arg/x1", 200.0), entry("block/t5/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 750.00);
	}
	
	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("out")));
		
		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("out")));
		
		Block block = block("block", t4,
				opt(condition("{ out -> out > 600 }", "out"), t5));
		
		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);
		
		block = exert(block, entry("block/t4/arg/x1", 200.0), entry("block/t4/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}
	
	@Test
	public void parsBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						result("block/result")));
				 
		Block block = block("block", context(entry("x1", 4), entry("x2", 5)),
				task(par("y", invoker("x1 * x2", pars("x1", "x2")))), 
				alt("altx", opt(condition("{ y -> y > 50 }", "y"), t4), 
				    opt(condition("{ y -> y <= 50 }", "y"), t5)));
				
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);
		
		block = exert(block, entry("block/x1", 10.0), entry("block/x2", 6.0));
		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.00);
	}
	
	@Test
	public void loopBlockTest() throws Exception {
		Block block = block("block", context(entry("x1", 10.0), entry("x2", 20.0), entry("z", 100.0)),
				loop(condition("{ x1, x2, z -> x1 + x2 < z }", "x1", "x2", "z"), 
						task(par("x1", invoker("x1 + 3", pars("x1"))))));
		
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "x1"));
		assertEquals(value(context(block), "x1"), 82.00);
	}
}
