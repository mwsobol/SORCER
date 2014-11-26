package sorcer.eol.jobs;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Averager;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.provider.Exerter;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.ServiceTasker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.*;

import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/eol")
public class NetJobExertions implements SorcerConstants {
	private final static Logger logger = Logger.getLogger(NetJobExertions.class.getName());
	
	@Test
	public void exertAdderProviderTest() throws Exception {
		Task t5 = task("t5",
				sig("add", Adder.class),
				context("add", inEnt("arg, x1", 20.0),
						inEnt("arg, x2", 80.0), result("result/y")));
		
		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		assertEquals(value(context(t5), "result/y"), 100.0);
	}
	
	@Test
	public void evaluateAdder() throws Exception {
		Task t5 = task("t5",
				sig("add", Adder.class),
				context("add", inEnt("arg, x1", 20.0),
						inEnt("arg, x2", 80.0), result("result/y")));
		
		assertEquals(value(t5), 100.0);
	}
	
	@Test
	public void evaluateAverager() throws Exception {
		Task t5 = task(
				"t5",
				sig("average", Averager.class),
				context("average", inEnt("arg, x1", 20.0),
						inEnt("arg, x2", 80.0), result("result/y")));
		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		assertEquals(value(context(t5), "result/y"), 50.0);
	}
	
	@Test
	public void arithmeticNetFiTaskT() throws Exception {
		Task task = task("add",
				srvFi("net", sig("add", Adder.class)),
				srvFi("object", sig("add", AdderImpl.class)),
				context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));
		
		logger.info("sFi: " + srvFi(task));
		logger.info("sFis: " + srvFis(task));

//		task = exert(task, sFi("object"));
//		logger.info("exerted: " + task);
//		assertTrue((Double)get(task) == 100.0);
		
		task = exert(task, srvFi("net"));
		logger.info("exerted: " + task);
		assertTrue("Wrong value for 100.0", (Double)get(task) == 100.0);
	}
	
	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
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
	public void arithmeticFiBatchJob() throws Exception {
		Task t3 = task("t3", 
				srvFi("object", sig("subtract", SubtractorImpl.class), sig("average", AveragerImpl.class)),
				srvFi("net", sig("subtract", Subtractor.class), sig("average", Averager.class)),
				context("t3-cxt", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y", null)));
				
		Task t4 = task("t4", srvFi("object", sig("multiply", MultiplierImpl.class)),
				srvFi("net", sig("multiply", Multiplier.class)),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y", null)));

		Task t5 = task("t5", srvFi("object", sig("add", AdderImpl.class)),
				srvFi("net", sig("add", Adder.class)),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Job job = job("j1", srvFi("object", sig("service", ServiceJobber.class)),
				srvFi("net", sig("service", Jobber.class)),
				job("j2", sig("service", ServiceJobber.class), t4, t5), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")),
				fiContext("mix1", srvFi("j1", "net"), csFi("j1/j2/t4", "net")),
				fiContext("mix2", srvFi("j1", "net"), csFi("j1/j2/t4", "net"), csFi("j1/j2/t5", "net")));

		//The Jobber and  all tasks are local with 'subtract' signature
		Job result = exert(job, srvFi("object"), csFi("j1/t3", "object", "subtract"));
		logger.info("result context: " + serviceContext(result));
		assertTrue((Double)get(result, "j1/t3/result/y") == 400.0);		

//		//The Jobber and  all tasks are local with 'average' signature
//		result = exert(job, sFi("object"), csFi("j1/t3", "object", "average"));
//		logger.info("result context: " + jobContext(result));
//		assertTrue((Double) get(result, "j1/t3/result/y") == 300.0);
	}
	
	@Ignore
	@Test
	public void arithmeticFiJobTest() throws ExertionException, SignatureException, ContextException, RemoteException {
		Task t3 = task("t3", srvFi("object", sig("subtract", SubtractorImpl.class)),
				srvFi("net", sig("subtract", Subtractor.class)),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y")));

		Task t4 = task("t4", srvFi("object", sig("multiply", MultiplierImpl.class)),
				srvFi("net", sig("multiply", Multiplier.class)),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task("t5", srvFi("object", sig("add", AdderImpl.class)),
				srvFi("net", sig("add", Adder.class)),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Job job = job("j1", srvFi("object", sig("service", ServiceJobber.class)),
				srvFi("net", sig("service", Jobber.class)),
				job("j2", sig("service", ServiceJobber.class), t4, t5), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")),
				fiContext("mix1", srvFi("j1", "net"), csFi("j1/j2/t4", "net")),
				fiContext("mix2", srvFi("j1", "net"), csFi("j1/j2/t4", "net"), csFi("j1/j2/t5", "net")));
		
//		logger.info("sFi j1: " + sFi(job));
//		logger.info("sFis j1: " + sFis(job));
//		logger.info("sFi j2: " + sFi(exertion(job, "j1/j2")));
//		logger.info("sFis j2: " + sFis(exertion(job, "j1/tj2")));
//		logger.info("sFi t3: " + sFi(exertion(job, "j1/t3")));
//		logger.info("sFi t4: " + sFi(exertion(job, "j1/j2/t4")));
//		logger.info("sFi t5: " + sFi(exertion(job, "j1/j2/t5")));
//		logger.info("job context: " + job.getJobContext());
		
		//The Jobber and  all tasks are local
		job = exert(job);
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double)get(job, "j1/t3/result/y") == 400.0);
		
		// The remote Jobber with the all local task
		job = exert(job, srvFi("net"));
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double)get(job, "j1/t3/result/y") == 400.0);

		// The local Jobber with the remote Adder
		job = exert(job, csFi("j1/j2/t4", "net"));
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double)get(job, "j1/t3/result/y") == 400.0);
		
		// The remote Jobber with the remote Adder
//		job = exert(job, cFi("j1/j2/t5", "net"));
		job = exert(job, srvFi("object"), csFi("j1/j2/t4", "net"), csFi("j1/j2/t5", "net"));
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double) get(job, "j1/t3/result/y") == 400.0);
				
		job = exert(job, fiContext(srvFi("j1", "net"), csFi("j1/j2/t4", "net")));
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double)get(job, "j1/t3/result/y") == 400.0);
		
		job = exert(job, fiContext("mix2"));
		logger.info("job context: " + serviceContext(job));
		assertTrue((Double)get(job, "j1/t3/result/y") == 400.0);
		
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
		Job job = job("j1", 
				job("j2", t4, t5, strategy(flow, access)), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
				
		return job;
	}
	
	public static Exertion createJob() throws Exception {
		return createJob(Flow.SEQ, Access.PUSH);
	}

	@Test
	public void exertJobPushParTest() throws Exception {
		Job job = createJob(Flow.PAR, Access.PUSH);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + serviceContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPushSeqTest() throws Exception {
		Job job = createJob(Flow.SEQ, Access.PUSH);
		logger.info("job j1: " + job);
		job = exert(job);
		logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + serviceContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPullParTest() throws Exception {
		Job job = createJob(Flow.PAR, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + serviceContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void exertJobPullSeqTest() throws Exception {
		Job job = createJob(Flow.SEQ, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + serviceContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}
	
	@Test
	public void testLocalNetJobComposition() throws Exception {
		Task t3 = task(
				"t3",
				sig("subtract", Subtractor.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y", null)));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		Context context = serviceContext(exert(job));
		logger.info("job context: " + context);
		assertEquals(get(context, "j1/t3/result/y"), 400.0);
	}
	
	@Test
	public void exerterTest() throws Exception {
	Task f5 = task(
			"f5",
			sig("add", Adder.class),
			context("add", inEnt("arg/x1", 20.0),
					inEnt("arg/x2", 80.0), outEnt("result/y", null)),
			strategy(Monitor.NO, Wait.YES));
	
//	long start = System.currentTimeMillis();
	Exerter exerter = (Exerter)provider(sig( Exerter.class));
	Exertion out = exert(exerter, f5);
	 
//	long end = System.currentTimeMillis();
	
//	logger.info("task f5 context: " + context(out));
//	logger.info("task f5 result/y: " + get(context(out), "result/y"));
	assertEquals(get(out, "result/y"), 100.00);
	}
		
	public static Context createContext() throws Exception {
		return context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0));
	}
	
	@Test
	public void contexterTest() throws Exception {
		Task cxtt = task("getAddContext", sig("getContext", Contexter.class, prvName("Add Contexter")),
				context("add", input("arg/x1"), input("arg/x2")));
		 
		Context result = context(exert(cxtt));
//		logger.info("contexter context 1: " + result);
		
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
	
		cxtt = task("appendContext", sig("appendContext", Contexter.class, prvName("Add Contexter")),
				context("add", inEnt("arg/x1", 200.0), inEnt("arg/x2", 800.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 2: " + result);

		cxtt = task("getAddContext", sig("getContext", Contexter.class, prvName("Add Contexter")),
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
	
	@Test
	public void netContexterTaskTest() throws Exception {
		Task t5 = task("t5", sig("add", Adder.class), 
					sig("getContext", Contexter.class, prvName("Add Contexter"), Signature.APD),
					context("add", inEnt("arg/x1"), inEnt("arg/x2"),
						result("result/y")));

		Context result =  context(exert(t5));
		logger.info("out context: " + result);
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
		assertEquals(get(result, "result/y"), 100.0);
	}
	
	public Job createProvisionedJob() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply", Multiplier.class,
					deploy(implementation(ServiceTasker.class.getName()),
						classpath("ex6-arithmetic-beans.jar"),
						codebase("ex6-arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/multiplier-prv.config"),
						maintain(1),
						idle("3h"))),
				context("multiply", inEnt("arg/x1", 10.0d),
						inEnt("arg/x2", 50.0d), outEnt("result/y1", null)));

		Task f5 = task(
				"f5",
				sig("add", Adder.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/adder-prv.config"),
						idle(60*3))),
				context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
						outEnt("result/y2", null)));

		Task f3 = task(
				"f3",
				sig("subtract", Subtractor.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/subtractor-prv.config"))),
				context("subtract", inEnt("arg/x5", null),
						inEnt("arg/x6", null), outEnt("result/y3", null)));

		// job("f1", job("f2", f4, f5), f3,
		// job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
				pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
		
		return f1;
	}

	@Test
	public void arithmeticJobExertleter() throws Exception {
		// get the current value of the exertlet
		Task task = task("eval", sig("getValue", Evaluation.class, prvName("Arithmetic Exertleter")));
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));

		assertEquals(value(task, "j1/t3/result/y"), 400.0);

		// update inputs contexts
		Context multiplyContext = context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 70.0));
		Context addContext = context("add", inEnt("arg/x1", 90.0), inEnt("arg/x2", 110.0));
		Context invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exertleter")), invokeContext);
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 500.0);

		// update contexts partially
		multiplyContext = context("multiply", inEnt("arg/x1", 20.0));
		addContext = context("add", inEnt("arg/x1", 80.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exertleter")), invokeContext);
//		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 1210.0);

		// reverse the state to the initial one
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
	public void createMogram() throws Exception {

		ParModel vm = parModel(
				"Hello Arithmetic #2",
				// inputs
				par("x1"), par("x2"), par("x3", 20.0), par("x4"),
				// outputs
				par("t4", invoker("x1 * x2", pars("x1", "x2"))),
				par("t5",
					task("t5",
						sig("add", Adder.class),
						cxt("add", inEnt("arg/x3"),inEnt("arg/x4"),
						result("result/y")))),
				par("j1", invoker("t4 - t5", pars("t4", "t5"))));

		vm = put(vm, ent("x1", 10.0), ent("x2", 50.0),
				ent("x4", 80.0));
				 
		assertEquals(value(par(vm, "j1")), 400.0);
	}

}
