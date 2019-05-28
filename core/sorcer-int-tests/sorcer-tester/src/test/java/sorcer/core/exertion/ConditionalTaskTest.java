package sorcer.core.exertion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.co.operator;
import sorcer.core.context.model.EntModel;
import sorcer.service.*;
import sorcer.service.modeling.mog;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.alt;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.opt;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ConditionalTaskTest {
	private final static Logger logger = LoggerFactory.getLogger(ConditionalTaskTest.class);

	@Test
	public void arithmeticTaskTest() throws Exception {
		// to test tracing of execution enable ServiceRoutine.debug
		EntModel pm = new EntModel("prc-model");

		Task task = task(
				"add",
				sig("add", AdderImpl.class),
				context(operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0),
						result("result/y")));

		OptTask ift = opt("ift", condition(pm,
				"{ x, y -> x > y }", "x", "y"), task);

		add(pm, ift);
		put(pm, "x", 10.0);
		put(pm, "y", 20.0);

		task = exert(ift);
		// logger.info("task: " + task);
		assertEquals(get(task, Condition.CONDITION_VALUE), false);
		assertEquals(get(task, Condition.CONDITION_TARGET), "add");

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		task = exert(ift);
		// logger.info("opt eval: " + exert(ift));
		assertEquals(get(task, Condition.CONDITION_VALUE), true);
		assertEquals(get(task, Condition.CONDITION_TARGET), "add");
		assertEquals(get(task, "result/y"), 100.0);
	}

	@Test
	public void altExertionTest() throws Exception {
		EntModel pm = entModel("prc-model");
		pm.putValue("x1", 30.0);
		pm.putValue("y1", 20.0);
		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);


		mog t3 = xrt("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", operator.inVal("arg/x1", 200.0), operator.inVal("arg/x2", 50.0),
						result("result/y")));

		mog t4 = xrt("t4", sig("multiply", MultiplierImpl.class),
				cxt("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
						result("result/y")));

		mog t5 = xrt("t5", sig("add", AdderImpl.class),
				cxt("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0),
						result("result/y")));
		
		OptTask opt1 = opt("opt1", condition(pm,
				"{ x1, y1 -> x1 > y1 }", "x1", "y1"), t3);

		OptTask opt2 = opt("opt2", condition(pm,
				"{ x2, y2 -> x2 >= y2 }", "x2", "y2"), t4);

		// no condition means condition(true)
		OptTask opt3 = opt("op3", t5);

		AltTask alt = alt("alt", opt1, opt2, opt3);
		add(pm, opt1, opt2, opt3, alt);

//		logger.info("opt1 eval: " + eval(opt1));
		assertTrue(exec(opt1).equals(150.0));
//		logger.info("opt2 eval: " + eval(opt2));
		assertTrue(exec(opt2).equals(500.0));
//		logger.info("opt3 eval: " + eval(opt3));
		assertTrue(exec(opt3).equals(100.0));
//		logger.info("alt eval: " + eval(alt));
		assertTrue(exec(alt).equals(150.0));

		pm.putValue("x1", 10.0);
		pm.putValue("y1", 20.0);
//		logger.info("opt eval: " + eval(alt));
		logger.info("pm context 1: " + pm);
		assertTrue(exec(alt).equals(500.0));
		
		pm.putValue("x2", 40.0);
		pm.putValue("y2", 50.0);
		logger.info("pm context 2: " + pm);
//		logger.info("opt valueX: " + eval(alt));
		assertTrue(exec(alt).equals(100.0));
	}

	@Test
	public void loopExertionTest() throws RemoteException, ContextException {
//		final EntModel pm = model("prc-model");
//		final Var<Double> x = var("x", 1.0);
//		Var y = var("y", groovy("x + 1", x));
//		add(pm, x);
//		add(pm, y);
//		
//		// update x and y for the loop condition (z) depends on
//		Runnable update = new Runnable() {
//			 public void run() {
//				 try {
//					while ((Double)eval(pm, "x") < 25.0) {
//						 setValue(x, eval(x) + 1.0);
//						 System.out.println("running ... " + eval(pm, "x"));
//						 Thread.sleep(200);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			 }
//		};
//		
//		new Thread(update).start();
//		
//		Var vloop = loop("vloop",
//				condition(pm, "{ x -> x < 20 }", "x"), 
//				y);
//		
//		add(pm, vloop);
//		assertEquals(eval(vloop), 20.0);

//		logger.info("loop eval: " + eval(pm, "vloop"));
	}
}
