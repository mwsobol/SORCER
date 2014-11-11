package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.list;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.exertion;
import static sorcer.eo.operator.exertions;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.name;
import static sorcer.eo.operator.names;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.path;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.serviceContext;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.xrt;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class ServiceExertionTest {
	private final static Logger logger = Logger
			.getLogger(ServiceExertionTest.class.getName());

	private Exertion eTask, eJob;
	// to avoid spelling errors in test cases define instance variables
	private String arg = "arg", result = "result";
	private String x1 = "x1", x2 = "x2", y = "y";
	
	static {
		ServiceExertion.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome() + "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
	}
	
	@Before
	public void setUp() throws Exception {
		// create an exertions
		eTask = createTask();	
		eJob = createJob();
	}
	
	@Test
	public void exertTaskTest() throws ExertionException, ContextException {
		eTask = exert(eTask);

		// exert and them get the value from task's context
		//logger.info("eTask value @ result/y = " + get(exert(eTask), path(result, y)));
		assertTrue("Wrong eTask value for 100.0", get(eTask, path(result, y)).equals(100.0));
		
		//logger.info("eTask value @ arg/x1 = " + exert(eTask, path("arg/x1")));
		assertTrue("Wrong eTask value for 20.0", get(eTask, path("arg/x1")).equals(20.0));

		//logger.info("eTask value @  arg/x2 = " + exert(eTask, "arg/x2"));
		assertTrue("Wrong eTask value for 80.0", get(eTask, "arg/x2").equals(80.0));
	}
	
	@Test
	public void exertJobTest() throws ExertionException, ContextException {
		// just get value from job's context
		//logger.info("eJob value @  t3/arg/x2 = " + get(eJob, "j1/t3/arg/x2"));
		assertTrue("Wrong eJob value for " + Context.none, 
				get(eJob, "/j1/t3/arg/x2").equals(Context.none));
		
		// exert and then get the value from job's context
		eJob = exert(eJob);
		
		logger.info("eJob jobContext: " + serviceContext(eJob));
		//logger.info("eJob value @  j2/t5/arg/x1 = " + get(eJob, "j2/t5/arg/x1"));
		assertTrue("Wrong eJob value for 20.0", get(eJob, "/j1/j2/t5/arg/x1").equals(20.0));
			
		//logger.info("eJob value @ j2/t4/arg/x1 = " + exert(eJob, path("j1/j2/t4/arg/x1")));
		assertTrue("Wrong eJob value for 10.0", get(eJob, "/j1/j2/t4/arg/x1").equals(10.0));

		//logger.info("eJob value @  j1/j2/t5/arg/x2 = " + exert(eJob, "j1/j2/t5/arg/x2"));
		assertTrue("Wrong eJob value for 80.0", get(eJob, "/j1/j2/t5/arg/x2").equals(80.0));
		
		//logger.info("eJob value @  j2/t5/arg/x1 = " + exert(eJob, "j2/t5/arg/x1"));
		assertTrue("Wrong eJob value for 20.0", get(eJob, "/j1/j2/t5/arg/x1").equals(20.0));
		
		//logger.info("eJob value @  j2/t4/arg/x2 = " + exert(eJob, "j2/t4/arg/x2"));
		assertTrue("Wrong eJob value for 50.0", get(eJob, "/j1/j2/t4/arg/x2").equals(50.0));
			
		logger.info("job context: " + serviceContext(eJob));
		logger.info("value at j1/t3/result/y: " + get(eJob, "j1/t3/result/y"));
		logger.info("value at t3, result/y: " + get(eJob, "t3", "result/y"));

		// absolute path
		assertEquals("Wrong value for 400.0", get(eJob, "/j1/t3/result/y"), 400.0);
		//local t3 path
		assertEquals("Wrong value for 400.0", get(eJob, "t3", "result/y"), 400.0);
	}

	@Test
	public void accessingComponentExertionsTest() throws EvaluationException,
			RemoteException, ExertionException {
		//logger.info("eJob exertions: " + names(exertions(eJob)));
		assertTrue(names(exertions(eJob)).equals(list("t4", "t5", "j2", "t3", "j1")));
		
		//logger.info("t4 exertion: " + exertion(eJob, "t4"));
		assertTrue(name(exertion(eJob, "j1/j2/t4")).equals("t4"));
		
		//logger.info("j2 exertion: " + exertion(eJob, "j2"));
		assertTrue(name(exertion(eJob, "j1/j2")).equals("j2"));
		
		//logger.info("j2 exertion names: " + names(exertions(exertion(eJob, "j2"))));
		assertTrue(names(exertions(exertion(eJob, "j1/j2"))).equals(list("t4", "t5", "j2")));
	}

	// a simple task
	private Exertion createTask() throws Exception {
		
//		Task task = task("t1", sig("add", Adder.class), 
//		   context("add", in(path(arg, x1), 20.0), in(path(arg, x2), 80.0),
//		      out(path(result, y), null)));

		Task task = task("t1", sig("add", AdderImpl.class), 
				   context("add", inEnt(path(arg, x1), 20.0), inEnt(path(arg, x2), 80.0),
				      outEnt(path(result, y), null)));
		
		return task;
	}
	
	// two level job composition
	private Exertion createJob() throws Exception {
	
//		Task t3 = task("t3", sig("subtract", Subtractor.class), 
//		   context("subtract", in(path(arg, x1), null), in(path(arg, x2), null),
//		      out(path(result, y), null)));
//		
//		Task t4 = task("t4", sig("multiply", Multiplier.class), 
//				   context("multiply", in(path(arg, x1), 10.0), in(path(arg, x2), 50.0),
//				      out(path(result, y), null)));
//		
//		Task t5 = task("t5", sig("add", Adder.class), 
//		   context("add", in(path(arg, x1), 20.0), in(path(arg, x2), 80.0),
//		      out(path(result, y), null)));
//
//		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
//		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
//		Job job = job("j1", job("j2", t4, t5), t3,
//		   pipe(out(t4, path(result, y)), in(t3, path(arg, x1))),
//		   pipe(out(t5, path(result, y)), in(t3, path(arg, x2))));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inEnt(path(arg, x1), null), inEnt(path(arg, x2), null),
						outEnt(path(result, y))));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inEnt(path(arg, x1), 10.0), inEnt(path(arg, x2), 50.0),
						outEnt(path(result, y))));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inEnt(path(arg, x1), 20.0), inEnt(path(arg, x2), 80.0),
						outEnt(path(result, y))));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
		Job job = job("j1", sig("execute", ServiceJobber.class), 
					job("j2", sig("execute", ServiceJobber.class), t4, t5), 
					t3,
					pipe(out(t4, path(result, y)), in(t3, path(arg, x1))),
					pipe(out(t5, path(result, y)), in(t3, path(arg, x2))));
				
		return job;
	}
	
	//@Ignore
	@Test
	public void exertXrtTest() throws Exception {
		Exertion xrt = createXrt();
		logger.info("job context " + ((Job)xrt).getJobContext());
		
		logger.info("xrt value @  t3/arg/x1 = " + get(xrt, "t3/arg/x1"));
		logger.info("xrt value @  t3/arg/x2 = " + get(xrt, "t3/arg/x2"));
		logger.info("xrt value @  t3/result/y = " + get(xrt, "t3/result/y"));

		//assertTrue("Wrong xrt value for " + Context.Value.NULL, get(srv, "t3/arg/x2").equals(Context.Value.NULL));
	}
	
	// two level job composition
	private Exertion createXrt() throws Exception {
		// using the data context in jobs
		Task t3 = xrt("t3", sig("subtract", SubtractorImpl.class), 
				cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y")));

		Task t4 = xrt("t4", sig("multiply", MultiplierImpl.class), 
				cxt("multiply", inEnt("super/arg/x1"), inEnt("arg/x2", 50.0),
						outEnt("result/y")));

		Task t5 = xrt("t5", sig("add", AdderImpl.class), 
				cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
						outEnt("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
		Job job = xrt("j1", sig("execute", ServiceJobber.class), 
					cxt(inEnt("arg/x1", 10.0), outEnt("job/result")), 
				xrt("j2", sig("execute", ServiceJobber.class), t4, t5), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
				
		return job;
	}
}
