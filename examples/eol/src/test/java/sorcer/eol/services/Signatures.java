package sorcer.eol.services;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.service.*;
import sorcer.util.Sorcer;

import java.io.IOException;
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
@ProjectContext("examples/eol")
public class Signatures {
	private final static Logger logger = Logger.getLogger(Signatures.class.getName());

	@Test
	public void referencingInstances() throws SignatureException,
			EvaluationException, ExertionException, ContextException {
		
		Object obj = new Date();
		Signature s = sig("getTime", obj);
		
		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Date);
		
	}
	
	
	@Test
	public void referencingUtilityClass() throws Exception {
		
		Signature ms = sig("random", Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv == Math.class);
		assertTrue(value(service("random", ms)) instanceof Double);
		logger.info("random: " + value(service("random", ms)));
		
		ms = sig("max", Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[] { double.class, double.class }), 
				args(new Object[] { 200.11, 3000.0 }));
		
		// request the service
		logger.info("max: " + value(service("max", ms, cxt)));
		assertTrue(value(service("max", ms, cxt)) instanceof Double);
		assertTrue(value(service("max", ms, cxt)).equals(3000.0));
		
	}
	

	@Ignore
	@Test
	//TODO
	public void referencingClassWithConstructor() throws SignatureException,
			EvaluationException, ExertionException, ContextException, IOException {
		
		Signature s = sig("getTime", Date.class);
		
		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		logger.info("selector of s: " + selector(s));
		logger.info("service type of s: " + type(s));
		assertTrue(prv instanceof Date);
		service("time", s);

		logger.info("time: " + value(service("time", s)));
		assertTrue(value(service("time", s)) instanceof Long);
		
	}
	
	
	@Test
	public void referencingFactoryClass() throws SignatureException,
			EvaluationException, ExertionException, ContextException, IOException {
		
		Signature ps = sig("get", Calendar.class, "getInstance");
		
		Context cxt = context(
				parameterTypes(new Class[] { int.class }), 
				args(new Object[] { Calendar.MONTH }));
		
		// get service provider for signature
		Object prv = provider(ps);
		assertTrue(prv instanceof Calendar);
		
		// request the service
		logger.info("time: " + value(service("month", ps, cxt)));
		assertTrue(value(service("month", ps, cxt)) instanceof Integer);
		assertTrue(value(service("month", ps, cxt)).equals(((Calendar)prv).get(Calendar.MONTH)));
		
	}
	
	
	@Test
	public void referencingProviderImpl() throws SignatureException  {
		
		// the AdderImpl class (service bean) implements the Adder interface 
		Signature ps = sig(AdderImpl.class);
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);
		
	}
	
	
	@Test
	public void localService() throws SignatureException, 
		ExertionException, ContextException  {
		
		Signature lps = sig("add", AdderImpl.class);
		Object prv = provider(lps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);
		
		// request the local service
		Service as = service("as", 
				lps,
				context("add", 
						inEnt("arg/x1", 20.0), 
						inEnt("arg/x2", 80.0), 
						result("result/y")));

		assertEquals(100.0, value(as));		
		
	}
	
	
	@Test
	public void referencingRemoteProvider() throws SignatureException  {
		
		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of rps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);
		
	}


	@Test
	public void referencingNamedRemoteProvider() throws SignatureException  {

		Signature ps = sig("add", Adder.class, prvName("Adder"));
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

	}


	@Test
	public void remoteService() throws SignatureException, 
	ExertionException, ContextException  {

		Signature rps = sig("add", Adder.class);
		Object prv = provider(rps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote (net) service
		Service as = service("as", 
				rps,
				context("add", 
						inEnt("arg/x1", 20.0), 
						inEnt("arg/x2", 80.0), 
						result("result/y")));

		assertEquals(100.0, value(as));	

	}
	
}