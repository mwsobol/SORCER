package sorcer.core.signature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.service.Context;
import sorcer.service.Service;
import sorcer.service.Signature;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class SignatureTest {

	private static final Logger logger = LoggerFactory.getLogger(SignatureTest.class);

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

		logger.info("getTime: " + value(mogram("gt", s)));
		assertTrue(value(mogram("gt", s)) instanceof Long);

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
		mogram("time", s);

		logger.info("time: " + value(mogram("time", s)));
		assertTrue(value(mogram("time", s)) instanceof Long);

	}


	@Test
	public void referencingUtilityClass() throws Exception {

		Signature ms = sig(Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv == Math.class);

		logger.info("random: " + value(mogram("random", ms)));
		assertTrue(value(mogram("random", ms)) instanceof Double);

		ms = sig(Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[] { double.class, double.class }),
				args(new Object[] { 200.11, 3000.0 }));

		// request the service
		logger.info("max: " + value(mogram("max", ms, cxt)));
		assertTrue(value(mogram("max", ms, cxt)) instanceof Double);
		assertTrue(value(mogram("max", ms, cxt)).equals(3000.0));

	}


	@Test
	public void referencingFactoryClass() throws Exception {

		Signature ps = sig("get", Calendar.class, "getInstance");

		Context cxt = context(
				parameterTypes(new Class[] { int.class }),
				args(new Object[] { Calendar.MONTH }));

		// get service provider for signature
		Object prv = provider(ps);
		logger.info("prv: " + prv);
		assertTrue(prv instanceof Calendar);

		// request the service
		logger.info("time: " + value(mogram("month", ps, cxt)));
		assertTrue(value(mogram("month", ps, cxt)) instanceof Integer);
		assertTrue(value(mogram("month", ps, cxt)).equals(((Calendar)prv).get(Calendar.MONTH)));

	}


	@Test
	public void localService() throws Exception  {

		Signature lps = sig("add", AdderImpl.class);
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);

		// request the local service
		Service as = mogram("as", lps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));

	}

	@Test
	public void reveresedLocalService() throws Exception  {

		Signature lps = sig(AdderImpl.class, "add");
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);

		// request the local service
		Service as = mogram("as", lps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));

	}

	@Test
	public void referencingRemoteProvider() throws Exception  {

		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the local service
		Service as = mogram("as", rps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));

	}

	@Test
	public void reversedReferencingRemoteProvider() throws Exception  {

		Signature rps = sig(Adder.class, "add");
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the local service
		Service as = mogram("as", rps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));

	}

	@Test
	public void referencingNamedRemoteProvider() throws Exception  {

		Signature ps = sig("add", Adder.class, prvName("Adder"));
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the local service
		Service as = mogram("as", ps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));
	}

}
