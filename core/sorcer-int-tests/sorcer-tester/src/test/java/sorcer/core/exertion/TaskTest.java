package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiply;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Provision;
import sorcer.service.Strategy.Wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class TaskTest {
	private final static Logger logger = LoggerFactory.getLogger(TaskTest.class);

	@Test
	public void freeArithmeticTaskTest() throws Exception {
		//to test tracing of execution enable ServiceExertion.debug 		
		Exertion task = task("add",
				sig("add"),
				context(inVal("arg/x1"), inVal("arg/x2"),
						result("result/y")));
		
		logger.info("get task: " + task);
		logger.info("get context: " + context(task));
		
		Object val = eval(task, inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				strategy(sig("add", AdderImpl.class), Access.PUSH, Wait.YES));
		
		logger.info("get eval: " + val);
		assertEquals("Wrong eval for 100", val, 100.0);
	}
	
	@Test
	public void arithmeticTaskTest() throws Exception {
		//to test tracing of execution enable ServiceExertion.debug 
		ServiceExertion.debug = true;
		
		Task task = task("add",
				sig("add", AdderImpl.class),
				context(inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("result/y")));
		
		// EXERTING
		task = exert(task);
		logger.info("exerted: " + task);
		assertTrue("Wrong eval for 100.0", (Double)returnValue(task) == 100.0);
		print(exceptions(task));
		assertTrue(exceptions(task).size() == 0);
		print(trace(task));
		
		// EVALUATING
		set(task, "result/y", Context.none);
		print(task);
		
		double val = (Double) exec(task);
		//logger.info("get eval: " + val);
		assertTrue("Wrong eval for 100.0", val == 100.0);
	}

	@Test
	public void arithmeticCustomContextTest() throws Exception {
		//to test tracing of execution enable ServiceExertion.debug
		ServiceExertion.debug = true;

		Task task = task("add",
				sig("add", AdderImpl.class),
				context(CustomContext.class, inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("result/y")));

		// EXERTING
		task = exert(task);
		logger.info("exerted: " + task);
		assertTrue("Wrong eval for 100.0", (Double) returnValue(task) == 100.0);
		print(exceptions(task));
		assertTrue(exceptions(task).size() == 0);
		print(trace(task));
	}

		@Test
	public void argTaskTest() throws Exception {
		Task t4 = task("t4", sig("multiply", new Multiply()),
				context(
						types( double[].class),
						args(new double[]{10.0, 50.0}),
						result("result/y")));

		//logger.info("t4: " + eval(t4));
		assertTrue(eval(t4).equals(500.0));
	}

	@Test
	public void arithmeticMultiFiObjectTaskTest() throws Exception {
		ServiceExertion.debug = true;

		Task task = task("add",
				sFi("net", sig("add", Adder.class)),
				sFi("object", sig("add", AdderImpl.class)),
				context(inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("result/y")));

		logger.info("task fi: " + fi(task));
		assertTrue(fis(task).size() == 2);
		logger.info("selecled fi: " + fiName(task));
		assertTrue(fiName(task).equals("net"));

		task = exert(task, fi("object"));
		logger.info("exerted: " + context(task));
		assertTrue(fiName(task).equals("object"));
		assertTrue(returnValue(task).equals(100.0));
	}

	@Test
	public void arithmeticMultiFiNetTaskTest() throws Exception {
		ServiceExertion.debug = true;

		Task task = task("add",
				sFi("net", sig("add", Adder.class)),
				sFi("object", sig("add", AdderImpl.class)),
				context(inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("result/y")));

		logger.info("task fi: " + fi(task));
		assertTrue(fis(task).size() == 2);
		logger.info("selected Fi: " + fiName(task));
		assertTrue(fiName(task).equals("net"));

		task = exert(task, fi("net"));
		logger.info("exerted: " + context(task));
		assertTrue(fiName(task).equals("net"));
		assertTrue("Wrong eval for 100.0", (Double)returnValue(task) == 100.0);
	}
	
	@Test
	public void deployTest() throws Exception {
		Task t5 = task("f5",
			sig("add", Adder.class,
					deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
				context("add", inVal("arg/x3", 20.0d), inVal("arg/x4", 80.0d),
							outVal("result/y")),
				strategy(Provision.YES));
		logger.info("t5 is provisionable: " + t5.isProvisionable());
		assertTrue(t5.isProvisionable());
	}
}
	
