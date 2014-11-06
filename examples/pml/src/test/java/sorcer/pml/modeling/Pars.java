package sorcer.pml.modeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.persistent;
import static sorcer.co.operator.url;
import static sorcer.eo.operator.asis;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.cxt;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.store;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.dbPar;
import static sorcer.po.operator.invoker;
import static sorcer.po.operator.par;
import static sorcer.po.operator.parFi;
import static sorcer.po.operator.parModel;
import static sorcer.po.operator.pars;
import static sorcer.po.operator.put;
import static sorcer.po.operator.set;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.Test;

import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.service.Context;
import sorcer.service.ServiceExertion;
import sorcer.util.Sorcer;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Pars {
	private final static Logger logger = Logger.getLogger(Pars.class
			.getName());

	static {
		ServiceExertion.debug = true;
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/policy/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "aritmeticInvokeModel-bean.jar" });
	}

	@Test
	public void runtimeScope() throws Exception {
		
		// a par is a variable (entry) evaluated in its own scope (context)
		Par y = par("y",
				invoker("(x1 * x2) - (x3 + x4)", pars("x1", "x2", "x3", "x4")));
		Object val = value(y, ent("x1", 10.0), ent("x2", 50.0),
				ent("x3", 20.0), ent("x4", 80.0));
		// logger.info("y value: " + val);
		assertEquals(val, 400.0);

	}
	
	
	@Test
	public void contextScope() throws Exception {
		
		// invokers use contextual scope of pars
		Par<?> add = par("add", invoker("x + y", pars("x", "y")));
		Context<Double> cxt = context(ent("x", 10.0), ent("y", 20.0));
		logger.info("par value: " + value(add, cxt));
		// evaluate a par 
		assertTrue(value(add, cxt).equals(30.0));

		// invoke with another context
		cxt = context(ent("x", 20.0), ent("y", 30.0));
		add = par(cxt, "add", invoker("x + y", pars("x", "y")));
		logger.info("par value: " + value(add));
		assertTrue(value(add).equals(50.0));

	}
	
	@Test
	public void dbParOperator() throws Exception {	
		
		Par<Double> dbp1 = persistent(par("design/in", 25.0));
		Par<String> dbp2 = dbPar("url/sobol", "http://sorcersoft.org/sobol");

		assertFalse(asis(dbp1) instanceof URL);
		assertFalse(asis(dbp2) instanceof URL);
			
		URL dbp1Url = url(dbp1);
		URL dbp2Url = url(dbp2);
		
		assertTrue(value(dbp1Url).equals(25.0));
		assertEquals(value(dbp2Url), "http://sorcersoft.org/sobol");
		
		assertTrue(value(dbp1).equals(25.0));
		assertEquals(value(dbp2), "http://sorcersoft.org/sobol");

		// update persistent values
		set(dbp1, 30.0);
		set(dbp2, "http://sorcersoft.org");
	
		assertTrue(value(url(dbp1)).equals(30.0));
		assertEquals(value(url(dbp2)), "http://sorcersoft.org");
		
		assertTrue(asis(dbp1) instanceof URL);
		assertTrue(asis(dbp2) instanceof URL);
	}

	
	@Test
	public void parFidelities() throws Exception {
		
		Par<Double> dbp = dbPar("shared/value", 25.0);
		
		Par multi = par("multi", 
				parFi(ent("init/value"), 
				dbp,
				ent("invoke", invoker("x + y", pars("x", "y")))));
		
		Context<Double> cxt = context(ent("x", 10.0), 
				ent("y", 20.0), ent("init/value", 49.0));
		
		set(dbp, 50.0);
		
		assertTrue(value(multi, cxt, parFi("shared/value")).equals(50.0));
		logger.info("shared/value: " + value(multi, cxt, parFi("shared/value")));
		
		assertTrue(value(multi, cxt, parFi("init/value")).equals(49.0));
		logger.info("init/value: " + value(multi, cxt, parFi("init/value")));
			
		assertTrue(value(multi, cxt, parFi("invoke")).equals(30.0));
		logger.info("invoke: " + value(multi, cxt, parFi("invoke")));
		
	}
	
	@Test
	public void parModelOperator() throws Exception {
		
		ParModel pm = parModel("par-model", par("v1", 1.0), par("v2", 2.0));
		add(pm, par("x", 10.0), ent("y", 20.0));
		// add an active ent, no scope
		add(pm, invoker("add1", "x + y", pars("x", "y")));
		// add a par with own scope
		add(pm, par(context(ent("x", 30), ent("y", 40.0)),
				invoker("add2", "x + y", pars("x", "y"))));
		
		assertEquals(value(pm, "add1"), 30.0);
		// change the scope of add1
		set(pm, "x", 20.0);
		assertEquals(value(pm, "add1"), 40.0);
		
		assertEquals(value(pm, "add2"), 70.0);
		// x is changed but add2 value is the same, has its own scope
		set(pm, "x", 20.0);
		assertEquals(value(pm, "add2"), 70.0);
		
	}
	
	
}
