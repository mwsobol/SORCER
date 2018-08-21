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
import sorcer.core.SorcerConstants;
import sorcer.core.context.model.ent.EntryModel;
import sorcer.core.provider.*;
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
import static sorcer.po.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class NetJobExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(NetJobExertions.class);
	
	// two level job composition with PULL and PAR execution
	private static Job createJob(Flow flow, Access access) throws Exception {
		
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
//						outVal("result/y")), strategy(Monitor.YES));
						outVal("result/y")));
		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
//						outVal("result/y")), strategy(Monitor.YES));
						outVal("result/y")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
//						outVal("result/y")), strategy(Monitor.YES));
						outVal("result/y")));


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
		//logger.info("job j1 eval @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		
	}

	@Test
	public void exertJobPushSeqTest() throws Exception {
		ServiceMogram.debug = true;

		Job job = createJob(Flow.SEQ, Access.PUSH);
//		logger.info("job j1: " + job);
		job = exert(job);
		logger.info("job j1: " + job.describe());
//		logger.info("job j1 job context: " + context(job, "j1/t3));
//		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 eval @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		
	}
	
	@Test
	public void exertJobPullParTest() throws Exception {

		Job job = createJob(Flow.PAR, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 eval @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);

	}
	
	@Test
	public void exertJobPullSeqTest() throws Exception {
		
		Job job = createJob(Flow.SEQ, Access.PULL);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + upcontext(job));
		//logger.info("job j1 eval @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
		
	}
	
	@Test
	public void localJobber() throws Exception {
		
		Task t3 = task(
				"t3",
				sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
						outVal("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y", null)));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y", null)));

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
				context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
						outVal("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y", null)));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1", // sig("exert", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Provider shell = (Provider) provider(sig(RemoteServiceShell.class));
		Context out = upcontext(exert(shell, job));
		logger.info("job context: " + out);
		assertEquals(value(out, "j1/t3/result/y"), 400.0);

	}

	@Test
	public void serviceShellRemote() throws Exception {
		
		Task t3 = task(
				"t3",
				sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
						outVal("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("exert", Jobber.class, Shell.REMOTE),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertEquals(value(context, "j1/t3/result/y"), 400.0);

	}
	
	public static Context createContext() throws Exception {
		return context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0));
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
				context("add", inVal("arg/x1", 200.0), inVal("arg/x2", 800.0)));

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
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0)));

		result = context(exert(cxtt));
//		logger.info("contexter context 4: " + result);
		assertEquals(value(result, "arg/x1"), 20.0);
		assertEquals(value(result, "arg/x2"), 80.0);
		
	}

	@Test
	public void arithmeticJobNetExerter() throws Exception {

		// get the current eval of the exertlet
		Exerter exerter = task("exert", sig("exert", Exerter.class, prvName("Arithmetic Exerter")));
		Context out = exert(exerter, context());
		logger.info("out: " + out);
		assertEquals(value(out, "j1/t3/result/y"), 400.0);

		// update inputs contexts
		Context multiplyContext = context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 70.0));
		Context addContext = context("add", inVal("arg/x1", 90.0), inVal("arg/x2", 110.0));
		Context invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		exerter = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
		out = exert(exerter, invokeContext);
		logger.info("j1/t3/result/y: " + value(out, "j1/t3/result/y"));
		assertEquals(value(out, "j1/t3/result/y"), 500.0);

		// update contexts partially
		multiplyContext = context("multiply", inVal("arg/x1", 20.0));
		addContext = context("add", inVal("arg/x1", 80.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		exerter = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
		out = exert(exerter, invokeContext);
		logger.info("j1/t3/result/y: " + value(out, "j1/t3/result/y"));
		assertEquals(value(out, "j1/t3/result/y"), 1210.0);

		// reverse the state to the initial one
		multiplyContext = context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0));
		addContext = context("add", inVal("arg/x1", 80.0), inVal("arg/x2", 20.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		exerter = task("invoke", sig("invoke", Invocation.class, prvName("Arithmetic Exerter")), invokeContext);
		out = exert(exerter, invokeContext);
		logger.info("j1/t3/result/y: " + value(out, "j1/t3/result/y"));
		assertEquals(value(out, "j1/t3/result/y"), 400.0);
	}

	@Test
	public void createModelWithTask() throws Exception {

		EntryModel vm = procModel(
				"Hello Arithmetic #2",
				// inputs
				val("x1"), val("x2"), proc("x3", 20.0), val("x4"),
				// outputs
				proc("t4", invoker("x1 * x2", args("x1", "x2"))),
				proc("t5",
					task("t5",
						sig("add", Adder.class),
						cxt("add", inVal("x3"), inVal("x4"),
						result("result/y")))),
				proc("j1", invoker("t4 - t5", args("t4", "t5"))));

		setValues(vm, val("x1", 10.0), val("x2", 50.0), val("x4", 80.0));
				 
		assertTrue(exec(vm, "j1").equals(400.0));
	}

}
