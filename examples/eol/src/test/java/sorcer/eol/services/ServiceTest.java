package sorcer.eol.services;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.from;
import static sorcer.co.operator.inEntry;
import static sorcer.co.operator.outEntry;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.jobContext;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.service;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.srv;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.value;

import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.Service;
import sorcer.service.SignatureException;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ServiceTest {
	private final static Logger logger = Logger
			.getLogger(ServiceTest.class.getName());

	static {
		String version = "5.0.0-SNAPSHOT";
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + version + "-dl.jar",  "sorcer-dl-"+version +".jar" });
		
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}
	
	
	@Test
	public void exertTask() throws Exception  {

		Service t5 = service("t5", sig("add", AdderImpl.class),
				cxt("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		Service out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));
		
		assertEquals(100.0, value(cxt, "result/y"));

	}
	
	
	public void taskValue() throws SignatureException, ExertionException, ContextException  {

		Service t5 = service("t5", sig("add", AdderImpl.class),
				cxt("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		Object out = value(t5);
		logger.info("out value: " + out);
		assertEquals(100.0, out);
		
	}

	
	@Test
	public void exertJob() throws Exception {

		Task t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inEntry("arg/x1"), inEntry("arg/x2"), outEntry("result/y")));

		Task t4 = srv("t4", sig("multiply", MultiplierImpl.class),
				// cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
				cxt("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						outEntry("result/y")));

		Task t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		Service job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
				srv("j1", sig(ServiceJobber.class),
					cxt(inEntry("arg/x1", 10.0),
					result("job/result", from("j1/t3/result/y"))),
				srv("j2", sig(ServiceJobber.class), t4, t5), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		logger.info("srv job context: " + jobContext(job));
		logger.info("srv j1/t3 context: " + context(job, "j1/t3"));
		logger.info("srv j1/j2/t4 context: " + context(job, "j1/j2/t4"));
		logger.info("srv j1/j2/t5 context: " + context(job, "j1/j2/t5"));

		Service exertion = exert(job);
		logger.info("srv job context: " + jobContext(exertion));
		logger.info("exertion value @ j1/t3/arg/x2 = " + get(exertion, "j1/t3/arg/x2"));
		assertEquals(100.0, get(exertion, "j1/t3/arg/x2"));
		
	}
	
	
	@Test
	public void jobValue() throws Exception {

		Task t3 = srv("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inEntry("arg/x1"), inEntry("arg/x2"), result("result/y")));

		Task t4 = srv("t4", sig("multiply", MultiplierImpl.class),
				cxt("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0), result("result/y")));

		Task t5 = srv("t5", sig("add", AdderImpl.class),
				cxt("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0), result("result/y")));

		Job job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
				srv("j1", sig(ServiceJobber.class), result("job/result", from("j1/t3/result/y")),
				srv("j2", sig(ServiceJobber.class), t4, t5, strategy(Flow.PAR, Access.PULL)), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		Object out = value(job);
		logger.info("srv job value: " + out);
		assertEquals(400.0, out);
		
	}
}
	
	