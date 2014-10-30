package sorcer.eol.blocks;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inEntry;
import static sorcer.co.operator.loop;
import static sorcer.eo.operator.add;
import static sorcer.eo.operator.block;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;

import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Averager;
import sorcer.core.SorcerConstants;
import sorcer.service.Block;
import sorcer.service.ServiceExertion;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticSpaceBlockTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticSpaceBlockTest.class.getName());

	static {
		ServiceExertion.debug = true;
		String version = "5.0.0-SNAPSHOT";
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + version + "-dl.jar",  "sorcer-dl-"+version +".jar" });
		System.out.println("CLASSPATH :" + System.getProperty("java.class.path"));
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}

	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
		Task spaceTask = task(
				"space task",
				sig("add", Adder.class),
				context("add", inEntry("arg/x1", 20.0),
						inEntry("arg/x2", 80.0), result("result/${name}")),
				strategy(Access.PULL, Wait.YES));
		
		Block spaceBlock = block(loop(10), spaceTask);
		
		logger.info("block size: " + spaceBlock.size());
		
		Task  masterTask = task(
				"t5",
				sig("average", Averager.class),
				context(result("result/average")));
		
		add(spaceBlock, masterTask);
		
		spaceBlock = (Block)exert(spaceBlock);
		logger.info("block context" + context(spaceBlock));
		assertEquals(get(context(spaceBlock), "result/average"), 100.00);
	}
		
}
