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
import sorcer.service.*;
import sorcer.service.Signature.Direction;

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
public class  LocalBlockExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(LocalBlockExertions.class);

	@Test
	public void explicitDataBlockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x3", 20.0), inEnt("arg/x4", 80.0),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3);

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}


	@Test
	public void closingBlockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x3"), inEnt("arg/x4"),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
				inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
				inEnt("arg/x3", 20.0), inEnt("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void overwritingLocalContextBlockTest() throws Exception {

		// in t4: inEnt("arg/x1", 20.0), inEnt("arg/x2", 10.0)
		// cosed with 10.0 and 50.0 respectively
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply",  inEnt("arg/x1", 20.0), inEnt("arg/x2", 10.0),
						result("arg/t4", Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x3"), inEnt("arg/x4"),
						result("arg/t5", Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
				inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
				inEnt("arg/x3", 20.0), inEnt("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void altServceTest() throws Exception {

//		Context scope = context(ent("y1", 100), ent("y2", 200));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("block/result")));

		Service sb = block("block",
				context(ent("y1", 100), ent("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4),
						opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));

		Service out = exert(sb);
//		logger.info("block context1: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(out), "block/result"), 100.00);

		// the initial scope of block is updated
		bind(sb, ent("y1", 200.0), ent("y2", 100.0));
//		logger.info("block context1: " + context(sb));

//		out = exert(sb, ent("y1", 200.0), ent("y2", 100.0));
		out = exert(sb);
//		logger.info("block context2: " + context(out));
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

		Block block = block("block",
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

		Block block = block("block",
				t4,
				t5,
				alt(opt(condition(cxt -> (double)v(cxt, "t4") > (double)v(cxt, "t5")), t3),
						opt(condition(cxt -> (double)v(cxt, "t4") <= (double)v(cxt, "t5")), t6)));

//		logger.info("block: " + block);
//		logger.info("exertions: " + exertions(block));
//		logger.info("block context: " + context(block));

		Block result = exert(block);
//		logger.info("block context: " + context(result));
//		logger.info("result: " + value(context(result), "block/result"));
		assertEquals(value(context(result), "block/result"), 400.00);

//		TODO state after execution if we wanat to reuse the block for another execution
// 		problem with clearScope() that is commented due to conflict with
// 		return value path when being as input path
//		result = exert(block, ent("block/t5/arg/x1", 200.0), ent("block/t5/arg/x2", 800.0));
//		logger.info("block context: " + context(result));
//		logger.info("result: " + value(context(result), "block/result"));
//		assertEquals(value(context(result), "block/result"), 750.00);

		t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						result("arg/t4", Direction.IN)));

		t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("arg/t5", Direction.IN)));

		t6 = task("t6", sig("average", AveragerImpl.class),
				context("average", inEnt("arg/t4"), inEnt("arg/t5"),
						result("block/result", Direction.OUT)));

		block = block("block",
				t4,
				t5,
				alt(opt(condition(cxt -> (double)value(cxt, "t4") > (double)value(cxt, "t5")), t3),
						opt(condition(cxt -> (double)value(cxt, "t4") <= (double)value(cxt, "t5")), t6)));
		result = exert(block, ent("block/t5/arg/x1", 200.0), ent("block/t5/arg/x2", 800.0));
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

		Block block = block("block",
				t4,
				opt(condition(cxt -> (double)value(cxt, "out") > 600.0), t5));

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
		Block block = block("block",
				context(ent("x1", 10.0), ent("x2", 20.0), ent("z", 100.0)),
				loop(condition(cxt -> (double)value(cxt, "x1") + (double)value(cxt, "x2")
								< (double)value(cxt, "z")),
						task(par("x1", invoker("x1 + 3", pars("x1"))))));

		block = exert(block);
		logger.info("block context: " + context(block));
		logger.info("result: " + value(context(block), "x1"));
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
				alt(opt(condition(cxt -> (int)v(cxt, "y") > 50), t4),
						opt(condition(cxt -> (int)value(cxt, "y") <= 50 ), t5)));

		logger.info("block: " + block);
		logger.info("exertions: " + exertions(block));
		logger.info("block context: " + context(block));

		block = exert(block);
//		logger.info("block context: " + context(block));
//		logger.info("result: " + value(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

//		block = exert(block, ent("x1", 10.0), ent("x2", 6.0));
////		logger.info("block context: " + context(block));
////		logger.info("result: " + value(context(block), "block/result"));
//		assertEquals(value(context(block), "block/result"), 500.00);

	}

}
