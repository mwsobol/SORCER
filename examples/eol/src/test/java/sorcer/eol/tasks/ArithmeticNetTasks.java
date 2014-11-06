package sorcer.eol.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sFi;
import static sorcer.eo.operator.sFis;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.srv;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.core.provider.Exerter;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Monitor;
import sorcer.service.Strategy.Wait;
import sorcer.service.Task;
import sorcer.util.ProviderAccessor;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ArithmeticNetTasks {
	private final static Logger logger = Logger
			.getLogger(ArithmeticNetTasks.class.getName());

	static {
		String sorcerVersion = "5.0.0-SNAPSHOT";
		String riverVersion = "2.2.2";
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/policy/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + sorcerVersion + "-dl.jar",  
				"sorcer-dl-"+sorcerVersion +".jar", "jsk-dl-"+riverVersion+".jar" });
		
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
//		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}
	
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = srv("t5", sig("add", Adder.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

		Exertion out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));

		assertEquals(100.0, value(cxt, "result/y"));
	}
	
	
	@Test
	public void valueTask() throws SignatureException, ExertionException, ContextException  {

		Task t5 = srv("t5", sig("add", Adder.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

		Object out = value(t5);
		logger.info("out value: " + out);
		assertEquals(100.0, out);
		
	}

	
	@Test
	public void arithmeticSpaceTask() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y")),
				strategy(Access.PULL, Wait.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals("Wrong value for 100.0", get(t5, "result/y"), 100.0);
	}

	
	@Test
	public void exerterTest() throws Exception {

		Task f5 = task(
			"f5",
			sig("add", Adder.class),
			context("add", inEnt("arg/x1", 20.0),
					inEnt("arg/x2", 80.0), outEnt("result/y", null)),
			strategy(Monitor.NO, Wait.YES));
	
	Exertion out = null;
//	long start = System.currentTimeMillis();
	Exerter exerter = ProviderAccessor.getExerter();
//	logger.info("got exerter: " + exerter);

	out = exerter.exert(f5);
//	long end = System.currentTimeMillis();
	
//	logger.info("task f5 context: " + context(out));
//	logger.info("task f5 result/y: " + get(context(out), "result/y"));
	assertEquals(get(out, "result/y"), 100.00);
	
	}
	
	
	@Test
	public void arithmeticNetFiTask() throws ExertionException, SignatureException, ContextException, RemoteException {
		Task task = task("add",
				sFi("net", sig("add", Adder.class)),
				sFi("object", sig("add", AdderImpl.class)),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));
		
		logger.info("sFi: " + sFi(task));
		logger.info("sFis: " + sFis(task));

//		task = exert(task, sFi("object"));
//		logger.info("exerted: " + task);
//		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
		
		task = exert(task, sFi("net"));
		logger.info("exerted: " + task);
		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
	
	}
	
		
	
	@Test
	public void arithmeticFiBatchTask() throws ExertionException, SignatureException, ContextException, RemoteException {
		
		Task t4 = task("t4", sFi("object", sig("multiply", MultiplierImpl.class), sig("add", AdderImpl.class)),
				sFi("net", sig("multiply", Multiplier.class), sig("add", Adder.class)),
				context("shared", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y", null)));

		t4 = exert(t4);
		logger.info("task cont4text: " + context(t4));
		
		t4 = exert(t4, sFi("net"));
		logger.info("task cont4text: " + context(t4));
		
	}

	
}
	
	