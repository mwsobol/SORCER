package sorcer.core.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.AveragerImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Task;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.ent.operator.ent;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ArithmeticNoNetBlockTest implements SorcerConstants {

	private static final Logger logger = LoggerFactory.getLogger(ArithmeticNoNetBlockTest.class);

	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/outDispatcher")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/outDispatcher")));
		
		Block block = block("block", sig("exert", ServiceConcatenator.class),
				context(ent("y1", 100), ent("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4), 
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("outDispatcher: " + eval(context(block), "block/outDispatcher"));
		assertEquals(value(context(block), "block/outDispatcher"), 100.00);

		block = exert(block, ent("block/y1", 200.0), ent("block/y2", 100.0));
//		logger.info("block context: " + context(block));
//		logger.info("outDispatcher: " + eval(context(block), "block/outDispatcher"));
		assertEquals(value(context(block), "block/outDispatcher"), 500.0);
	}
	
	@Test
	public void taskAltBlockTest() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/outDispatcher")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", AveragerImpl.class), 
				context("average", inVal("arg/t4"), inVal("arg/t5"),
						result("block/outDispatcher")));
		
		Block block = block("block", sig("exert", ServiceConcatenator.class), t4, t5, alt(
				opt(condition("{ t4, t5 -> t4 > t5 }", "t4", "t5"), t3), 
				opt(condition("{ t4, t5 -> t4 <= t5 }", "t4", "t5"), t6)));
		
//		logger.info("block: " + block);
//		logger.info("mograms: " + mograms(block));
//		logger.info("block context: " + context(block));

		Block out = exert(block);
//		logger.info("outDispatcher: " + eval(context(block), "block/outDispatcher"));
		assertEquals(value(context(out), "block/outDispatcher"), 400.00);

//		logger.info("block: " + context(block));
		out = exert(block, ent("block/t5/arg/x1", 200.0), ent("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(outGovernance));
//		logger.info("outDispatcher: " + get(context(outGovernance), "block/outDispatcher"));
		assertEquals(value(context(block), "block/outDispatcher"), 750.00);
	}
	
	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("outGovernance")));
		
		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("outGovernance")));
		
		Block block = block("block", sig("exert", ServiceConcatenator.class), t4,
				opt(condition("{ outGovernance -> outGovernance > 600 }", "outGovernance"), t5));
		
		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("outDispatcher: " + value(context(block), "outGovernance"));
		assertEquals(value(context(block), "outGovernance"), 500.0);
		
		block = exert(block, ent("block/t4/arg/x1", 200.0), ent("block/t4/arg/x2", 800.0));
		logger.info("block context: " + context(block));
		logger.info("outDispatcher: " + value(context(block), "outGovernance"));
		assertEquals(value(context(block), "outGovernance"), 100.0);
	}

	@Test
	public void batchFiTask() throws Exception {

		Task t4 = task("t4",
				sigFi("object", sig("multiply", MultiplierImpl.class), sig("add", AdderImpl.class)),
				sigFi("net", sig("multiply", Multiplier.class), sig("add", Adder.class)),
				context("shared", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("outDispatcher/y")));

		t4 = exert(t4, fi("object"));
		logger.info("task context: " + context(t4));

	}
}
