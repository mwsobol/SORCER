package sorcer.pml.modeling;

import groovy.lang.Closure;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.context.model.ent.*;
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

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class ProcModels {

	private final static Logger logger = LoggerFactory.getLogger(ProcModels.class.getName());

	private ProcModel pm;
	private Proc<Double> x;
	private Proc<Double> y;


	@Before
	public void initProcModel() throws Exception {

		pm = new ProcModel();
		x = proc("x", 10.0);
		y = proc("y", 20.0);

	}


	@Test
	public void closingProcScope() throws Exception {

		// a proc is a variable (entry) evaluated in its own scope (context)
		Proc y = proc("y",
				invoker("(x1 * x2) - (x3 + x4)", args("x1", "x2", "x3", "x4")));
		Object val = eval(y, proc("x1", 10.0), proc("x2", 50.0),
				proc("x3", 20.0), proc("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);

	}


	@Test
	public void createProcModel() throws Exception {

		ProcModel model = procModel(
				"Hello Arithmetic Domain #1",
				// inputs
				ent("x1"), ent("x2"), proc("x3", 20.0),
				proc("x4", 80.0),
				// outputs
				proc("t4", invoker("x1 * x2", args("x1", "x2"))),
				proc("t5", invoker("x3 + x4", args("x3", "x4"))),
				proc("j1", invoker("t4 - t5", args("t4", "t5"))));

		logger.info("model: " + model);

		assertEquals(eval(proc(model, "t4")), null);

		assertTrue(eval(proc(model, "t5")).equals(100.0));

		assertEquals(eval(proc(model, "j1")), null);

		logger.info("model: " + model);

//		eval(model, "j1", proc("x1", 10.0), proc("x2", 50.0)).equals(400.0);

		assertTrue(eval(model, "j1", val("x1", 10.0), val("x2", 50.0)).equals(400.0));

//		// equivalent to the above line
//		assertEquals(
//				eval(proc(put(model, proc("x1", 10.0), proc("x2", 50.0)), "j1")),
//				400.0);

		assertTrue(eval(proc(model, "j1")).equals(400.0));

		// get model response
		Response mr = (Response) eval(model, //proc("x1", 10.0), proc("x2", 50.0),
				result("y", outPaths("t4", "t5", "j1")));
		assertTrue(names(mr).equals(list("t4", "t5", "j1")));
		assertTrue(values(mr).equals(list(500.0, 100.0, 400.0)));

	}


	@Test
	public void createMogram() throws Exception {

		ProcModel vm = procModel(
				"Hello Arithmetic #2",
				// inputs
				ent("x1"), ent("x2"), proc("x3", 20.0), ent("x4"),
				// outputs
				proc("t4", invoker("x1 * x2", args("x1", "x2"))),
				proc("t5",
						task(sig("add", AdderImpl.class),
								cxt("add", inVal("arg/x3"),
										inVal("arg/x4"),
										result("result/y")))),
				proc("j1", invoker("t4 - t5", args("t4", "t5"))));

		vm = put(vm, proc("x1", 10.0), proc("x2", 50.0),
				proc("x4", 80.0));

		assertTrue(eval(proc(vm, "j1")).equals(400.0));

	}


	@Test
	public void procInvoker() throws Exception {

		ProcModel pm = new ProcModel("proc-model");
		add(pm, proc("x", 10.0));
		add(pm, proc("y", 20.0));
		add(pm, proc("add", invoker("x + y", args("x", "y"))));

		assertTrue(eval(pm, "x").equals(10.0));
		assertTrue(eval(pm, "y").equals(20.0));
		logger.info("add eval: " + eval(pm, "add"));
		assertTrue(eval(pm, "add").equals(30.0));

        responseUp(pm, "add");
		logger.info("pm context eval: " + eval(pm));
		assertTrue(eval(pm).equals(30.0));

		setValue(pm, "x", 100.0);
		setValue(pm, "y", 200.0);
		logger.info("add eval: " + eval(pm, "add"));
		assertTrue(eval(pm, "add").equals(300.0));

		assertTrue(eval(pm, val("x", 200.0), val("y", 300.0)).equals(500.0));

	}


	@Test
	public void procModelTest() throws Exception {
		ProcModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		assertTrue(eval(pm, "x").equals(10.0));
		assertTrue(eval(pm, "y").equals(20.0));
		assertTrue(eval(pm, "add").equals(30.0));

		// now evaluate model for its target       
        responseUp(pm, "add");
		assertEquals(eval(pm), 30.0);
	}


	@Test
	public void expendingProcModelTest() throws Exception {
		ProcModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		Proc x = proc(pm, "x");
		logger.info("proc x: " + x);
		setValue(x, 20.0);
		logger.info("val x: " + eval(x));
		logger.info("val x: " + eval(pm, "x"));

		put(pm, "y", 40.0);

		assertTrue(eval(pm, "x").equals(20.0));
		assertTrue(eval(pm, "y").equals(40.0));
		assertTrue(eval(pm, "add").equals(60.0));

        responseUp(pm, "add");
		assertEquals(eval(pm), 60.0);

		add(pm, proc("x", 10.0), proc("y", 20.0));
		assertTrue(eval(pm, "x").equals(10.0));
		assertTrue(eval(pm, "y").equals(20.0));

		assertTrue(eval(pm, "add").equals(30.0));

		Object out = response(pm, "add");
		assertTrue(out.equals(30.0));
		assertTrue(eval(pm).equals(30.0));

		// with new arguments, closure
		assertTrue(eval(pm, proc("x", 20.0), proc("y", 30.0)).equals(50.0));

		add(pm, proc("z", invoker("(x * y) + add", args("x", "y", "add"))));
		logger.info("z eval: " + eval(pm, "z"));
		assertTrue(eval(pm, "z").equals(650.0));

	}


	@Test
	public void parInvokers() throws Exception {

		// all var parameters (x1, y1, y2) are not initialized
		Proc y3 = proc("y3", invoker("x + y2", args("x", "y2")));
		Proc y2 = proc("y2", invoker("x * y1", args("x", "y1")));
		Proc y1 = proc("y1", invoker("x1 * 5", ent("x1")));

		ProcModel pc = procModel(y1, y2, y3);
		// any dependent values or args can be updated or added any time
		put(pc, "x", 10.0);
		put(pc, "x1", 20.0);

		logger.info("y3 eval: " + eval(pc, "y3"));
		assertEquals(eval(pc, "y3"), 1010.0);
	}


	@Test
	public void entryPersistence() throws Exception {

		Context cxt = context("multiply", dbVal("arg/x0", 1.0), dbInVal("arg/x1", 10.0),
				dbOutVal("arg/x2", 50.0), outVal("result/y"));

		assertEquals(value(cxt, "arg/x0"), 1.0);
		assertEquals(value(cxt, "arg/x1"), 10.0);
		assertEquals(value(cxt, "arg/x2"), 50.0);

		assertTrue(asis(cxt, "arg/x0") instanceof Proc);
		assertTrue(asis(cxt, "arg/x1") instanceof Proc);
		assertTrue(asis(cxt, "arg/x2") instanceof Proc);

		put(cxt, "arg/x0", 11.0);
		put(cxt, "arg/x1", 110.0);
		put(cxt, "arg/x2", 150.0);

		assertEquals(value(cxt, "arg/x0"), 11.0);
		assertEquals(value(cxt, "arg/x1"), 110.0);
		assertEquals(value(cxt, "arg/x2"), 150.0);

		assertTrue(asis(cxt, "arg/x0") instanceof Proc);
		assertTrue(asis(cxt, "arg/x1") instanceof Proc);
		assertTrue(asis(cxt, "arg/x2") instanceof Proc);
	}


	@Test
	public void argVsProcPersistence() throws Exception {

		// persistable just indicates that argument is persistent,
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
		URL sUrl = new URL("http://sorcersoft.org");
		Proc p1 = proc("design/in", 30.0);
		Proc p2 = proc("url", sUrl);
		URL url1 = storeVal(p1);
		URL url2 = storeVal(p2);
//
//		assertTrue(asis(p1) instanceof URL);
//		assertEquals(content(url1), 30.0);
//		assertEquals(eval(p1), 30.0);
//
//		assertTrue(asis(p2) instanceof URL);
//		assertEquals(content(url2), sUrl);
//		assertEquals(eval(p2), sUrl);
//
//		// store args in the data store
//		p1 = proc("design/in", 30.0);
//		p2 = proc("url", sUrl);
//		URL url3 = store(p1);
//		URL url4 = store(p2);
//
//		assertTrue(asis(p1) instanceof Double);
//		assertEquals(content(url1), 30.0);
//		assertEquals(eval(p1), 30.0);
//
//		assertTrue(asis(p2) instanceof URL);
//		assertEquals(content(url2), sUrl);
//		assertEquals(eval(p2), sUrl);

	}


	@Test
	public void aliasedProcsTest() throws Exception {

		Context cxt = context(proc("design/in1", 25.0), proc("design/in2", 35.0));

		// mapping parameters to cxt, x1 and x2 are proc aliases
		Proc x1 = proc(cxt, "x1", "design/in1");
		Proc x2 = as(proc("x2", "design/in2"), cxt);

		assertTrue(eval(x1).equals(25.0));
		setValue(x1, 45.0);
		assertTrue(eval(x1).equals(45.0));

		assertTrue(eval(x2).equals(35.0));
		setValue(x2, 55.0);
		assertTrue(eval(x2).equals(55.0));

		ProcModel pc = procModel(x1, x2);
		assertTrue(eval(pc, "x1").equals(45.0));
		assertTrue(eval(pc, "x2").equals(55.0));

	}

	@Test
	public void mappableProcPersistence() throws Exception {

		Context cxt = context(val("url", "htt://sorcersoft.org"), val("design/in", 25.0));

		// persistent proc
		Entry dbIn = persistent(as(proc("dbIn", "design/in"), cxt));
		assertTrue(eval(dbIn).equals(25.0));  	// is persisted
		assertTrue(dbIn.asis().equals("design/in"));
		assertTrue(eval((Entry) asis(cxt, "design/in")).equals(25.0));
		assertTrue(value(cxt, "design/in").equals(25.0));

		setValue((Value)dbIn, 30.0); 	// is persisted
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
	public void exertionProcs() throws Exception {

		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				outVal("result/y"));

		Context c5 = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				outVal("result/y", null));

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
						outVal("result/y")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);

		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5, sig("exert", ServiceJobber.class)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));


		// context and job parameters
		Proc x1p = as(proc("x1p", "arg/x1"), c4);
		Proc x2p = as(proc("x2p", "arg/x2"), c4);
		Proc j1p = as(proc("j1p", "j1/t3/result/y"), j1);

		// setting context parameters in a job
		setValue(x1p, 10.0);
		setValue(x2p, 50.0);

		// update proc references
		j1 = exert(j1);
		c4 = taskContext("j1/t4", j1);
//		logger.info("j1 eval: " + jobContext(job));
//		logger.info("j1p eval: " + eval(j1p));

		// get job parameter eval
		assertTrue(eval(j1p).equals(400.0));

		// set job parameter eval
		setValue(j1p, 1000.0);
		assertTrue(eval(j1p).equals(1000.0));

		// map args are aliased args
		ProcModel pc = procModel(x1p, x2p, j1p);
		logger.info("y eval: " + eval(pc, "y"));

	}


	@Test
	public void associatingContexts() throws Exception {

		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				outVal("result/y"));

		Context c5 = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				outVal("result/y"));

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
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
		assertTrue(get(task, "result/y").equals(410.0));

	}


	@Test
	public void procModelConditions() throws Exception {

		final ProcModel pm = new ProcModel("proc-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		Condition flag = new Condition(pm,
				cxt -> (double)value(cxt, "x") > (double)value(cxt, "y") );

		assertTrue(pm.getValue("x").equals(10.0));
		assertTrue(pm.getValue("y").equals(20.0));
		logger.info("condition eval: " + flag.isTrue());
		assertEquals(flag.isTrue(), false);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("condition eval: " + flag.isTrue());
		assertEquals(flag.isTrue(), true);
	}


	@Test
	public void closingConditions() throws Exception {

		ProcModel pm = new ProcModel("proc-model");
		pm.putValue(Condition._closure_, new ServiceInvoker(pm));
		// free variables, no args for the invoker
		((ServiceInvoker) pm.get(Condition._closure_))
				.setEvaluator(invoker("{ double x, double y -> x > y }", args("x", "y")));

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
	public void invokerLoopTest() throws Exception {

		ProcModel pm = procModel("proc-model");
		add(pm, proc("x", 1));
		add(pm, proc("y", invoker("x + 1", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(eval(pm, "iloop"), 48);

	}


	@Test
	public void callableAttachment() throws Exception {

		final ProcModel pm = procModel();
		final Proc<Double> x = proc("x", 10.0);
		final Proc<Double> y = proc("y", 20.0);
		Proc z = proc("z", invoker("x + y", x, y));
		add(pm, x, y, z);

		// update vars x and y that loop condition (var z) depends on
		Callable update = new Callable() {
			public Double call() throws ContextException,
					InterruptedException, RemoteException {
				while ((Double) x.getValue() < 60.0) {
					x.setValue((Double) x.getValue() + 1.0);
					y.setValue((Double) y.getValue() + 1.0);
				}
				return (Double) eval(x) + (Double) eval(y) + (Double)eval(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call"), 260.0);

	}


	@Test
	public void callableAttachmentWithArgs() throws Exception {

		final ProcModel pm = procModel();
		final Proc<Double> x = proc("x", 10.0);
		final Proc<Double> y = proc("y", 20.0);
		Proc z = proc("z", invoker("x + y", x, y));
		add(pm, x, y, z, proc("limit", 60.0));

		// anonymous local class implementing Callable interface
		Callable update = new Callable() {
			public Double call() throws Exception {
				while (x.getValue() < (Double)value(pm, "limit")) {
					x.setValue(x.getValue() + 1.0);
					y.setValue(y.getValue() + 1.0);
				}
				return eval(x) + eval(y) + (Double)value(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call", context(proc("limit", 100.0))), 420.0);
	}


	// member class implementing Callable interface used below with methodAttachmentWithArgs()
	public class Config implements Callable {

		public Double call() throws Exception {
			while (x.getValue() < (Double)value(pm, "limit")) {
				x.setValue(x.getValue() + 1.0);
				y.setValue(y.getValue() + 1.0);
			}
			logger.info("x: " + x.getValue());
			logger.info("y: " + y.getValue());
			logger.info("z: " + value(pm, "z"));
			return eval(x) + eval(y) + (Double)value(pm, "z");
		}

	}


	@Test
	public void attachMethodInvokerWithContext() throws Exception {

		Proc z = proc("z", invoker("x + y", x, y));
		add(pm, x, y, z, proc("limit", 60.0));

		add(pm, methodInvoker("call", new Config()));
//		logger.info("call eval:" + invoke(pm, "call"));
		assertEquals(invoke(pm, "call", context(proc("limit", 100.0))), 420.0);

	}


	@Test
	public void attachAgent() throws Exception {

		String sorcerVersion = System.getProperty("sorcer.version");

		// set the sphere/radius in the model
		put(pm, "sphere/radius", 20.0);
		// attach the agent to the proc-model and invoke
        add(pm, agent("getSphereVolume",
                Volume.class.getName(), new URL(Sorcer
                        .getWebsterUrl() + "/pml-" + sorcerVersion+".jar")));

		Object result = value((Context)value(pm,"getSphereVolume"), "sphere/volume");
		logger.info("result: " +result);

		assertTrue(result.equals(33510.32163829113));

		// invoke the agent directly
		invoke(pm,
				"getSphereVolume",
                agent("getSphereVolume",
                        "sorcer.arithmetic.tester.volume.Volume",
                        new URL(Sorcer.getWebsterUrl()
                                + "/sorcer-tester-" + sorcerVersion+".jar")));

//		logger.info("val: " + eval(pm, "sphere/volume"));
		assertTrue(value(pm, "sphere/volume").equals(33510.32163829113));

	}

}
