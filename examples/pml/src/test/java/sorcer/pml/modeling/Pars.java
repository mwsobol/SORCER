package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.service.Context;

import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class Pars {
	private final static Logger logger = LoggerFactory.getLogger(Pars.class.getName());

	@Test
	public void parScope() throws Exception {
		// a par is a variable (entry) evaluated with its own scope (context)
		Context<Double> cxt = context(ent("x", 20.0), ent("y", 30.0));

		// par with its context scope
		Par<?> add = par(cxt, "add", invoker("x + y", pars("x", "y")));
		logger.info("par value: " + value(add));
		assertTrue(value(add).equals(50.0));

	}


	@Test
	public void contextScope() throws Exception {

		Context<Double> cxt = context(ent("x", 20.0), ent("y", 30.0));
		Par<?> add = par(cxt, "add", invoker("x + y", pars("x", "y")));

		// adding a par to the context updates par's scope
		add(cxt, add);

		// evaluate the entry of the context
		logger.info("context add value: " + value(cxt, "add"));
		assertTrue(value(cxt, "add").equals(50.0));

	}
	
	
	@Test
	public void closingParWihEntries() throws Exception {
		Par y = par("y",
				invoker("(x1 * x2) - (x3 + x4)", pars("x1", "x2", "x3", "x4")));
		Object val = value(y, ent("x1", 10.0), ent("x2", 50.0), ent("x3", 20.0), ent("x4", 80.0));
		// logger.info("y value: " + val);
		assertEquals(val, 400.0);
	}

	@Test
	public void closingParWitScope() throws Exception {

		// invokers use contextual scope of pars
		Par<?> add = par("add", invoker("x + y", pars("x", "y")));

		Context<Double> cxt = context(ent("x", 10.0), ent("y", 20.0));
		logger.info("par value: " + value(add, cxt));
		// evaluate a par 
		assertTrue(value(add, cxt).equals(30.0));

	}

	@Test
	public void dbParOperator() throws Exception {	
		
		Par<Double> dbp1 = persistent(par("design/in", 25.0));
		Par<String> dbp2 = dbPar("url/sobol", "http://sorcersoft.org/sobol");

		assertEquals(asis(dbp1), 25.0);
		assertTrue(asis(dbp2) instanceof URL);
			
		URL dbp1Url = storeArg(dbp1);
		URL dbp2Url = storeArg(dbp2);
		
		assertTrue(content(dbp1Url).equals(25.0));
		assertEquals(content(dbp2Url), "http://sorcersoft.org/sobol");
		
		assertTrue(value(dbp1).equals(25.0));
		assertEquals(value(dbp2), "http://sorcersoft.org/sobol");

		// update persistent values
		set(dbp1, 30.0);
		set(dbp2, "http://sorcersoft.org");
	
		assertTrue(content(storeArg(dbp1)).equals(30.0));
		assertEquals(content(storeArg(dbp2)), "http://sorcersoft.org");
		
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

		assertTrue(value(multi, cxt, parFi("init/value")).equals(49.0));

		assertTrue(value(multi, cxt, parFi("invoke")).equals(30.0));

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
