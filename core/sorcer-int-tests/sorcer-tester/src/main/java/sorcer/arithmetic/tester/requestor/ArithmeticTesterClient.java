package sorcer.arithmetic.tester.requestor;

import net.jini.config.EmptyConfiguration;
import org.slf4j.Logger;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.RemoteAdder;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.core.SorcerConstants;
import sorcer.service.Exertion;
import sorcer.service.*;
import sorcer.service.Strategy.*;
import sorcer.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.so.operator.*;

/**
 * Testing parameter passing between tasks within the same service job. Two
 * numbers are added by the first task, then two numbers are multiplied by the
 * second one. The results of the first task and the second task are passed on
 * to the third task that subtracts the result of task two from the result of
 * task one. The {@link sorcer.core.context.PositionalContext} is used for consumer's
 * data in this test.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticTesterClient implements SorcerConstants {

	private static Logger logger = Log.getTestLog();

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		logger.info("running: " + args[0]);
        Accessor.create(EmptyConfiguration.INSTANCE);
		Mogram result = null;
		ArithmeticTesterClient tester = new ArithmeticTesterClient();
		if (args[0].equals("f5"))
			result = tester.f5();
		if (args[0].equals("f5m"))
			result = tester.f5m();
		else if (args[0].equals("f5inh"))
			result = tester.f5inh();
		else if (args[0].equals("f5a"))
			result = tester.f5a();
		else if (args[0].equals("f5pull"))
			result = tester.f5pull();
		else if (args[0].equals("f1provisioned"))
			result = tester.f1provisioned();
		else if (args[0].equals("f1"))
			result = tester.f1();
		else if (args[0].equals("f1a"))
			result = tester.f1a();
		else if (args[0].equals("f1b"))
			result = tester.f1b();
		else if (args[0].equals("f1c"))
			result = tester.f1c();
		else if (args[0].equals("f1PARpull"))
			result = tester.f1PARpull();
		else if (args[0].equals("f1SEQpull"))
			result = tester.f1SEQpull();
		else if (args[0].equals("f5xS_PULL"))
			result = tester.f5xS_PULL(args[1]);
		else if (args[0].equals("f5xS_PUSH"))
			result = tester.f5xS_PUSH(args[1]);
		else if (args[0].equals("f5xP"))
			result = tester.f5xP(args[1], args[2]);
		else if (args[0].equals("f5exerter"))
			result = tester.f5exerter();

//		logger.info(">>>>>>>>>>>>> exceptions: " + exceptions(result));
//		logger.info(">>>>>>>>>>>>> result context: " + context(result));
	}

	// two level composition
	private Routine f1a() throws Exception {
		String arg = "arg", result = "result";
		String x1 = "x1", x2 = "x2", y = "y";

		Task f3 = task("f3", sig("multiply", Multiplier.class),
				   context("multiply", inVal(attPath(arg, x1), 10.0), inVal(attPath(arg, x2), 50.0),
				      outVal(attPath(result, y), null)));

		Task f4 = task("f4", sig("add", Adder.class),
		   context("add", inVal(attPath(arg, x1), 20.0), inVal(attPath(arg, x2), 80.0),
		      outVal(attPath(result, y), null)));

		Task f5 = task("f5", sig("subtract", Subtractor.class),
				   context("subtract", inVal(attPath(arg, x1), null), inVal(attPath(arg, x2), null),
				      outVal(attPath(result, y), null)));

		// Service Composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PARALLEL, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f3, f4), f5, strategy(Provision.NO),
		   pipe(outPoint(f3, attPath(result, y)), inPoint(f5, attPath(arg, x1))),
		   pipe(outPoint(f4, attPath(result, y)), inPoint(f5, attPath(arg, x2))));

		Job out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + upcontext(out));
			logger.info("job f1/f5/result/y: " + get(out, "f1/f5/result/y"));
		} else {
			logger.info("job execution failed");
		}

		return out;
	}

	// two level composition
	private Routine f1() throws Exception {

		Task f4 = task("f4", sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0d), inVal("arg/x2", 50.0d),
						outVal("result/y1", null)));

		Task f5 = task("f5", sig("add", Adder.class),
				context("add", inVal("arg/x3", 20.0d), inVal("arg/x4", 80.0d),
						inVal("result/y2", null)));

		Task f3 = task("f3", sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x5", null), inVal("arg/x6", null),
						outVal("result/y3", null)));

		//job("f1", job("f2", f4, f5), f3,
		//job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		Routine out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + upcontext(out));
			logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
		} else {
			logger.info("job execution failed");
		}

		return out;
	}

	private Routine f1provisioned() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply", Multiplier.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/multiplier-prv.config"))),
				context("multiply", inVal("arg/x1", 10.0d),
						inVal("arg/x2", 50.0d), outVal("result/y1", null)));

		Task f5 = task(
				"f5",
				sig("add", Adder.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/adder-prv.config"))),
				context("add", inVal("arg/x3", 20.0d), inVal("arg/x4", 80.0d),
						outVal("result/y2", null)));

		Task f3 = task(
				"f3",
				sig("subtract", Subtractor.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/subtractor-prv.config"))),
				context("subtract", inVal("arg/x5", null),
						inVal("arg/x6", null), outVal("result/y3", null)));

		// job("f1", job("f2", f4, f5), f3,
		// job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		Routine out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + upcontext(out));
			logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
		} else {
			logger.info("job execution failed");
		}

		return out;
	}

	// one level composition
	private Routine f1b() throws Exception {
		String arg = "arg", result = "result";
		String x1 = "x1", x2 = "x2", y = "y";

		Task f3 = task("f3", sig("subtract", Subtractor.class),
		   context("subtract", inVal(attPath(arg, x1), null), inVal(attPath(arg, x2), null),
		      outVal(attPath(result, y), null)));

		Task f4 = task("f4", sig("multiply", Multiplier.class),
				   context("multiply", inVal(attPath(arg, x1), 10.0), inVal(attPath(arg, x2), 50.0),
				      outVal(attPath(result, y), null)));

		Task f5 = task("f5", sig("add", Adder.class),
		   context("add", inVal(attPath(arg, x1), 20.0), inVal(attPath(arg, x2), 80.0),
		      outVal(attPath(result, y), null)));

		// Service Composition f1(f4(x1, x2), f5(x1, x2), f3(x1, x2))
		Job f1= job("f1", f4, f5, f3,
		   pipe(outPoint(f4, attPath(result, y)), inPoint(f3, attPath(arg, x1))),
		   pipe(outPoint(f5, attPath(result, y)), inPoint(f3, attPath(arg, x2))));

		Routine out = exert(f1);
		logger.info("job f1 context: " + upcontext(out));
		logger.info("job f1 f1/f3/result/y: " + get(out, attPath("f1", "f3", result, y)));

		return out;
	}

	// job composed of job and task with PUSH/SEQ strategy
	private Routine f1c() throws Exception {

		Task f4 = task("f4", sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y1")));

		Task f5 = task("f5", sig("add", Adder.class),
				context("add", inVal("arg/x3", 20.0), inVal("arg/x4", 80.0),
						outVal("result/y2")));

		Task f3 = task("f3", sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x5", null), inVal("arg/x6"),
						outVal("result/y3")));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5), f3,
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		Routine out = exert(f1);

		logger.info("job f1 context: " + upcontext(out));
		logger.info("job f1 f3/result/y3: " + get(out, "f1/f3/result/y3"));

		return out;
	}

	// composition with mixed flow/access strategy
	private Routine f1PARpull() throws Exception {

		Task f4 = task("f4", sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y1")), Access.PULL);

		Task f5 = task("f5", sig("add", Adder.class),
				context("add", inVal("arg/x3", 20.0), inVal("arg/x4", 80.0),
						outVal("result/y2")), Access.PULL);

		Task f3 = task("f3", sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x5"), inVal("arg/x6"),
						outVal("result/y3")));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5, strategy(Access.PULL, Flow.PAR)), f3,
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		long start = System.currentTimeMillis();
		Routine out = exert(f1);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");

		logger.info("out key: " + name(out));
		logger.info("job f1 context: " + context(out));
		logger.info("job f1 job context: " + upcontext(out));
		logger.info("job f1 f3/result/y3: " + get(out, "f1/f3/result/y3"));
		logger.info("task f4 trace: " + trace(xrt(out, "f1/f2/f4")));
		logger.info("task f5 trace: " + trace(xrt(out, "f1/f2/f5")));
		logger.info("task f3 trace: " +  trace(xrt(out, "f1/f3")));
		logger.info("task f2 trace: " +  trace(xrt(out, "f1/f2")));
		logger.info("task f1 trace: " +  trace(out));
		return out;
	}

private Routine f1SEQpull() throws Exception {

		Task f4 = task("f4", sig("multiply", Multiplier.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y1")), Access.PULL);

		Task f5 = task("f5", sig("add", Adder.class),
				context("add", inVal("arg/x3", 20.0), inVal("arg/x4", 80.0),
						outVal("result/y2")), Access.PULL);

		Task f3 = task("f3", sig("subtract", Subtractor.class),
				context("subtract", inVal("arg/x5"), inVal("arg/x6"),
						outVal("result/y3")));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5, strategy(Access.PULL, Flow.SEQ)), f3,
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		long start = System.currentTimeMillis();
		Routine out = exert(f1);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");

		logger.info("out key: " + name(out));
		logger.info("job f1 context: " + context(out));
		logger.info("job f1 job context: " + upcontext(out));
		logger.info("job f1 f3/result/y3: " + get(out, "f1/f3/result/y3"));
		logger.info("task f4 trace: " + trace(xrt(out, "f1/f2/f4")));
		logger.info("task f5 trace: " + trace(xrt(out, "f1/f2/f5")));
		logger.info("task f3 trace: " +  trace(xrt(out, "f1/f3")));
		logger.info("task f2 trace: " +  trace(xrt(out, "f1/f2")));
		logger.info("task f1 trace: " +  trace(out));
		return out;
	}

	private Routine f5() throws Exception {
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y", null)),
				strategy(Monitor.NO, Wait.YES, Provision.YES));

		Routine out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Mogram f5exerter() throws Exception {
		Mogram f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y", null)),
				strategy(Monitor.NO, Wait.YES));

		Mogram out = null;
		long start = System.currentTimeMillis();
		Exertion exerter = Accessor.get().getService(null, Exertion.class);
		logger.info("got exerter: " + exerter);

		out = exerter.exert(f5, null);
		long end = System.currentTimeMillis();

		if (out.getExceptions().size() > 0) {
			System.out.println("Execeptions: " + out.getExceptions());
			return out;
		}

		System.out.println("Execution time by exerter: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Routine f5m() throws Exception {
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y")),
				strategy(Monitor.YES, Wait.YES));

		Routine out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}


	private Routine f5inh() throws Exception {

		Task f5 = task(
				"f5",
				sig("add", RemoteAdder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y")),
				strategy(Monitor.YES, Wait.YES));

		Routine out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Routine f5pull() throws Exception {

		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y")),
				strategy(Access.PULL, Wait.YES));

		Routine out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 control: " + control(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Routine f5a() throws Exception {

		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), outVal("result/y")),
				strategy(Monitor.YES, Wait.NO));

		logger.info("task f5 control context: " + control(f5));

		Routine out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Routine f5xS_PULL(String repeat) throws Exception {
		int to = new Integer(repeat);

		Task f5 = task("f5", sig("add", Adder.class),
		   context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
		      outVal("result/y", null),
		      strategy(Access.PULL, Wait.YES)));

		f5.setAccess(Access.PULL);

		Routine out = null;
		long start = System.currentTimeMillis();
		for (int i = 0; i < to; i++) {
			f5.setName("f5-" + i);
			f5.getContext().setName("f5-" + i);
			out = exert(f5);
			System.out.println("out context: " + name(f5) + "\n" + context(out));
		}
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		return out;
	}

	private Routine f5xS_PUSH(String repeat) throws Exception {
		int to = new Integer(repeat);

		Task f5 = task("f5", sig("add", Adder.class),
		   context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
		      outVal("result/y", null),
		      strategy(Access.PUSH, Wait.YES)));

		f5.setAccess(Access.PUSH);

		Routine out = null;
		long start = System.currentTimeMillis();
		for (int i = 0; i < to; i++) {
			f5.setName("f5-" + i);
			f5.getContext().setName("f5-" + i);
			out = exert(f5);
			System.out.println("out context: " + name(f5) + "\n" + context(out));
		}
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		return out;
	}

	private Task getTask() throws RoutineException, SignatureException,
			ContextException {

		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
					inVal("arg/x2", 80.0), outVal("result/y")));
		return f5;
	}

	private Routine f5xP(String poolSizeStr, String size) throws Exception {
		int poolSize = new Integer(size);
		int tally = new Integer(size);
		Task task = null;
		ExertCallable ec = null;
		long start = System.currentTimeMillis();
		List<Future<Routine>> fList = new ArrayList<Future<Routine>>(tally);
		ExecutorService pool = Executors.newFixedThreadPool(poolSize);
		for (int i = 0; i < tally; i++) {
			task = getTask();
			task.getControlContext().setAccessType(Access.PULL);
			task.setName("f5-" + i);
			ec = new ExertCallable(task);
			logger.info("exertion submit: " + task.getName());
			Future<Routine> future = pool.submit(ec);
			fList.add(future);
		}
		pool.shutdown();
		for (int i = 0; i < tally; i++) {
			logger.info("got future eval for: " + fList.get(i).get().getName());
		}
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end - start) + " ms.");
		logger.info("got last context #" + tally + "\n" + fList.get(tally-1).get().getContext());
		logger.info("run in parallel: " + tally);
		return fList.get(tally-1).get();
	}

}
