package sorcer.pml.modeling;

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
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.core.invoker.AltInvoker;
import sorcer.core.invoker.Updater;
import sorcer.core.invoker.OptInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.modeling.ContextModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.setValue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.setValue;
import static sorcer.po.operator.add;
import static sorcer.po.operator.alt;
import static sorcer.po.operator.*;
import static sorcer.po.operator.get;
import static sorcer.po.operator.loop;
import static sorcer.po.operator.opt;
import static sorcer.po.operator.put;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class Invokers {
	private final static Logger logger = LoggerFactory.getLogger(Invokers.class);

	private ProcModel pm;
	private Proc x;
	private Proc y;
	private Proc z;

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
			// x setValue from 'arg'
			assertTrue(eval(x).equals(200.0));
			// y setValue from construtor's context 'in'
			assertTrue(eval(y).equals(30.0));
			assertTrue(eval(z).equals(170.0));
			return (double)eval(x) + (double)eval(y) + (double)value(pm, "z");
		}
	};

	@Before
	public void initParModel() throws Exception {
		pm = new ProcModel();
		x = proc("x", 10.0);
		y = proc("y", 20.0);
		z = proc("z", invoker("x - y", x, y));
	}

	@Test
	public void lambdaInvoker() throws Exception {

		ContextModel mo = model(val("x", 10.0), val("y", 20.0),
				proc(invoker("lambda",
					(Context<Double> cxt) -> value(cxt, "x") + value(cxt, "y") + 30,
					args("x", "y"))));
		logger.info("invoke eval: " + eval(mo, "lambda"));
		assertEquals(eval(mo, "lambda"), 60.0);
	}

	@Test
	public void objectMethodInvoker() throws Exception {
		setValue(x, 10.0);
		setValue(y, 20.0);
		add(pm, x, y, z);

//		logger.info("x:" + eval(pm, "x"));
//		logger.info("y:" + eval(pm, "y"));
//		logger.info("y:" + eval(pm, "z"));

		Context in = context(val("x", 20.0), val("y", 30.0));
		Context arg = context(val("x", 200.0), val("y", 300.0));
		add(pm, methodInvoker("update", new ContextUpdater(in), arg));
		logger.info("call eval:" + invoke(pm, "update"));
		assertEquals(invoke(pm, "update"), 400.0);
	}

	@Test
	public void groovyInvoker() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm, proc("x", 10.0), proc("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", args("x", "y")));
		logger.info("invoke eval: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
		logger.info("get eval: " + value(pm, "expr"));
		assertTrue(value(pm, "expr").equals(60.0));
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
	public void invokeParJob() throws Exception {
		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				result("result/y"));
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

		// logger.info("return path:" + j1.getReturnJobPath());
		assertEquals(j1.getReturnPath().path, "j1/t3/result/y");

		ProcModel pm = procModel("proc-model");
		add(pm, as(proc("x1p", "arg/x1"), c4), as(proc("x2p", "arg/x2"), c4), j1);
		// setting context parameters in a job
		setValue(pm, "x1p", 10.0);
		setValue(pm, "x2p", 50.0);

		add(pm, j1);
		// logger.info("call eval:" + invoke(pm, "j1"));
		assertEquals(invoke(pm, "j1"), 400.0);
	}

	@Test
	public void invokerProc() throws Exception {

		Proc<Double> x1 = proc("x1", 1.0);

		// logger.info("invoke eval:" + invoke(x1));
		assertEquals(invoke(x1), 1.0);
	}

	@Test
	public void substituteInvokeArgs() throws Exception {
		Proc x1, x2, y;

		x1 = proc("x1", 1.0);
		x2 = proc("x2", 2.0);
		y = proc("y", invoker("x1 + x2", x1, x2));
		
		logger.info("y: " + eval(y));
		assertTrue(eval(y).equals(3.0));

		Object val = invoke(y, ent("x1", 10.0), ent("x2", 20.0));
		logger.info("y: " + val);

		logger.info("y: " + eval(y));
		assertTrue(eval(y).equals(30.0));
	}

	@Test
	public void exertionInvoker() throws Exception {
		Context c4 = context("multiply", inVal("arg/x1"), inVal("arg/x2"),
				result("result/y"));
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
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		// mapping via the context
		ProcModel pm = procModel("proc-model");
		add(pm, as(proc("x1p", "arg/x1"), c4), as(proc("x2p", "arg/x2"), c4), j1);

		// setting context parameters in a job
		setValue(pm, "x1p", 10.0);
		setValue(pm, "x2p", 50.0);

		add(pm, exertInvoker(j1, "j1/t3/result/y"));
		// logger.info("call eval:" + invoke(pm, "invoke j1"));
		assertEquals(invoke(pm, "j1"), 400.0);
	}

	@Test
	public void modelConditions() throws Exception {
		final ProcModel pm = new ProcModel("proc-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("condition", invoker("x > y", args("x", "y")));

		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		// logger.info("condition eval: " + pm.getValue("condition"));
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
				return (Boolean) conditionalContext.getValue("condition");
			}
		};
		assertEquals(eval.getValue(), true);
	}

	@Test
	public void optInvoker() throws Exception {
		ProcModel pm = new ProcModel("proc-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				"{ x, y -> x > y }", "x", "y"), 
					invoker("x + y", args("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		logger.info("x: " + value(pm, "x"));
		logger.info("y: " + value(pm, "y"));
		logger.info("opt" + value(pm, "opt"));
		
		assertEquals(opt.getValue(), null);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt eval: " + opt.getValue());
		assertEquals(opt.getValue(), 500.0);
	}

	@Test
	public void polOptInvoker() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm,
				proc("x", 10.0),
				proc("y", 20.0),
				opt("opt", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y", args("x", "y"))));

		logger.info("opt eval: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), null);

		put(pm, "x", 300.0);
		put(pm, "y", 200.0);
		logger.info("opt eval: " + value(pm, "opt"));
        assertTrue(value(pm, "opt").equals(500.0));
	}

	@Test
	public void altInvoker() throws Exception {
		ProcModel pm = new ProcModel("proc-model");
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
		logger.info("opt eval: " + alt.invoke());
		assertEquals(eval(alt), 70.0);

		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		logger.info("opt eval: " + alt.invoke());
		assertEquals(eval(alt), 50.0);
	}

	@Test
	public void polAltInvoker() throws Exception {
		ProcModel pm = procModel("proc-model");
		// add(pm, entry("x", 10.0), entry("y", 20.0), proc("x2", 50.0),
		// proc("y2", 40.0), proc("x3", 50.0), proc("y3", 60.0));
		add(pm, proc("x", 10.0), proc("y", 20.0), proc("x2", 50.0),
				proc("y2", 40.0), proc("x3", 50.0), proc("y3", 60.0));

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

		put(pm, proc("x", 300.0), proc("y", 200.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 510.0);

		put(pm, proc("x", 10.0), proc("y", 20.0), proc("x2", 40.0),
				proc("y2", 50.0), proc("x3", 50.0), proc("y3", 60.0));
		logger.info("alt eval: " + eval(alt));
		assertEquals(eval(alt), 70.0);
	}

	@Test
	public void invokerLoop() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm, val("x", 1));
		add(pm, proc("y", invoker("x + 1", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"), 2)));
		Invocation z2 = invoker(pm, "z");

		ServiceInvoker iloop = loop("iloop", condition(pm, "{ z -> z < 50 }", "z"), z2);
		add(pm, iloop);
		assertEquals(value(pm, "iloop"), 48);
	}

	@Test
	public void incrementorStepBy1() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm, val("x", 1));
		add(pm, proc("y", invoker("x + 1", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"))));
		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
        assertTrue(value(pm, "z").equals(13));
	}

	@Test
	public void incrementorStepBy2() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm, proc("x", 1));
		add(pm, proc("y", invoker("x + 1", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"), 2)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "z"));
		}
		assertEquals(value(pm, "z"), 24);
	}

	@Test
	public void incrementorDouble() throws Exception {
		ProcModel pm = procModel("proc-model");
		add(pm, proc("x", 1.0));
		add(pm, proc("y", invoker("x + 1.2", args("x"))));
		add(pm, proc("z", inc(invoker(pm, "y"), 2.1)));

		for (int i = 0; i < 10; i++) {
			logger.info("" + next(pm, "z"));
		}
		// logger.info("" + eval(pm,"y++2.1"));
		assertEquals(value(pm, "z"), 25.300000000000004);
	}
}
