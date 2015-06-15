package sorcer.pml.modeling;

import groovy.lang.Closure;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Agent;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.pml.provider.impl.Volume;
import sorcer.service.*;
import sorcer.util.Response;
import sorcer.util.Sorcer;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.names;
import static sorcer.co.operator.outPaths;
import static sorcer.co.operator.persistent;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.response;
import static sorcer.mo.operator.responseUp;
import static sorcer.po.operator.add;
import static sorcer.po.operator.asis;
import static sorcer.po.operator.*;
import static sorcer.po.operator.loop;
import static sorcer.po.operator.put;
import static sorcer.po.operator.set;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class ParModels {

	private final static Logger logger = LoggerFactory.getLogger(ParModels.class.getName());

	private ParModel pm;
	private Par<Double> x;
	private Par<Double> y;


	@Before
	public void initParModel() throws Exception {

		pm = new ParModel();
		x = par("x", 10.0);
		y = par("y", 20.0);

	}


	@Test
	public void closingParScope() throws Exception {

		// a par is a variable (entry) evaluated in its own scope (context)
		Par y = par("y",
				invoker("(x1 * x2) - (x3 + x4)", pars("x1", "x2", "x3", "x4")));
		Object val = value(y, ent("x1", 10.0), ent("x2", 50.0),
				ent("x3", 20.0), ent("x4", 80.0));
		// logger.info("y value: " + val);
		assertEquals(val, 400.0);

	}


	@Test
	public void createParModel() throws Exception {

		ParModel model = parModel(
				"Hello Arithmetic Model #1",
				// inputs
				par("x1"), par("x2"), par("x3", 20.0),
				par("x4", 80.0),
				// outputs
				par("t4", invoker("x1 * x2", pars("x1", "x2"))),
				par("t5", invoker("x3 + x4", pars("x3", "x4"))),
				par("j1", invoker("t4 - t5", pars("t4", "t5"))));

		logger.info("model: " + model);

		assertEquals(value(par(model, "t4")), null);

		assertEquals(value(par(model, "t5")), 100.0);

		assertEquals(value(par(model, "j1")), null);

		logger.info("model: " + model);

		value(model, "j1", ent("x1", 10.0), ent("x2", 50.0)).equals(400.0);

		assertTrue(value(model, "j1", ent("x1", 10.0), ent("x2", 50.0)).equals(400.0));

//		// equivalent to the above line
//		assertEquals(
//				value(par(put(model, ent("x1", 10.0), ent("x2", 50.0)), "j1")),
//				400.0);

		assertEquals(value(par(model, "j1")), 400.0);

		// get model response
		Response mr = (Response) value(model, //ent("x1", 10.0), ent("x2", 50.0),
				result("y", outPaths("t4", "t5", "j1")));
		assertTrue(names(mr).equals(list("t4", "t5", "j1")));
		assertTrue(values(mr).equals(list(500.0, 100.0, 400.0)));

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
						task(sig("add", AdderImpl.class),
								cxt("add", inEnt("arg/x3"),
										inEnt("arg/x4"),
										result("result/y")))),
				par("j1", invoker("t4 - t5", pars("t4", "t5"))));

		vm = put(vm, ent("x1", 10.0), ent("x2", 50.0),
				ent("x4", 80.0));

		assertEquals(value(par(vm, "j1")), 400.0);

	}


	@Test
	public void parInvoker() throws Exception {

		ParModel pm = new ParModel("par-model");
		add(pm, ent("x", 10.0));
		add(pm, ent("y", 20.0));
		add(pm, ent("add", invoker("x + y", pars("x", "y"))));

		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		logger.info("add value: " + value(pm, "add"));
		assertEquals(value(pm, "add"), 30.0);

        responseUp(pm, "add");
		logger.info("pm context value: " + value(pm));
		assertEquals(value(pm), 30.0);

		set(pm, "x", 100.0);
		set(pm, "y", 200.0);
		logger.info("add value: " + value(pm, "add"));
		assertEquals(value(pm, "add"), 300.0);

		assertTrue(value(pm, ent("x", 200.0), ent("y", 300.0)).equals(500.0));

	}


	@Test
	public void parModelTest() throws Exception {
		ParModel pm = parModel(par("x", 10.0), par("y", 20.0),
				par("add", invoker("x + y", pars("x", "y"))));

		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);
		assertEquals(value(pm, "add"), 30.0);

		// now evaluate model for its target       
        responseUp(pm, "add");
		assertEquals(value(pm), 30.0);
	}


	@Test
	public void expendingParModelTest() throws Exception {
		ParModel pm = parModel(par("x", 10.0), par("y", 20.0),
				par("add", invoker("x + y", pars("x", "y"))));

		Par x = par(pm, "x");
		logger.info("par x: " + x);
		set(x, 20.0);
		logger.info("val x: " + value(x));
		logger.info("val x: " + value(pm, "x"));

		put(pm, "y", 40.0);

		assertEquals(value(pm, "x"), 20.0);
		assertEquals(value(pm, "y"), 40.0);
		assertEquals(value(pm, "add"), 60.0);

        responseUp(pm, "add");
		assertEquals(value(pm), 60.0);

		add(pm, par("x", 10.0), par("y", 20.0));
		assertEquals(value(pm, "x"), 10.0);
		assertEquals(value(pm, "y"), 20.0);

		assertEquals(value(pm, "add"), 30.0);

		response(pm, "add");
		assertEquals(value(pm), 30.0);

		// with new arguments, closure
		assertTrue(value(pm, par("x", 20.0), par("y", 30.0)).equals(50.0));

		add(pm, par("z", invoker("(x * y) + add", pars("x", "y", "add"))));
		logger.info("z value: " + value(pm, "z"));
		assertEquals(value(pm, "z"), 650.0);

	}

	@Test
	public void parInvokers() throws Exception {

		// all var parameters (x1, y1, y2) are not initialized
		Par y3 = par("y3", invoker("x + y2", pars("x", "y2")));
		Par y2 = par("y2", invoker("x * y1", pars("x", "y1")));
		Par y1 = par("y1", invoker("x1 * 5", par("x1")));

		ParModel pc = parModel(y1, y2, y3);
		// any dependent values or pars can be updated or added any time
		put(pc, "x", 10.0);
		put(pc, "x1", 20.0);

		logger.info("y3 value: " + value(pc, "y3"));
		assertEquals(value(pc, "y3"), 1010.0);
	}

	@Test
	public void entryPersistence() throws Exception {

		Context cxt = context("multiply", dbEnt("arg/x0", 1.0), dbInEnt("arg/x1", 10.0),
				dbOutEnt("arg/x2", 50.0), outEnt("result/y"));

		assertEquals(value(cxt, "arg/x0"), 1.0);
		assertEquals(value(cxt, "arg/x1"), 10.0);
		assertEquals(value(cxt, "arg/x2"), 50.0);

		assertTrue(asis(cxt, "arg/x0") instanceof Par);
		assertTrue(asis(cxt, "arg/x1") instanceof Par);
		assertTrue(asis(cxt, "arg/x2") instanceof Par);

		put(cxt, "arg/x0", 11.0);
		put(cxt, "arg/x1", 110.0);
		put(cxt, "arg/x2", 150.0);

		assertEquals(value(cxt, "arg/x0"), 11.0);
		assertEquals(value(cxt, "arg/x1"), 110.0);
		assertEquals(value(cxt, "arg/x2"), 150.0);

		assertTrue(asis(cxt, "arg/x0") instanceof Par);
		assertTrue(asis(cxt, "arg/x1") instanceof Par);
		assertTrue(asis(cxt, "arg/x2") instanceof Par);
	}

	@Test
	public void argVsParPersistence() throws Exception {

		// persistable just indicates that argument is persistent,
		// for example when value(par) is invoked
		Par dbp1 = persistent(par("design/in", 25.0));
		Par dbp2 = dbPar("url", "myUrl1");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		assertEquals(value(dbp1), 25.0);
		assertEquals(value(dbp2), "myUrl1");

		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store par args in the data store
		URL sUrl = new URL("http://sorcersoft.org");
		Par p1 = par("design/in", 30.0);
		Par p2 = par("url", sUrl);
		URL url1 = storeArg(p1);
		URL url2 = storeArg(p2);
//
//		assertTrue(asis(p1) instanceof URL);
//		assertEquals(content(url1), 30.0);
//		assertEquals(value(p1), 30.0);
//
//		assertTrue(asis(p2) instanceof URL);
//		assertEquals(content(url2), sUrl);
//		assertEquals(value(p2), sUrl);
//
//		// store pars in the data store
//		p1 = par("design/in", 30.0);
//		p2 = par("url", sUrl);
//		URL url3 = store(p1);
//		URL url4 = store(p2);
//
//		assertTrue(asis(p1) instanceof Double);
//		assertEquals(content(url1), 30.0);
//		assertEquals(value(p1), 30.0);
//
//		assertTrue(asis(p2) instanceof URL);
//		assertEquals(content(url2), sUrl);
//		assertEquals(value(p2), sUrl);

	}

	@Test
	public void aliasedParsTest() throws Exception {

		Context cxt = context(ent("design/in1", 25.0), ent("design/in2", 35.0));

		// mapping parameters to cxt, z1 and x2 are par aliases
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
	public void mappableParPersistence() throws Exception {

		Context cxt = context(ent("url", "myUrl"), ent("design/in", 25.0));

		// persistent par
		Par dbIn = persistent(par("dbIn", "design/in", cxt));
		assertEquals(value(dbIn), 25.0);  	// is persisted
		assertEquals(dbIn.asis(), "design/in");
		assertEquals(value((Evaluation)asis(cxt, "design/in")), 25.0);
		assertEquals(value(cxt, "design/in"), 25.0);

		set(dbIn, 30.0); 	// is persisted
		assertEquals(value(dbIn), 30.0);

		// associated context is updated accordingly
		assertEquals(value(cxt, "design/in"), 30.0);
		assertTrue(asis(cxt, "design/in") instanceof Par);
		assertTrue(asis((Par)asis(cxt, "design/in")) instanceof URL);

		// not persistent par
		Par up = par("up", "url", cxt);
		assertEquals(value(up), "myUrl");

		set(up, "newUrl");
		assertEquals(value(up), "newUrl");

	}

	@Test
	public void exertionParsTest() throws Exception {

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
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));


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
	public void associatingContexts() throws Exception {

		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				outEnt("result/y"));

		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				outEnt("result/y"));

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
						outEnt("result/y", null)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5, sig("service", ServiceJobber.class)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));


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

		logger.info("j1 job context: " + upcontext(j1));


		Task t6 = task("t6", sig("add", AdderImpl.class),
				context("add", inEnt("arg/x1", t4x1p), inEnt("arg/x2", j1p),
						outEnt("result/y")));

		Task task = exert(t6);
//		logger.info("t6 context: " + context(task));
		assertEquals(get(task, "result/y"), 410.0);

	}


	@Test
	public void parModelConditions() throws Exception {

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
	public void closingConditions() throws Exception {

		ParModel pm = new ParModel("par-model");
		pm.putValue(Condition._closure_, new ServiceInvoker(pm));
		// free variables, no pars for the invoker
		((ServiceInvoker) pm.get(Condition._closure_))
				.setEvaluator(invoker("{ double x, double y -> x > y }", pars("x", "y")));

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


	@Ignore
	@Test
	public void runnableAttachment() throws Exception {

		ParModel pm = parModel();
		final Par x = par("x", 10.0);
		final Par y = par("y", 20.0);
		Par z = par("z", invoker("x + y", x, y));
		add(pm, x, y, z);

		// update vars x and y that loop condition (var z) depends on
		Runnable update = new Runnable() {
			public void run() {
				try {
					while ((Double)x.getValue() < 60.0) {
						x.setValue((Double)x.getValue() + 1.0);
						y.setValue((Double)y.getValue() + 1.0);
						Thread.sleep(100);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		add(pm, runnableInvoker("thread", update));
		ServiceInvoker vloop = loop("vloop", condition(pm, "{ z -> z < 50 }", "z"), z);
		add(pm, vloop);
		invoke(pm, "thread");

		// the tread above creates a race condition for x and y
		assertEquals((Double)value(pm, "vloop") <= 52.0 && (Double)get(pm, "vloop") > 30.0, true);

	}

	@Test
	public void callableAttachment() throws Exception {

		final ParModel pm = parModel();
		final Par<Double> x = par("x", 10.0);
		final Par<Double> y = par("y", 20.0);
		Par z = par("z", invoker("x + y", x, y));
		add(pm, x, y, z);

		// update vars x and y that loop condition (var z) depends on
		Callable update = new Callable() {
			public Double call() throws ContextException,
					InterruptedException, RemoteException {
				while ((Double) x.getValue() < 60.0) {
					x.setValue((Double) x.getValue() + 1.0);
					y.setValue((Double) y.getValue() + 1.0);
				}
				return (Double)value(x) + (Double)value(y) + (Double)value(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call"), 260.0);

	}

	@Test
	public void callableAttachmentWithArgs() throws Exception {

		final ParModel pm = parModel();
		final Par<Double> x = par("x", 10.0);
		final Par<Double> y = par("y", 20.0);
		Par z = par("z", invoker("x + y", x, y));
		add(pm, x, y, z, par("limit", 60.0));

		// anonymous local class implementing Callable interface
		Callable update = new Callable() {
			public Double call() throws Exception {
				while ((Double) x.getValue() < (Double)value(pm, "limit")) {
					x.setValue((Double) x.getValue() + 1.0);
					y.setValue((Double) y.getValue() + 1.0);
				}
				return (Double)value(x) + (Double)value(y) + (Double)value(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call", context(ent("limit", 100.0))), 420.0);
	}

	// member class implementing Callable interface used below with methodAttachmentWithArgs()
	public class Config implements Callable {

		public Double call() throws Exception {
			while ((Double) x.getValue() < (Double)value(pm, "limit")) {
				x.setValue((Double) x.getValue() + 1.0);
				y.setValue((Double) y.getValue() + 1.0);
			}
			return (Double)value(x) + (Double)value(y) + (Double)value(pm, "z");
		}

	}


	@Test
	public void methodAttachmentWithArgs() throws Exception {

		Par z = par("z", invoker("x + y", x, y));
		add(pm, x, y, z, par("limit", 60.0));

		add(pm, methodInvoker("call", new Config()));
		logger.info("call value:" + invoke(pm, "call"));
		assertEquals(invoke(pm, "call", context(ent("limit", 100.0))), 420.0);

	}

	@Test
	public void attachAgent() throws Exception {

		String sorcerVersion = System.getProperty("sorcer.version");

		// set the sphere/radius in the model
		put(pm, "sphere/radius", 20.0);
		// attach the agent to the par-model and invoke
		pm.add(new Agent("getSphereVolume",
				Volume.class.getName(), new URL(Sorcer
				.getWebsterUrl() + "/pml-"+sorcerVersion+".jar")));

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
