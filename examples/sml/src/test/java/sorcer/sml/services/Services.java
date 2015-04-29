package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Context;
import sorcer.service.Service;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Services {
	private final static Logger logger = LoggerFactory.getLogger(Services.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Service t5 = service("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Service out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));
		
		assertEquals(100.0, value(cxt, "result/y"));

	}


	@Test
	public void evaluateTask() throws Exception  {

		Service t5 = service("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						result("result/y")));

		// get a single context argument
		assertEquals(100.0, exec(t5));

		// get the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
				exec(t5, result("result/z", outPaths("arg/x1", "result/z")))));
	}

	
	@Test
	public void exertJob() throws Exception {

		Service t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));

		Service t4 = srv("t4", sig("multiply", MultiplierImpl.class),
				// cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
				cxt("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Service t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		Service job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
				srv("j1", sig(ServiceJobber.class),
					cxt(inEnt("arg/x1", 10.0),
					result("job/result", outPaths("j1/t3/result/y"))),
					srv("j2", sig(ServiceJobber.class), t4, t5),
					t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
		
		logger.info("srv job context: " + serviceContext(job));
		logger.info("srv j1/t3 context: " + context(job, "j1/t3"));
		logger.info("srv j1/j2/t4 context: " + context(job, "j1/j2/t4"));
		logger.info("srv j1/j2/t5 context: " + context(job, "j1/j2/t5"));

		Service exertion = exert(job);
		logger.info("srv job context: " + serviceContext(exertion));
		logger.info("exertion value @ j1/t3/arg/x2 = " + get(exertion, "j1/t3/arg/x2"));
		assertEquals(100.0, get(exertion, "j1/t3/arg/x2"));
		
	}
	
	
	@Test
	public void evaluateJob() throws Exception {

		Service t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"), result("result/y")));

		Service t4 = srv("t4", sig("multiply", MultiplierImpl.class),
				cxt("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0), result("result/y")));

		Service t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

        //TODO: CHECK Access.PULL doesn't work with ServiceJobber!!!
		Service job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
				srv("j1", sig(ServiceJobber.class), result("job/result", outPaths("j1/t3/result/y")),
					srv("j2", sig(ServiceJobber.class), t4, t5, strategy(Flow.PAR, Access.PUSH)),
					t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		// get the result value
		assertEquals(400.0, exec(job));

		// get the subcontext output from the exertion
		assertTrue(context(ent("j1/j2/t4/result/y", 500.0),
				ent("j1/j2/t5/result/y", 100.0),
				ent("j1/t3/result/y", 400.0)).equals(
					exec(job, result("result/z",
						outPaths("j1/j2/t4/result/y", "j1/j2/t5/result/y", "j1/t3/result/y")))));

		
	}
}
	
	
