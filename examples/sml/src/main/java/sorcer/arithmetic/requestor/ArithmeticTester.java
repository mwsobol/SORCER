package sorcer.arithmetic.requestor;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.RemoteAdder;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.core.provider.Exerter;
import sorcer.core.requestor.SorcerRequestor;
import sorcer.service.*;
import sorcer.service.Strategy.*;
import sorcer.util.Log;
import sorcer.util.ProviderAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

/**
 * Testing parameter passing between tasks within the same service job. Two
 * numbers are added by the first task, then two numbers are multiplied by the
 * second one. The results of the first task and the second task are passed on
 * to the third task that subtracts the result of task two from the result of
 * task one. The {@link sorcer.core.context.PositionalContext} is used for requestor's
 * data in this test.
 * 
 * @see ArithmeticTester
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ArithmeticTester extends SorcerRequestor {

	private static Logger logger = Log.getTestLog();

	@Override
	public void run(String... args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		logger.info("running: " + args[0]);
		Exertion result = null;
		ArithmeticTester tester = new ArithmeticTester();
		if (args[1].equals("f5"))
			result = tester.f5();
		if (args[1].equals("f5m"))
			result = tester.f5m();
		else if (args[1].equals("f5inh"))
			result = tester.f5inh();
		else if (args[1].equals("f5a"))
			result = tester.f5a();
		else if (args[1].equals("f5pull"))
			result = tester.f5pull();
		else if (args[1].equals("f1provisioned"))
			result = tester.f1provisioned();
		else if (args[1].equals("f1"))
			result = tester.f1();
		else if (args[1].equals("f1a"))
			result = tester.f1a();
		else if (args[1].equals("f1b"))
			result = tester.f1b();
		else if (args[1].equals("f1c"))
			result = tester.f1c();
		else if (args[1].equals("f1PARpull"))
			result = tester.f1PARpull();
		else if (args[1].equals("f1SEQpull"))
			result = tester.f1SEQpull();
		else if (args[1].equals("f5xS_PULL"))
			result = tester.f5xS_PULL(args[1]);
		else if (args[1].equals("f5xS_PUSH"))
			result = tester.f5xS_PUSH(args[1]);
		else if (args[1].equals("f5xP"))
			result = tester.f5xP(args[1], args[2]);
		else if (args[1].equals("f5exerter"))
			result = tester.f5exerter();
		
//		logger.info(">>>>>>>>>>>>> exceptions: " + exceptions(result));
//		logger.info(">>>>>>>>>>>>> result context: " + context(result));
	}

	// two level composition
	private Exertion f1a() throws Exception {
		String arg = "arg", result = "result";
		String x1 = "x1", x2 = "x2", y = "y";

		Task f3 = task("f3", sig("multiply", Multiplier.class), 
				   context("multiply", inEnt(path(arg, x1), 10.0), inEnt(path(arg, x2), 50.0),
				      outEnt(path(result, y), null)));
		
		Task f4 = task("f4", sig("add", Adder.class), 
		   context("add", inEnt(path(arg, x1), 20.0), inEnt(path(arg, x2), 80.0),
		      outEnt(path(result, y), null)));
		
		Task f5 = task("f5", sig("subtract", Subtractor.class), 
				   context("subtract", inEnt(path(arg, x1), null), inEnt(path(arg, x2), null),
				      outEnt(path(result, y), null)));

		// Service Composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PARALLEL, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f3, f4), f5, strategy(Provision.NO),
		   pipe(outPoint(f3, path(result, y)), inPoint(f5, path(arg, x1))),
        pipe(outPoint(f4, path(result, y)), inPoint(f5, path(arg, x2))));

		Job out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + serviceContext(out));
			logger.info("job f1/f5/result/y: " + get(out, "f1/f5/result/y"));
		} else {
			logger.info("job execution failed");
		}
		
		return out;
	}
	
	// two level composition
	private Exertion f1() throws Exception {
		
		Task f4 = task("f4", sig("multiply", Multiplier.class),
				context("multiply", inEnt("arg/x1", 10.0d), inEnt("arg/x2", 50.0d),
						outEnt("result/y1", null)));

		Task f5 = task("f5", sig("add", Adder.class),
				context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
						inEnt("result/y2", null)));

		Task f3 = task("f3", sig("subtract", Subtractor.class),
				context("subtract", inEnt("arg/x5", null), inEnt("arg/x6", null),
						outEnt("result/y3", null)));

		//job("f1", job("f2", f4, f5), f3,		
		//job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1 = job("f1", job("f2", f4, f5), f3, strategy(Provision.NO),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		Exertion out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + serviceContext(out));
			logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
		} else {
			logger.info("job execution failed");
		}
		
		return out;
	}
	
	private Exertion f1provisioned() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply", Multiplier.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/multiplier-prv.config"))),
				context("multiply", inEnt("arg/x1", 10.0d),
						inEnt("arg/x2", 50.0d), outEnt("result/y1", null)));

		Task f5 = task(
				"f5",
				sig("add", Adder.class,
					deploy(classpath("arithmetic-beans.jar"),
						codebase("arithmetic-dl.jar"),
						configuration("bin/examples/ex6/configs/adder-prv.config"))),
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
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
		
		Exertion out = exert(f1);
		if (out != null) {
			logger.info("job f1 context: " + serviceContext(out));
			logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
		} else {
			logger.info("job execution failed");
		}
		
		return out;
	}
	
	// one level composition
	private Exertion f1b() throws Exception {
		String arg = "arg", result = "result";
		String x1 = "x1", x2 = "x2", y = "y";

		Task f3 = task("f3", sig("subtract", Subtractor.class), 
		   context("subtract", inEnt(path(arg, x1), null), inEnt(path(arg, x2), null),
		      outEnt(path(result, y), null)));
		
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				   context("multiply", inEnt(path(arg, x1), 10.0), inEnt(path(arg, x2), 50.0),
				      outEnt(path(result, y), null)));
		
		Task f5 = task("f5", sig("add", Adder.class), 
		   context("add", inEnt(path(arg, x1), 20.0), inEnt(path(arg, x2), 80.0),
		      outEnt(path(result, y), null)));

		// Service Composition f1(f4(x1, x2), f5(x1, x2), f3(x1, x2))
		Job f1= job("f1", f4, f5, f3,
		   pipe(outPoint(f4, path(result, y)), inPoint(f3, path(arg, x1))),
		   pipe(outPoint(f5, path(result, y)), inPoint(f3, path(arg, x2))));

		Exertion out = exert(f1);
		logger.info("job f1 context: " + serviceContext(out));
		logger.info("job f1 f1/f3/result/y: " + get(out, path("f1", "f3", result, y)));
		
		return out;
	}
	
	// job composed of job and task with PUSH/SEQ strategy
	private Exertion f1c() throws Exception {
		
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				context("multiply", inEnt(path("arg/x1"), 10.0), inEnt(path("arg/x2"), 50.0),
						outEnt(path("result/y1"), null)));

		Task f5 = task("f5", sig("add", Adder.class), 
				context("add", inEnt(path("arg/x3"), 20.0), inEnt(path("arg/x4"), 80.0),
						outEnt(path("result/y2"), null)));

		Task f3 = task("f3", sig("subtract", Subtractor.class), 
				context("subtract", inEnt(path("arg/x5"), null), inEnt(path("arg/x6"), null),
						outEnt(path("result/y3"), null)));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5), f3,
				pipe(outPoint(f4, path("result/y1")), inPoint(f3, path("arg/x5"))),
				pipe(outPoint(f5, path("result/y2")), inPoint(f3, path("arg/x6"))));

		Exertion out = exert(f1);

		logger.info("job f1 context: " + serviceContext(out));
		logger.info("job f1 f3/result/y3: " + get(out, path("f1/f3/result/y3")));

		return out;
	}
	
	// composition with mixed flow/access strategy
	private Exertion f1PARpull() throws Exception {
		
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				context("multiply", inEnt(path("arg/x1"), 10.0), inEnt(path("arg/x2"), 50.0),
						outEnt(path("result/y1"), null)), Access.PULL);

		Task f5 = task("f5", sig("add", Adder.class), 
				context("add", inEnt(path("arg/x3"), 20.0), inEnt(path("arg/x4"), 80.0),
						outEnt(path("result/y2"), null)), Access.PULL);

		Task f3 = task("f3", sig("subtract", Subtractor.class), 
				context("subtract", inEnt(path("arg/x5"), null), inEnt(path("arg/x6"), null),
						outEnt(path("result/y3"), null)));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5, strategy(Access.PULL, Flow.PAR)), f3,
				pipe(outPoint(f4, path("result/y1")), inPoint(f3, path("arg/x5"))),
				pipe(outPoint(f5, path("result/y2")), inPoint(f3, path("arg/x6"))));

		long start = System.currentTimeMillis();
		Exertion out = exert(f1);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		
		logger.info("out name: " + name(out));
		logger.info("job f1 context: " + context(out));
		logger.info("job f1 job context: " + serviceContext(out));
		logger.info("job f1 f3/result/y3: " + get(out, path("f1/f3/result/y3")));
		logger.info("task f4 trace: " + trace(exertion(out, "f1/f2/f4")));
		logger.info("task f5 trace: " + trace(exertion(out, "f1/f2/f5")));
		logger.info("task f3 trace: " +  trace(exertion(out, "f1/f3")));
		logger.info("task f2 trace: " +  trace(exertion(out, "f1/f2")));
		logger.info("task f1 trace: " +  trace(out));
		return out;
	}
	
private Exertion f1SEQpull() throws Exception {
		
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				context("multiply", inEnt(path("arg/x1"), 10.0), inEnt(path("arg/x2"), 50.0),
						outEnt(path("result/y1"), null)), Access.PULL);

		Task f5 = task("f5", sig("add", Adder.class), 
				context("add", inEnt(path("arg/x3"), 20.0), inEnt(path("arg/x4"), 80.0),
						outEnt(path("result/y2"), null)), Access.PULL);

		Task f3 = task("f3", sig("subtract", Subtractor.class), 
				context("subtract", inEnt(path("arg/x5"), null), inEnt(path("arg/x6"), null),
						outEnt(path("result/y3"), null)));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5, strategy(Access.PULL, Flow.SEQ)), f3,
				pipe(outPoint(f4, path("result/y1")), inPoint(f3, path("arg/x5"))),
				pipe(outPoint(f5, path("result/y2")), inPoint(f3, path("arg/x6"))));
		
		long start = System.currentTimeMillis();
		Exertion out = exert(f1);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		
		logger.info("out name: " + name(out));
		logger.info("job f1 context: " + context(out));
		logger.info("job f1 job context: " + serviceContext(out));
		logger.info("job f1 f3/result/y3: " + get(out, path("f1/f3/result/y3")));
		logger.info("task f4 trace: " + trace(exertion(out, "f1/f2/f4")));
		logger.info("task f5 trace: " + trace(exertion(out, "f1/f2/f5")));
		logger.info("task f3 trace: " +  trace(exertion(out, "f1/f3")));
		logger.info("task f2 trace: " +  trace(exertion(out, "f1/f2")));
		logger.info("task f1 trace: " +  trace(out));
		return out;
	}

	private Exertion f5() throws Exception {
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Monitor.NO, Wait.YES, Provision.YES));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Exertion f5exerter() throws Exception {
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Monitor.NO, Wait.YES));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		Exerter exerter = Accessor.getService(Exerter.class);
		logger.info("got exerter: " + exerter);

		out = exerter.exert(f5);
		long end = System.currentTimeMillis();
		
		if (out.getExceptions().size() > 0) {
			System.out.println("Execeptins: " + out.getExceptions());
			return out;
		}
			
		System.out.println("Execution time by exerter: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}
	
	private Exertion f5m() throws Exception {
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Monitor.YES, Wait.YES));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	
	private Exertion f5inh() throws Exception {
		
		Task f5 = task(
				"f5",
				sig("add", RemoteAdder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Monitor.YES, Wait.YES));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}

	private Exertion f5pull() throws Exception {

		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Access.PULL, Wait.YES));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 control: " + control(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}
	
	private Exertion f5a() throws Exception {

		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), outEnt("result/y", null)),
				strategy(Monitor.YES, Wait.NO));
		
		logger.info("task f5 control context: " + control(f5));
		
		Exertion out = null;
		long start = System.currentTimeMillis();
		out = exert(f5);
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end-start) + " ms.");
		logger.info("task f5 context: " + context(out));
		logger.info("task f5 result/y: " + get(context(out), "result/y"));

		return out;
	}
	
	private Exertion f5xS_PULL(String repeat) throws Exception {
		int to = new Integer(repeat);
		
		Task f5 = task("f5", sig("add", Adder.class), 
		   context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
		      outEnt("result/y", null),
		      strategy(Access.PULL, Wait.YES)));
		
		f5.setAccess(Access.PULL);
		
		Exertion out = null;
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
	
	private Exertion f5xS_PUSH(String repeat) throws Exception {
		int to = new Integer(repeat);
		
		Task f5 = task("f5", sig("add", Adder.class), 
		   context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
		      outEnt("result/y", null),
		      strategy(Access.PUSH, Wait.YES)));
		
		f5.setAccess(Access.PUSH);
		
		Exertion out = null;
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
	
	private Task getTask() throws ExertionException, SignatureException,
			ContextException {
		
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
					inEnt("arg/x2", 80.0), outEnt("result/y", null)));
		return f5;
	}
	
	private Exertion f5xP(String poolSizeStr, String size) throws Exception {
		int poolSize = new Integer(size);
		int tally = new Integer(size);
		Task task = null;
		ExertionCallable ec = null;
		long start = System.currentTimeMillis();
		List<Future<Exertion>> fList = new ArrayList<Future<Exertion>>(tally);
		ExecutorService pool = Executors.newFixedThreadPool(poolSize);
		for (int i = 0; i < tally; i++) {
			task = getTask();
			task.getControlContext().setAccessType(Access.PULL);
			task.setName("f5-" + i);
			ec = new ExertionCallable(task);
			logger.info("exertion submit: " + task.getName());
			Future<Exertion> future = pool.submit(ec);
			fList.add(future);
		}
		pool.shutdown();
		for (int i = 0; i < tally; i++) {
			logger.info("got future value for: " + fList.get(i).get().getName());
		}
		long end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end - start) + " ms.");
		logger.info("got last context #" + tally + "\n" + fList.get(tally-1).get().getContext());
		logger.info("run in parallel: " + tally);
		return fList.get(tally-1).get();
	}
	
}
