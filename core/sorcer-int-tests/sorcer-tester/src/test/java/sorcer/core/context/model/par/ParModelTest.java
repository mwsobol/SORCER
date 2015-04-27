package sorcer.core.context.model.par;

import groovy.lang.Closure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.util.Sorcer;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.persistent;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.*;
import static sorcer.po.operator.asis;
import static sorcer.po.operator.put;
import static sorcer.po.operator.set;
import static sorcer.mo.operator.*;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ParModelTest {
	private final static Logger logger = Logger.getLogger(ParModelTest.class.getName());
	public static String sorcerVersion = System.getProperty("sorcer.version");

	@Test
	public void adderParTest() throws EvaluationException, RemoteException,
			ContextException {
		ParModel pm = parModel("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("add", "x + y", pars("x", "y")));

//		logger.info("adder value: " + value(pm, "add"));
		assertEquals(value(pm, "add"), 30.0);
		set(pm, "x", 20.0);
//		assertEquals(value(pm, "add"), 40.0);
	}
	
	@Test
	public void contextInvoker() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("add", new ServiceInvoker(pm));
		((ServiceInvoker)pm.get("add"))
			.setPars(pars("x", "y"))
			.setEvaluator(invoker("x + y", pars("x", "y")));
		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		logger.info("add value: " + pm.getValue("add"));
		assertEquals(pm.getValue("add"), 30.0);

		logger.info("invoker value: " 
				+ ((ServiceInvoker) pm.get("add")).invoke());

		pm.addResponsePath("add");
		logger.info("pm context value: " + pm.getValue());
		assertEquals(pm.getValue(), 30.0);
		
		pm.putValue("x", 100.0);
		pm.putValue("y", 200.0);
		logger.info("add value: " + pm.getValue("add"));
		assertEquals(pm.getValue("add"), 300.0);		

		assertEquals(pm.invoke(context(inEnt("x", 200.0), inEnt("y", 300.0))), 500.0);
	}
	

	@Test
	public void parModelTest() throws RemoteException, ContextException {
		ParEntry x = new ParEntry("x", 10.0);
		ParEntry y = new ParEntry("y", 20.0);
		ParEntry add = new ParEntry("add", invoker("x + y", pars("x", "y")));

		ParModel pm = new ParModel("arithmetic-model");
		pm.add(x, y, add);

		assertEquals(x.getValue(), 10.0);
		assertEquals(pm.getValue("x"), 10.0);

		assertEquals(y.getValue(), 20.0);
		assertEquals(pm.getValue("y"), 20.0);

		logger.info("add context value: " + pm.getValue("add"));
		logger.info("add par value: " + add.getValue());
		assertEquals(add.getValue(), 30.0);
		assertEquals(pm.getValue("add"), 30.0);

		pm.addResponsePath("add");
		logger.info("pm context value: " + pm.invoke(null));
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
		ParModel pm = parModel(par("x", 10.0), par("y", 20.0),
				par("add", invoker("x + y", pars("x", "y"))));

        addResponse(pm, "add");

		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		assertEquals(value(pm, "add"), 30.0);

		assertEquals(value(pm), 30.0);
	}
	
	@Test
	public void mutateParModeltTest() throws RemoteException,
			ContextException { 
		ParModel pm = parModel(par("x", 10.0), par("y", 20.0),
				par("add", invoker("x + y", pars("x", "y"))));

        addResponse(pm, "add");

		ParEntry x = par(pm, "x");
		logger.info("par x: " + x);
		set(x, 20.0);
		logger.info("val x: " + value(x));
		logger.info("val x: " + value(pm, "x"));

		put(pm, "y", 40.0);

		assertEquals(value(pm, "x"), 20.0);
		assertEquals(value(pm, "y"), 40.0);
		assertEquals(value(pm, "add"), 60.0);

		// get modeling taget
		assertEquals(value(pm), 60.0);

		add(pm, par("x", 10.0), par("y", 20.0));
		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);

		logger.info("par model2:" + pm);
		assertEquals(value(pm, "add"), 30.0);

		value(pm, "add");
		assertEquals(value(pm), 30.0);

		// with new arguments, closure
		assertTrue(value(pm, par("x", 20.0), par("y", 30.0)).equals(50.0));

		add(pm, par("z", invoker("(x * y) + add", pars("x", "y", "add"))));
		logger.info("z value: " + value(pm, "z"));
		assertEquals(value(pm, "z"), 650.0);
	}
		
	@Test
	public void contextParsTest() throws RemoteException,
			ContextException {
		Context cxt = context(ent("url", "myUrl"), ent("design/in", 25.0));

		// mapping parameters to cxt, m1 and m2 are par aliases 
		ParEntry p1 = par("p1", "design/in", cxt);
		assertEquals(value(p1), 25.0);
		set(p1, 30.0);
		assertEquals(value(p1), 30.0);
		
		ParEntry p2 = par("p2", "url", cxt);
		assertEquals(value(p2), "myUrl");
		set(p2, "newUrl");
		assertEquals(value(p2), "newUrl");
	}
	
	@Test
	public void persistableContext() throws ContextException, RemoteException {
		Context c4 = new ServiceContext("multiply");
		c4.putDbValue("arg/x1", 10.0);
		c4.putValue("arg/x2", 50.0);
		c4.putDbValue("result/y", null);
		
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);

		assertTrue(asis(c4, "arg/x1") instanceof ParEntry);
		assertEquals(asis(c4, "arg/x2"), 50.0);

		assertTrue(storeArg(c4, "arg/x1") instanceof URL);
		assertTrue(storeArg(c4, "arg/x2") instanceof URL);

		c4.putValue("arg/x1", 110.0);
		c4.putValue("arg/x2", 150.0);

		assertEquals(value(c4, "arg/x1"), 110.0);
		assertEquals(value(c4, "arg/x2"), 150.0);

		assertTrue(asis(c4, "arg/x1") instanceof ParEntry);
		assertFalse(asis(c4, "arg/x2") instanceof ParEntry);
	}
	
	@Test
	public void persistableEolParModel() throws ContextException, RemoteException {
		Context c4 = context("multiply",
				dbEnt("arg/x0", 1.0), dbInEnt("arg/x1", 10.0),
				dbOutEnt("arg/x2", 50.0), outEnt("result/y"));
		
		assertEquals(value(c4, "arg/x0"), 1.0);
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);
		
		assertTrue(asis(c4, "arg/x0") instanceof ParEntry);
		assertTrue(asis(c4, "arg/x1") instanceof ParEntry);
		assertTrue(asis(c4, "arg/x2") instanceof ParEntry);
		
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

		assertTrue(asis(c4, "arg/x0") instanceof ParEntry);
		assertTrue(asis(c4, "arg/x1") instanceof ParEntry);
		assertTrue(asis(c4, "arg/x2") instanceof ParEntry);
	}
	
	@Test
	public void persistableParsTest() throws SignatureException, ExertionException, ContextException, IOException {	
		// persistable just indicates that parameter is set given value that can be persist,
		// for example when value(par) is invoked
		ParEntry dbp1 = persistent(par("design/in", 25.0));
		ParEntry dbp2 = dbPar("url", "myUrl1");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);
		
		assertEquals(value(dbp1), 25.0);
		assertEquals(value(dbp2), "myUrl1");
		
		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store par args in the data store
		URL url1 = storeArg(par("design/in", 30.0));
		URL url2 = storeArg(par("url", "myUrl2"));
		
		assertEquals(content(url1), 30.0);
		assertEquals(content(url2), "myUrl2");
	}
	
	@Test
	public void persistableMappableParsTest() throws SignatureException, ExertionException, ContextException, IOException {
		Context cxt = context(ent("url", "myUrl"), ent("design/in", 25.0));

		// persistent par
		ParEntry dbIn = persistent(par("dbIn", "design/in", cxt));
		assertEquals(value(dbIn), 25.0);  	// is persisted
		logger.info("value dbIn asis design/in 1: " + dbIn.getMappable().asis("design/in"));

		assertTrue(asis(cxt,"design/in") instanceof ParEntry);
		assertEquals(value((ParEntry)asis(cxt, "design/in")), 25.0);
		assertEquals(value(cxt, "design/in"), 25.0);

		set(dbIn, 30.0); 	// is persisted
		
//		logger.info("value dbIn asis: " + dbIn.asis());
//		logger.info("value dbIn asis design/in 2: " + dbIn.getMappable().asis("design/in"));

		logger.info("value dbIn: " + value(dbIn));
		assertEquals(value(dbIn), 30.0);
		
		// not persistent par
		ParEntry up = par("up", "url", cxt);
		assertEquals(value(up), "myUrl");
		
		set(up, "newUrl");
		assertEquals(value(up), "newUrl");
	}
	
	@Test
	public void aliasedParsTest() throws ContextException, RemoteException {
		Context cxt = context(ent("design/in1", 25.0), ent("design/in2", 35.0));
		
		ParEntry x1 = par("x1", "design/in1", cxt);
		ParEntry x2 = par("x2", "design/in2", cxt);
	
		assertEquals(value(x1), 25.0);
		set(x1, 45.0);
		assertEquals(value(x1), 45.0);
		
		assertEquals(value(x2), 35.0);
		set(x2, 55.0);
		assertEquals(value(x2), 55.0);
		
		ParModel pc = parModel(x1, x2);
		assertEquals(value(pc, "x1"), 45.0);
		assertEquals(value(pc, "x2"), 55.0);
	}
	
	@Test
	public void exertionParsTest() throws RemoteException,
			ContextException, ExertionException, SignatureException {

		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				outEnt("result/y"));
				
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				outEnt("result/y"));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2"),
						outEnt("result/y")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5, sig("service", ServiceJobber.class)), 
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
		
		
		// context and job parameters
		ParEntry x1p = par("x1p", "arg/x1", c4);
		ParEntry x2p = par("x2p", "arg/x2", c4);
		ParEntry j1p = par("j1p", "j1/t3/result/y", j1);
		
		// setting context parameters in a job
		set(x1p, 10.0);
		set(x2p, 50.0);
		
		// update par references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
		logger.info("j1 value: " + serviceContext(j1));
		logger.info("j1p value: " + value(j1p));
		
		// get job parameter value
		assertEquals(value(j1p), 400.0);
		
		// set job parameter value
		set(j1p, 1000.0);
		assertEquals(value(j1p), 1000.0);

		// par model with exertions
		ParModel pc = parModel(x1p, x2p, j1p);
		exert(j1);
		// j1p is the alias to context value of j1 at j1/t3/result/y
		assertEquals(value(pc, "j1p"), 400.0);
	}
	
	@Test
	public void associatingContextsTest() throws RemoteException,
			ContextException, ExertionException, SignatureException {
		
		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				outEnt("result/y"));
				
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				outEnt("result/y"));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y", null)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5, sig("service", ServiceJobber.class)), 
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
		
		
		ParEntry c4x1p = par("c4x1p", "arg/x1", c4);
		ParEntry c4x2p = par("c4x2p", "arg/x2", c4);
		// job j1 parameter j1/t3/result/y is used in the context of task t6
		ParEntry j1p = par("j1p", "j1/t3/result/y", j1);
		ParEntry t4x1p = par("t4x1p", "j1/j2/t4/arg/x1", j1);
		
		// setting context parameters in a job
		set(c4x1p, 10.0);
		set(c4x2p, 50.0);
				
		// update par references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
		
		// get job parameter value
		assertEquals(value(j1p), 400.0);
		
		logger.info("j1 job context: " + serviceContext(j1));
		
		
		Task t6 = task("t6", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", t4x1p), inEnt("arg/x2", j1p),
						outEnt("result/y")));

		Task task = exert(t6);
//		logger.info("t6 context: " + context(task));
		assertEquals(get(task, "result/y"), 410.0);
	}

	@Test
	public void conditionalClosures() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");
		pm.putValue(Condition._closure_, new ServiceInvoker(pm));
		// free variables, no pars for the invoker
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
	public void conditionalParModel() throws RemoteException, ContextException {
		final ParModel pm = new ParModel("par-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("condition", new ServiceInvoker(pm));
		((ServiceInvoker)pm.get("condition"))
			.setPars(pars("x", "y"))
			.setEvaluator(invoker("x > y", pars("x", "y")));
		
		Condition flag = new Condition(pm, "condition");
		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		logger.info("condition value: " + flag.isTrue());
		assertEquals(flag.isTrue(), false);
		
		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("condition value: " + flag.isTrue());
		assertEquals(flag.isTrue(), true);
	}

	@Test
	public void closingParModelConditions() throws RemoteException, ContextException {
		final ParModel pm = new ParModel("par-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		
		Condition flag = new Condition(pm, 
				"{ x, y -> x > y }", "x", "y");
		
		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		logger.info("condition value: " + flag.isTrue());
		assertEquals(flag.isTrue(), false);
		
		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("condition value: " + flag.isTrue());
		assertEquals(flag.isTrue(), true);
	}

	
	@Test
		public void attachAgent() throws Exception {

		String sorcerVersion = System.getProperty("sorcer.version");

		ParModel pm = new ParModel();
		ParEntry<Double> x = par("x", 10.0);
		ParEntry<Double> y = par("y", 20.0);
		ParEntry z = par("z", invoker("x - y", x, y));
		
		// set the sphere/radius in the model
		put(pm, "sphere/radius", 20.0);
		// attach the agent to the par-model and invoke
		pm.add(new Agent("getSphereVolume",
				"sorcer.arithmetic.tester.volume.Volume",
				new URL(Sorcer.getWebsterUrl()
						+ "/sorcer-tester-" + sorcerVersion + ".jar")));

		Entry ent = (Entry) get((Context)value(pm,"getSphereVolume"), "sphere/volume");

//		logger.info("val: " + value(ent));
		assertEquals(value(ent), 33510.32163829113);

		// invoke the agent directly
		invoke(pm,
				"getSphereVolume",
				new Agent("getSphereVolume",
						"sorcer.arithmetic.tester.volume.Volume",
						new URL(Sorcer.getWebsterUrl()
								+ "/sorcer-tester-"+sorcerVersion+".jar")));

//		logger.info("val: " + value(pm, "sphere/volume"));
		assertEquals(value(pm, "sphere/volume"), 33510.32163829113);
	}
}
