package sorcer.core.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.context.PositionalContext;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ArithmeticNoNetTest implements SorcerConstants {

	private static final Logger logger = LoggerFactory.getLogger(ArithmeticNoNetTest.class);

	@Test
	public void testTaskConcatenation() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Routine job = new ObjectJob("3tasks");
		job.addMogram(task1);
		job.addMogram(task2);
		job.addMogram(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/eval", "arg1/eval", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/eval", "arg2/eval", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job)job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job)job).getJobValue("3tasks/subtract/result/eval"), 400.0);
	}

	@Test
	public void testJobHierachicalComposition() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Job internal = new ObjectJob("2tasks");
		internal.addMogram(task2);
		internal.addMogram(task1);
		
		Routine job = new ObjectJob("1job1task");
		job.addMogram(internal);
		job.addMogram(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/eval", "arg1/eval", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/eval", "arg2/eval", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job) job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job) job).getJobValue("1job1task/subtract/result/eval"), 400.0);
	}
	
	@Test
	public void testJobStrategy() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();
		
		Job internal = new ObjectJob("2tasks");
		internal.addMogram(task2);
		internal.addMogram(task1);
		internal.getControlContext().setFlowType(Flow.PAR);
		internal.getControlContext().setAccessType(Access.PUSH);

		Routine job = new ObjectJob("1job1task");
		job.addMogram(internal);
		job.addMogram(task3);
		internal.getControlContext().setFlowType(Flow.SEQ);
		internal.getControlContext().setAccessType(Access.PUSH);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/eval", "arg1/eval", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/eval", "arg2/eval", task3.getContext());
		
		job = job.exert();

		logger.info("job context: " + ((Job)job).getJobContext());
		// result at the provider's default path"
		assertEquals(((Job)job).getJobValue("1job1task/subtract/result/eval"), 400.0);
	}
	
	private Task getAddTask() throws Exception {
		Context context = new PositionalContext("add");
		context.putInValue("arg1/eval", 20.0);
		context.putInValue("arg2/eval", 80.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/eval", 0.0);
		Signature method = new ObjectSignature("add", AdderImpl.class);
		Task task = new ObjectTask("add", method);
		task.setContext(context);
		return task;
	}

	private Task getMultiplyTask() throws Exception {
		Context context = new PositionalContext("multiply");
		context.putInValue("arg1/eval", 10.0);
		context.putInValue("arg2/eval", 50.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/eval", 0.0);
		Signature method = new ObjectSignature("multiply", MultiplierImpl.class);
		Task task = new ObjectTask("multiply", method);
		task.setContext(context);
		return task;
	}

	private Task getSubtractTask() throws Exception {
		PositionalContext context = new PositionalContext("subtract");
		// We want to stick in the result of multiply in here
		context.putInValueAt("arg1/eval", 0.0, 1);
		// We want to stick in the result of add in here
		context.putInValueAt("arg2/eval", 0.0, 2);
		Signature method = new ObjectSignature("subtract", SubtractorImpl.class);
		Task task = new ObjectTask("subtract",
				"processing results from two previouseky executed tasks", method);
		task.setContext(context);
		return task;
	}

	@Test
	public void objectTaskFidelity() throws Exception {

		Task t4 = task("t4",
				sigFi("object1", sig("multiply", MultiplierImpl.class)),
				sigFi("object2", sig("add", AdderImpl.class)),
				context("shared", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Context out = context(exert(t4, fi("object1")));
		logger.info("task context: " + context(t4));
		assertTrue(value(out, "result/y").equals(500.0));

		out = context(exert(t4, fi("object2")));
		logger.info("task context: " + context(t4));
		assertTrue(value(out, "result/y").equals(60.0));
	}

	@Test
	public void contexterTest() throws Exception {
		// getValue a context for the template context in the task
		Task cxtt = task("addContext", sig("getContext", ArithmeticNetTest.createContext()),
				context("add", input("arg/x1"), input("arg/x2")));

		Context result = context(exert(cxtt));
//		logger.info("contexter context: " + result);
		assertTrue(get(result, "arg/x1").equals(20.0));
		assertTrue(get(result, "arg/x2").equals(80.0));
	}
	
	@Test
	public void objectContexterTaskTest() throws Exception {
		Task t5 = task("t5", sig("add", AdderImpl.class), 
					type(sig("getContext", ArithmeticNetTest.createContext()), Signature.APD),
					context("add", inVal("arg/x1"), inVal("arg/x2"),
							result("result/y")));
		
		Context result = context(exert(t5));
//		logger.info("task context: " + result);
		assertTrue(get(result, "result/y").equals(100.0));
	}
	
	@Test
	public void exertSrvTest() throws Exception {
		Job srv = createSrv();
		logger.info("srv job context: " + upcontext(srv));
		logger.info("srv j1/t3 context: " + context(srv, "j1/t3"));
		logger.info("srv j1/j2/t4 context: " + context(srv, "j1/j2/t4"));
		logger.info("srv j1/j2/t5 context: " + context(srv, "j1/j2/t5"));

		srv = exert(srv);
		logger.info("srv job context: " + upcontext(srv));

		// logger.info("srv eval @  t3/arg/x2 = " + getValue(srv, "j1/t3/arg/x2"));
		assertEquals(get(srv, "j1/t3/arg/x2"), 100.0);
	}

	// two level job composition
	private Job createSrv() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/x1"), inVal("arg/x2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				// cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
				cxt("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		// Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL,
		// Access.PULL)), t3,
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		return job;
	}

}
