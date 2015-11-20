package sorcer.sml.jobs;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.RemoteServiceShell;
import sorcer.core.provider.ServiceTasker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Strategy.Provision;
import sorcer.service.Strategy.Shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.input;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.*;
import static sorcer.po.operator.put;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class NetJobExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(NetJobExertions.class);
	
	// two level job composition with PULL and PAR execution
	private static Job createJob(Flow flow, Access access) throws Exception {
		
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
//						outEnt("result/y")), strategy(Monitor.YES));
						outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
//						outEnt("result/y")), strategy(Monitor.YES));
						outEnt("result/y")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
//						outEnt("result/y")), strategy(Monitor.YES));
						outEnt("result/y")));


		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1", 
//				job("j2", t4, t5, strategy(flow, access, Monitor.YES)),
				t3,
                job("j2", t4, t5, strategy(flow, access)),

                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
//                strategy(Monitor.YES));
				
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
		logger.info("job j1 job context: " + upcontext(job));
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
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		
	}
	
	@Test
	public void exertJobPullParTest() throws Exception {

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
		
		Job job = createJob(Flow.SEQ, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		
	}
	
	@Test
	public void localJobber() throws Exception {
		
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
				"j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertEquals(value(context, "j1/t3/result/y"), 400.0);
	}

	@Test
	public void remoteServiceShell() throws Exception {
		
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
		Job job = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Object context = exec(sig(RemoteServiceShell.class), job);
		logger.info("job context: " + context);
		assertEquals(value((Context)context, "j1/t3/result/y"), 400.0);

	}

	@Test
	public void serviceShellRemote() throws Exception {
		
		Task t3 = task(
				"t3",
				sig("subtract", Subtractor.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("exert", ServiceJobber.class, Shell.REMOTE),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertEquals(value(context, "j1/t3/result/y"), 400.0);

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
		
		assertEquals(value(result, "arg/x1"), 20.0);
		assertEquals(value(result, "arg/x2"), 80.0);
	
		cxtt = task("appendContext", sig("appendContext", Contexter.class, prvName("Add Contexter")),
				context("add", inEnt("arg/x1", 200.0), inEnt("arg/x2", 800.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 2: " + result);

		cxtt = task("getAddContext", sig("getContext", Contexter.class, prvName("Add Contexter")),
				context("add", input("arg/x1"), input("arg/x2")));

		result = context(exert(cxtt));
//		logger.info("contexter context 3: " + result);
		
		assertEquals(value(result, "arg/x1"), 200.0);
		assertEquals(value(result, "arg/x2"), 800.0);
		
		// reset the contexter
		cxtt = task("appendContext", sig("appendContext", Contexter.class, prvName("Add Contexter")),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 4: " + result);
		assertEquals(value(result, "arg/x1"), 20.0);
		assertEquals(value(result, "arg/x2"), 80.0);
		
	}
	
	@Test
	public void netContexterTaskTest() throws Exception {
		
		Task t5 = task("t5", sig("add", Adder.class), 
					sig("getContext", Contexter.class, prvName("Add Contexter"), Signature.APD),
					context("add", inEnt("arg/x1"), inEnt("arg/x2"),
						result("result/y")));

		Context result =  context(exert(t5));
		logger.info("out context: " + result);
		assertEquals(value(result, "arg/x1"), 20.0);
		assertEquals(value(result, "arg/x2"), 80.0);
		assertEquals(value(result, "result/y"), 100.0);
		
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
				context("subtract", inEnt("arg/x5", null),
						inEnt("arg/x6"), outEnt("result/y3")));

		// job("f1", job("f2", f4, f5), f3,
		// job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
		
		return f1;
	}

	@Ignore
	@Test
	public void arithmeticJobExerter() throws Exception {

		// get the current value of the exertlet
		Task task = task("eval", sig("getValue", Evaluation.class, prvName("Arithmetic Exerter")));
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 400.0);

		// update inputs contexts
		Context multiplyContext = context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 70.0));
		Context addContext = context("add", inEnt("arg/x1", 90.0), inEnt("arg/x2", 110.0));
		Context invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 500.0);

		// update contexts partially
		multiplyContext = context("multiply", inEnt("arg/x1", 20.0));
		addContext = context("add", inEnt("arg/x1", 80.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
//		logger.info("j1/t3/result/y: " + value(task, "j1/t3/result/y"));
		assertEquals(value(task, "j1/t3/result/y"), 1210.0);

		// reverse the state to the initial one
		multiplyContext = context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0));
		addContext = context("add", inEnt("arg/x1", 80.0), inEnt("arg/x2", 20.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		task = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
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
				 
		assertTrue(value(par(vm, "j1")).equals(400.0));
	}

	private Job getMultiFiJob() throws Exception {

		Task t3 = task("t3",
				sFi("object", sig("subtract", SubtractorImpl.class)),
				sFi("net", sig("subtract", Subtractor.class)),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y")));

		Task t4 = task("t4",
				sFi("object", sig("multiply", MultiplierImpl.class)),
				sFi("net", sig("multiply", Multiplier.class)),
				context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = task("t5",
				sFi("object", sig("add", AdderImpl.class)),
				sFi("net", sig("add", Adder.class)),
				context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Job job = job("j1",
				sFi("object", sig("exert", ServiceJobber.class)),
				sFi("net", sig("exert", Jobber.class)),
				job("j2",
						sFi("object", sig("exert", ServiceJobber.class)),
						sFi("net", sig("exert", Jobber.class)),
						t4, t5),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
				sFi("job1", cFi("j1/j2/t4", "object"), cFi("j1/j2/t5", "net")),
				sFi("job2",  cFi("j1/j2", "net"),
						cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")),
				sFi("job3",  cFi("j1", "net"), cFi("j1/j2", "net"),
						cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")));

		return (Job)tracable(job);
	}

	// Fix marshaling object signatures
	@Ignore
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

		// Jobbers and  all tasks are local
		Context out = upcontext(exert(job));
		logger.info("job context: " + out);
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));

		//  Local Jobbers with remote Multiplier nad Adder
		job = getMultiFiJob();
		job = exert(job, fi("object"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net"));
		logger.info("job context: " + upcontext(job));
		logger.info("job trace: " + trace(job));
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));

		// Local Jobbers, Adder, and Multiplier with remote Subtractor
		job = getMultiFiJob();
		job = exert(job, cFi("j1", "object"), cFi("j1/t3", "net"));
		logger.info("job context: " + upcontext(job));
		logger.info("job trace: " + trace(job));
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));

		// Composite fidelity for local execution with remote Adder
		job = getMultiFiJob();
		job = exert(job, fi("job1"));
		logger.info("job context: " + upcontext(job));
		logger.info("job trace: " + trace(job));
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));

		// Composite fidelity for j1 local, j2 remote with all
		// remote component services
		job = getMultiFiJob();
		job = exert(job, fi("job2"));
		logger.info("job context: " + upcontext(job));
		logger.info("job trace: " + trace(job));
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));

		// Composite fidelity for all remote services
		job = getMultiFiJob();
		job = exert(job, fi("job3"));
		logger.info("job context: " + upcontext(job));
		logger.info("job trace: " + trace(job));
		assertTrue(value(out, "j1/t3/result/y").equals(400.0));
	}

}
