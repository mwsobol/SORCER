package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.service.Context;
import sorcer.service.Service;
import sorcer.service.Signature;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Signatures {
	private final static Logger logger = Logger.getLogger(Signatures.class.getName());

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

		logger.info("getTime: " + reply(service("gt", s)));
		assertTrue(reply(service("gt", s)) instanceof Long);

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
		service("time", s);

		logger.info("time: " + reply(service("time", s)));
		assertTrue(reply(service("time", s)) instanceof Long);

	}


	@Test
	public void referencingUtilityClass() throws Exception {

		Signature ms = sig(Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv == Math.class);

		logger.info("random: " + reply(service("random", ms)));
		assertTrue(reply(service("random", ms)) instanceof Double);

		ms = sig(Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[]{double.class, double.class}),
				args(new Object[]{200.11, 3000.0}));

		// request the service
		logger.info("max: " + reply(service("max", ms, cxt)));
		assertTrue(reply(service("max", ms, cxt)) instanceof Double);
		assertTrue(reply(service("max", ms, cxt)).equals(3000.0));

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
		logger.info("time: " + reply(service("month", ps, cxt)));
		assertTrue(reply(service("month", ps, cxt)) instanceof Integer);
		assertTrue(reply(service("month", ps, cxt)).equals(((Calendar) prv).get(Calendar.MONTH)));

	}


	@Test
	public void localService() throws Exception {

		Signature lps = sig("add", AdderImpl.class);
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);

		// request the local service
		Service as = service("as", lps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));

	}


	@Test
	public void referencingRemoteProvider() throws Exception {

		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote service
		Service as = service("as", rps,
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
		Service as = service("as", ps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));
	}

	@Test
	public void localSigConnector() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context connector = inConn(
				inEnt("arg/x1", "y1"),
				inEnt("arg/x2", "y2"));

		Signature ps = sig("add", AdderImpl.class, prvName("Adder"), connector);

		// request the remote service
		Service as = service("as", ps, cxt);

		logger.info("input context: " + context(as));

		Service task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

	@Test
	public void rmoteSigConnector() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Context connector = inConn(
				inEnt("arg/x1", "y1"),
				inEnt("arg/x2", "y2"));

		Signature ps = sig("add", Adder.class, prvName("Adder"), connector);

		// request the remote service
		Service as = service("as", ps, cxt);

		logger.info("input context: " + context(as));

		Service task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

}