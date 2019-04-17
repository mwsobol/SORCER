package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.modeling.mog;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;
import static sorcer.so.operator.eval;
import static sorcer.so.operator.exert;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ServiceExertionTest {
	private final static Logger logger = LoggerFactory.getLogger(ServiceExertionTest.class);

	private Routine eTask, eJob;
	// to avoid spelling errors in test cases define instance variables
	private String arg = "arg", result = "outDispatcher";
	private String x1 = "x1", x2 = "x2", y = "y";
	
	static {
		ServiceRoutine.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome() + "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
	}
	
	@Before
	public void setUp() throws Exception {
		// create an mograms
		eTask = createTask();	
		eJob = createJob();
	}
	
	@Test
	public void exertTaskTest() throws Exception {
		eTask = exert(eTask);

		// exert and them get the eval from task's context
		//logger.info("eTask eval @ outDispatcher/y = " + get(exert(eTask), path(outDispatcher, y)));
		assertTrue("Wrong eTask eval for 100.0", get(eTask, attPath(result, y)).equals(100.0));
		
		//logger.info("eTask eval @ arg/x1 = " + exert(eTask, path("arg/x1")));
		assertTrue("Wrong eTask eval for 20.0", get(eTask, attPath("arg/x1")).equals(20.0));

		//logger.info("eTask eval @  arg/x2 = " + exert(eTask, "arg/x2"));
		assertTrue("Wrong eTask eval for 80.0", get(eTask, "arg/x2").equals(80.0));
	}
	
	@Test
	public void exertJobTest() throws Exception {
		// get eval from job's exerted context
		assertTrue(eval(eJob, "j1/t3/arg/x2").equals(100.0));
		
		// exert and then get the job's context (upcotext - a kind of supercontext)
		Context out = upcontext(exert(eJob));
		logger.info("job context: " + out);

		//logger.info("eJob eval @  j2/t5/arg/x1 = " + get(eJob, "j2/t5/arg/x1"));
		assertTrue(value(out, "j1/j2/t5/arg/x1").equals(20.0));
			
		//logger.info("eJob eval @ j2/t4/arg/x1 = " + exert(eJob, path("j1/j2/t4/arg/x1")));
		assertTrue(value(out, "j1/j2/t4/arg/x1").equals(10.0));

		//logger.info("eJob eval @  j1/j2/t5/arg/x2 = " + exert(eJob, "j1/j2/t5/arg/x2"));
		assertTrue(value(out, "j1/j2/t5/arg/x2").equals(80.0));
		
		//logger.info("eJob eval @  j2/t5/arg/x1 = " + exert(eJob, "j2/t5/arg/x1"));
		assertTrue(value(out, "j1/j2/t5/arg/x1").equals(20.0));
		
		//logger.info("eJob eval @  j2/t4/arg/x2 = " + exert(eJob, "j2/t4/arg/x2"));
		assertTrue( value(out, "j1/j2/t4/arg/x2").equals(50.0));

		// final outDispatcher by three services
		assertEquals(value(out, "j1/t3/outDispatcher/y"), 400.0);
	}

	@Test
	public void accessingComponentExertionsTest() throws EvaluationException,
			RemoteException, RoutineException {
		//logger.info("eJob mograms: " + names(mograms(eJob)));
		assertTrue(names(mograms(eJob)).equals(list("t4", "t5", "j2", "t3", "j1")));

		//logger.info("t4 exertion: " + exertion(eJob, "t4"));
		assertTrue(name(xrt(eJob, "j1/j2/t4")).equals("t4"));
		
		//logger.info("j2 exertion: " + exertion(eJob, "j2"));
		assertTrue(name(xrt(eJob, "j1/j2")).equals("j2"));
		
		//logger.info("j2 exertion names: " + names(mograms(exertion(eJob, "j2"))));
		assertTrue(names(mograms(xrt(eJob, "j1/j2"))).equals(list("t4", "t5", "j2")));
	}

	// a simple task
	private Routine createTask() throws Exception {
		
//		Task task = task("t1", sig("add", Adder.class), 
//		   context("add", in(path(arg, x1), 20.0), in(path(arg, x2), 80.0),
//		      outGovernance(path(outDispatcher, y), null)));

		Task task = task("t1", sig("add", AdderImpl.class), 
				   context("add", inVal(attPath(arg, x1), 20.0), inVal(attPath(arg, x2), 80.0),
				      outVal(attPath(result, y), null)));
		
		return task;
	}
	
	// two level job composition
	private Routine createJob() throws Exception {
	
//		Task t3 = task("t3", sig("subtract", Subtractor.class), 
//		   context("subtract", in(path(arg, x1), null), in(path(arg, x2), null),
//		      outGovernance(path(outDispatcher, y), null)));
//		
//		Task t4 = task("t4", sig("multiply", Multiplier.class), 
//				   context("multiply", in(path(arg, x1), 10.0), in(path(arg, x2), 50.0),
//				      outGovernance(path(outDispatcher, y), null)));
//		
//		Task t5 = task("t5", sig("add", Adder.class), 
//		   context("add", in(path(arg, x1), 20.0), in(path(arg, x2), 80.0),
//		      outGovernance(path(outDispatcher, y), null)));
//
//		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
//		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
//		Job job = job("j1", job("j2", t4, t5), t3,
//		   pipe(outGovernance(t4, path(outDispatcher, y)), in(t3, path(arg, x1))),
//		   pipe(outGovernance(t5, path(outDispatcher, y)), in(t3, path(arg, x2))));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class), 
				context("subtract", inVal(attPath(arg, x1)), inVal(attPath(arg, x2)),
						outVal(attPath(result, y))));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), 
				context("multiply", inVal(attPath(arg, x1), 10.0), inVal(attPath(arg, x2), 50.0),
						outVal(attPath(result, y))));

		Task t5 = task("t5", sig("add", AdderImpl.class), 
				context("add", inVal(attPath(arg, x1), 20.0), inVal(attPath(arg, x2), 80.0),
						outVal(attPath(result, y))));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
		Job job = job("j1", sig("exert", ServiceJobber.class),
					job("j2", sig("exert", ServiceJobber.class), t4, t5),
					t3,
					pipe(outPoint(t4, attPath(result, y)), inPoint(t3, attPath(arg, x1))),
					pipe(outPoint(t5, attPath(result, y)), inPoint(t3, attPath(arg, x2))));
				
		return job;
	}
	
	//@Ignore
	@Test
	public void exertXrtTest() throws Exception {
		Mogram xrt = createXrt();
		logger.info("job context " + ((Job)xrt).getJobContext());
		
		logger.info("xrt eval @  t3/arg/x1 = " + get(xrt, "t3/arg/x1"));
		logger.info("xrt eval @  t3/arg/x2 = " + get(xrt, "t3/arg/x2"));
		logger.info("xrt eval @  t3/outDispatcher/y = " + get(xrt, "t3/outDispatcher/y"));

		//assertTrue("Wrong xrt eval for " + Context.Value.NULL, get(srv, "t3/arg/x2").equals(Context.Value.NULL));
	}
	
	// two level job composition
	private Mogram createXrt() throws Exception {
		// using the data context in jobs
		mog t3 = xrt("t3", sig("subtract", SubtractorImpl.class),
				cxt("subtract", inVal("arg/x1"), inVal("arg/x2"),
						outVal("outDispatcher/y")));

		mog t4 = xrt("t4", sig("multiply", MultiplierImpl.class),
				cxt("multiply", inVal("super/arg/x1"), inVal("arg/x2", 50.0),
						outVal("outDispatcher/y")));

		mog t5 = xrt("t5", sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("outDispatcher/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
		mog job = xrt("j1", sig("exert", ServiceJobber.class),
					cxt(inVal("arg/x1", 10.0), outVal("job/outDispatcher")),
				xrt("j2", sig("exert", ServiceJobber.class), t4, t5),
				t3,
				pipe(outPoint(t4, "outDispatcher/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "outDispatcher/y"), inPoint(t3, "arg/x2")));
				
		return job;
	}
}
