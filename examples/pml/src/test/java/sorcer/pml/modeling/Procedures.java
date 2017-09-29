package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
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

	public void smlSyntax() throws Exception {

		// Signatures
		sig s1 = sig("s1", Class.class);
		sig s2 = sig("s2", Class.class);
		Object o1 = exec(s1);

		// Entries
		val v1 = val("x1", 10.8);
		func f1 = proc("x2", 20.0);
		func f2 = func("v1 + f1", args("v1", "f1"));
		func f3 = lmbd("s1", args("v1", "f1"));
		func f4 = neo("x3", 1.0);
		func f5 = srv(sig("s1", Class.class));
		ent e1 = ent("x1", 10.0);
		ent f5b = srv(s1);
		ent v1b = val("x1", 10.8);

		// Contexts
		cxt c1 = context(v1, val("x4", 10.8), f1);

		// Mograms
		mog t1 = task(s1, c1);
		mog t2 = task(s1, s2, c1);
		mog m1 = model(v1, f1, f2, f3);
		mog m2 = model(m1, s1, t1);
		mog x1 = block(t1, t2, m1);
		mog x2 = job(t1, job(t2, m1));

		// Outputs
		Object o2 = value(v1);
		Object o3 = eval(f1);
        Object o4 = eval(e1);
		Object o5 = value(context(), "path");
		Object o6 = response(model(), "path");
		cxt c3 = response((Model) model());
		Object o7 = eval(t1);
		Object o8 = eval(block());
		Object o9 = eval(job());

		mog m3 = exert(task());
		mog m4 = exert(job());
		mog m5 = exert(model());
		cxt c4 = context(exert(job()));
		Object o10= exec(m3);
		Object o11 = exec(m5);

	}


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

		Context<Double> cxt = model(proc("x", 20.0), proc("y", 30.0));
		Proc add = proc("add", invoker("x + y", args("x", "y")), cxt);

		// adding a proc to the context updates proc's scope
		add(cxt, add);

		// process the entry of the context
		logger.info("context add eval: " + value(cxt, "add"));
		assertTrue(value(cxt, "add").equals(50.0));

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
		Proc<?> add = proc("add", invoker("x + y", args("x", "y")));

		Context<Double> cxt = context(val("x", 10.0), val("y", 20.0));
		logger.info("proc eval: " + eval(add, cxt));
		// process a proc
		assertTrue(eval(add, cxt).equals(30.0));

	}

	@Test
	public void dbProcOperator() throws Exception {
		
		Proc<Double> dbp1 = persistent(proc("design/in", 25.0));
		Proc<String> dbp2 = dbEnt("url/sobol", "http://sorcersoft.org/sobol");

		assertTrue(asis(dbp1).equals(25.0));
		assertEquals(asis(dbp2).getClass(), URL.class);
			
		URL dbp1Url = storeVal(dbp1);
		URL dbp2Url = storeVal(dbp2);

		assertTrue(content(dbp1Url).equals(25.0));
		assertEquals(content(dbp2Url), "http://sorcersoft.org/sobol");

		assertTrue(eval(dbp1).equals(25.0));
		assertEquals(eval(dbp2), "http://sorcersoft.org/sobol");

		// update persistent values
		setValue(dbp1, 30.0);
		setValue(dbp2, "http://sorcersoft.org");
	
		assertTrue(content(storeVal(dbp1)).equals(30.0));
		assertEquals(content(storeVal(dbp2)), "http://sorcersoft.org");

		assertEquals(asis(dbp1).getClass(), URL.class);
		assertEquals(asis(dbp2).getClass(), URL.class);

	}

	@Test
	public void procFidelities() throws Exception {
		
		Proc<Double> dbp = dbEnt("shared/eval", 25.0);
		
		Proc multi = proc("multi",
				pFi(ent("init/eval"),
				    dbp,
				    proc("invoke", invoker("x + y", args("x", "y")))));

		Context<Double> cxt = context(proc("x", 10.0),
				proc("y", 20.0), proc("init/eval", 49.0));

		setValue(dbp, 50.0);
		assertTrue(eval(dbp).equals(50.0));

		assertTrue(eval(multi, cxt, pFi("shared/eval")).equals(50.0));
		assertTrue(eval(multi, cxt, pFi("init/eval")).equals(49.0));
		assertTrue(eval(multi, cxt, pFi("invoke")).equals(30.0));

	}
	
	@Test
	public void procModelOperator() throws Exception {
		
		ProcModel pm = procModel("proc-model", proc("v1", 1.0), proc("v2", 2.0));
		add(pm, proc("x", 10.0), proc("y", 20.0));
		// add an active proc, no scope
		add(pm, invoker("add1", "x + y", args("x", "y")));
		// add a proc with own scope
		add(pm, proc(invoker("add2", "x + y", args("x", "y")),
				context(proc("x", 30.0), proc("y", 40.0))));
		
		assertEquals(eval(pm, "add1"), 30.0);
		// change the scope of add1
		setValue(pm, "x", 20.0);
		assertEquals(eval(pm, "add1"), 40.0);

		assertEquals(eval(pm, "add2"), 70.0);
		// x is changed but add2 eval is the same, has its own scope
		setValue(pm, "x", 20.0);
		assertEquals(eval(pm, "add2"), 70.0);
		
	}
}
