package sorcer.arithmetic.job;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.entry;
import static sorcer.co.operator.from;
import static sorcer.co.operator.inEntry;
import static sorcer.co.operator.input;
import static sorcer.co.operator.outEntry;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.srv;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.type;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoker;
import static sorcer.po.operator.par;
import static sorcer.po.operator.parModel;
import static sorcer.po.operator.pars;
import static sorcer.po.operator.put;

import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Task;
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
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-dl.jar",
				"sorcer-dl.jar" });
		System.out.println("CLASSPATH :"
				+ System.getProperty("java.class.path"));
	}
	
	@Test
	public void exertAdderProviderTest() throws Exception {
		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inEntry("arg, x1", 20.0),
						inEntry("arg, x2", 80.0), result("result/y")));
		
		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		assertEquals(value(context(t5), "result/y"), 100.0);
	}
	
	@Test
	public void execAdderProviderTest() throws Exception {
		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inEntry("arg, x1", 20.0),
						inEntry("arg, x2", 80.0), result("result/y")));
		
		assertEquals(value(t5), 100.0);
	}
	
	@Test
	public void averagerProviderTest() throws Exception {
		Task t5 = task(
				"t5",
				sig("average", AveragerImpl.class),
				context("average", inEntry("arg, x1", 20.0),
						inEntry("arg, x2", 80.0), result("result/y")));
		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		assertEquals(value(context(t5), "result/y"), 50.0);
	}
	
	@Test
	public void testTaskConcatenation() throws Exception {
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEntry("arg/x1"), inEntry("arg/x2"),
						outEntry("result/y")));

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						outEntry("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		Job job = job(sig("service", ServiceJobber.class),
				"j1", t4, t5, t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
		
		Context context = context(exert(job));
		logger.info("job context: " + context);
		assertEquals(get(context, "j1/t3/result/y"), 400.0);
	}

	@Test
	public void testJobHierachicalComposition() throws Exception {
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEntry("arg/x1", null), inEntry("arg/x2", null),
						outEntry("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						outEntry("result/y", null)));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		Context context = context(exert(job));
		logger.info("job context: " + context);
		assertEquals(get(context, "j1/t3/result/y"), 400.0);
	}
	
	@Test
	public void arithmeticPars() throws Exception {
		Par y = par("y",
				invoker("(x1 * x2) - (x3 + x4)", pars("x1", "x2", "x3", "x4")));
		Object val = value(y, entry("x1", 10.0), entry("x2", 50.0),
				entry("x3", 20.0), entry("x4", 80.0));
		// logger.info("y value: " + val);
		assertEquals(val, 400.0);

	}

	@Test
	public void createParModel() throws Exception {

		ParModel vm = parModel(
				"Hello Arithmetic Model #1",
				// inputs
				par("x1"), par("x2"), par("x3", 20.0),
				par("x4", 80.0),
				// outputs
				par("t4", invoker("x1 * x2", pars("x1", "x2"))),
				par("t5", invoker("x3 + x4", pars("x3", "x4"))),
				par("j1", invoker("t4 - t5", pars("t4", "t5"))));

//		logger.info("t4 value: " + value(par(vm, "t4")));
		assertEquals(value(par(vm, "t4")), null);

		logger.info("t5 value: " + value(par(vm, "t5")));
		assertEquals(value(par(vm, "t5")), 100.0);

		// logger.info("j1 value: " + value(par(vm, "j1")));
		assertEquals(value(par(vm, "j1")), null);

		// logger.info("j1 value: " + value(var(put(vm, entry("x1", 10.0),
		// entry("x2", 50.0)), "j1")));
		assertEquals(
				value(par(put(vm, entry("x1", 10.0), entry("x2", 50.0)), "j1")),
				400.0);
		// logger.info("j1 value: " + value(par(vm, "j1")));
		assertEquals(value(par(vm, "j1")), 400.0);

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
						sig("add", AdderImpl.class),
						cxt("add", inEntry("arg/x3"),
								inEntry("arg/x4"),
						result("result/y")))),
				par("j1", invoker("t4 - t5", pars("t4", "t5"))));

		vm = put(vm, entry("x1", 10.0), entry("x2", 50.0),
				entry("x4", 80.0));
				 
		assertEquals(value(par(vm, "j1")), 400.0);
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
					context("add", inEntry("arg/x1"), inEntry("arg/x2"),
						result("result/y")));
		
		Context result = context(exert(t5));
//		logger.info("task context: " + result);
		assertEquals(get(result, "result/y"), 100.0);
	}
	
	@Test
	public void serviceJob() throws Exception {
		Task t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inEntry("arg/x1"), inEntry("arg/x2"), outEntry("result/y")));

		Task t4 = srv("t4",
				sig("multiply", MultiplierImpl.class),
				// cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
				cxt("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						outEntry("result/y")));

		Task t5 = srv(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = srv(
				"j1",
				sig("execute", ServiceJobber.class),
				cxt(inEntry("arg/x1", 10.0),
						result("job/result", from("j1/t3/result/y"))),
				srv("j2", sig("execute", ServiceJobber.class), t4, t5), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		Context context = context(exert(job));
		logger.info("job context: " + context);
		get(context, "j1/t3/result/y");
		assertEquals(get(context, "j1/t3/arg/x1"), 500.0);
		assertEquals(get(context, "j1/t3/arg/x2"), 100.0);
		assertEquals(get(context, "j1/t3/result/y"), 400.0);
	}
	
}
