package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Shell;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.*;

import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.instance;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.provider;
import static sorcer.mo.operator.inConn;
import static sorcer.mo.operator.outConn;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Signatures {
	private final static Logger logger = LoggerFactory.getLogger(Signatures.class.getName());

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
				parameterTypes(new Class[]{double.class, double.class}),
				args(new Object[]{ 200.11, 3000.0 }));

		// request the service
		logger.info("max: " + value(mogram("max", ms, cxt)));
		assertTrue(value(mogram("max", ms, cxt)) instanceof Double);
		assertTrue(value(mogram("max", ms, cxt)).equals(3000.0));

	}


	@Test
	public void staticMethodWithArgs() throws SignatureException {
		Signature sig = sig(Math.class, "max",
				new Class[]{double.class, double.class},
				new Object[]{ 200.11, 3000.0 });
		logger.info("max: " + instance(sig));
		assertTrue(instance(sig).equals(3000.0));
	}


    @Test
    public void staticMethodWithNoArgs()  {
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
    public void staticMethodWithNoArgs2()  {
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
		logger.info("time: " + value(mogram("month", ps, cxt)));
		assertTrue(value(mogram("month", ps, cxt)) instanceof Integer);
		assertTrue(value(mogram("month", ps, cxt)).equals(((Calendar) prv).get(Calendar.MONTH)));

	}

	@Test
	public void providerService() throws Exception {

		// request the remote service
		Context cxt = context("add",
				sig("add", Adder.class),
				inEnt("arg/x1", 20.0),
				inEnt("arg/x2", 80.0),
				result("result/y"));

		Context out = service(prv(sig(Adder.class)),cxt);
		assertTrue(value(out, "result/y").equals(100.0));

	}

	@Test
	public void mogramService() throws Exception {

		// request the remote service
		Mogram as = exertion("as", sig("add", Adder.class),
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		Exertion out = service(as);
		assertTrue(value(context(out)).equals(100.0));

	}

	@Test
	public void referencingRemoteProvider() throws Exception {

		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote service
		Service as = mogram("as", rps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));

	}

	@Test
	public void localService() throws Exception {

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
	public void referencingNamedRemoteProvider() throws Exception {

		Signature ps = sig("add", Adder.class, prvName("Adder"));
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote service
		Service as = mogram("as", ps,
				context("add",
						inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0),
						result("result/y")));

		assertTrue(value(as).equals(100.0));
	}

	@Test
	public void signatureLocalService() throws Exception {

		Context cxt = context(
				inEnt("y1", 20.0),
				inEnt("y2", 80.0),
				result("result/y"));

		Signature ls = sig("add", AdderImpl.class);
		Context result = service(ls, cxt);
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

		Signature rs = sig("add", Adder.class);
		Context result = service(rs, cxt);
		assertEquals(20.0, value(result, "y1"));
		assertEquals(80.0, value(result, "y2"));
		assertEquals(100.0, value(result, "result/y"));
	}

	@Test
	public void remoteShellService() throws Exception {
		// The SORCER Service Shell as a service provider
		Task f5 = task(
				"f5",
				sig("add", Adder.class),
				context("add", inEnt("arg/x1", 20.0),
						inEnt("arg/x2", 80.0), result("result/y")));

		Context  out = service(sig(Shell.class), f5);
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

		Context  out = service(sig("add", Adder.class, ServiceShell.REMOTE), f5);
		assertEquals(get(out), 100.00);
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
		Mogram as = mogram("as", ps, cxt);

		logger.info("input context: " + context(as));

		Service task = exert(as);

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

		// declare remote frontend service
		Mogram as = mogram("as", ps, cxt);

		logger.info("input context: " + context(as));

		// exert backend service
		Service task = exert(as);

		logger.info("input context: " + context(task));

		assertEquals(20.0, value(context(task), "arg/x1"));
		assertEquals(80.0, value(context(task), "arg/x2"));
		assertEquals(100.0, value(context(task), "result/y"));
	}

}
