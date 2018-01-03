package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Proc;
import sorcer.service.Context;
import sorcer.service.modeling.*;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class Procedures {
	private final static Logger logger = LoggerFactory.getLogger(Procedures.class.getName());

	@Test
	public void procScope() throws Exception {
		// a proc is a variable (entry) evaluated with its own scope (context)
		Context<Double> cxt = context(proc("x", 20.0), proc("y", 30.0));

		// proc with its context scope
		Proc add = proc("add", invoker("x + y", args("x", "y")), cxt);
		logger.info("proc eval: " + eval(add));
		assertTrue(eval(add).equals(50.0));
	}


	@Test
	public void modelScope() throws Exception {

		Model mdl = model(proc("x", 20.0), proc("y", 30.0));
		Proc add = proc("add", invoker("x + y", args("x", "y")), mdl);

		// adding a proc to the model updates proc's scope
		add(mdl, add);

		// evaluate entry of the context
		logger.info("eval add : " + eval(mdl, "add"));
		assertTrue(eval(mdl, "add").equals(50.0));

	}
	
	@Test
	public void closingProcWihEntries() throws Exception {
		Proc y = proc("y",
				invoker("(x1 * x2) - (x3 + x4)", args("x1", "x2", "x3", "x4")));
		Object val = eval(y, val("x1", 10.0), val("x2", 50.0), val("x3", 20.0), val("x4", 80.0));
		// logger.info("y eval: " + val);
		assertEquals(val, 400.0);
	}

	@Test
	public void closingProcWitScope() throws Exception {

		// invokers use contextual scope of args
		Proc add = proc("add", invoker("x + y", args("x", "y")));

		Context<Double> cxt = context(val("x", 10.0), val("y", 20.0));
		logger.info("proc eval: " + eval(add, cxt));
		// process a proc
		assertTrue(eval(add, cxt).equals(30.0));

	}

	@Test
	public void dbProcOperator() throws Exception {
		
		Proc<Double> dbp1 = persistent(proc("design/in", 25.0));
		Proc<String> dbp2 = dbEnt("url/sobol", "http://sorcersoft.org/sobol");

		// dbp1 is declared to be persisted
		assertTrue(asis(dbp1).equals(25.0));
		assertEquals(asis(dbp1).getClass(), Double.class);
		// dbp2 is persisted already
		assertEquals(asis(dbp2).getClass(), URL.class);
			
		URL dbp1Url = storeVal(dbp1);
		URL dbp2Url = (URL) impl(dbp2);

		assertTrue(content(dbp1Url).equals(25.0));
		assertEquals(content(dbp2Url), "http://sorcersoft.org/sobol");

		assertTrue(eval(dbp1).equals(25.0));
		assertEquals(eval(dbp2), "http://sorcersoft.org/sobol");

		// TODO update does not occur
		// update persistent values
		setValue(dbp1, 30.0);
		setValue(dbp2, "http://sorcersoft.org");

		logger.info("dbp1: " + eval(dbp1));
		logger.info("dbp2: " + eval(dbp2));

		assertTrue(eval(dbp1).equals(30.0));
		assertTrue(eval(dbp2).equals("http://sorcersoft.org"));

		assertEquals(asis(dbp1).getClass(), URL.class);
		assertEquals(asis(dbp2).getClass(), URL.class);
	}

	@Test
	public void procFidelities() throws Exception {
		
		Proc<Double> dbp = dbEnt("shared/eval", 25.0);
		
		Proc multi = proc("multi",
				pFi(proc("init/eval"),
                	dbp,
					proc("invoke", invoker("x + y", args("x", "y")))));

		Model mdl = model(proc("x", 10.0),
				proc("y", 20.0), proc("init/eval", 49.0));

		setValue(dbp, 50.0);
		assertTrue(eval(dbp).equals(50.0));

		assertTrue(eval(multi, mdl, pFi("shared/eval")).equals(50.0));
		assertTrue(eval(multi, mdl, pFi("init/eval")).equals(49.0));
		assertTrue(eval(multi, mdl, pFi("invoke")).equals(30.0));

	}
	
	@Test
	public void procModelOperator() throws Exception {
		
		Model mdl = procModel("proc-model", proc("v1", 1.0), proc("v2", 2.0));
		add(mdl, proc("x", 10.0), proc("y", 20.0));
		// add an active proc, no scope
		add(mdl, invoker("add1", "x + y", args("x", "y")));
		// add a proc with own scope
		add(mdl, proc(invoker("add2", "x + y", args("x", "y")),
				context(proc("x", 30.0), proc("y", 40.0))));
		
		assertEquals(eval(mdl, "add1"), 30.0);
		// change the scope of add1
		setValue(mdl, "x", 20.0);
		assertEquals(eval(mdl, "add1"), 40.0);

		assertEquals(eval(mdl, "add2"), 70.0);
		// x is changed but add2 eval is the same, has its own scope
		setValue(mdl, "x", 20.0);
		assertEquals(eval(mdl, "add2"), 70.0);
		
	}
}
