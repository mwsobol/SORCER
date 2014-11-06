package junit.sorcer.core.provider;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.entry;
import static sorcer.eo.operator.alt;
import static sorcer.eo.operator.block;
import static sorcer.eo.operator.condition;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.opt;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;

import java.io.File;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.SorcerConstants;
import sorcer.service.Block;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLocator;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticNetBlockTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNoNetTest.class.getName());
	
	static {
		ServiceExertion.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new RMISecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-arithmetic-beans.jar",  "sorcer-dl.jar" });
		System.out.println("CLASSPATH :" + System.getProperty("java.class.path"));
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
	}
			
	@BeforeClass
	public static void setUpOnce() throws IOException, InterruptedException,
			ExertionException, ContextException, SignatureException {
		
		//Version with AntTask
		String antBuild = Sorcer.getHome()
				+ "/modules/sorcer/src/junit/sorcer/core/provider/bin/build.xml";
		File antFile = new File(antBuild);
		exert(task("spawn", antFile));
		
//		// SORCER ExecUtils
//		Service service = Accessor.getService(sig(Multiplier.class));
//		if (service == null) {
//			CmdResult result = ExecUtils.execCommand("ant -f "
//					+ Sorcer.getHome()
//					+ "/modules/sorcer/src/junit/sorcer/core/provider/bin/boot-all.xml spawn");
//			logger.info("out: " + result.getOut());
//			logger.info("err: " + result.getErr());
//			logger.info("status: " + result.getExitValue());
//		}
//
//		// Version with Ant Project
//		String antBuild = Sorcer.getHome()
//				+ "/modules/sorcer/src/junit/sorcer/core/provider/bin/boot-all.xml";
//		File antFile = new File(antBuild);
//		Project project = new Project();
//		project.init();
//		ProjectHelper.configureProject(project, antFile);
//		project.executeTarget("spawn");
//		Thread.sleep(2000);
	}

//	@AfterClass
//	public static void cleanup() throws RemoteException, InterruptedException,
//			SignatureException {
//		Sorcer.destroyNode(null, Adder.class);
//	}
	
	@Test
	public void getProxy() throws Exception {
		Object proxy = ProviderLookup.getProvider(sig("multiply", Multiplier.class));
		logger.info("Multiplier proxy: " + proxy);
		
		proxy = ProviderLocator.getProvider(sig("multiply", Multiplier.class));
		logger.info("Multiplier proxy: " + proxy);
		
		 proxy = ProviderAccessor.getProvider(sig("multiply", Multiplier.class));
		logger.info("Multiplier proxy: " + proxy);
	}
				
	@Test
	public void contextAltTest() throws Exception {
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						result("block/result")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
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
				context("subtract", in("arg/t4"), in("arg/t5"),
						result("block/result")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						result("arg/t4")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						result("arg/t5")));
		
		Task t6 = task("t6", sig("average", Averager.class), 
				context("average", in("arg/t4"), in("arg/t5"),
						result("block/result")));
		
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
				context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						result("out")));
		
		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
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
	
}
