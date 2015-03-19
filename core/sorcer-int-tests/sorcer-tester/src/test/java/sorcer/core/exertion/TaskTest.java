package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Provision;
import sorcer.service.Strategy.Wait;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class TaskTest {
	private final static Logger logger = Logger.getLogger(TaskTest.class
			.getName());

	@Test
	public void freeArithmeticTaskTest() throws ExertionException, SignatureException, ContextException {
		//to test tracing of execution enable ServiceExertion.debug 		
		Exertion task = task("add",
				sig("add"),
				context(inEnt("arg/x1"), inEnt("arg/x2"),
						result("result/y")));
		
		logger.info("get task: " + task);
		logger.info("get context: " + context(task));
		
		Object val = value(task, inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				strategy(sig("add", AdderImpl.class), Access.PUSH, Wait.YES));
		
		logger.info("get value: " + val);
		assertEquals("Wrong value for 100", val, 100.0);
	}
	
	@Test
	public void arithmeticTaskTest() throws ExertionException, SignatureException, ContextException, RemoteException {
		//to test tracing of execution enable ServiceExertion.debug 
		ServiceExertion.debug = true;
		
		Task task = task("add",
				sig("add", AdderImpl.class),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));
		
		// EXERTING
		task = exert(task);
		logger.info("exerted: " + task);
		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
		print(exceptions(task));
		assertTrue(exceptions(task).size() == 0);
		print(trace(task));
		
		// EVALUATING
		set(task, "result/y", Context.none);
		print(task);
		
		double val = (Double)value(task);
		//logger.info("get value: " + val);
		assertTrue("Wrong value for 100.0", val == 100.0);
		//logger.info("exec trace: " + trace(task));
		//logger.info("trace  size: " + trace(task).size());
		//assertTrue(trace(task).size() == 1);
		logger.info("exceptions: " + exceptions(task));
		//assertTrue(exceptions(task).size() == 0);

//		val = (Double)get(task, "result/y");
//		//logger.info("get value: " + val);
//		assertTrue("Wrong value for 100.0", val == 100.0);
//		
//		task = exert(task);
//		val = (Double)get(context(task), "result/y");
//		//logger.info("get value: " + val);
//		assertTrue("Wrong value for 100.0", val == 100.0);
//		//assertTrue(trace(task).size() == 2);
//		//           assertTrue(exceptions(task).size() == 0);
//		
//		put(task, entry("arg/x1", 1.0), entry("arg/x2", 5.0));
//		val = (Double)value(task);
//		logger.info("evaluate: " + val);
//		assertTrue("Wrong value for 6.0", val == 6.0);
//				
//		val = (Double)value(task, entry("arg/x1", 2.0), entry("arg/x2", 10.0));
//		logger.info("evaluate: " + val);
//		assertTrue("Wrong value for 12.0", val == 12.0);
//		
//		logger.info("task context: " + context(task));
//		logger.info("get value: " + get(task));
//		assertTrue("Wrong value for 12.0", get(task).equals(12.0));
	}
	
	@Test
	public void arithmeticFiTaskTest() throws ExertionException, SignatureException, ContextException, RemoteException {
		//to test tracing of execution enable ServiceExertion.debug 
		ServiceExertion.debug = true;
		
		Task task = task("add",
				srvFi("net", sig("add", Adder.class)),
				srvFi("object", sig("add", AdderImpl.class)),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));
		
//		logger.info("sFi: " + sFi(task));
//		logger.info("sFis: " + sFis(task));

		task = exert(task, srvFi("object"));
		logger.info("exerted: " + task);
		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
		
//		task = exert(task, fi("net"));
//		logger.info("exerted: " + task);
//		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
	}
	
//	@Test
//	public void exertObjectTaskTest() throws Exception {
//		ServiceExertion.debug = true;
//		ObjectTask objTask = new ObjectTask("t4", new ObjectSignature("multiply", Multiply.class, double[].class));
//		ServiceContext cxt = new ServiceContext();
//		Object arg = new double[] { 10.0, 50.0 };
//		//cxt.setReturnPath("result/y").setArgs(new double[] {10.0, 50.0});
//		cxt.setReturnPath("result/y").setArgs(arg);
//		objTask.setContext(cxt);
//		
//		//logger.info("objTask value: " + value(objTask));
//		assertEquals("Wrong value for 500.0", value(objTask), 500.0);
//		
//		ObjectTask objTask2 = (ObjectTask)task("t4", sig("multiply", new Multiply(), double[].class), 
//				context(args(new double[] {10.0, 50.0}), result("result/y")));
//		//logger.info("objTask2 value: " + value(objTask2));
//		assertEquals("Wrong value for 500.0", value(objTask2), 500.0);
//	}
	

//	@Test
//	public void t4_TaskTest() throws Exception {
//		Task t4 = task("t4", sig("multiply", new Multiply(), double[].class),
//				context(args(new double[] { 10.0, 50.0 }), result("result/y")));
//
//		//logger.info("t4: " + value(t4));
//		assertEquals("Wrong value for 500.0", value(t4), 500.0);
//	}

	
	@Test
	public void deployTest() throws Exception {
		Task t5 = task("f5",
			sig("add", Adder.class,
					deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
				context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
							outEnt("result/y")),
				strategy(Provision.YES));
		logger.info("t5 is provisionable: " + t5.isProvisionable());
		assertTrue(t5.isProvisionable());
	}
}
	
