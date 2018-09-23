package sorcer.cml.modeling;

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
import sorcer.core.context.model.ent.EntryModel;
import sorcer.core.context.model.ent.Call;
import sorcer.core.invoker.AltInvoker;
import sorcer.core.invoker.Updater;
import sorcer.core.invoker.OptInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.pipe;
import static sorcer.ent.operator.alt;
import static sorcer.ent.operator.*;
import static sorcer.ent.operator.get;
import static sorcer.ent.operator.loop;
import static sorcer.ent.operator.opt;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/cml")
public class Invokers {
	private final static Logger logger = LoggerFactory.getLogger(Invokers.class);

	private EntryModel em;
	private Call x;
	private Call y;
	private Call z;

	/// member subclass of Updater with Context parameter used below with
	// contextMethodAttachmentWithArgs()
	// there are constructor's context and invoke metod's context as parameters
	public class ContextUpdater extends Updater {
		public ContextUpdater(Context context) {
			super(context);
		}

		public Double update(Context arg) throws Exception {
			setValue(x, value(arg, "x"));
			setValue(y, value(context, "y"));
			// x set from 'arg'
			assertTrue(exec(x).equals(200.0));
			// y set from construtor's context 'in'
			assertTrue(exec(y).equals(30.0));
			assertTrue(exec(z).equals(170.0));
			return (double)exec(x) + (double)exec(y) + (double)value(em, "z");
		}
	};

	@Before
	public void initProcModel() throws Exception {
		em = new EntryModel();
		x = call("x", 10.0);
		y = call("y", 20.0);
		z = call("z", invoker("x - y", args("x", "y")));
	}

	@Test
	public void lambdaInvoker() throws Exception {

		Model mo = model(val("x", 10.0), val("y", 20.0),
				call(invoker("lambda",
					(Context<Double> cxt) -> value(cxt, "x") + value(cxt, "y") + 30,
					args("x", "y"))));
//		logger.info("invoke eval: " + eval(mo, "lambda"));
		assertEquals(exec(mo, "lambda"), 60.0);
	}

	@Test
	public void objectMethodInvoker() throws Exception {
		setValue(x, 10.0);
		setValue(y, 20.0);
		add(em, x, y, z);

//		logger.info("x:" + eval(em, "x"));
//		logger.info("y:" + eval(em, "y"));
//		logger.info("y:" + eval(em, "z"));

		Context in = context(val("x", 20.0), val("y", 30.0));
		Context arg = context(val("x", 200.0), val("y", 300.0));
		add(em, methodInvoker("update", new ContextUpdater(in), arg));
		logger.info("call eval:" + invoke(em, "update"));
		assertEquals(invoke(em, "update"), 400.0);
	}

	@Test
	public void groovyInvoker() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, call("x", 10.0), call("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", args("x", "y")));
		logger.info("invoke eval: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
		logger.info("get eval: " + eval(pm, "expr"));
		assertTrue(exec(pm, "expr").equals(60.0));
	}

	@Test
	public void serviceNeurons() throws Exception {
		Model nm = aneModel("neural-model");
		add(nm, neu("x1", 10.0), neu("x2", 20.0));
		add(nm, neu("x3", weights(val("x1", 2.0), val("x2", 10.0)), signals("x1", "x2")));

//        logger.info("activate x1: " + activate(em, "x1"));
        assertEquals(activate(nm, "x1"), 10.0);

//        logger.info("activate x3: " + activate(em, "x3"));
        assertEquals(activate(nm, "x3"), 220.0);

//        logger.info("activate x3: " + activate(em, "x3", th("x3", 200.0)));
        assertEquals(activate(nm, "x3", th("x3", 200.0)), 1.0);

//        logger.info("activate x3: " + activate(em, "x3", th("x3", 0.0), bias("x3", 50.0)));
        assertEquals(activate(nm, "x3", th("x3", 0.0), bias("x3", 50.0)), 270.0);
	}

	@Test
	public void serviceNeuronFidelities() throws Exception {
		Model nm = model("neural-model",
			neu("x1", 10.0), neu("x2", 20.0),
			neu("x3", weights(val("x1", 2.0), val("x2", 5.0)), signals("x1", "x2")),
			neu("x4", mnFi(
					nFi("n1", signals("x1", "x2"), weights(val("x1", 1.5), val("x2", 10.0))),
					nFi("n2", signals("x1", "x2"), weights(val("x1", 2.0), val("x2", 12.0))))));

//      logger.info("activate1 x4: " + activate(nm, "x4", fi("x4", "n1")));
		assertEquals(activate(nm, "x4", fi("x4", "n1")), 215.0);

//		logger.info("activate2 x4: " + activate(em, "x4", th("x4", 200.0), fi("x4", "n1")));
		assertEquals(activate(nm, "x4", th("n1", 200.0), fi("x4", "n1")), 1.0);

//      logger.info("activate3 x4: " + activate(em, "x4", th("x4", 0.0), fi("x4", "n2")));
        assertEquals(activate(nm, "x4", th("n2", 0.0), fi("x4", "n2")), 260.0);
	}

	@Test
	public void invokeTask() throws Exception {
		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 50.0), inVal("arg/x2", 10.0),
						result("result/y")));

		// logger.info("invoke eval:" + invoke(t4));
		assertEquals(invoke(t4), 500.0);
	}

	@Test
	public void invokeJob() throws Exception {
		Context c4 = context("multiply", inVal("arg/x1", 50.0),
				inVal("arg/x2", 10.0), result("result/y"));
		Context c5 = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				result("result/y"));

		// mograms
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"), outVal("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("exert", ServiceJobber.class),
					job("j2", t4, t5, sig("exert", ServiceJobber.class)), t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					result("j1/t3/result/y"));

		// logger.info("invoke eval:" + invoke(j1));
		assertEquals(invoke(j1), 400.0);
	}

	@Test
	public void invokerProc() throws Exception {

		Call x1 = call("x1", 1.0);

		// logger.info("invoke eval:" + invoke(x1));
		assertEquals(exec(x1), 1.0);
	}

	@Test
	public void substituteInvokeArgs() throws Exception {
		Call x1, x2, y;

		x1 = call("x1", 1.0);
		x2 = call("x2", 2.0);
		y = call("y", invoker("x1 + x2", x1, x2));
		
		logger.info("y: " + exec(y));
		assertTrue(exec(y).equals(3.0));

		Object val = exec(y, context(val("x1", 10.0), val("x2", 20.0)));
		logger.info("y: " + val);
		assertTrue(val.equals(30.0));
	}

	@Test
	public void modelConditions() throws Exception {
		final EntryModel pm = new EntryModel("call-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("condition", invoker("x > y", args("x", "y")));

		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		// logger.info("condition eval: " + em.execute("condition"));
		assertEquals(pm.getValue("condition"), false);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("condition eval: " + pm.getValue("condition"));
		assertEquals(pm.getValue("condition"), true);

		// enclosing class conditional context
		Condition c = new Condition() {
			@Override
			public boolean isTrue() throws ContextException {
				return (Boolean) pm.getValue("condition");
			}
		};
		assertEquals(c.isTrue(), true);

		// provided conditional context
		Condition eval = new Condition(pm) {
			@Override
			public boolean isTrue() throws ContextException {
				try {
					return (Boolean) conditionalContext.getValue("condition");
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
		};
		assertEquals(eval.evaluate(), true);
	}

	@Test
	public void optInvoker() throws Exception {
		EntryModel pm = new EntryModel("call-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				"{ x, y -> x > y }", "x", "y"), 
					invoker("x + y", args("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		logger.info("x: " + value(pm, "x"));
		logger.info("y: " + value(pm, "y"));
		logger.info("opt" + value(pm, "opt"));
		
		assertEquals(opt.evaluate(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + opt.evaluate());
		assertEquals(opt.evaluate(), 500.0);
	}

	@Test
	public void cretaeOptInvoker() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm,
				val("x", 10.0),
				val("y", 20.0),
				opt("opt", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y", args("x", "y"))));

		logger.info("opt eval: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), null);

		setValues(pm, val("x", 300.0), val("y", 200.0));
		logger.info("opt eval: " + value(pm, "opt"));
        assertTrue(value(pm, "opt").equals(500.0));
	}

	@Test
	public void altInvoker() throws Exception {
		EntryModel pm = new EntryModel("call-model");
		pm.putValue("x", 30.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		pm.putValue("x3", 70.0);
		pm.putValue("y3", 60.0);

		OptInvoker opt1 = new OptInvoker("opt1", condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y + 10",
					args("x", "y")));

		OptInvoker opt2 = new OptInvoker("opt2", condition(pm,
				"{ x2, y2 -> x2 > y2 }", "x2", "y2"), invoker(
				"x + y + 20", args("x", "y")));

		OptInvoker opt3 = new OptInvoker("op3", condition(pm,
				"{ x3, y3 -> x3 > y3 }", "x3", "y3"), invoker(
				"x + y + 30", args("x", "y")));

		// no condition means condition(true)
		OptInvoker opt4 = new OptInvoker("opt4", invoker("x + y + 40",
				args("x", "y")));

		AltInvoker alt = new AltInvoker("alt", opt1, opt2, opt3, opt4);
		add(pm, opt1, opt2, opt3, opt4, alt);

		logger.info("opt1 eval: " + eval(opt1));
		assertEquals(eval(opt1), 60.0);
		logger.info("opt2 eval: " + eval(opt2));
		assertEquals(eval(opt2), 70.0);
		logger.info("opt3 eval: " + eval(opt3));
		assertEquals(eval(opt3), 80.0);
		logger.info("opt4 eval: " + eval(opt4));
		assertEquals(eval(opt4), 90.0);
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 60.0);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + eval(alt));
		assertEquals(eval(alt), 510.0);

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 40.0);
		pm.putValue("y2", 50.0);
		pm.putValue("x3", 50.0);
		pm.putValue("y3", 60.0);
		logger.info("opt eval: " + alt.compute());
		assertEquals(eval(alt), 70.0);

		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		logger.info("opt eval: " + alt.compute());
		assertEquals(eval(alt), 50.0);
	}

	@Test
	public void smlAltInvoker() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, val("x", 10.0), val("y", 20.0), val("x2", 50.0),
				val("y2", 40.0), val("x3", 50.0), val("y3", 60.0));

		AltInvoker alt = alt(
				"alt",
				opt("opt1", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y + 10", args("x", "y"))),
				opt("opt2", condition(pm, "{ x2, y2 -> x2 > y2 }", "x2", "y2"),
						invoker("x + y + 20", args("x", "y"))),
				opt("opt3", condition(pm, "{ x3, y3 -> x3 > y3 }", "x3", "y3"),
						invoker("x + y + 30", args("x", "y"))),
				opt("opt4", invoker("x + y + 40", args("x", "y"))));

		add(pm, alt, get(alt, 0), get(alt, 1), get(alt, 2), get(alt, 3));

		logger.info("opt1 eval : " + value(pm, "opt1"));
		assertEquals(value(pm, "opt1"), null);
		logger.info("opt2 eval: " + value(pm, "opt2"));
        assertTrue(value(pm, "opt2").equals(50.0));
		logger.info("opt3 eval: " + value(pm, "opt3"));
		assertEquals(value(pm, "opt3"), null);
		logger.info("opt4 eval: " + value(pm, "opt4"));
        assertTrue(value(pm, "opt4").equals(70.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 50.0);

		setValues(pm, val("x", 300.0), val("y", 200.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 510.0);

		setValues(pm, val("x", 10.0), val("y", 20.0), val("x2", 40.0),
				val("y2", 50.0), val("x3", 50.0), val("y3", 60.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 70.0);
	}

	@Test
	public void invokerLoop() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, val("x", 1));
		add(pm, call("y", invoker("x + 1", args("x"))));
		add(pm, call("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(value(pm, "iloop"), 48);
	}

	@Test
	public void incrementorStepBy1() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, val("x", 1));
		add(pm, call("y", invoker("x + 1", args("x"))));
		add(pm, call("z", inc(invoker(pm, "y"))));
		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
        assertTrue(value(pm, "z").equals(13));
	}

	@Test
	public void incrementorStepBy2() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, call("x", 1));
		add(pm, call("y", invoker("x + 1", args("x"))));
		add(pm, call("z", inc(invoker(pm, "y"), 2)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
		assertEquals(value(pm, "z"), 24);
	}

	@Test
	public void incrementorDouble() throws Exception {
		EntryModel pm = entModel("call-model");
		add(pm, call("x", 1.0));
		add(pm, call("y", invoker("x + 1.2", args("x"))));
		add(pm, call("z", inc(invoker(pm, "y"), 2.1)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + next(pm, "z"));
		}
		// logger.info("" + eval(em,"y++2.1"));
		assertEquals(value(pm, "z"), 25.300000000000004);
	}
}