package sorcer.sml.blocks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Service;
import sorcer.service.Signature.Direction;
import sorcer.service.Task;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.alt;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.opt;
import static sorcer.po.operator.*;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class LocalBlockExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(LocalBlockExertions.class);

	@Test
	public void altServceTest() throws Exception {

//		Context scopeContext = context(ent("y1", 100), ent("y2", 200));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("block/result")));

		Service sb = service("block", sig(ServiceConcatenator.class),
				context(ent("y1", 100), ent("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4),
						opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));

//		Service out = exert(sb);
////		logger.info("block context1: " + context(block));
////		logger.info("result: " + value(context(block), "block/result"));
//		assertEquals(value(context(out), "block/result"), 100.00);

		bind(sb, ent("y1", 200.0), ent("y2", 100.0));
		Service out = exert(sb);

		// the initial scope of block is cleared
		out = exert(sb, ent("y1", 200.0), ent("y2", 100.0));
		logger.info("block context2: " + context(out));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(out), "block/result"), 500.0);

	}
	
	@Test
	public void directAltBlockTest() throws Exception {
		
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
	public void altBlockTest() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("arg/t5", Direction.IN)));
		
		Task t6 = task("t6", sig("average", AveragerImpl.class), 
				context("average", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));
		
		Block block = block("block", sig("execute", ServiceConcatenator.class),
				t4, t5, 
				alt(opt(condition("{ t4, t5 -> t4 > t5 }", "t4", "t5"), t3),
					opt(condition("{ t4, t5 -> t4 <= t5 }", "t4", "t5"), t6)));
		
//		logger.info("block: " + block);
//		logger.info("exertions: " + exertions(block));
//		logger.info("block context: " + context(block));

		Block result = exert(block);
//		logger.info("block context: " + context(result));
//		logger.info("result: " + value(context(result), "block/result"));
		assertEquals(value(context(result), "block/result"), 400.00);

		result = exert(block, ent("block/t5/arg/x1", 200.0), ent("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(result));
//		logger.info("result: " + value(context(result), "block/result"));
		assertEquals(value(context(result), "block/result"), 750.00);
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
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "condition/value"), false);
		assertEquals(value(context(block), "out"), 500.0);

		block = exert(block, ent("block/t4/arg/x1", 200.0), ent("block/t4/arg/x2", 800.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}
	
	
	@Test
	public void loopBlockTest() throws Exception {
		Block block = block("block", sig("execute", ServiceConcatenator.class), 
				context(ent("x1", 10.0), ent("x2", 20.0), ent("z", 100.0)),
				loop(condition("{ x1, x2, z -> x1 + x2 < z }", "x1", "x2", "z"), 
						task(par("x1", invoker("x1 + 3", par("x1"))))));
		
		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "x1"));
		assertEquals(value(context(block), "x1"), 82.00);
	}

	
	@Test
	public void parBlockTest() throws Exception {

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("block/result")));

		Block block = block("block", sig("execute", ServiceConcatenator.class),
				context(inEnt("x1", 4), inEnt("x2", 5)),
				task(par("y", invoker("x1 * x2", pars("x1", "x2")))),
				alt(opt(condition("{ y -> y > 50 }", "y"), t4),
						opt(condition("{ y -> y <= 50 }", "y"), t5)));

		logger.info("block: " + block);
		logger.info("exertions: " + exertions(block));
		logger.info("block context: " + context(block));

		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, ent("x1", 10.0), ent("x2", 6.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.00);

	}
	
}
