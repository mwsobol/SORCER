package sorcer.core.context.model.par;

import groovy.lang.Closure;
import org.junit.Test;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.util.Sorcer;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.co.operator.persistent;
import static sorcer.co.operator.url;
import static sorcer.eo.operator.asis;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.*;
import static sorcer.po.operator.asis;
import static sorcer.po.operator.put;
import static sorcer.po.operator.set;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ParModelTest {
	private final static Logger logger = Logger.getLogger(ParModelTest.class.getName());

	static {
		ServiceExertion.debug = true;
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "aritmeticInvokeModel-bean.jar" });
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		//System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}

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

		pm.setReturnPath("add");
		logger.info("pm context value: " + pm.getValue());
		assertEquals(pm.getValue(), 30.0);
		
		pm.putValue("x", 100.0);
		pm.putValue("y", 200.0);
		logger.info("add value: " + pm.getValue("add"));
		assertEquals(pm.getValue("add"), 300.0);		

		assertEquals(pm.invoke(context(inEnt("x", 200.0), inEnt("y", 300.0))), 500.0);
	}
	

	@Test
	public void parContextTest() throws RemoteException, ContextException {
		Par x = new Par("x", 10.0);
		Par y = new Par("y", 20.0);
		Par add = new Par("add", invoker("x + y", pars("x", "y")));

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

		pm.setReturnPath("add");
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

		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		assertEquals(value(pm, "add"), 30.0);

		value(pm, "add");
		assertEquals(value(pm), 30.0);
	}
	
	@Test
	public void mutateParContextTest() throws RemoteException,
			ContextException { 
		ParModel pm = parModel(par("x", 10.0), par("y", 20.0),
				par("add", invoker("x + y", pars("x", "y"))));
		
		Par x = par(pm, "x");
		logger.info("par x: " + x);
		set(x, 20.0);
		logger.info("val x: " + value(x));
		logger.info("val x: " + value(pm, "x"));

		put(pm, "y", 40.0);
		
		logger.info("par model1:" + pm);
		
		assertEquals(value(pm, "x"), 20.0);
		assertEquals(value(pm, "y"), 40.0);
		assertEquals(value(pm, "add"), 60.0);
		
		value(pm, "add");
		assertEquals(value(pm), 60.0);
		
		add(pm, par("x", 10.0), par("y", 20.0));
		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		
		logger.info("par model2:" + pm);
		assertEquals(value(pm, "add"), 30.0);
		
		value(pm, "add");
		assertEquals(value(pm), 30.0);
		
		// with new arguments, closure
		assertEquals(Double.valueOf(30.0d), (Double)value(pm, par("x", 10.0), par("y", 20.0)));
		
		add(pm, par("z", invoker("(x * y) + add", pars("x", "y", "add"))));
		logger.info("z value: " + value(pm, "z"));
		assertEquals(value(pm, "z"), 230.0);
	}
		
	@Test
	public void contextParsTest() throws RemoteException,
			ContextException {
		Context cxt = context(ent("url", "myUrl"), ent("design/in", 25.0));

		// mapping parameters to cxt, m1 and m2 are par aliases 
		Par p1 = par("p1", "design/in", cxt);
		assertEquals(value(p1), 25.0);
		set(p1, 30.0);
		assertEquals(value(p1), 30.0);
		
		Par p2 = par("p2", "url", cxt);
		assertEquals(value(p2), "myUrl");
		set(p2, "newUrl");
		assertEquals(value(p2), "newUrl");
	}
	
	@Test
	public void persistableContext() throws ContextException, RemoteException {
		Context c4 = new ServiceContext("multiply");
		c4.putDbValue("arg/x1", 10.0);
		c4.putValue("arg/x2", 50.0);
		c4.putDbValue("result/y", Context.none);
		
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);
		
		assertTrue(asis(c4, "arg/x1") instanceof Par);
		assertFalse(asis(c4, "arg/x2") instanceof Par);
		
		logger.info("arg/x1 URL: " + url(c4, "arg/x1"));
		assertTrue(url(c4, "arg/x1") instanceof URL);
		assertFalse(url(c4, "arg/x2") instanceof URL);
		
		c4.putValue("arg/x1", 110.0);
		c4.putValue("arg/x2", 150.0);
		
		assertEquals(value(c4, "arg/x1"), 110.0);
		assertEquals(value(c4, "arg/x2"), 150.0);
		
		assertTrue(asis(c4, "arg/x1") instanceof Par);
		assertFalse(asis(c4, "arg/x2") instanceof Par);
	}
	
	@Test
	public void persistableEolContext() throws ContextException, RemoteException {
		Context c4 = context("multiply", dbEnt("arg/x0", 1.0), dbInEnt("arg/x1", 10.0), dbOutEnt("arg/x2", 50.0), outEnt("result/y"));
		
		assertEquals(value(c4, "arg/x0"), 1.0);
		assertEquals(value(c4, "arg/x1"), 10.0);
		assertEquals(value(c4, "arg/x2"), 50.0);
		
		assertTrue(asis(c4, "arg/x0") instanceof Par);
		assertTrue(asis(c4, "arg/x1") instanceof Par);
		assertTrue(asis(c4, "arg/x2") instanceof Par);
		
		logger.info("arg/x0 URL: " + url(c4, "arg/x0"));
		logger.info("arg/x1 URL: " + url(c4, "arg/x1"));
		logger.info("arg/x2 URL: " + url(c4, "arg/x2"));
		assertTrue(url(c4, "arg/x0") instanceof URL);
		assertTrue(url(c4, "arg/x1") instanceof URL);
		assertTrue(url(c4, "arg/x2") instanceof URL);
		
		c4.putValue("arg/x0", 11.0);
		c4.putValue("arg/x1", 110.0);
		c4.putValue("arg/x2", 150.0);
		
		assertEquals(value(c4, "arg/x0"), 11.0);
		assertEquals(value(c4, "arg/x1"), 110.0);
		assertEquals(value(c4, "arg/x2"), 150.0);

		assertTrue(asis(c4, "arg/x0") instanceof Par);
		assertTrue(asis(c4, "arg/x1") instanceof Par);
		assertTrue(asis(c4, "arg/x2") instanceof Par);
	}
	
	@Test
	public void persistableParsTest() throws SignatureException, ExertionException, ContextException, IOException {	
		// persistable just indicates that parameter is set given value that can be persist,
		// for example when value(par) is invoked
		Par dbp1 = persistent(par("design/in", 25.0));
		Par dbp2 = dbPar("url", "myUrl1");

		assertFalse(asis(dbp1) instanceof URL);
		assertFalse(asis(dbp2) instanceof URL);
		
		assertEquals(value(dbp1), 25.0);
		assertEquals(value(dbp2), "myUrl1");
		
		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store par args in the data store
		URL url1 = url(par("design/in", 30.0));
		URL url2 = url(par("url", "myUrl2"));
		
		assertEquals(value(url1), 30.0);
		assertEquals(value(url2), "myUrl2");
	}
	
	@Test
	public void persistableMappableParsTest() throws SignatureException, ExertionException, ContextException, IOException {
		Context cxt = context(ent("url", "myUrl"), ent("design/in", 25.0));

		// persistent par
		Par dbIn = persistent(par("dbIn", "design/in", cxt));
		assertEquals(value(dbIn), 25.0);  	// is persisted
		logger.info("value dbIn asis design/in 1: " + dbIn.getMappable().asis("design/in"));

		assertTrue(asis(cxt,"design/in") instanceof Par);
		assertEquals(value((Par)asis(cxt, "design/in")), 25.0);
		assertEquals(value(cxt, "design/in"), 25.0);

		set(dbIn, 30.0); 	// is persisted
		
//		logger.info("value dbIn asis: " + dbIn.asis());
//		logger.info("value dbIn asis design/in 2: " + dbIn.getMappable().asis("design/in"));

		logger.info("value dbIn: " + value(dbIn));
		assertEquals(value(dbIn), 30.0);
		
		// not persistent par
		Par up = par("up", "url", cxt);
		assertEquals(value(up), "myUrl");
		
		set(up, "newUrl");
		assertEquals(value(up), "newUrl");
	}
	
	@Test
	public void aliasedParsTest() throws ContextException, RemoteException {
		Context cxt = context(ent("design/in1", 25.0), ent("design/in2", 35.0));
		
		Par x1 = par("x1", "design/in1", cxt);
		Par x2 = par("x2", "design/in2", cxt);
	
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
				outEnt("result/y", null));
		
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
						outEnt("result/y")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5, sig("service", ServiceJobber.class)), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
		
		
		// context and job parameters
		Par x1p = par("x1p", "arg/x1", c4);
		Par x2p = par("x2p", "arg/x2", c4);
		Par j1p = par("j1p", "j1/t3/result/y", j1);
		
		// setting context parameters in a job
		set(x1p, 10.0);
		set(x2p, 50.0);
		
		// update par references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
//		logger.info("j1 value: " + jobContext(job));
//		logger.info("j1p value: " + value(j1p));
		
		// get job parameter value
		assertEquals(value(j1p), 400.0);
		
		// set job parameter value
		set(j1p, 1000.0);
		assertEquals(value(j1p), 1000.0);
		
		// map pars are aliased pars
		ParModel pc = parModel(x1p, x2p, j1p);
		logger.info("y value: " + value(pc, "y"));
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
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
		
		
		Par c4x1p = par("c4x1p", "arg/x1", c4);
		Par c4x2p = par("c4x2p", "arg/x2", c4);
		// job j1 parameter j1/t3/result/y is used in the context of task t6
		Par j1p = par("j1p", "j1/t3/result/y", j1);
		Par t4x1p = par("t4x1p", "j1/j2/t4/arg/x1", j1);
		
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
	public void conditionalContext() throws RemoteException, ContextException {
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
	public void conditionClosureContext() throws RemoteException, ContextException {
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
	public void conditionalFreeContext() throws RemoteException, ContextException {
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
		ParModel pm = new ParModel();
		Par<Double> x = par("x", 10.0);
		Par<Double> y = par("y", 20.0);
		Par z = par("z", invoker("x - y", x, y));
		
		// set the sphere/radius in the model
		put(pm, "sphere/radius", 20.0);
		// attach the agent to the par-model and invoke
		pm.add(new Agent("getSphereVolume",
				"junit.sorcer.core.invoker.service.Volume", new URL(Sorcer
						.getWebsterUrl() + "/ju-volume-bean.jar")));

		Object val =  get((Context)value(pm,"getSphereVolume"), "sphere/volume");
				 
//		 logger.info("call getSphereVolume:" + get((Context)value(pm,
//				 "getSphereVolume"), "sphere/volume"));
		assertEquals(
				get((Context) value(pm, "getSphereVolume"), "sphere/volume"),
				33510.32163829113);
		assertEquals(
				get((Context) invoke(pm, "getSphereVolume"), "sphere/volume"),
				33510.32163829113);

		// invoke the agent directly
		invoke(pm,
				"getSphereVolume",
				new Agent("getSphereVolume",
						"junit.sorcer.vfe.evaluator.service.Volume", new URL(
								Sorcer.getWebsterUrl() + "/ju-volume-bean.jar")));

//		logger.info("call getSphereVolume:"
//				+ invoke(pm, "getSphereVolume",
//						agent("getSphereVolume",
//								"junit.sorcer.vfe.evaluator.service.Volume",
//								new URL(Sorcer.getWebsterUrl()
//										+ "/ju-volume-bean.jar"))));

		assertEquals(
				get((Context) invoke(pm, "getSphereVolume",
						agent("getSphereVolume",
								"junit.sorcer.vfe.evaluator.service.Volume",
								new URL(Sorcer.getWebsterUrl()
										+ "/ju-volume-bean.jar"))),
						"sphere/volume"), 33510.32163829113);
	}
}
