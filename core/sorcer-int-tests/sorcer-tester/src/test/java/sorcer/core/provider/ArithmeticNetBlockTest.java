package sorcer.core.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Averager;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.core.SorcerConstants;
import sorcer.service.Block;
import sorcer.service.Task;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLookup;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inVal;
import static sorcer.co.operator.val;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ArithmeticNetBlockTest implements SorcerConstants {
	private static final Logger logger = LoggerFactory.getLogger(ArithmeticNetBlockTest.class);

	@Test
    public void getProxy() throws Exception {
        Object proxy = new ProviderLookup().getProvider(sig("multiply", Multiplier.class));
        logger.info("Multiplier proxy: " + proxy);
		
        proxy = new  ProviderAccessor().getProvider(sig("multiply", Multiplier.class));
        logger.info("Multiplier proxy: " + proxy);
    }
				
	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));
		
		Block block = block("block", // sig(Concatenator.class),
				context(val("y1", 100), val("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4), 
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, val("block/y1", 200.0), val("block/y2", 100.0));
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);
	}

	@Test
	public void taskAltBlockTest() throws Exception {
		Task t3 = task("t3",  sig("subtract", Subtractor.class), 
				context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", Averager.class),
				context("average", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result")));
		
		Block block = block("block", //sig(Concatenator.class),
				t4, t5,
				alt(opt(condition("{ t4, t5 -> t4 > t5 }", "t4", "t5"), t3),
					opt(condition("{ t4, t5 -> t4 <= t5 }", "t4", "t5"), t6)));

		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 400.00);

		block = exert(block, val("block/t5/arg/x1", 200.0), val("block/t5/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 750.00);
	}
	
	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("out")));
		
		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("out")));
		
		Block block = block("block", //sig(Concatenator.class),
				t4,
				opt(condition("{ out -> out > 600 }", "out"), t5));
		
		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + getValue(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);
		
		block = exert(block, val("block/t4/arg/x1", 200.0), val("block/t4/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}
	
}
