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
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.ent.*;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.pml.provider.impl.Volume;
import sorcer.service.*;
import sorcer.util.Row;
import sorcer.util.Sorcer;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/cml")
public class EntModels {

	private final static Logger logger = LoggerFactory.getLogger(EntModels.class.getName());

	private EntModel em;
	private Pro<Double> x;
	private Pro<Double> y;


	@Before
	public void initEntModel() throws Exception {

		em = new EntModel();
		x = pro("x", 10.0);
		y = pro("y", 20.0);

	}

	@Test
	public void closingEntScope() throws Exception {

		// a pro is a variable (entry) evaluated in its own scope (context)
		Pro y = pro("y",
				invoker("(x1 * x2) - (x3 + x4)", args("x1", "x2", "x3", "x4")));
		Object val = exec(y, val("x1", 10.0), val("x2", 50.0),
                val("x3", 20.0), val("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);

	}

	@Test
	public void closingExprScope() throws Exception {

		// a pro is a variable (entry) evaluated in its own scope (context)
		Pro y = pro("y",
				expr("(x1 * x2) - (x3 + x4)", args("x1", "x2", "x3", "x4")));
		Object val = exec(y, val("x1", 10.0), val("x2", 50.0),
				val("x3", 20.0), val("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);

	}

	@Test
	public void createEntModel() throws Exception {

		EntModel model = entModel(
				"Hello Arithmetic Domain #1",
				// inputs
				val("x1"), val("x2"), val("x3", 20.0),
				val("x4", 80.0),
				// outputs
				pro("t4", invoker("x1 * x2", args("x1", "x2"))),
				pro("t5", invoker("x3 + x4", args("x3", "x4"))),
				pro("j1", invoker("t4 - t5", args("t4", "t5"))));

		assertTrue(exec(model, "t5").equals(100.0));

		assertEquals(exec(model, "j1"), null);

		eval(model, "j1", pro("x1", 10.0), pro("x2", 50.0)).equals(400.0);

		assertTrue(exec(model, "j1", val("x1", 10.0), val("x2", 50.0)).equals(400.0));

		assertTrue(exec(model, "j1").equals(400.0));

		// get model response
		Row mr = (Row) query(model, //pro("x1", 10.0), pro("x2", 50.0),
				result("y", outPaths("t4", "t5", "j1")));
		assertTrue(names(mr).equals(list("t4", "t5", "j1")));
		assertTrue(values(mr).equals(list(500.0, 100.0, 400.0)));
	}

	@Test
	public void createEntModelWithTask() throws Exception {

		EntModel vm = entModel(
			"Hello Arithmetic #2",
			// inputs
			val("x1"), val("x2"), val("x3", 20.0), val("x4"),
			// outputs
			pro("t4", invoker("x1 * x2", args("x1", "x2"))),
			pro("t5",
				task(sig("add", AdderImpl.class),
					cxt("add", inVal("x3"), inVal("x4"),
						result("result/y")))),
			pro("j1", invoker("t4 - t5", args("t4", "t5"))));

		setValues(vm, val("x1", 10.0), val("x2", 50.0),
			val("x4", 80.0));

		assertTrue(exec(vm, "j1").equals(400.0));
	}

    @Test
	public void callInvoker() throws Exception {

		EntModel pm = new EntModel("pro-model");
		add(pm, pro("x", 10.0));
		add(pm, pro("y", 20.0));
		add(pm, pro("add", invoker("x + y", args("x", "y"))));

		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));
		logger.info("add eval: " + exec(pm, "add"));
		assertTrue(exec(pm, "add").equals(30.0));

        responseUp(pm, "add");
		logger.info("em context eval: " + eval(pm));
		assertTrue(value(eval(pm), "add").equals(30.0));

		setValue(pm, "x", 100.0);
		setValue(pm, "y", 200.0);
		logger.info("add eval: " + exec(pm, "add"));
		assertTrue(exec(pm, "add").equals(300.0));

		assertTrue(value(eval(pm, val("x", 200.0), val("y", 300.0)), "add").equals(500.0));

	}


	@Test
	public void callModelTest() throws Exception {
		EntModel pm = entModel(pro("x", 10.0), pro("y", 20.0),
				pro("add", invoker("x + y", args("x", "y"))));

		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));
		assertTrue(exec(pm, "add").equals(30.0));

		// now evaluee model for its responses
        responseUp(pm, "add");
		assertEquals(value(eval(pm), "add"), 30.0);
	}


	@Test
	public void expendingCallModelTest() throws Exception {
		EntModel pm = entModel(pro("x", 10.0), pro("y", 20.0),
				pro("add", invoker("x + y", args("x", "y"))));

		Pro x = pro(pm, "x");
		logger.info("pro x: " + x);
		setValue(x, 20.0);
		logger.info("val x: " + exec(x));
		logger.info("val x: " + eval(pm, "x"));

		put(pm, "y", 40.0);

		assertTrue(exec(pm, "x").equals(20.0));
		assertTrue(exec(pm, "y").equals(40.0));
		assertTrue(exec(pm, "add").equals(60.0));

        responseUp(pm, "add");
		assertEquals(value(eval(pm), "add"), 60.0);

		add(pm, pro("x", 10.0), pro("y", 20.0));
		assertTrue(exec(pm, "x").equals(10.0));
		assertTrue(exec(pm, "y").equals(20.0));

		assertTrue(exec(pm, "add").equals(30.0));

		Object out = exec(pm, "add");
		assertTrue(out.equals(30.0));
		assertTrue(value(eval(pm), "add").equals(30.0));

		// with new arguments, closure
		assertTrue(value(eval(pm, pro("x", 20.0), pro("y", 30.0)), "add").equals(50.0));

		add(pm, pro("z", invoker("(x * y) + add", args("x", "y", "add"))));
		logger.info("z eval: " + eval(pm, "z"));
		assertTrue(exec(pm, "z").equals(650.0));

	}


	@Test
	public void callInvokers() throws Exception {

		// all var parameters (x1, y1, y2) are not initialized
		Pro y3 = pro("y3", invoker("x + y2", args("x", "y2")));
		Pro y2 = pro("y2", invoker("x * y1", args("x", "y1")));
		Pro y1 = pro("y1", invoker("x1 * 5", args("x1")));

		EntModel pc = entModel(y1, y2, y3);
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

        assertTrue(asis(cxt, "arg/x0") instanceof Pro);
        assertTrue(asis(cxt, "arg/x1") instanceof Pro);
        assertTrue(asis(cxt, "arg/x2") instanceof Pro);

        put(cxt, "arg/x0", 11.0);
        put(cxt, "arg/x1", 110.0);
        put(cxt, "arg/x2", 150.0);

        assertEquals(value(cxt, "arg/x0"), 11.0);
        assertEquals(value(cxt, "arg/x1"), 110.0);
        assertEquals(value(cxt, "arg/x2"), 150.0);

        assertTrue(asis(cxt, "arg/x0") instanceof Pro);
        assertTrue(asis(cxt, "arg/x1") instanceof Pro);
        assertTrue(asis(cxt, "arg/x2") instanceof Pro);
    }


	@Test
	public void argVsEntPersistence() throws Exception {

		// persistable just indicates that argument is persistent,
		// for example when eval(pro) is invoked
		Pro dbp1 = persistent(pro("design/in", 25.0));
		Pro dbp2 = dbEnt("url", "myUrl1");

		assertFalse(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		assertTrue(exec(dbp1).equals(25.0));
		assertTrue(exec(dbp2).equals("myUrl1"));

		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);

		// store pro args in the data store
		URL sUrl = new URL("http://sorcersoft.org");
		Pro p1 = pro("design/in", 30.0);
		Pro p2 = pro("url", sUrl);
		URL url1 = storeVal(p1);
		URL url2 = storeVal(p2);

		assertTrue(asis(p1) instanceof URL);
		assertEquals(content(url1), 30.0);
		assertEquals(exec(p1), 30.0);

		assertTrue(asis(p2) instanceof URL);
		assertEquals(content(url2), sUrl);
		assertEquals(exec(p2), sUrl);

		// store args in the data store
		p1 = pro("design/in", 30.0);
		p2 = pro("url", sUrl);
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
	public void entModelConditions() throws Exception {

		final EntModel pm = new EntModel("pro-model");
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

		EntModel pm = new EntModel("pro-model");
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

		EntModel pm = entModel("pro-model");
		add(pm, pro("x", 1));
		add(pm, pro("y", invoker("x + 1", args("x"))));
		add(pm, pro("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(exec(pm, "iloop"), 48);

	}


	@Test
	public void callableAttachment() throws Exception {

		final EntModel pm = entModel();
		final Pro<Double> x = pro("x", 10.0);
		final Pro<Double> y = pro("y", 20.0);
		Pro z = pro("z", invoker("x + y", x, y));
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

		add(pm, callableInvoker("pro", update));
		assertEquals(invoke(pm, "pro"), 260.0);

	}


	@Test
	public void callableAttachmentWithArgs() throws Exception {

		final EntModel pm = entModel();
		final Pro<Double> x = pro("x", 10.0);
		final Pro<Double> y = pro("y", 20.0);
		Pro z = pro("z", invoker("x + y", x, y));
		add(pm, x, y, z, pro("limit", 60.0));

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

		add(pm, callableInvoker("pro", update));
		assertEquals(invoke(pm, "pro", context(pro("limit", 100.0))), 420.0);
	}


	// member class implementing Callable interface used below with methodAttachmentWithArgs()
	public class Config implements Callable {

		public Double call() throws Exception {
			while (x.evaluate() < (Double)value(em, "limit")) {
				x.setValue(x.evaluate() + 1.0);
				y.setValue(y.evaluate() + 1.0);
			}
			logger.info("x: " + x.evaluate());
			logger.info("y: " + y.evaluate());
			logger.info("z: " + value(em, "z"));
			return exec(x) + exec(y) + (Double)value(em, "z");
		}

	}


	@Test
	public void attachMethodInvokerWithContext() throws Exception {

		Pro z = pro("z", invoker("x + y", x, y));
		add(em, x, y, z, val("limit", 60.0));

		add(em, methodInvoker("call", new Config()));
//		logger.info("pro eval:" + invoke(em, "pro"));
		assertEquals(invoke(em, "call", context(val("limit", 100.0))), 420.0);

	}


	@Test
	public void attachAgent() throws Exception {

		String sorcerVersion = System.getProperty("sorcer.version");

		// set the sphere/radius in the model
		put(em, "sphere/radius", 20.0);
		// attach the agent to the pro-model and invoke
        add(em, agent("getSphereVolume",
                Volume.class.getName(), new URL(Sorcer
                        .getWebsterUrl() + "/pml-" + sorcerVersion+".jar")));

		Object result = value((Context)value(em,"getSphereVolume"), "sphere/volume");
		logger.info("result: " +result);

		assertTrue(result.equals(33510.32163829113));

		// invoke the agent directly
//		invoke(em,
//				"getSphereVolume",
//                agent("getSphereVolume",
//                        "Volume",
//                        new URL(Sorcer.getWebsterUrl()
//                                + "/sorcer-tester-" + sorcerVersion+".jar")));
//
////		logger.info("val: " + eval(em, "sphere/volume"));
//		assertTrue(get(em, "sphere/volume").equals(33510.32163829113));

	}

}
