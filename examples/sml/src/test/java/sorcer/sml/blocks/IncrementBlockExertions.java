package sorcer.sml.blocks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.*;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class IncrementBlockExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(IncrementBlockExertions.class);

	@Test
	public void entryIncrementor() throws Exception {
		Context cxt = model("add", inEnt("arg/x1", 20),
						inEnt("arg/x2", 80.0), result("result++"));

		Incrementor z2 = inc(invoker(cxt, "arg/x1"), 2);
		assertEquals(next(z2), 22);
		assertEquals(next(z2), 24);
	}

	@Test
	public void taskIncrementor() throws Exception {
		Task spaceTask = task(
				"space task",
				sig("add", AdderImpl.class),
				model("add", inEnt("arg/x1", 20),
						inEnt("arg/x2", 80.0), result("task/result")));

		Incrementor i = inc(invoker(context(spaceTask), "arg/x1"), 2);
		Context cxt2 = model(ent("z2", i));
		assertEquals(value(cxt2, "z2"), 22);
		assertEquals(value(cxt2, "z2"), 24);
	}

	@Test
	public void taskIncrement() throws Exception {
		Task t = task(
				"space task",
				sig("add", AdderImpl.class),
				model("add", inEnt("arg/x1", inc("arg/x2", 2.0)),
						inEnt("arg/x2", 80.0), result("task/result")),
				strategy(Strategy.Access.PULL, Strategy.Wait.YES));

//		logger.info("result: " + value(t));
		assertEquals(value(t), 162.00);
	}

//	@Test
//	public void taskIncrementLoop() throws Exception {
//		Task spaceTask = task(
//				"space task",
//				sig("add", AdderImpl.class),
//				model("add", inEnt("arg/x1", inc("arg/x2", 2.0)),
//						inEnt("arg/x2", 80.0), result("task/result")),
//				strategy(Strategy.Access.PULL, Strategy.Wait.YES));
//
//		Block spaceBlock = block(sig(ServiceConcatenator.class),
//				loop(0, 10, spaceTask));
//
//		spaceBlock = exert(spaceBlock);
//		logger.info("block context" + context(spaceBlock));
////		assertEquals(get(context(spaceBlock), "result/average"), 100.00);
//	}
}
