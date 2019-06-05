package sorcer.core.signature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.service.*;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static sorcer.co.operator.inVal;
import static sorcer.co.operator.instance;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.inConn;
import static sorcer.mo.operator.outConn;
import static sorcer.mo.operator.value;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class SignatureTest {
	private final static Logger logger = LoggerFactory.getLogger(SignatureTest.class);

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

		// getValue service provider - a given object
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Date);

		logger.info("getTime: " + exec(xrt("gt", s)));
		assertTrue(exec(xrt("gt", s)) instanceof Long);

	}


	@Test
	public void referencingClassWithConstructor() throws Exception {

		Signature s = sig("getTime", Date.class);

		// getValue service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		logger.info("selector of s: " + selector(s));
		logger.info("service fiType of s: " + type(s));
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
				types(new Class[]{double.class, double.class}),
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
				types(new Class[]{int.class}),
				args(new Object[]{Calendar.MONTH}));

		// getValue service provider for signature
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

		Signature lps = sig("add", AdderImpl.class);
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);

		// request the local service
		Service as = task("as", lps,
				context("add",
						inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0),
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
		Service as = task("as", rps,
				context("add",
						inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0),
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
		Service as = task("as", ps,
				context("add",
						inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0),
						result("result/y")));

		assertEquals(100.0, exec(as));
	}

	@Test
	public void localSigOutputConnector() throws Exception {

		Context cxt = context(
				inVal("y1", 20.0),
				inVal("y2", 80.0),
				result("result/y"));

		Context outConnector = outConn(
				inVal("arg/x1", "y1"),
				inVal("arg/x2", "y2"));

		Signature ps = sig("add", AdderImpl.class, prvName("Adder"), outConnector);

		// request the remote service
		Mogram as = task("as", ps, cxt);

		logger.info("input context: " + context(as));

		Mogram task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

	@Test
	public void remoteSigInputConnector() throws Exception {

		Context cxt = context(
				inVal("y1", 20.0),
				inVal("y2", 80.0),
				result("result/y"));

		Context inc = inConn(
				inVal("arg/x1", "y1"),
				inVal("arg/x2", "y2"));

		Signature ps = sig("add", Adder.class, prvName("Adder"), inc);

		// request the remote service
		Mogram as = task("as", ps, cxt);

		logger.info("input context: " + context(as));

		Mogram task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

}
