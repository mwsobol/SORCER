package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.provider.RemoteServiceShell;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;
import sorcer.service.Strategy.Shell;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.inConn;
import static sorcer.mo.operator.outConn;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Signatures {
	private final static Logger logger = LoggerFactory.getLogger(Signatures.class);

	@Test
	public void newInstance() throws Exception {

		// Object orientation
		Signature s = sig("new", Date.class);
		// create a new instance
		Object obj = instance(s);
		logger.info("provider of s: " + obj);
		assertTrue(obj instanceof Date);

	}


	@Test
	public void referencingInstances() throws Exception {

		Object obj = new Date();
		Signature s = sig("getTime", obj);

		// get service provider - a given object
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Date);

		logger.info("getTime: " + exec(xrt("gt", s)));
		assertTrue(exec(xrt("gt", s)) instanceof Long);

	}


	@Test
	public void referencingClassWithConstructor() throws Exception {

		Signature s = sig("getTime", Date.class);

		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		logger.info("selector of s: " + selector(s));
		logger.info("service type of s: " + type(s));
		assertTrue(prv instanceof Date);
		logger.info("time: " + exec(xrt("time", s)));
		assertTrue(exec(xrt("time", s)) instanceof Long);

	}


	@Test
	public void referencingUtilityClass() throws Exception {

		Signature ms = sig(Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv == Math.class);

		logger.info("random: " + exec(xrt("random", ms)));
		assertTrue(exec(xrt("random", ms)) instanceof Double);

		ms = sig(Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[]{double.class, double.class}),
				args(new Object[]{200.11, 3000.0}));

		// request the service
		logger.info("max: " + exec(task("max", ms, cxt)));
		assertTrue(exec(task("max", ms, cxt)) instanceof Double);
		assertTrue(exec(task("max", ms, cxt)).equals(3000.0));

	}

	@Test
	public void StaticMethodWithArgs() throws SignatureException {
		Signature sig = sig(Math.class, "max",
				new Class[]{double.class, double.class},
				new Object[]{200.11, 3000.0});
		logger.info("max: " + instance(sig));
		assertTrue(instance(sig).equals(3000.0));
	}

	@Test
	public void StaticMethodWithNoArgs()  {
		Exception thrown = null;
		try {
			Signature sig = sig(Math.class, "random");
			logger.info("random: " + instance(sig));
		} catch(Exception e) {
			thrown = e;
		}
		assertNull(thrown);
	}

	@Test
	public void StaticMethodWithNoArgs2()  {
		Exception thrown = null;
		try {
			Signature sig = sig("random", Math.class);
			logger.info("random: " + instance(sig));
		} catch(Exception e) {
			thrown = e;
		}
		assertNull(thrown);
	}

	@Test
	public void referencingFactoryClass() throws Exception {

		Signature ps = sig("get", Calendar.class, "getInstance");

		Context cxt = context(
				parameterTypes(new Class[]{int.class}),
				args(new Object[]{Calendar.MONTH}));

		// get service provider for signature
		Object prv = provider(ps);
		logger.info("prv: " + prv);
		assertTrue(prv instanceof Calendar);

		// request the service
		logger.info("time: " + exec(task("month", ps, cxt)));
		assertTrue(exec(task("month", ps, cxt)) instanceof Integer);
		assertTrue(exec(task("month", ps, cxt)).equals(((Calendar) prv).get(Calendar.MONTH)));

	}


	@Test
	public void localService() throws Exception {

		// request the local service
		Servicer as = task("as", sig("add", AdderImpl.class),
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));

	}


	@Test
	public void referencingRemoteProvider() throws Exception {

		// request the remote service
		Servicer as = task("as", sig("add", Adder.class),
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));

	}


	@Test
	public void referencingNamedRemoteProvider() throws Exception {

		Signature ps = sig("add", Adder.class, prvName("Adder"));
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote service
		Servicer as = task("as", ps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));
	}


	@Test
	public void signatureLocalService() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context result = exec(sig("add", AdderImpl.class), cxt);
		assertEquals(20.0, value(result, "y1"));
		assertEquals(80.0, value(result, "y2"));
		assertEquals(100.0, value(result, "result/y"));
	}


	@Test
	public void signatureRemoteService() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context result = exec(sig("add", Adder.class), cxt);
		assertEquals(20.0, value(result, "y1"));
		assertEquals(80.0, value(result, "y2"));
		assertEquals(100.0, value(result, "result/y"));
	}


	@Test
	public void evaluateNetletSignature() throws Exception {
		String netlet = "src/main/netlets/ha-job-local.ntl";
		assertEquals(evaluate(mogram(sig(file(netlet)))), 400.00);
	}


	@Test
	public void execNetletSignature() throws Exception {
		String netlet = "src/main/netlets/ha-job-local.ntl";
		assertEquals(exec(sig(file(netlet))), 400.00);
	}

	@Test
	public void localShellService1() throws Exception {
		// The SORCER Service Shell as a service provider
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/y")));

		Context  out = exec(sig(ServiceShell.class), f5);
		assertEquals(get(out), 100.00);
	}

	@Test
	public void remoteShellService() throws Exception {
		// The SORCER Remote Service Shell as a service provider
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/y")));

		Context  out = exec(sig(RemoteServiceShell.class), f5);
		assertEquals(get(out), 100.00);
	}

	@Test
	public void remoteShellService2() throws Exception {
		// The SORCER Service Shell as a service provider
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/y")));

		Context  out = exec(sig("add", Adder.class, Shell.REMOTE), f5);
		assertEquals(get(out), 100.00);
	}

	@Test
	public void netletSignature() throws Exception {
		String netlet = System.getProperty("project.dir")+"/src/main/netlets/ha-job-local.ntl";

		Signature sig = sig(file(netlet));
//		logger.info("job service: " + exec(sig));
		assertTrue(exec(sig).equals(400.0));
	}

	@Test
	public void netletSignatureprovider() throws Exception {
		String netlet = System.getProperty("project.dir")+"/src/main/netlets/ha-job-local.ntl";

		Servicer srv = (Servicer)provider(sig(file(netlet)));
//		logger.info("job service: " + exec(srv));
		assertTrue(exec(srv).equals(400.0));
	}

	@Test
	public void localSigConnector() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context outConnector = outConn(
				inEnt("arg/x1", "y1"),
				inEnt("arg/x2", "y2"));

		Signature ps = sig("add", AdderImpl.class, prvName("Adder"), outConnector);

		// request the remote service
		Servicer as = task("as", ps, cxt);

		logger.info("input context: " + context(as));

		Servicer task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}


	@Test
	public void remoteSigConnector() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context inc = inConn(
				inEnt("arg/x1", "y1"),
				inEnt("arg/x2", "y2"));

		Signature ps = sig("add", Adder.class, prvName("Adder"), inc);

		// request the remote service
		Servicer as = task("as", ps, cxt);

		logger.info("input context: " + context(as));

		Servicer task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

}
