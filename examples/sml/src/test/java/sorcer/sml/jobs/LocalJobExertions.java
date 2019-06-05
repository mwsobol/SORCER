package sorcer.sml.jobs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.ArithmeticUtil;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.ent.operator.ent;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class LocalJobExertions implements SorcerConstants {

	private final static Logger logger = LoggerFactory.getLogger(LocalJobExertions.class);

	@Test
	public void jobPipelineValBased() throws Exception {

		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")));

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")));

		Job job = job(sig("exert", ServiceJobber.class),
				"j1", t4, t5, t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job upcontext: " + context);
		assertTrue(value(context, "j1/t3/result/y").equals(400.0));

	}

	@Test
	public void jobPipelineEntBased() throws Exception {

		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", in(val("arg/x1")), in(val("arg/x2")),
						out(val("result/y"))));

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", in(ent("arg/x1", 10.0)), in(ent("arg/x2", 50.0)),
						out(ent("result/y"))));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				context("add", in(ent("arg/x1", 20.0)), in(ent("arg/x2", 80.0)),
						out(ent("result/y"))));

		Job job = job(sig("exert", ServiceJobber.class),
				"j1", t4, t5, t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job upcontext: " + context);
		assertTrue(value(context, "j1/t3/result/y").equals(400.0));

	}

	@Test
	public void nestedJob() throws Exception {

		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y", null)));

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job upcontext: " + context);
		assertTrue(value(context, "j1/t3/result/y").equals(400.0));

	}

	@Test
	public void pathBindingContextScope() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/z1"), inVal("arg/z2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				cxt("multiply", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/y1"), inVal("arg/y2"),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(y1, y2)), t3(x1, x2))
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						inVal("arg/y1", 20.0), inVal("arg/y2", 80.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/z1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/z2")));

		Object result = exec(job);
		logger.info("job result: " + result);
		assertTrue(result.equals(400.0));

	}

	@Test
	public void outerContextPaths() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/z1"), inVal("arg/z2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				cxt("multiply", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/y1"), inVal("arg/y2"),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(y1, y2)), t3(x1, x2))
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						inVal("arg/y1", 20.0), inVal("arg/y2", 80.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/z1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/z2")));

		Context outer = upcontext(job);
		logger.info("job upcontext: " + outer);
		assertTrue(value(outer, "j1/j2/t4/arg/x1").equals(10.0));
		assertTrue(value(outer, "j1/j2/t4/arg/x2").equals(50.0));

		assertTrue(value(outer, "j1/j2/t5/arg/y1").equals(20.0));
		assertTrue(value(outer, "j1/j2/t5/arg/y2").equals(80.0));

	}

	@Test
	public void executedUpcontextScope() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/z1"), inVal("arg/z2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				cxt("multiply", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/y1"), inVal("arg/y2"),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(y1, y2)), t3(x1, x2))
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						inVal("arg/y1", 20.0), inVal("arg/y2", 80.0),
						result(outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/z1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/z2")));


		Job out = exert(job);

//		logger.info("job j1 context: " + context(job));

		Context outerContext = upcontext(out);
//		logger.info("job j1 upcontext: " + outerContext);
//		logger.info("task t3 context: " + context(getValue(job, "j1/t3")));

		assertTrue(value(outerContext, "j1/j2/t4/arg/x1").equals(10.0));
		assertTrue(value(outerContext, "j1/j2/t4/arg/x2").equals(50.0));
		assertTrue(value(outerContext, "j1/j2/t4/result/y").equals(500.0));

		assertTrue(value(outerContext, "j1/j2/t5/arg/y1").equals(20.0));
		assertTrue(value(outerContext, "j1/j2/t5/arg/y2").equals(80.0));
		assertTrue(value(outerContext, "j1/j2/t5/result/y").equals(100.0));

		assertTrue(value(outerContext, "j1/t3/arg/z1").equals(500.0));
		assertTrue(value(outerContext, "j1/t3/arg/z2").equals(100.0));
		assertTrue(value(outerContext, "j1/t3/result/y").equals(400.0));

	}

	@Test
	public void contextPathShadowing() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/x1"), inVal("arg/x2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				cxt("multiply", inVal("arg/x1", 11.0), inVal("arg/x2"),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/y1"), inVal("arg/y2"),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(y1, y2)), t3(x1, x2))
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						inVal("arg/y1", 20.0), inVal("arg/y2", 80.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Object result = exec(job);
		logger.info("job result: " + result);
		assertTrue(result.equals(450.0));

	}

	@Test
	public void contexterService() throws Exception {

		// getValue a context for the template context in the task
		Task cxtt = task("addContext", sig("getContext", NetJobExertions.createContext()),
				context("add", input("arg/x1"), input("arg/x2")));

		Context result = context(exert(cxtt));
//		logger.info("contexter context: " + result);
		assertTrue(value(result, "arg/x1").equals(20.0));
		assertTrue(value(result, "arg/x2").equals(80.0));

	}
	
	@Test
	public void objectContexterTask() throws Exception {

		Task t5 = task("t5", sig("add", AdderImpl.class), 
					type(sig("getContext", NetJobExertions.createContext()), Signature.APD),
					context("add", inVal("arg/x1"), inVal("arg/x2"),
						result("result/y")));
		
		Context result = context(exert(t5));
//		logger.info("task context: " + result);
		assertTrue(value(result, "result/y").equals(100.0));

	}
	
	@Test
	public void exertJob() throws Exception {

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
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context outerContext = upcontext(exert(job));
		logger.info("job upcontext: " + outerContext);
		assertTrue(value(outerContext, "j1/t3/arg/x1").equals(500.0));
		assertTrue(value(outerContext, "j1/t3/arg/x2").equals(100.0));
		assertTrue(value(outerContext, "j1/t3/result/y").equals(400.0));

	}

	@Test
	public void evaluateJob() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/x1"), inVal("arg/x2"), outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				// "arg/x1; eval is in the scope of job j1
				cxt("multiply", inVal("arg/x1"), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task(
				"t5",
				sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1",
				sig("exert", ServiceJobber.class),
				cxt(inVal("arg/x1", 10.0),
						result("job/result", outPaths("j1/t3/result/y"))),
				job("j2", sig("exert", ServiceJobber.class), t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Object result = exec(job);
		logger.info("job result: " + result);
		assertTrue(result.equals(400.0));

	}

	@Test
	public void arithmeticLocalJobExerter() throws Exception {

		// exertion used as as service provider
		Job exerter = ArithmeticUtil.createLocalJob();

		Context out  = exert(exerter, context(ent("j1/t3/result/y")));
		logger.info("j1/t3/result/y: " + value(out, "j1/t3/result/y"));
		assertEquals(value(out, "j1/t3/result/y"), 400.0);

		// update inputs contexts
		Context multiplyContext = context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 70.0));
		Context addContext = context("add", inVal("arg/x1", 90.0), inVal("arg/x2", 110.0));
		Context invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		out = (exert(exerter, invokeContext));
		logger.info("j1/t3/result/y: " + out);
		assertEquals(value(out, "j1/t3/result/y"), 500.0);

		// update contexts partially
		multiplyContext = context("multiply", inVal("arg/x1", 20.0));
		addContext = context("add", inVal("arg/x1", 80.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		out = (exert(exerter, invokeContext));
		logger.info("j1/t3/result/y: " + out);
		assertEquals(value(out, "j1/t3/result/y"), 1210.0);

		// reverse the state to the initial one
		multiplyContext = context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0));
		addContext = context("add", inVal("arg/x1", 80.0), inVal("arg/x2", 20.0));
		invokeContext = context("invoke");
		link(invokeContext, "t4", multiplyContext);
		link(invokeContext, "t5", addContext);
		out = (exert(exerter, invokeContext));
		logger.info("j1/t3/result/y: " + out);
		assertEquals(value(out, "j1/t3/result/y"), 400.0);
	}
}
