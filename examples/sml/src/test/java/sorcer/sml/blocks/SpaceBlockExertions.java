package sorcer.sml.blocks;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;
import sorcer.service.Task;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.loop;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class SpaceBlockExertions implements SorcerConstants {
	private final static Logger logger = Logger.getLogger(SpaceBlockExertions.class.getName());

	@Ignore
	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
		Task spaceTask = task(
				"space task",
				sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/${name}")),
				strategy(Access.PULL, Wait.YES));
		
		Block spaceBlock = block(sig(ServiceConcatenator.class), loop(10), spaceTask);
		
		logger.info("block size: " + spaceBlock.size());
		
		Task  masterTask = task(
				"t5",
				sig("average", AveragerImpl.class),
				context(result("result/average")));
		
		add(spaceBlock, masterTask);
		
		spaceBlock = exert(spaceBlock);
		logger.info("block context" + context(spaceBlock));
		assertEquals(get(context(spaceBlock), "result/average"), 100.00);
	}
		
}
