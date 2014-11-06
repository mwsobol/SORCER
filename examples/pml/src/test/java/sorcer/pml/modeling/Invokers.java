package sorcer.pml.modeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.condition;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.alt;
import static sorcer.po.operator.asis;
import static sorcer.po.operator.cmdInvoker;
import static sorcer.po.operator.exertInvoker;
import static sorcer.po.operator.get;
import static sorcer.po.operator.inc;
import static sorcer.po.operator.invoke;
import static sorcer.po.operator.invoker;
import static sorcer.po.operator.loop;
import static sorcer.po.operator.methodInvoker;
import static sorcer.po.operator.next;
import static sorcer.po.operator.opt;
import static sorcer.po.operator.par;
import static sorcer.po.operator.parModel;
import static sorcer.po.operator.pars;
import static sorcer.po.operator.put;
import static sorcer.po.operator.runnableInvoker;
import static sorcer.po.operator.set;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.jini.core.transaction.TransactionException;

import org.junit.BeforeClass;
import org.junit.Test;

import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.AltInvoker;
import sorcer.core.invoker.Invocable;
import sorcer.core.invoker.OptInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.pml.provider.impl.Volume;
import sorcer.service.Condition;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Invokers {
	private final static Logger logger = Logger.getLogger(Invokers.class
			.getName());

	private ParModel pm; 
	private Par<Double> x;
	private Par<Double> y;
	private Par z;
	
	static {
		ServiceExertion.debug = true;
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		System.setProperty("java.util.logging.config.file", Sorcer.getHome()
				+ "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-invoker-beans.jar" });
	}

	// member subclass of Invocable with Context parameter used below with
	// contextMethodAttachmentWithArgs()
	// there are constructor's context and invoke metod's context as parameters
	
	@BeforeClass
	public void initParModel() throws EvaluationException, RemoteException {
		pm = new ParModel();
		x = par("x", 10.0);
		y = par("y", 20.0);
		par("z", invoker("x - y", x, y));
	}

	public class Update extends Invocable {
		public Update(Context context) {
			super(context);
		}
		public Double invoke(Context arg) throws Exception {
			set(x, value(arg, "x"));
			set(y, value(context, "y"));
			// x set from 'arg'
			Assert.assertEquals((Double) value(x), 200.0);
			// y set from construtor's context 'in'
			Assert.assertEquals((Double) value(y), 30.0);
			Assert.assertEquals((Double) value(z), 170.0);
			return (Double)value(x) + (Double)value(y) + (Double)value(pm, "z");
		}
	};

	@Test
	public void methodInvokerTest() throws RemoteException, ContextException {
		set(x, 10.0);
		set(y, 20.0);
		add(pm, x, y, z);

//		logger.info("x:" + value(pm, "x"));
//		logger.info("y:" + value(pm, "y"));
//		logger.info("y:" + value(pm, "z"));

		Context in = context(ent("x", 20.0), ent("y", 30.0));
		Context arg = context(ent("x", 200.0), ent("y", 300.0));
		add(pm, methodInvoker("invoke", new Update(in), arg));
		logger.info("call value:" + invoke(pm, "invoke"));
		assertEquals(invoke(pm, "invoke"), 400.0);
	}

	@Test
	public void groovyInvokerTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		ParModel pm = parModel("par-model");
		add(pm, par("x", 10.0), par("y", 20.0));
		add(pm, invoker("expr", "x + y + 30", pars("x", "y")));
		logger.info("invoke value: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
		logger.info("get value: " + value(pm, "expr"));
		assertEquals(value(pm, "expr"), 60.0);
	}

	@Test
	public void invokeTaskTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {

		Task t4 = task(
				"t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inEnt("arg/x1", 50.0), inEnt("arg/x2", 10.0),
						result("result/y")));

		// logger.info("invoke value:" + invoke(t4));
		assertEquals(invoke(t4), 500.0);
	}

	@Test
	public void invokeJobTest() throws RemoteException, ContextException,
			SignatureException, ExertionException, TransactionException {
		Context c4 = context("multiply", inEnt("arg/x1", 50.0),
				inEnt("arg/x2", 10.0), result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
					job("j2", t4, t5, sig("service", ServiceJobber.class)), t3,
					pipe(out(t4, "result/y"), in(t3, "arg/x1")),
					pipe(out(t5, "result/y"), in(t3, "arg/x2")),
					result("j1/t3/result/y"));

		// logger.info("invoke value:" + invoke(j1));
		assertEquals(invoke(j1), 400.0);
	}

	@Test
	public void invokeParJobTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
					job("j2", t4, t5, sig("service", ServiceJobber.class)), t3,
					pipe(out(t4, "result/y"), in(t3, "arg/x1")),
					pipe(out(t5, "result/y"), in(t3, "arg/x2")),
					result("j1/t3/result/y"));

		// logger.info("return path:" + j1.getReturnJobPath());
		assertEquals(j1.getReturnPath().path, "j1/t3/result/y");

		ParModel pm = parModel("par-model");
		add(pm, par("x1p", "arg/x1", c4), par("x2p", "arg/x2", c4), j1);
		// setting context parameters in a job
		set(pm, "x1p", 10.0);
		set(pm, "x2p", 50.0);

		add(pm, j1);
		// logger.info("call value:" + invoke(pm, "j1"));
		assertEquals(invoke(pm, "j1"), 400.0);
	}

	@Test
	public void invokeVarTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {

		Par<Double> x1 = par("x1", 1.0);

		// logger.info("invoke value:" + invoke(x1));
		assertEquals(invoke(x1), 1.0);
	}

	@Test
	public void substituteArgsTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Par x1, x2, y;

		x1 = par("x1", 1.0);
		x2 = par("x2", 2.0);
		y = par("y", invoker("x1 + x2", x1, x2));
		
		logger.info("y: " + value(y));
		assertEquals(value(y), 3.0);

		Object val = invoke(y, ent("x1", 10.0), ent ("x2", 20.0));
		logger.info("y: " + val);

		logger.info("y: " + value(y));
		assertEquals(value(y), 30.0);
	}

	@Test
	public void exertionInvokerTest() throws RemoteException, ContextException,
			SignatureException, ExertionException {
		Context c4 = context("multiply", inEnt("arg/x1"), inEnt("arg/x2"),
				result("result/y"));
		Context c5 = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				result("result/y"));

		// exertions
		Task t3 = task(
				"t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));
		Task t4 = task("t4", sig("multiply", MultiplierImpl.class), c4);
		Task t5 = task("t5", sig("add", AdderImpl.class), c5);

		Job j1 = job("j1", sig("service", ServiceJobber.class),
				job("j2", t4, t5, sig("service", ServiceJobber.class)), t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));

		ParModel pm = parModel("par-model");
		add(pm, par("x1p", "arg/x1", c4), par("x2p", "arg/x2", c4), j1);
		// setting context parameters in a job
		set(pm, "x1p", 10.0);
		set(pm, "x2p", 50.0);

		add(pm, exertInvoker("invoke j1", j1, "j1/t3/result/y"));
		// logger.info("call value:" + invoke(pm, "invoke j1"));
		assertEquals(invoke(pm, "invoke j1"), 400.0);
	}

	@Test
	public void cmdInvokerTest() throws SignatureException, ExertionException,
			ContextException, IOException {
		String cp = Sorcer.getHome() + "/classes" + File.pathSeparator
				+ Sorcer.getHome() + "/lib/river/lib/jsk-platform.jar"
				+ File.pathSeparator + Sorcer.getHome() + "/lib/river/lib/jsk-lib.jar ";
		ServiceInvoker cmd = cmdInvoker("volume",
				"java -cp  " + cp + Volume.class.getName() + " cylinder");

		par("multiply", invoker("x * y", pars("x", "y")));
		ParModel pm = add(parModel(), par("x", 10.0), par("y"), par(cmd),
				par("add", invoker("x + y", pars("x", "y"))));

		CmdResult result = (CmdResult) invoke(pm, "volume");
		// get from the result the volume of cylinder and assign to y parameter
		if (result.getExitValue() != 0) {
			logger.info("cmd result: " + result);
			throw new RuntimeException();
		}
		Properties props = new Properties();
		props.load(new StringReader(result.getOut()));
		set(pm, "y", new Double(props.getProperty("cylinder/volume")));

		logger.info("x value:" + value(pm, "x"));
		logger.info("y value:" + value(pm, "y"));
		logger.info("multiply value:" + value(pm, "add"));
		assertEquals(value(pm, "add"), 47.69911184307752);
	}

	@Test
	public void conditionalInvoker() throws RemoteException, ContextException {
		final ParModel pm = new ParModel("par-model");
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("condition", invoker("x > y", pars("x", "y")));

		assertEquals(pm.getValue("x"), 10.0);
		assertEquals(pm.getValue("y"), 20.0);
		// logger.info("condition value: " + pm.getValue("condition"));
		assertEquals(pm.getValue("condition"), false);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		 logger.info("condition value: " + pm.getValue("condition"));
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
	public void optInvokerTest() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");

		OptInvoker opt = new OptInvoker("opt", new Condition(pm,
				"{ x, y -> x > y }", "x", "y"), 
					invoker("x + y", pars("x", "y")));

		pm.add(opt);
		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);

		logger.info("x: " + value(pm, "x"));
		logger.info("y: " + value(pm, "y"));
		logger.info("opt" + value(pm, "opt"));
		
//		assertEquals(opt.getValue(), null);
//
//		pm.putValue("x", 300.0);
//		pm.putValue("y", 200.0);
//		logger.info("opt value: " + opt.getValue());
//		assertEquals(opt.getValue(), 500.0);
	}

	@Test
	public void polOptInvokerTest() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm,
				par("x", 10.0),
				par("y", 20.0),
				opt("opt", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y", pars("x", "y"))));

		logger.info("opt value: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), null);

		put(pm, "x", 300.0);
		put(pm, "y", 200.0);
		logger.info("opt value: " + value(pm, "opt"));
		assertEquals(value(pm, "opt"), 500.0);
	}

	@Test
	public void altInvokerTest() throws RemoteException, ContextException {
		ParModel pm = new ParModel("par-model");
		pm.putValue("x", 30.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		pm.putValue("x3", 70.0);
		pm.putValue("y3", 60.0);

		OptInvoker opt1 = new OptInvoker("opt1", condition(pm,
				"{ x, y -> x > y }", "x", "y"), invoker("x + y + 10",
						pars("x", "y")));

		OptInvoker opt2 = new OptInvoker("opt2", condition(pm,
				"{ x2, y2 -> x2 > y2 }", "x2", "y2"), invoker(
				"x + y + 20", pars("x", "y")));

		OptInvoker opt3 = new OptInvoker("op3", condition(pm,
				"{ x3, y3 -> x3 > y3 }", "x3", "y3"), invoker(
				"x + y + 30", pars("x", "y")));

		// no condition means condition(true)
		OptInvoker opt4 = new OptInvoker("opt4", invoker("x + y + 40",
				pars("x", "y")));

		AltInvoker alt = new AltInvoker("alt", opt1, opt2, opt3, opt4);
		add(pm, opt1, opt2, opt3, opt4, alt);

		logger.info("opt1 value: " + value(opt1));
		assertEquals(value(opt1), 60.0);
		logger.info("opt2 value: " + value(opt2));
		assertEquals(value(opt2), 70.0);
		logger.info("opt3 value: " + value(opt3));
		assertEquals(value(opt3), 80.0);
		logger.info("opt4 value: " + value(opt4));
		assertEquals(value(opt4), 90.0);
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 60.0);

		pm.putValue("x", 300.0);
		pm.putValue("y", 200.0);
		logger.info("opt value: " + value(alt));
		assertEquals(value(alt), 510.0);

		pm.putValue("x", 10.0);
		pm.putValue("y", 20.0);
		pm.putValue("x2", 40.0);
		pm.putValue("y2", 50.0);
		pm.putValue("x3", 50.0);
		pm.putValue("y3", 60.0);
		logger.info("opt value: " + alt.invoke());
		assertEquals(value(alt), 70.0);

		pm.putValue("x2", 50.0);
		pm.putValue("y2", 40.0);
		logger.info("opt value: " + alt.invoke());
		assertEquals(value(alt), 50.0);
	}

	@Test
	public void polAltInvokerTest() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		// add(pm, entry("x", 10.0), entry("y", 20.0), par("x2", 50.0),
		// par("y2", 40.0), par("x3", 50.0), par("y3", 60.0));
		add(pm, par("x", 10.0), par("y", 20.0), par("x2", 50.0),
				par("y2", 40.0), par("x3", 50.0), par("y3", 60.0));

		AltInvoker alt = alt(
				"alt",
				opt("opt1", condition(pm, "{ x, y -> x > y }", "x", "y"),
						invoker("x + y + 10", pars("x", "y"))),
				opt("opt2", condition(pm, "{ x2, y2 -> x2 > y2 }", "x2", "y2"),
						invoker("x + y + 20", pars("x", "y"))),
				opt("opt3", condition(pm, "{ x3, y3 -> x3 > y3 }", "x3", "y3"),
						invoker("x + y + 30", pars("x", "y"))),
				opt("opt4", invoker("x + y + 40", pars("x", "y"))));

		add(pm, alt, get(alt, 0), get(alt, 1), get(alt, 2), get(alt, 3));

		logger.info("opt1 value : " + value(pm, "opt1"));
		assertEquals(value(pm, "opt1"), null);
		logger.info("opt2 value: " + value(pm, "opt2"));
		assertEquals(value(pm, "opt2"), 50.0);
		logger.info("opt3 value: " + value(pm, "opt3"));
		assertEquals(value(pm, "opt3"), null);
		logger.info("opt4 value: " + value(pm, "opt4"));
		assertEquals(value(pm, "opt4"), 70.0);
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 50.0);

		put(pm, ent("x", 300.0), ent("y", 200.0));
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 510.0);

		put(pm, ent("x", 10.0), ent("y", 20.0), ent("x2", 40.0),
				ent("y2", 50.0), ent("x3", 50.0), ent("y3", 60.0));
		logger.info("alt value: " + value(alt));
		assertEquals(value(alt), 70.0);
	}

	@Test
	public void loopInvokerTest() throws RemoteException, ContextException {
		final ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));

		// update x and y for the loop condition (z) depends on
		Runnable update = new Runnable() {
			public void run() {
				try {
					while ((Integer) value(pm, "x") < 25) {
						set(pm, "x", (Integer) value(pm, "x") + 1);
						 System.out.println("running ... " + value(pm, "x"));
						Thread.sleep(100);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		add(pm, runnableInvoker("update", update));
		invoke(pm, "update");

		add(pm,
				loop("loop", condition(pm, "{ x -> x < 20 }", "x"),
						(ServiceInvoker) asis((Par) asis(pm, "y"))));

		// logger.info("loop value: " + value(pm, "loop"));
		assertTrue((Integer) value(pm, "loop") == 20);
	}

	@Test
	public void incrementorBy1Test() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));
		add(pm, inc("y++", invoker(pm, "y")));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "y++"));
		}
		assertEquals(value(pm, "y++"), 13);
	}

	@Test
	public void incrementorBy2Test() throws RemoteException, ContextException {
		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1));
		add(pm, par("y", invoker("x + 1", pars("x"))));
		add(pm, inc("y++2", invoker(pm, "y"), 2));

		for (int i = 0; i < 10; i++) {
			logger.info("" + value(pm, "y++2"));
		}
		assertEquals(value(pm, "y++2"), 24);
	}

	@Test
	public void incrementorDoubleTest() throws RemoteException,
			ContextException {
		ParModel pm = parModel("par-model");
		add(pm, ent("x", 1.0));
		add(pm, par("y", invoker("x + 1.2", pars("x"))));
		add(pm, inc("y++2.1", invoker(pm, "y"), 2.1));

		for (int i = 0; i < 10; i++) {
			logger.info("" + next(pm, "y++2.1"));
		}
		// logger.info("" + value(pm,"y++2.1"));
		assertEquals(value(pm, "y++2.1"), 25.300000000000004);
	}
}
