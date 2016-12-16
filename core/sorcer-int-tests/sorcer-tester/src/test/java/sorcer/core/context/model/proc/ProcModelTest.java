package sorcer.core.context.model.proc;

import groovy.lang.Closure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Agent;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.util.Sorcer;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.persistent;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.responseUp;
import static sorcer.po.operator.add;
import static sorcer.po.operator.*;
import static sorcer.po.operator.put;
import static sorcer.mo.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ProcModelTest {
	private final static Logger logger = LoggerFactory.getLogger(ProcModelTest.class);
	public static String sorcerVersion = System.getProperty("sorcer.version");

	@Test
	public void adderProcTest() throws EvaluationException, RemoteException,
			ContextException {
		ProcModel pm = procModel("proc-model");
		add(pm, ent("x", 10.0), ent("y", 20.0));
		add(pm, invoker("add", "x + y", args("x", "y")));

//		logger.info("adder eval: " + eval(pm, "add"));
		assertEquals(value(pm, "add"), 30.0);
		setValue(pm, "x", 20.0);
//		assertEquals(eval(pm, "add"), 40.0);
	}

	@Test
	public void contextInvoker() throws Exception {
		ProcModel pm = new ProcModel("proc-model");
		add(pm, ent("x", 10.0));
		add(pm, ent("y", 20.0));
		add(pm, ent("add", invoker("x + y", args("x", "y"))));

		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		assertEquals(pm.getValue("add"), 30.0);

		responseUp(pm, "add");
//		logger.info("pm context eval: " + pm.getValue());
		assertEquals(pm.getValue(), 30.0);

		pm.putValue("x", 100.0);
		pm.putValue("y", 200.0);
//		logger.info("add eval: " + pm.getValue("add"));
		assertEquals(pm.getValue("add"), 300.0);

		assertEquals(pm.invoke(context(inVal("x", 200.0), inVal("y", 300.0))), 500.0);
	}
	

	@Test
	public void parModelTest() throws RemoteException, ContextException {
		Proc x = new Proc("x", 10.0);
		Proc y = new Proc("y", 20.0);
		Proc add = new Proc("add", invoker("x + y", args("x", "y")));

		ProcModel pm = new ProcModel("arithmetic-model");
		pm.add(x, y, add);

		assertEquals(x.getValue(), 10.0);
		assertEquals(pm.getValue("x"), 10.0);

		assertEquals(y.getValue(), 20.0);
		assertEquals(pm.getValue("y"), 20.0);

		logger.info("add context eval: " + pm.getValue("add"));
		logger.info("add proc eval: " + add.getValue());
		assertEquals(add.getValue(), 30.0);
		assertEquals(pm.getValue("add"), 30.0);

		responseUp(pm, "add");
		logger.info("pm context eval: " + pm.invoke(null));
		assertEquals(pm.invoke(null), 30.0);

		x = pm.getPar("x");
		y = pm.getPar("y");
		add = pm.getPar("add");
		assertEquals(x.getValue(), 10.0);
		assertEquals(y.getValue(), 20.0);
		assertEquals(add.getValue(), 30.0);
	}

	@Test
	public void dslParModelTest() throws RemoteException,
			ContextException {
		ProcModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		responseUp(pm, "add");

		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		assertEquals(value(pm, "add"), 30.0);

		assertEquals(eval(pm), 30.0);
	}
	
	@Test
	public void mutateParModeltTest() throws RemoteException,
			ContextException { 
		ProcModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		responseUp(pm, "add");

		Proc x = proc(pm, "x");
		logger.info("proc x: " + x);
		setValue(x, 20.0);
		logger.info("val x: " + eval(x));
		logger.info("val x: " + value(pm, "x"));

		put(pm, "y", 40.0);

		assertEquals(value(pm, "x"), 20.0);
		assertEquals(value(pm, "y"), 40.0);
		assertEquals(value(pm, "add"), 60.0);

		// get modeling taget
		assertEquals(eval(pm), 60.0);

		add(pm, proc("x", 10.0), proc("y", 20.0));
		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);

		logger.info("proc model2:" + pm);
		assertEquals(value(pm, "add"), 30.0);

		value(pm, "add");
		assertEquals(eval(pm), 30.0);

		// with new arguments, closure
		assertTrue(value(pm, proc("x", 20.0), proc("y", 30.0)).equals(50.0));

		add(pm, proc("z", invoker("(x * y) + add", args("x", "y", "add"))));
		logger.info("z eval: " + value(pm, "z"));
		assertEquals(value(pm, "z"), 650.0);
	}
		
	@Test
	public void contextParsTest() throws RemoteException,
			ContextException {
		Context cxt = context(proc("url", "myUrl"), proc("design/in", 25.0));

		// mapping parameters to cxt, m1 and m2 are proc aliases
		Proc p1 = as(proc("p1", "design/in"), cxt);
		assertTrue(eval(p1).equals(25.0));
		setValue(p1, 30.0);
		assertTrue(eval(p1).equals(30.0));
		
		Proc p2 = as(proc("p2", "url"), cxt);
		assertEquals(eval(p2), "myUrl");
		setValue(p2, "newUrl");
		assertEquals(eval(p2), "newUrl");
	}
	
	@Test
	public void persistableContext() throws ContextException, RemoteException {
		Context c4 = new ServiceContext("multiply");
		c4.putDbValue("arg/x1", 10.0);
		c4.putValue("arg/x2", 50.0);
		c4.putDbValue("result/y", null);
		
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);

		assertTrue(asis(c4, "arg/x1") instanceof Proc);
		assertEquals(asis(c4, "arg/x2"), 50.0);

		assertTrue(storeArg(c4, "arg/x1") instanceof URL);
		assertTrue(storeArg(c4, "arg/x2") instanceof URL);

		c4.putValue("arg/x1", 110.0);
		c4.putValue("arg/x2", 150.0);

		assertEquals(value(c4, "arg/x1"), 110.0);
		assertEquals(value(c4, "arg/x2"), 150.0);

		assertTrue(asis(c4, "arg/x1") instanceof Proc);
		assertFalse(asis(c4, "arg/x2") instanceof Proc);
	}
	
	@Test
	public void dbBasedModel() throws ContextException, RemoteException {
		Context c4 = context("multiply",
				dbVal("arg/x0", 1.0), dbInVal("arg/x1", 10.0),
				dbOutVal("arg/x2", 50.0), outVal("result/y"));
		
		assertEquals(value(c4, "arg/x0"), 1.0);
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);
		
		assertTrue(asis(c4, "arg/x0") instanceof Proc);
		assertTrue(asis(c4, "arg/x1") instanceof Proc);
		assertTrue(asis(c4, "arg/x2") instanceof Proc);
		
		logger.info("arg/x0 URL: " + storeArg(c4, "arg/x0"));
		logger.info("arg/x1 URL: " + storeArg(c4, "arg/x1"));
		logger.info("arg/x2 URL: " + storeArg(c4, "arg/x2"));
		assertTrue(storeArg(c4, "arg/x0") instanceof URL);
		assertTrue(storeArg(c4, "arg/x1") instanceof URL);
		assertTrue(storeArg(c4, "arg/x2") instanceof URL);
		
		c4.putValue("arg/x0", 11.0);
		c4.putValue("arg/x1", 110.0);
		c4.putValue("arg/x2", 150.0);
		
		assertEquals(value(c4, "arg/x0"), 11.0);
		assertEquals(value(c4, "arg/x1"), 110.0);
		assertEquals(value(c4, "arg/x2"), 150.0);

		assertTrue(asis(c4, "arg/x0") instanceof Proc);
		assertTrue(asis(c4, "arg/x1") instanceof Proc);
		assertTrue(asis(c4, "arg/x2") instanceof Proc);
	}
	
	@Test
	public void persistableDbTest() throws SignatureException, ExertionException, ContextException, IOException {
		// persistable just indicates that parameter is setValue given eval that can be persist,
		// for example when eval(proc) is invoked
		Proc dbp1 = persistent(proc("design/in", 25.0));
		Proc dbp2 = dbEnt("url", "myUrl1");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);
		
		assertTrue(eval(dbp1).equals(25.0));
		assertTrue(eval(dbp2).equals("myUrl1"));

		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store proc args in the data store
		URL url1 = storeArg(proc("design/in", 30.0));
		URL url2 = storeArg(proc("url", "myUrl2"));
		
		assertEquals(content(url1), 30.0);
		assertEquals(content(url2), "myUrl2");
	}

	@Test
	public void mappableDbPersistence() throws Exception {

		Context cxt = context(val("url", "htt://sorcersoft.org"), val("design/in", 25.0));

		// persistent proc
		Proc dbIn = persistent(as(proc("dbIn", "design/in"), cxt));
		assertTrue(eval(dbIn).equals(25.0));  	// is persisted
		assertTrue(dbIn.asis().equals("design/in"));
		assertTrue(eval((Evaluation) asis(cxt, "design/in")).equals(25.0));
		assertTrue(value(cxt, "design/in").equals(25.0));

		setValue(dbIn, 30.0); 	// is persisted
		assertTrue(eval(dbIn).equals(30.0));

		// associated context is updated accordingly
		assertTrue(value(cxt, "design/in").equals(30.0));
		assertTrue(asis(cxt, "design/in") instanceof Proc);
		assertTrue(asis((Proc)asis(cxt, "design/in")) instanceof URL);

		// not persistent proc
		Proc sorcer = as(proc("sorcer", "url"), cxt);
		assertEquals(eval(sorcer), "htt://sorcersoft.org");

		setValue(sorcer, "htt://sorcersoft.org/sobol");
		assertTrue(eval(sorcer).equals("htt://sorcersoft.org/sobol"));
	}

	@Test
	public void aliasedProcTest() throws ContextException, RemoteException {
		Context cxt = context(proc("design/in1", 25.0), proc("design/in2", 35.0));
		
		Proc x1 = proc(cxt, "x1", "design/in1");
		Proc x2 = as(proc("x2", "design/in2"), cxt);

		assertTrue(eval(x1).equals(25.0));
		setValue(x1, 45.0);
		assertTrue(eval(x1).equals(45.0));

		assertTrue(eval(x2).equals(35.0));
		setValue(x2, 55.0);
		assertTrue(eval(x2).equals(55.0));
		
		ProcModel pc = procModel(x1, x2);
		assertTrue(value(pc, "x1").equals(45.0));
		assertTrue(value(pc, "x2").equals(55.0));
	}
	
	@Test
	public void exertionParsTest() throws Exception {

		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				outVal("result/y"));
				
		Context c5 = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				outVal("result/y"));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1", null), inVal("arg/x2"),
						outVal("result/y")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5, sig("exert", ServiceJobber.class)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
		
		
		// context and job procedures
		Proc x1p = as(proc("x1p", "arg/x1"), c4);
		Proc x2p = as(proc("x2p", "arg/x2"), c4);
		Proc j1p = as(proc("j1p", "j1/t3/result/y"), j1);
		
		// setting context parameters in a job
		setValue(x1p, 10.0);
		setValue(x2p, 50.0);
		
		// update proc references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
		logger.info("j1 eval: " + upcontext(j1));
		logger.info("j1p eval: " + eval(j1p));
		
		// get job parameter eval
		assertTrue(eval(j1p).equals(400.0));
		
		// setValue job parameter eval
		setValue(j1p, 1000.0);
		assertTrue(eval(j1p).equals(1000.0));

		// proc model with mograms
		ProcModel pc = procModel(x1p, x2p, j1p);
		exert(j1);
		// j1p is the alias to context eval of j1 at j1/t3/result/y
		assertTrue(value(pc, "j1p").equals(400.0));
	}
	
	@Test
	public void associatingContextsTest() throws Exception {
		
		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				outVal("result/y"));
				
		Context c5 = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				outVal("result/y"));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
						outVal("result/y", null)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5, sig("exert", ServiceJobber.class)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
		
		
		Proc c4x1p = as(proc("c4x1p", "arg/x1"), c4);
		Proc c4x2p = as(proc("c4x2p", "arg/x2"), c4);
		// job j1 parameter j1/t3/result/y is used in the context of task t6
		Proc j1p = proc("j1p", "j1/t3/result/y", j1);
		Proc t4x1p = proc("t4x1p", "j1/j2/t4/arg/x1", j1);
		
		// setting context parameters in a job
		setValue(c4x1p, 10.0);
		setValue(c4x2p, 50.0);
				
		// update proc references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
		
		// get job parameter eval
		assertTrue(eval(j1p).equals(400.0));
		
		logger.info("j1 job context: " + upcontext(j1));
		
		
		Task t6 = task("t6", sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", t4x1p), inVal("arg/x2", j1p),
						outVal("result/y")));

		Task task = exert(t6);
//		logger.info("t6 context: " + context(task));
		assertEquals(get(task, "result/y"), 410.0);
	}

	@Test
	public void conditionalClosures() throws RemoteException, ContextException {
		ProcModel pm = new ProcModel("proc-model");
		pm.putValue(Condition._closure_, new ServiceInvoker(pm));
		// free variables, no args for the invoker
		((ServiceInvoker) pm.get(Condition._closure_))
				.setEvaluator(invoker("{ double x, double y -> x > y }"));

		Closure c = (Closure)pm.getValue(Condition._closure_);
		logger.info("closure condition: " + c);
		Object[] args = new Object[] { 10.0, 20.0 };
		logger.info("closure condition 1: " + c.call(args));
		assertEquals(c.call(args), false);

		//reuse the closure again
		args = new Object[] { 20.0, 10.0 };
		logger.info("closure condition 2: " + c.call(args));
		assertEquals(c.call(args), true);

		// provided conditional context to get the closure only
		Condition eval = new Condition(pm) {
			@Override
			public boolean isTrue() throws ContextException {
				Closure c = (Closure)conditionalContext.getValue(Condition._closure_);
				Object[] args = new Object[] { conditionalContext.getValue("x"),
						conditionalContext.getValue("y") };
				return (Boolean) c.call(args);
			}
		};

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		assertEquals(eval.getValue(), false);

		pm.putValue("x", 20.0);
		pm.putValue("y", 10.0);
		assertEquals(eval.getValue(), true);
	}

	@Test
	public void closingModelConditions() throws RemoteException, ContextException {
		final ProcModel pm = new ProcModel("proc-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		
		Condition flag = new Condition(pm, "{ x, y -> x > y }", "x", "y");
		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		logger.info("condition eval: " + flag.isTrue());
		assertEquals(flag.isTrue(), false);
		
		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("condition eval: " + flag.isTrue());
		assertEquals(flag.isTrue(), true);
	}

	@Test
		public void attachAgent() throws Exception {

		String sorcerVersion = System.getProperty("sorcer.version");

		ProcModel pm = new ProcModel();
		Proc<Double> x = proc("x", 10.0);
		Proc<Double> y = proc("y", 20.0);
		Proc z = proc("z", invoker("x - y", x, y));
		
		// setValue the sphere/radius in the model
		put(pm, "sphere/radius", 20.0);
		// attach the agent to the proc-model and invoke
		pm.add(new Agent("getSphereVolume",
				"sorcer.arithmetic.tester.volume.Volume",
				new URL(Sorcer.getWebsterUrl()
						+ "/sorcer-tester-" + sorcerVersion + ".jar")));

		Object result =  value((Context)value(pm,"getSphereVolume"), "sphere/volume");

//		logger.info("val: " + eval(proc));
		assertTrue(result.equals(33510.32163829113));

		// invoke the agent directly
		invoke(pm,
				"getSphereVolume",
				new Agent("getSphereVolume",
						"sorcer.arithmetic.tester.volume.Volume",
						new URL(Sorcer.getWebsterUrl()
								+ "/sorcer-tester-"+sorcerVersion+".jar")));

//		logger.info("val: " + eval(pm, "sphere/volume"));
		assertTrue(value(pm, "sphere/volume").equals(33510.32163829113));
	}
}
