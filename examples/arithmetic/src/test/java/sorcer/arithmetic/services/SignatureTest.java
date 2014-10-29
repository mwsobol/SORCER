package sorcer.arithmetic.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.inEntry;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.parameterTypes;
import static sorcer.eo.operator.provider;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.selector;
import static sorcer.eo.operator.service;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.type;
import static sorcer.eo.operator.value;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.ExertionException;
import sorcer.service.Service;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.util.Sorcer;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
public class SignatureTest {
	private final static Logger logger = Logger
			.getLogger(SignatureTest.class.getName());

	static {
		String version = "5.0.0-SNAPSHOT";
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + version + "-dl.jar",  "sorcer-dl-"+version +".jar" });
		
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}
	
	
	@Test
	public void referencingObjectsAsProviders() throws SignatureException,
			EvaluationException, ExertionException, ContextException {
		
		Object obj = new Date();
		Signature s = sig("getTime", obj);
		
		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Date);
		
	}
	
	
	@Test
	public void referencingUtilityClassProviders() throws Exception {
		
		Signature ms = sig("random", Math.class, "random");
		Object prv = provider(ms);
		logger.info("provider of s: " + prv);
		assertTrue(prv instanceof Math);
		
		ms = sig("max", Math.class, "max");
		Context cxt = context(
				parameterTypes(new Class[] { double.class, double.class }), 
				args(new Object[] { 200.11, 3000.0 }));
		
		// request the service
		logger.info("time: " + value(service("max", ms, cxt)));
		assertTrue(value(service("max", ms, cxt)) instanceof Double);
		assertTrue(value(service("max", ms, cxt)).equals(3000.0));
		
	}
	
	
	@Test
	public void classWithConstructorAsService() throws SignatureException,
			EvaluationException, ExertionException, ContextException, IOException {
		
		Signature s = sig("getTime", Date.class);
		
		// get service provider for signature
		Object prv = provider(s);
		logger.info("provider of s: " + prv);
		logger.info("selector of s: " + selector(s));
		logger.info("service type of s: " + type(s));
		assertTrue(prv instanceof Date);
				
		value(service("time", s));
		// request the service
		logger.info("time: " + value(service("time", s)));
		assertTrue(value(service("time", s)) instanceof Long);
		
	}
	
	
	@Test
	public void classWithFactoryAsService() throws SignatureException,
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
	public void localSignature() throws SignatureException  {
		
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
		
		Signature ps = sig("add", AdderImpl.class);
		Object prv = provider(ps);
		assertTrue(prv instanceof AdderImpl);
		assertFalse(prv instanceof Proxy);
		
		// request the local service
		Service as = service("as", 
				ps,
				context("add", 
						inEntry("arg/x1", 20.0), 
						inEntry("arg/x2", 80.0), 
						result("result/y")));

		assertEquals(100.0, value(as));		
		
	}
	
	
	@Test
	public void remoteSignature() throws SignatureException  {
		
		Signature ps = sig("add", Adder.class);
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);
		
	}
	
	
	@Test
	public void remoteService() throws SignatureException, 
	ExertionException, ContextException  {

		Signature ps = sig("add", Adder.class);
		Object prv = provider(ps);
		logger.info("provider of ps: " + prv);
		assertTrue(prv instanceof Adder);
		assertTrue(prv instanceof Proxy);

		// request the remote (net) service
		Service as = service("as", 
				ps,
				context("add", 
						inEntry("arg/x1", 20.0), 
						inEntry("arg/x2", 80.0), 
						result("result/y")));

		assertEquals(100.0, value(as));	

	}
	
}