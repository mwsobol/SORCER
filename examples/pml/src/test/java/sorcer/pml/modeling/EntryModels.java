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
import sorcer.core.context.model.ent.*;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.pml.provider.impl.Volume;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ent;
import sorcer.service.modeling.func;
import sorcer.service.modeling.val;
import sorcer.util.Row;
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
public class EntryModels {

	private final static Logger logger = LoggerFactory.getLogger(EntryModels.class.getName());

	private EntryModel pm;
	private Proc<Double> x;
	private Proc<Double> y;


	@Before
	public void initProcModel() throws Exception {

		pm = new EntryModel();
		x = proc("x", 10.0);
		y = proc("y", 20.0);

	}

	@Test
	public void closingProcScope() throws Exception {

		// a proc is a variable (entry) evaluated in its own scope (context)
		Proc y = proc("y",
				invoker("(x1 * x2) - (x3 + x4)", args("x1", "x2", "x3", "x4")));
		Object val = exec(y, proc("x1", 10.0), proc("x2", 50.0),
				proc("x3", 20.0), proc("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);

	}


	@Test
	public void createProcModel() throws Exception {

		EntryModel model = procModel(
				"Hello Arithmetic Domain #1",
				// inputs
				val("x1"), val("x2"), val("x3", 20.0),
				val("x4", 80.0),
				// outputs
				proc("t4", invoker("x1 * x2", args("x1", "x2"))),
				proc("t5", invoker("x3 + x4", args("x3", "x4"))),
				proc("j1", invoker("t4 - t5", args("t4", "t5"))));

		assertTrue(exec(model, "t5").equals(100.0));

		assertEquals(exec(model, "j1"), null);

		eval(model, "j1", proc("x1", 10.0), proc("x2", 50.0)).equals(400.0);

		assertTrue(exec(model, "j1", val("x1", 10.0), val("x2", 50.0)).equals(400.0));

		assertTrue(exec(model, "j1").equals(400.0));

		// get model response
		Row mr = (Row) query(model, //proc("x1", 10.0), proc("x2", 50.0),
				result("y", outPaths("t4", "t5", "j1")));
		assertTrue(names(mr).equals(list("t4", "t5", "j1")));
		assertTrue(values(mr).equals(list(500.0, 100.0, 400.0)));
	}

	@Test
	public void createProcModelWithTask() throws Exception {

		EntryModel vm = procModel(
			"Hello Arithmetic #2",
			// inputs
			val("x1"), val("x2"), val("x3", 20.0), val("x4"),
			// outputs
			proc("t4", invoker("x1 * x2", args("x1", "x2"))),
			proc("t5",
				task(sig("add", AdderImpl.class),
					cxt("add", inVal("x3"), inVal("x4"),
						result("result/y")))),
			proc("j1", invoker("t4 - t5", args("t4", "t5"))));

		setValues(vm, val("x1", 10.0), val("x2", 50.0),
			val("x4", 80.0));

		assertTrue(exec(vm, "j1").equals(400.0));
	}

    @Test
	public void procInvoker() throws Exception {

		EntryModel pm = new EntryModel("proc-model");
		add(pm, proc("x", 10.0));
		add(pm, proc("y", 20.0));
		add(pm, proc("add", invoker("x + y", args("x", "y"))));

		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));
		logger.info("add eval: " + exec(pm, "add"));
		assertTrue(exec(pm, "add").equals(30.0));

        responseUp(pm, "add");
		logger.info("pm context eval: " + eval(pm));
		assertTrue(value(eval(pm), "add").equals(30.0));

		setValue(pm, "x", 100.0);
		setValue(pm, "y", 200.0);
		logger.info("add eval: " + exec(pm, "add"));
		assertTrue(exec(pm, "add").equals(300.0));

		assertTrue(value(eval(pm, val("x", 200.0), val("y", 300.0)), "add").equals(500.0));

	}


	@Test
	public void procModelTest() throws Exception {
		EntryModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));
		assertTrue(exec(pm, "add").equals(30.0));

		// now evaluee model for its responses
        responseUp(pm, "add");
		assertEquals(value(eval(pm), "add"), 30.0);
	}


	@Test
	public void expendingProcModelTest() throws Exception {
		EntryModel pm = procModel(proc("x", 10.0), proc("y", 20.0),
				proc("add", invoker("x + y", args("x", "y"))));

		Proc x = proc(pm, "x");
		logger.info("proc x: " + x);
		setValue(x, 20.0);
		logger.info("val x: " + exec(x));
		logger.info("val x: " + eval(pm, "x"));

		put(pm, "y", 40.0);

		assertTrue(exec(pm, "x").equals(20.0));
		assertTrue(exec(pm, "y").equals(40.0));
		assertTrue(exec(pm, "add").equals(60.0));

        responseUp(pm, "add");
		assertEquals(value(eval(pm), "add"), 60.0);

		add(pm, proc("x", 10.0), proc("y", 20.0));
		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));

		assertTrue(exec(pm, "add").equals(30.0));

		Object out = exec(pm, "add");
		assertTrue(out.equals(30.0));
		assertTrue(value(eval(pm), "add").equals(30.0));

		// with new arguments, closure
		assertTrue(value(eval(pm, proc("x", 20.0), proc("y", 30.0)), "add").equals(50.0));

		add(pm, proc("z", invoker("(x * y) + add", args("x", "y", "add"))));
		logger.info("z eval: " + eval(pm, "z"));
		assertTrue(exec(pm, "z").equals(650.0));

	}


	@Test
	public void parInvokers() throws Exception {

		// all var parameters (x1, y1, y2) are not initialized
		Proc y3 = proc("y3", invoker("x + y2", args("x", "y2")));
		Proc y2 = proc("y2", invoker("x * y1", args("x", "y1")));
		Proc y1 = proc("y1", invoker("x1 * 5", args("x1")));

		EntryModel pc = procModel(y1, y2, y3);
		// any dependent values or args can be updated or added any time
		put(pc, "x", 10.0);
		put(pc, "x1", 20.0);

		logger.info("y3 eval: " + exec(pc, "y3"));
		assertEquals(exec(pc, "y3"), 1010.0);
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

		assertTrue(exec(dbp1).equals(25.0));
		assertTrue(exec(dbp2).equals("myUrl1"));

		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store proc args in the data store
		URL sUrl = new URL("http://sorcersoft.org");
		Proc p1 = proc("design/in", 30.0);
		Proc p2 = proc("url", sUrl);
		URL url1 = storeVal(p1);
		URL url2 = storeVal(p2);

		assertTrue(asis(p1) instanceof URL);
		assertEquals(content(url1), 30.0);
		assertEquals(exec(p1), 30.0);

		assertTrue(asis(p2) instanceof URL);
		assertEquals(content(url2), sUrl);
		assertEquals(exec(p2), sUrl);

		// store args in the data store
		p1 = proc("design/in", 30.0);
		p2 = proc("url", sUrl);
		store(p1);
		store(p2);

		assertTrue(p1.getOut() instanceof Double);
		assertEquals(content(url1), 30.0);
		assertEquals(exec(p1), 30.0);

		assertTrue(p2.getOut() instanceof URL);
		assertEquals(content(url2), sUrl);
		assertEquals(exec(p2), sUrl);
	}

	@Test
	public void procModelConditions() throws Exception {

		final EntryModel pm = new EntryModel("proc-model");
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

		EntryModel pm = new EntryModel("proc-model");
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
				Closure c = null;
				try {
					c = (Closure)conditionalContext.getValue(Condition._closure_);
					Object[] args = new Object[] { conditionalContext.getValue("x"),
						conditionalContext.getValue("y") };
					return (Boolean) c.call(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
		};

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		assertEquals(eval.evaluate(), false);

		pm.putValue("x", 20.0);
		pm.putValue("y", 10.0);
		assertEquals(eval.evaluate(), true);

	}


	@Test
	public void invokerLoopTest() throws Exception {

		EntryModel pm = procModel("proc-model");
		add(pm, proc("x", 1));
		add(pm, proc("y", invoker("x + 1", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(exec(pm, "iloop"), 48);

	}


	@Test
	public void callableAttachment() throws Exception {

		final EntryModel pm = procModel();
		final Proc<Double> x = proc("x", 10.0);
		final Proc<Double> y = proc("y", 20.0);
		Proc z = proc("z", invoker("x + y", x, y));
		add(pm, x, y, z);

		// update vars x and y that loop condition (var z) depends on
		Callable update = new Callable() {
			public Double call() throws ContextException,
					InterruptedException, RemoteException {
				while ((Double) x.evaluate() < 60.0) {
					x.setValue((Double) x.evaluate() + 1.0);
					y.setValue((Double) y.evaluate() + 1.0);
				}
				return (Double) exec(x) + (Double) exec(y) + (Double)exec(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call"), 260.0);

	}


	@Test
	public void callableAttachmentWithArgs() throws Exception {

		final EntryModel pm = procModel();
		final Proc<Double> x = proc("x", 10.0);
		final Proc<Double> y = proc("y", 20.0);
		Proc z = proc("z", invoker("x + y", x, y));
		add(pm, x, y, z, proc("limit", 60.0));

		// anonymous local class implementing Callable interface
		Callable update = new Callable() {
			public Double call() throws Exception {
				while (x.evaluate() < (Double)value(pm, "limit")) {
					x.setValue(x.evaluate() + 1.0);
					y.setValue(y.evaluate() + 1.0);
				}
				return exec(x) + exec(y) + (Double)value(pm, "z");
			}
		};

		add(pm, callableInvoker("call", update));
		assertEquals(invoke(pm, "call", context(proc("limit", 100.0))), 420.0);
	}


	// member class implementing Callable interface used below with methodAttachmentWithArgs()
	public class Config implements Callable {

		public Double call() throws Exception {
			while (x.evaluate() < (Double)value(pm, "limit")) {
				x.setValue(x.evaluate() + 1.0);
				y.setValue(y.evaluate() + 1.0);
			}
			logger.info("x: " + x.evaluate());
			logger.info("y: " + y.evaluate());
			logger.info("z: " + value(pm, "z"));
			return exec(x) + exec(y) + (Double)value(pm, "z");
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
//		invoke(pm,
//				"getSphereVolume",
//                agent("getSphereVolume",
//                        "sorcer.pml.provider.impl.Volume",
//                        new URL(Sorcer.getWebsterUrl()
//                                + "/sorcer-tester-" + sorcerVersion+".jar")));
//
////		logger.info("val: " + eval(pm, "sphere/volume"));
//		assertTrue(value(pm, "sphere/volume").equals(33510.32163829113));

	}

}
