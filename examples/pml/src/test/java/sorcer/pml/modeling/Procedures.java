package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.po.operator;
import sorcer.service.Context;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.persistent;
import static sorcer.eo.operator.add;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.add;
import static sorcer.po.operator.*;
import static sorcer.mo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class Procedures {
	private final static Logger logger = LoggerFactory.getLogger(Procedures.class.getName());

	@Test
	public void parScope() throws Exception {
		// a proc is a variable (entry) evaluated with its own scope (context)
		Context<Double> cxt = context(proc("x", 20.0), proc("y", 30.0));

		// proc with its context scope
		Proc<?> add = proc("add", invoker("x + y", pars("x", "y")), cxt);
		logger.info("proc eval: " + eval(add));
		assertTrue(eval(add).equals(50.0));
	}


	@Test
	public void contextScope() throws Exception {

		Context<Double> cxt = context(proc("x", 20.0), proc("y", 30.0));
		Proc<?> add = proc("add", invoker("x + y", pars("x", "y")), cxt);

		// adding a proc to the context updates proc's scope
		add(cxt, add);

		// evaluate the entry of the context
		logger.info("context add eval: " + value(cxt, "add"));
		assertTrue(value(cxt, "add").equals(50.0));

	}
	
	
	@Test
	public void closingParWihEntries() throws Exception {
		Proc y = proc("y",
				invoker("(x1 * x2) - (x3 + x4)", pars("x1", "x2", "x3", "x4")));
		Object val = eval(y, proc("x1", 10.0), proc("x2", 50.0), proc("x3", 20.0), proc("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);
	}

	@Test
	public void closingParWitScope() throws Exception {

		// invokers use contextual scope of pars
		Proc<?> add = proc("add", invoker("x + y", pars("x", "y")));

		Context<Double> cxt = context(proc("x", 10.0), proc("y", 20.0));
		logger.info("proc eval: " + eval(add, cxt));
		// evaluate a proc
		assertTrue(eval(add, cxt).equals(30.0));

	}

	@Test
	public void dbParOperator() throws Exception {	
		
		Proc<Double> dbp1 = persistent(proc("design/in", 25.0));
		Proc<String> dbp2 = dbPar("url/sobol", "http://sorcersoft.org/sobol");

		assertTrue(asis(dbp1).equals(25.0));
		assertEquals(asis(dbp2).getClass(), URL.class);
			
		URL dbp1Url = storeArg(dbp1);
		URL dbp2Url = storeArg(dbp2);

		assertTrue(content(dbp1Url).equals(25.0));
		assertEquals(content(dbp2Url), "http://sorcersoft.org/sobol");
		
		assertTrue(eval(dbp1).equals(25.0));
		assertEquals(eval(dbp2), "http://sorcersoft.org/sobol");

		// update persistent values
		setValue(dbp1, 30.0);
		setValue(dbp2, "http://sorcersoft.org");
	
		assertTrue(content(storeArg(dbp1)).equals(30.0));
		assertEquals(content(storeArg(dbp2)), "http://sorcersoft.org");

		assertEquals(asis(dbp1).getClass(), URL.class);
		assertEquals(asis(dbp2).getClass(), URL.class);

	}

	@Test
	public void parFidelities() throws Exception {
		
		Proc<Double> dbp = dbPar("shared/eval", 25.0);
		
		Proc multi = proc("multi",
				parFi(ent("init/eval"),
				dbp,
				proc("invoke", invoker("x + y", pars("x", "y")))));
		
		Context<Double> cxt = context(proc("x", 10.0),
				proc("y", 20.0), proc("init/eval", 49.0));
		
		setValue(dbp, 50.0);

		assertTrue(eval(multi, cxt, parFi("shared/eval")).equals(50.0));

		assertTrue(eval(multi, cxt, parFi("init/eval")).equals(49.0));

		assertTrue(eval(multi, cxt, parFi("invoke")).equals(30.0));

	}
	
	@Test
	public void parModelOperator() throws Exception {
		
		ProcModel pm = procModel("proc-model", proc("v1", 1.0), proc("v2", 2.0));
		add(pm, proc("x", 10.0), proc("y", 20.0));
		// add an active proc, no scope
		add(pm, invoker("add1", "x + y", args("x", "y")));
		// add a proc with own scope
		add(pm, proc(invoker("add2", "x + y", args("x", "y")), context(proc("x", 30), proc("y", 40.0))
		));
		
		assertEquals(value(pm, "add1"), 30.0);
		// change the scope of add1
		setValue(pm, "x", 20.0);
		assertEquals(value(pm, "add1"), 40.0);

		assertEquals(value(pm, "add2"), 70.0);
		// x is changed but add2 eval is the same, has its own scope
		setValue(pm, "x", 20.0);
		assertEquals(value(pm, "add2"), 70.0);
		
	}
}
