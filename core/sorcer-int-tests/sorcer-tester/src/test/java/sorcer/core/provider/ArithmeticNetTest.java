package sorcer.core.provider;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Averager;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.*;
import sorcer.service.Strategy.*;
import sorcer.util.ProviderAccessor;
import sorcer.util.Sorcer;
import sorcer.util.Stopwatch;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ArithmeticNetTest implements SorcerConstants {

	private static final Logger logger = LoggerFactory.getLogger(ArithmeticNetTest.class);

	@Test
	public void getProviderTest() throws Exception {
		Cataloger catalog = ProviderAccessor.getCataloger();
		Object proxy = catalog.lookup(Adder.class);
		if (proxy != null)
			System.out.println("Adder: " + Arrays.toString(proxy.getClass().getInterfaces()));

		String[] pnames = catalog.getProviderList();
		logger.info("cataloger pnames: " + Arrays.toString(pnames));

		assertNotNull(proxy);
	}
	
	@Test
	public void providerAcessorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Object provider = Accessor.getService(new NetSignature(Averager.class));
		logger.info("INTERFACES: " + Arrays.toString(provider.getClass().getInterfaces()));
		assertTrue(Arrays.asList(provider.getClass().getInterfaces()).contains(Averager.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void getCatalogTest() throws Exception {
		Cataloger catalog = ProviderAccessor.getCataloger();
		System.out.println("Cataloger: " + catalog);
		String[] pnames = catalog.getProviderList();
		logger.info("cataloger pnames: " + Arrays.toString(pnames));
	}
	
	private Task getAddTask() throws Exception {
		Context context = new PositionalContext("add");
		context.putInValue("arg1/value", 20.0);
		context.putInValue("arg2/value", 80.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0);
		Signature method = new NetSignature("add", Adder.class);
		Task task = new NetTask("add", method);
		task.setContext(context);
		return task;
	}

	private Task getMultiplyTask() throws Exception {
		Context context = new PositionalContext("multiply");
		context.putInValue("arg1/value", 10.0);
		context.putInValue("arg2/value", 50.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0);
		Signature method = new NetSignature("multiply", Multiplier.class);
		Task task = new NetTask("multiply", method);
		task.setContext(context);
		return task;
	}

	private Task getSubtractTask() throws Exception {
		PositionalContext context = new PositionalContext("subtract");
		// We want to stick in the result of multiply in here
		context.putInValueAt("arg1/value", 0.0, 1);
		// We want to stick in the result of add in here
		context.putInValueAt("arg2/value", 0.0, 2);
		Signature method = new NetSignature("subtract", Subtractor.class);
		Task task = new NetTask("subtract",
				"processing results from two previous tasks", method);
		task.setContext(context);
		return task;
	}
	
	@Test
	public void arithmeticProviderTest() throws Exception {
        System.out.println("========== arithmeticProviderTest ==========");
		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inEnt("arg, x1", 20.0),
						inEnt("arg, x2", 80.0), result("result, y")));
		t5 = exert(t5);
		//logger.info("t5 context: " + context(t5));
		//logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 100.0", value(t5), 100.0);
	}
	
	@Test
	public void arithmeticNetMultiFiTaskTest() throws ExertionException, SignatureException, ContextException, RemoteException {
		Task task = task("add",
				sFi("net", sig("add", Adder.class)),
				sFi("object", sig("add", AdderImpl.class)),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));
		
		logger.info("sFi: " + sFi(task));
		logger.info("sFis: " + srvFis(task));

//		task = exert(task, sFi("object"));
//		logger.info("exerted: " + task);
//		assertTrue("Wrong value for 100.0", get(task).equals(100.0));
		
		task = exert(task, fi("net"));
		logger.info("exerted: " + task);
		assertTrue("Wrong value for 100.0", get(task).equals(100.0));
	}

	private Job getMultiFiJob() throws Exception {

		Task t3 = task("t3",
				sFi("object", sig("subtract", SubtractorImpl.class)),
				sFi("net", sig("subtract", Subtractor.class)),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y", null)));

		Task t4 = task("t4",
				sFi("object", sig("multiply", MultiplierImpl.class)),
				sFi("net", sig("multiply", Multiplier.class)),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y", null)));

		Task t5 = task("t5",
				sFi("object", sig("add", AdderImpl.class)),
				sFi("net", sig("add", Adder.class)),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y", null)));

		Job job = job("j1",
				sFi("object", sig("service", ServiceJobber.class)),
				sFi("net", sig("service", Jobber.class)),
				job("j2",
						sFi("object", sig("service", ServiceJobber.class)),
						sFi("net", sig("service", Jobber.class)),
						t4, t5),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
				sFi("job1", cFi("j1", "object"), cFi("j1/j2/t4", "object")),
				sFi("job2", cFi("j1", "net"), cFi("j1/t3", "net"),
						cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")));

		return job;
	}

	@Test
	public void arithmeticMultiFiJobTest() throws Exception {

		Job job = getMultiFiJob();

		logger.info("sFi j1: " + sFi(job));
		logger.info("sFis j1: " + sFis(job));
		logger.info("sFi j2: " + sFi(exertion(job, "j1/j2")));
		logger.info("sFis j2: " + sFis(exertion(job, "j1/tj2")));
		logger.info("sFi t3: " + sFi(exertion(job, "j1/t3")));
		logger.info("sFi t4: " + sFi(exertion(job, "j1/j2/t4")));
		logger.info("sFi t5: " + sFi(exertion(job, "j1/j2/t5")));
		logger.info("job context: " + upcontext(job));
		
		//The Jobber and  all tasks are local
		job = exert(job);
		logger.info("job context: " + upcontext(job));
		assertTrue(get(job, "j1/t3/result/y").equals(400.0));

		// The local Jobber with the remote Multiplier nad Adder
		job = getMultiFiJob();
		job = exert(job, fi("object"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net"));
		logger.info("job context: " + upcontext(job));
		assertTrue(get(job, "j1/t3/result/y").equals(400.0));

		// The local Jobber, Adder, and Multiplier with the remote Subtractor
		job = getMultiFiJob();
		job = exert(job, cFi("j1", "object"), cFi("j1/t3", "net"));
		logger.info("job context: " + upcontext(job));
		assertTrue(get(job, "j1/t3/result/y").equals(400.0));

		// composite fidelity for local execution
		job = getMultiFiJob();
		job = exert(job, fi("job1"));
		logger.info("job context: " + upcontext(job));
		assertTrue(get(job, "j1/t3/result/y").equals(400.0));

		// composite fidelity for network execution
		job = getMultiFiJob();
		job = exert(job, fi("job2"));
		logger.info("job context: " + upcontext(job));
		assertTrue(get(job, "j1/t3/result/y").equals(400.0));
	}

	@Test
	public void arithmeticMultiFiBatchTaskTest() throws Exception {
		
		Task t4 = task("t4",
				sFi("object", sig("multiply", MultiplierImpl.class), sig("add", AdderImpl.class)),
				sFi("net", sig("multiply", Multiplier.class), sig("add", Adder.class)),
				context("shared", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		t4 = exert(t4);
		logger.info("task context: " + context(t4));
		
		t4 = exert(t4, fi("net"));
		logger.info("task context: " + context(t4));
	}
	
	@Test
	public void averagerproxyProviderTest() throws Exception {
        System.out.println("========== averagerProviderTest ==========");
		Task t5 = task(
				"t5",
				sig("average", Averager.class),
				context("average", inEnt("arg, x1", 20.0),
						inEnt("arg, x2", 80.0), result("result, y")));
		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 50.0", value(t5), 50.0);
	}
	
	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
        System.out.println("========== arithmeticSpaceTaskTest ==========");
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
	public void exertJobPushParTest() throws Exception {
        System.out.println("========== exertJobPushParTest ==========");
		Job job = createJob(Flow.PAR, Access.PUSH);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPushSeqTest() throws Exception {
        System.out.println("========== exertJobPushSeqTest ==========");
		Job job = createJob(Flow.SEQ, Access.PUSH);
		logger.info("job j1: " + job);
		job = exert(job);
		logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPullParTest() throws Exception {
        System.out.println("========== exertJobPullParTest ==========");
		Job job = createJob(Flow.PAR, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPullSeqTest() throws Exception {
        System.out.println("========== exertJobPullSeqTest ==========");
		Job job = createJob(Flow.SEQ, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exerterTest() throws Exception {
        System.out.println("========== exerterTest ==========");
	Task f5 = task(
			"f5",
			sig("add", Adder.class),
			context("add", inEnt("arg/x1", 20.0),
					inEnt("arg/x2", 80.0), outEnt("result/y", null)),
			strategy(Monitor.NO, Wait.YES));
	
	Exertion out = null;
//	long start = System.currentTimeMillis();
	Exerter exerter = Accessor.getService(Exerter.class);
//	logger.info("got exerter: " + exerter);

	out = exerter.exert(f5);
//	long end = System.currentTimeMillis();
	
//	logger.info("task f5 context: " + context(out));
//	logger.info("task f5 result/y: " + get(context(out), "result/y"));
	assertEquals(get(out, "result/y"), 100.00);
	}
	
	@Test
	public void arithmeticEolExertleter() throws Exception {
        System.out.println("========== arithmeticEolExertleter ==========");
		// get the current value of the exertlet
		Task task = task("eval", sig("getValue", Evaluation.class, prvName("Arithmetic Exertleter")));
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 400.0);
	
		// change both the contexts completely
		Context multiplyContext = context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 70.0));
		Context addContext = context("add", inEnt("arg/x1", 90.0), inEnt("arg/x2", 110.0));
		Context invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exertleter")), invokeContext);
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 500.0);
	
		// change partially the contexts
		multiplyContext = context("multiply", inEnt("arg/x1", 20.0));
		addContext = context("add", inEnt("arg/x1", 80.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exertleter")), invokeContext);
//		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 1210.0);		
				
		// reverse the state of the exertleter
		multiplyContext = context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0));
		addContext = context("add", inEnt("arg/x1", 80.0), inEnt("arg/x2", 20.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exertleter")), invokeContext);
//		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 400.0);
	}
	
	@Test
	public void arithmeticApiExertleter() throws Exception {
        System.out.println("========== arithmeticApiExertleter ==========");
		// get the current value of the exertlet
		NetSignature signature = new NetSignature("getValue", Evaluation.class, 
				Sorcer.getActualName("Arithmetic Exertleter"));
		Task task = new NetTask("eval", signature);
		Task result = task.exert();
		Context out = (Context)result.getReturnValue();
		logger.info("result: " + result);
//		logger.info("j1/t3/result/y: " + out.getValue("j1/t3/result/y"));
		assertEquals(out.getValue("j1/t3/result/y"), 400.0);
		
		
		// change both the contexts completely
		Context addContext = new PositionalContext("add");
		addContext.putInValue("arg/x1", 90.0);
		addContext.putInValue("arg/x2", 110.0);
		
		Context multiplyContext = new PositionalContext("multiply");
		multiplyContext.putInValue("arg/x1", 10.0);
		multiplyContext.putInValue("arg/x2", 70.0);

		ServiceContext invokeContext = new ServiceContext("invoke");
		invokeContext.putLink("t5", addContext, "");
		invokeContext.putLink("t4", multiplyContext, "");
		
		signature = new NetSignature("invoke", Invocation.class, 
				Sorcer.getActualName("Arithmetic Exertleter"));
		
		task = new NetTask("invoke", signature, invokeContext);
		result = task.exert();
		logger.info("result context: " + result);
		out = result.getContext();
		logger.info("result context: " + out);
//		logger.info("j1/t3/result/y: " + out.getValue("j1/t3/result/y"));
//		assertEquals(out.getValue("j1/t3/result/y"), 500.0);

		
		// change partially the contexts
		addContext = new PositionalContext("add");
		addContext.putInValue("arg/x1", 80.0);
		
		multiplyContext = new PositionalContext("multiply");
		multiplyContext.putInValue("arg/x1", 20.0);

		invokeContext = new ServiceContext("invoke");
		invokeContext.putLink("t5", addContext, "");
		invokeContext.putLink("t4", multiplyContext, "");
		
		signature = new NetSignature("invoke", Invocation.class,
				Sorcer.getActualName("Arithmetic Exertleter"));
		
		task = new NetTask("invoke", signature, invokeContext);
		result = task.exert();
		out = result.getContext();
//		logger.info("result context: " + out);
//		logger.info("j1/t3/result/y: " + out.getValue("j1/t3/result/y"));
		assertEquals(out.getValue("j1/t3/result/y"), 1210.0);
	
		
		// reverse the state of the exertleter
		addContext = new PositionalContext("t5");
		addContext.putInValue("arg/x1", 20.0);
		addContext.putInValue("arg/x2", 80.0);
		multiplyContext = new PositionalContext("t4");
		multiplyContext.putInValue("arg/x1", 10.0);
		multiplyContext.putInValue("arg/x2", 50.0);

		invokeContext = new ServiceContext("invoke");
		invokeContext.putLink("t5", addContext, "");
		invokeContext.putLink("t4", multiplyContext, "");
		
		signature = new NetSignature("invoke", Invocation.class, 
				Sorcer.getActualName("Arithmetic Exertleter"));
		
		task = new NetTask("invoke", signature, invokeContext);
		result = (Task)task.exert();		
		out = result.getContext();
//		logger.info("result context: " + out);
//		logger.info("j1/t3/result/y: " + out.getValue("j1/t3/result/y"));
		assertEquals(out.getValue("j1/t3/result/y"), 400.0);
	}
	
	// two level job composition with PULL and PAR execution
	private static Job createJob(Flow flow, Access access) throws Exception {
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job job = job("j1",
		Job job = job("j1", //sig("service", RemoteJobber.class), 
				//job("j2", t4, t5),
				job("j2", t4, t5, strategy(flow, access)), 
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
				
		return job;
	}
			
	@Test
	public void contexterTest() throws Exception {
        System.out.println("========== contexterTest ==========");
		Task cxtt = task("addContext", sig("getContext", Contexter.class, prvName("Add Contexter")),
				context("add", input("arg/x1"), input("arg/x2")));
		 
		Context result = context(exert(cxtt));
//		logger.info("contexter context 1: " + result);
		
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
	
		cxtt = task("appendContext", sig("appendContext", Contexter.class, prvName("Add Contexter")),
				context("add", inEnt("arg/x1", 200.0), inEnt("arg/x2", 800.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 2: " + result);

		cxtt = task("addContext", sig("getContext", Contexter.class, prvName("Add Contexter")),
				context("add", input("arg/x1"), input("arg/x2")));

		result = context(exert(cxtt));
//		logger.info("contexter context 3: " + result);
		
		assertEquals(get(result, "arg/x1"), 200.0);
		assertEquals(get(result, "arg/x2"), 800.0);
		
		// reset the contexter
		cxtt = task("appendContext", sig("appendContext", Contexter.class, prvName("Add Contexter")),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 4: " + result);
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
	}
	
	public void netContexterTaskTest() throws Exception {
        System.out.println("========== netContexterTaskTest ==========");
		Task t5 = task("t5", sig("add", Adder.class), 
					sig("getContext", Contexter.class, prvName("Add Contexter"), Signature.APD),
					context("add", inEnt("arg/x1"), inEnt("arg/x2"),
						result("result/y")));

		Context result =  context(exert(t5));
//		logger.info("out context: " + result);
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
		assertEquals(get(result, "result/y"), 100.0);
	}

	public Job createProvisionedJob() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply", Multiplier.class,
					deploy(implementation(ServiceTasker.class.getName()),
						classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/multiplier-prv.config"),
						maintain(1),
						idle("3h"))),
				context("multiply", inEnt("arg/x1", 10.0d),
						inEnt("arg/x2", 50.0d), outEnt("result/y1")));

		Task f5 = task(
				"f5",
				sig("add", Adder.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/adder-prv.config"),
						idle(60*3))),
				context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
						outEnt("result/y2")));

		Task f3 = task(
				"f3",
				sig("subtract", Subtractor.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/subtractor-prv.config"))),
				context("subtract", inEnt("arg/x5"),
						inEnt("arg/x6"), outEnt("result/y3")));

		// job("f1", job("f2", f4, f5), f3,
		// job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
		
		return f1;
	}

	@Test
	public void testProvisionedJob() throws Exception {
        System.out.println("========== testProvisionedJob ==========");
		Job f1 = createProvisionedJob();
		List<Signature> allSigs = f1.getAllSignatures();
//		logger.info("all sigs size: " + allSigs.size());
		assertEquals(allSigs.size(), 5);
		allSigs = f1.getAllTaskSignatures();
//		logger.info("all net sigs size: " + allSigs.size());
		assertEquals(allSigs.size(), 3);
		List<Signature> netSigs = f1.getAllNetTaskSignatures();
//		logger.info("all net sigs size: " + allSigs.size());
		assertEquals(netSigs.size(), 3);

		List<ServiceDeployment> allDeployments = f1.getAllDeployments();
//		logger.info("allDeployments: " + allDeployments);
//		logger.info("allDeployments size: " + allDeployments.size());
		assertEquals(allDeployments.size(), 3);

		int f4Idle = ((ServiceSignature) ((Task) exertion(f1, "f1/f2/f4"))
				.getProcessSignature()).getDeployment().getIdle();
		int f5Idle = ((ServiceSignature) ((Task) exertion(f1, "f1/f2/f5"))
				.getProcessSignature()).getDeployment().getIdle();
//		logger.info("f4 idle: " + f4Idle);
//		logger.info("f5 idle: " + f5Idle);
		assertEquals(f4Idle, f5Idle);
	}
	
	@Test
	public void exertionDeploymentIdTest() throws Exception {
        System.out.println("========== exertionDeploymentIdTest ==========");
		Job job = createProvisionedJob();
		String did =  job.getDeploymentId();
		logger.info("job deploy id: " + did);
		assertEquals(did, "8ec89094533e199116f7d3a53b49ea7f");
	}

	public static Context createContext() throws Exception {
		return context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0));
	}

	public static Exertion createJob() throws Exception {
		return createJob(Flow.SEQ, Access.PUSH);
	}

}
