package junit.sorcer.core.provider;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.from;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.input;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.jobContext;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.srv;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.type;
import static sorcer.eo.operator.value;

import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.SorcerConstants;
import sorcer.core.context.PositionalContext;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.provider.Provider;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Strategy.Wait;
import sorcer.service.Task;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ArithmeticNoNetTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNoNetTest.class.getName());

	static {
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new RMISecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-arithmetic-beans.jar",
				"sorcer-dl.jar" });
		System.out.println("CLASSPATH :"
				+ System.getProperty("java.class.path"));
	}
	
	@Test
	public void testTaskConcatenation() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Exertion job = new ObjectJob("3tasks");
		job.addExertion(task1);
		job.addExertion(task2);
		job.addExertion(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/value", "arg1/value", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/value", "arg2/value", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job)job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job)job).getJobValue("3tasks/subtract/result/value"), 400.0);
	}

	@Test
	public void testJobHierachicalComposition() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Job internal = new ObjectJob("2tasks");
		internal.addExertion(task2);
		internal.addExertion(task1);
		
		Exertion job = new ObjectJob("1job1task");
		job.addExertion(internal);
		job.addExertion(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/value", "arg1/value", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/value", "arg2/value", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job)job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job)job).getJobValue("1job1task/subtract/result/value"), 400.0);
	}
	
	@Test
	public void testJobStrategy() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();
		
		Job internal = new ObjectJob("2tasks");
		internal.addExertion(task2);
		internal.addExertion(task1);
		internal.getControlContext().setFlowType(Flow.PAR);
		internal.getControlContext().setAccessType(Access.PUSH);

		Exertion job = new ObjectJob("1job1task");
		job.addExertion(internal);
		job.addExertion(task3);
		internal.getControlContext().setFlowType(Flow.SEQ);
		internal.getControlContext().setAccessType(Access.PUSH);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/value", "arg1/value", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/value", "arg2/value", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job)job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job)job).getJobValue("1job1task/subtract/result/value"), 400.0);
	}
	
	private Task getAddTask() throws Exception {
		Context context = new PositionalContext("add");
		context.putInValue("arg1/value", 20.0);
		context.putInValue("arg2/value", 80.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0.0);
		Signature method = new ObjectSignature("add", AdderImpl.class);
		Task task = new ObjectTask("add", method);
		task.setContext(context);
		return task;
	}

	private Task getMultiplyTask() throws Exception {
		Context context = new PositionalContext("multiply");
		context.putInValue("arg1/value", 10.0);
		context.putInValue("arg2/value", 50.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0.0);
		Signature method = new ObjectSignature("multiply", MultiplierImpl.class);
		Task task = new ObjectTask("multiply", method);
		task.setContext(context);
		return task;
	}

	private Task getSubtractTask() throws Exception {
		PositionalContext context = new PositionalContext("subtract");
		// We want to stick in the result of multiply in here
		context.putInValueAt("arg1/value", 0.0, 1);
		// We want to stick in the result of add in here
		context.putInValueAt("arg2/value", 0.0, 2);
		Signature method = new ObjectSignature("subtract", SubtractorImpl.class);
		Task task = new ObjectTask("subtract",
				"processing results from two previouseky executed tasks", method);
		task.setContext(context);
		return task;
	}
	
	@Test
	public void contexterTest() throws Exception {
		// get a context for the template context in the task
		Task cxtt = task("addContext", sig("getContext", ArithmeticNetTest.createContext()),
				context("add", input("arg/x1"), input("arg/x2")));

		Context result = context(exert(cxtt));
//		logger.info("contexter context: " + result);
		assertEquals(get(result, "arg/x1"), 20.0);
		assertEquals(get(result, "arg/x2"), 80.0);
	}
	
	@Test
	public void objectContexterTaskTest() throws Exception {
		Task t5 = task("t5", sig("add", AdderImpl.class), 
					type(sig("getContext", ArithmeticNetTest.createContext()), Signature.APD),
					context("add", in("arg/x1"), in("arg/x2"),
						result("result/y")));
		
		Context result = context(exert(t5));
//		logger.info("task context: " + result);
		assertEquals(get(result, "result/y"), 100.0);
	}
	
	@Test
	public void exertSrvTest() throws Exception {
		Job srv = createSrv();
		logger.info("srv job context: " + jobContext(srv));
		logger.info("srv j1/t3 context: " + context(srv, "j1/t3"));
		logger.info("srv j1/j2/t4 context: " + context(srv, "j1/j2/t4"));
		logger.info("srv j1/j2/t5 context: " + context(srv, "j1/j2/t5"));

		srv = exert(srv);
		logger.info("srv job context: " + jobContext(srv));

		// logger.info("srv value @  t3/arg/x2 = " + get(srv, "j1/t3/arg/x2"));
		assertEquals(get(srv, "j1/t3/arg/x2"), 100.0);
	}

	// two level job composition
	private Job createSrv() throws Exception {
		Task t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", in("arg/x1"), in("arg/x2"), out("result/y")));

		Task t4 = srv("t4",
				sig("multiply", MultiplierImpl.class),
				// cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
				cxt("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						out("result/y")));

		Task t5 = srv(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						out("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		// Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL,
		// Access.PULL)), t3,
		Job job = srv(
				"j1",
				sig("execute", ServiceJobber.class),
				cxt(in("arg/x1", 10.0),
						result("job/result", from("j1/t3/result/y"))),
				srv("j2", sig("execute", ServiceJobber.class), t4, t5), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		return job;
	}

	@Ignore
	@Test
	public void arithmeticPushTaskTest() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", in("arg, x1", 20.0), in("arg, x2", 80.0),
						result("result, y")));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 100.0", value(t5), 100.0);
	}

	@Ignore
	@Test
	public void arithmeticMultiServiceTest() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Arithmetic.class),
				context("add", in("arg, x1", 20.0), in("arg, x2", 80.0),
						result("result, y")));

		t5 = exert(t5);
		// logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 100.0", get(t5), 100.0);
	}

	@Ignore
	@Test
	public void accessArithmeticProviderTest() throws Exception {
		Provider provider = ProviderAccessor
				.getProvider(sig("add", Adder.class));
		logger.info("provider: " + provider.getProviderName());

		provider = ProviderAccessor.getProvider(sig("add", Arithmetic.class));
		logger.info("provider: " +  provider.getProviderName());
	}

	@Ignore
	@Test
	public void lookupArithmeticProviderTest() throws Exception {
		Provider provider = ProviderLookup
				.getProvider(sig("add", Adder.class));
		logger.info("provider: " + provider.getProviderName());

		provider = ProviderLookup.getProvider(sig("add", Arithmetic.class));
		logger.info("provider: " +  provider.getProviderName());
	}
	
	@Ignore
	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						out("result/y", null)), strategy(Access.PULL, Wait.YES));

		logger.info("t5 init context: " + context(t5));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals("Wrong value for 100.0", get(t5, "result/y"), 100.0);
	}

	@Ignore
	@Test
	public void arithmeticSpaceMultiserviceTaskTest() throws Exception {
		Task t5 = task(
				"t5",
				sig("add", Arithmetic.class),
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						out("result/y", null)), strategy(Access.PULL, Wait.YES));

		logger.info("t5 init context: " + context(t5));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals("Wrong value for 100.0", get(t5, "result/y"), 100.0);
	}

	@Ignore
	@Test
	public void exertJobPullParTest() throws Exception {
		Job job = createJob(Flow.PAR);
		job = exert(job);
		// logger.info("job j1: " + job);
		// logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + jobContext(job));
		// logger.info("job j1 value @ j1/t3/result/y = " + get(job,
		// "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}

	@Ignore
	@Test
	public void exertJobPullSeqTest() throws Exception {
		Job job = createJob(Flow.SEQ);
		job = exert(job);
		// logger.info("job j1: " + job);
		// logger.info("job j1 job context: " + context(job));
		logger.info("job j1 job context: " + jobContext(job));
		// logger.info("job j1 value @ j1/t3/result/y = " + get(job,
		// "j1/t3/result/y"));
		assertEquals(get(job, "j1/t3/result/y"), 400.00);
	}

	// two level job composition with PULL and PAR execution
	private Job createJob(Flow flow) throws Exception {
		Task t3 = task(
				"t3",
				sig("subtract", Subtractor.class),
				context("subtract", in("arg/x1", null), in("arg/x2", null),
						out("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", Multiplier.class),
				context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						out("result/y", null)));

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						out("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		// Job job = job("j1",
		Job job = job(
				"j1", // sig("service", RemoteJobber.class),
				// job("j2", t4, t5),
				job("j2", t4, t5, strategy(flow, Access.PULL)), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		return job;
	}

}
