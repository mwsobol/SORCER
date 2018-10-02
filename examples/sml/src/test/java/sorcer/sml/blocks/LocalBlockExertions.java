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
import sorcer.service.Context;
import sorcer.service.Mogram;
import sorcer.service.Signature.Direction;
import sorcer.service.Task;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inVal;
import static sorcer.co.operator.val;
import static sorcer.mo.operator.*;
import static sorcer.eo.operator.alt;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.opt;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class  LocalBlockExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(LocalBlockExertions.class);

	@Test
	public void explicitDataBlockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x3", 20.0), inVal("arg/x4", 80.0),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3);

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);
	}

	@Test
	public void closingBlockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1"), inVal("arg/x2"),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x3"), inVal("arg/x4"),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
                inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                inVal("arg/x3", 20.0), inVal("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void overwritingLocalContextBlockTest() throws Exception {

		// in t4: inVal("arg/x1", 20.0), inVal("arg/x2", 10.0)
		// cosed with 10.0 and 50.0 respectively
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 20.0), inVal("arg/x2", 10.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x3"), inVal("arg/x4"),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
                inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                inVal("arg/x3", 20.0), inVal("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void altServceTest() throws Exception {

//		Context scope = context(call("y1", 100), call("y2", 200));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));

		Block sb = block("block",
				context(val("y1", 100), val("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4),
						opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));

		Mogram out = exert(sb);
//		logger.info("block context1: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(out), "block/result"), 100.00);

		// the initial scope of block is updated
		bind(sb, val("y1", 200.0), val("y2", 100.0));
//		logger.info("block context1: " + context(sb));

//		out = exert(sb, val("y1", 200.0), val("y2", 100.0));
		out = exert(sb);
//		logger.info("block context2: " + context(out));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(out), "block/result"), 500.0);

	}

	@Test
	public void directAltBlockTest() throws Exception {

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));

		Block block = block("block",
				context(val("y1", 100), val("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4),
						opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));

		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, val("block/y1", 200.0), val("block/y2", 100.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);

	}

	@Test
	public void altBlockTest() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("arg/t5", Direction.IN)));

		Task t6 = task("t6", sig("average", AveragerImpl.class),
                context("average", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		Block block = block("block", t4, t5,
				alt(opt(condition((Context<Double> cxt) -> value(cxt, "arg/t4") > value(cxt, "arg/t5")), t3),
						opt(condition((Context<Double> cxt) -> value(cxt, "arg/t4") <= value(cxt, "arg/t5")), t6)));

//		logger.info("block: " + block);
//		logger.info("mograms: " + mograms(block));
//		logger.info("block context: " + context(block));

		Block result = exert(block);
//		logger.info("block context: " + context(result));
//		logger.info("result: " + eval(context(result), "block/result"));
		assertEquals(value(context(result), "block/result"), 400.00);

//		TODO the state after execution changes to reuse the block for another execution?
// 		problem with clearScope() that is commented due to conflict with
// 		return eval path when being as input path
//		result = exert(block, val("block/t5/arg/x1", 200.0), val("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(result));
//		logger.info("result: " + eval(context(result), "block/result"));
//		assertEquals(eval(context(result), "block/result"), 750.00);

		t3 = task("t3", sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("arg/t5", Direction.IN)));

		t6 = task("t6", sig("average", AveragerImpl.class),
                context("average", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Direction.OUT)));

		block = block("block", t4, t5,
				alt(opt(condition((Context<Double> cxt) -> (double)value(cxt, "arg/t4") > (double)value(cxt, "arg/t5")), t3),
						opt(condition((Context<Double> cxt) -> value(cxt, "arg/t4") <= value(cxt, "arg/t5")), t6)));
		result = exert(block, val("block/t5/arg/x1", 200.0), val("block/t5/arg/x2", 800.0));
		assertEquals(value(context(result), "block/result"), 750.00);
	}

	@Test
	public void optBlockTest() throws Exception {
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("out")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("out")));

		Block block = block("block", t4,
				opt(condition((Context<Double> cxt) -> value(cxt, "out") > 600.0), t5));

		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "condition/eval"), false);
		assertEquals(value(context(block), "out"), 500.0);

		block = exert(block, val("block/t4/arg/x1", 200.0), val("block/t4/arg/x2", 800.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);
	}

	@Test
	public void loopBlockTest() throws Exception {
		Block block = block("block",
				context(val("x1", 10.0), val("x2", 20.0), val("z", 100.0)),
				loop(condition((Context<Double> cxt) -> value(cxt, "x1") + value(cxt, "x2")
								< value(cxt, "z")),
						task(call("x1", invoker("x1 + 3", args("x1"))))));

		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "x1"));
		assertEquals(value(context(block), "x1"), 82.00);
	}

	@Test
	public void callBlockTest() throws Exception {

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));

		Block block = block("block", sig("exert", ServiceConcatenator.class),
                context(inVal("x1", 4.0), inVal("x2", 5.0)),
				task(call("y", invoker("x1 * x2", args("x1", "x2")))),
				alt(opt(condition((Context<Double> cxt) -> value(cxt, "y") > 50.0), t4),
						opt(condition((Context<Double> cxt) -> value(cxt, "y") <= 50.0), t5)));

		logger.info("block: " + block);
		logger.info("mograms: " + mograms(block));
		logger.info("block context: " + context(block));

		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, val("block/x1", 10.0), val("block/x2", 6.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.00);
	}

}
