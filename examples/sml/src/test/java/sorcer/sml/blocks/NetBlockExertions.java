package sorcer.sml.blocks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Concatenator;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Signature;
import sorcer.service.Task;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.ent;
import static sorcer.co.operator.inVal;
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
public class NetBlockExertions implements SorcerConstants, Serializable {
	private final static Logger logger = LoggerFactory.getLogger(NetBlockExertions.class);

	@Test
	public void blockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", Subtractor.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Signature.Direction.OUT)));

		Task t4 = task("t4", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4", Signature.Direction.IN)));

		Task t5 = task("t5", sig("add", Adder.class),
                context("add", inVal("arg/x3", 20.0), inVal("arg/x4", 80.0),
						result("arg/t5", Signature.Direction.IN)));

		Block block = block("block", t4, t5, t3);

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void contextBlockTest() throws Exception {

		Task t3 = task("t3", sig("subtract", Subtractor.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Signature.Direction.OUT)));

		Task t4 = task("t4", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1"), inVal("arg/x2"),
						result("arg/t4", Signature.Direction.IN)));

		Task t5 = task("t5", sig("add", Adder.class),
                context("add", inVal("arg/x3"), inVal("arg/x4"),
						result("arg/t5", Signature.Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
                inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                inVal("arg/x3", 20.0), inVal("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void shadowingContextBlockTest() throws Exception {

		// in t4: inVal("arg/x1", 20.0), inVal("arg/x2", 10.0)
		Task t3 = task("t3", sig("subtract", Subtractor.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Signature.Direction.OUT)));

		Task t4 = task("t4", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1", 20.0), inVal("arg/x2", 10.0),
						result("arg/t4", Signature.Direction.IN)));

		Task t5 = task("t5", sig("add", Adder.class),
                context("add", inVal("arg/x3"), inVal("arg/x4"),
						result("arg/t5", Signature.Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
                inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                inVal("arg/x3", 20.0), inVal("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);

	}

	@Test
	public void contextAltTest() throws Exception {

		Task t4 = task(sig("multiply", Multiplier.class),
                context(inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task(sig("add", Adder.class),
                context(inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));
		
		Block block = block(context(val("y1", 100), val("y2", 200)),
				alt(opt(condition("{ y1, y2 -> y1 > y2 }", "y1", "y2"), t4),
					opt(condition("{ y1, y2 -> y1 <= y2 }", "y1", "y2"), t5)));
		
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

//		bind(block, prc("y1", 200.0), prc("y2", 100.0));
//		block = exert(block);

		block = exert(block, val("block/y1", 200.0), val("block/y2", 100.0));
//		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.0);
	}

	@Test
	public void taskAltBlockTest() throws Exception {

		Task t3 = task("t3",  sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", AveragerImpl.class),
                context("average", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result")));

		Block block = block("block", t4, t5,
				alt(opt(condition((Context <Double> cxt) ->
						value(cxt, "arg/t4") > value(cxt, "arg/t5")), t3),
					opt(condition(cxt ->
						(double)value(cxt, "arg/t4") <= (double)value(cxt, "arg/t5")), t6)));

		block = exert(block);
		logger.info("block context 1: " + context(block));
		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 400.00);

		block = exert(block, val("block/t5/arg/x1", 200.0), val("block/t5/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
		logger.info("result: " + eval(context(block), "block/result"));
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
		
		Block block = block("block", sig(Concatenator.class),
				t4,
				opt(condition(cxt -> (double)value(cxt, "out") > 600.0), t5));
		
		block = exert(block);
		logger.info("block context 1: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 500.0);

		block = exert(block, prc("block/t4/arg/x1", 200.0), prc("block/t4/arg/x2", 800.0));
		logger.info("block context 2: " + context(block));
//		logger.info("result: " + eval(context(block), "out"));
		assertEquals(value(context(block), "out"), 100.0);

	}

	@Test
	public void procBlockTest() throws Exception {

		Task t4 = task("t4", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", Adder.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("block/result")));
				 
		Block block = block("block", sig(Concatenator.class),
				context(ent("x1", 4.0), ent("x2", 5.0)),
				task(prc("y", invoker("x1 * x2", args("x1", "x2")))),
				alt(opt(condition(cxt -> (Double)value(cxt, "y") > 50.0), t4),
						opt(condition(cxt -> (Double)value(cxt, "y") <= 50 ), t5)));
				
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 100.00);

		block = exert(block, prc("block/x1", 10.0), prc("block/x2", 6.0));
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "block/result"));
		assertEquals(value(context(block), "block/result"), 500.00);

	}

	@Test
	public void loopBlockTest() throws Exception {

		Block block = block("block", sig(Concatenator.class),
				context(val("x1", 10.0), val("x2", 20.0), val("z", 100.0)),
				loop(condition(cxt -> (double)value(cxt, "x1") + (double)value(cxt, "x2")
								< (double)value(cxt, "z")),
						task(prc("x1", invoker("x1 + 3", args("x1"))))));
		
		block = exert(block);
		logger.info("block context: " + context(block));
//		logger.info("result: " + eval(context(block), "x1"));
		assertEquals(value(context(block), "x1"), 82.00);

	}

}
