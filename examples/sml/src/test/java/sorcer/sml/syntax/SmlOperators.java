package sorcer.sml.syntax;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.context.ServiceContext;
import sorcer.core.plexus.Morpher;
import sorcer.service.*;
import sorcer.service.modeling.*;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.inVal;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.job;
import static sorcer.so.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.ent.operator.srv;
import static sorcer.so.operator.exert;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class SmlOperators {

	private final static Logger logger = LoggerFactory.getLogger(SmlOperators.class.getName());

	@Ignore
	public void smlBasicSyntax() throws Exception {

		// Signatures
		sig op1 = sig("op1", Object.class);
		sig op2 = sig("op2", Collection.class, List.class);

		// Evaluators
		evr ev1 = expr("exprssionToExecute");

		// Entries
		val v1 = val("x2", 10.6);
		func p1 = pro("x3", ev1);
		func s1 = srv(op1);
		func f1 = lmbd("s1", args("v1", "f1"));
		func f2 = neu("x6", 1.0);

		// ent - generic operator for all entries
		ent e1 = ent(sig("s1", Class.class));
		ent e2 = ent("x3", ev1);

		// Data Contexts
		cxt c1 = context(v1, val("x4", 10.8), execEnt(p1), execEnt(f2));

		// Mograms
		mog m1 = model(v1, p1, f1, f2);
		mog t1 = task(op1, c1);
		mog t2 = task(op1, op2, c1);
		mog ex1 = block(t1, t2, m1);
		mog ex2 = job(t1, job(t2, m1));
		mog m2 = model(m1, op1, t1, ex2);

		// Object outputs
		Object o1 = exec(op1);
		Object o2 = exec(f1);
		Object o3 = exec(e1);
		Object o4 = exec(t1);
		Object o5 = exec(block());
		Object o6 = exec(job());
		Object o7 = exec(m1);
		Object o8 = exec(ev1);

		// Object specific outputs
		Object o9 = get(v1);
		Object o10 = value(context(), "path");
		Object o11 = exec(model(), "path");
		Object o12 = exec(model(), "path", "domain");

		// Entries for results of exec
		ent e3 = execEnt(v1);
		ent e4 = execEnt(f1);
		ent e5 = execEnt(job());
		ent e6 = execEnt(model());;
		ent e7 = execEnt(model(), "path");

		// Exerting mograms
		mog m3 = exert(task());
		mog m4 = exert(job());
		mog m5 = exert(model());

		// Data context of mograms
		cxt c2 = context(job());
		cxt c3 = context(exert(job()));

		// Evaluate mograms
		cxt c4 = eval(model());
		cxt c5 = eval(ex2);
		// Mogram results
		cxt out1 = result(model());
		cxt out2 = result(job());

		// Evalate specific models
		// Context, Table, row is rsp (Response)
		rsp r1 = eval(model());
		rsp r2 = row(c2);

		// clear service
		clear(ex2);
	}

	@Test
	public void morphingMultiFidelityModel() throws Exception {

		Morpher morpher1 = (mgr, mFi, value) -> {
			Fidelity<Signature> fi =  mFi.getFidelity();
			if (fi.getSelectName().equals("add")) {
				if (((Double) value) <= 200.0) {
					mgr.morph("sysFi2");
				} else {
					mgr.morph("sysFi3");
				}
			} else if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("multiply")) {
				mgr.morph("sysFi3");
			}
		};

		Morpher morpher2 = (mgr, mFi, value) -> {
			Fidelity<Signature> fi =  mFi.getFidelity();
			if (fi.getSelectName().equals("divide")) {
				if (((Double) value) <= 9.0) {
					mgr.morph("sysFi4");
				} else {
					mgr.morph("sysFi3");
				}
			}
		};

		fi fi2 = metaFi("sysFi2",fi("mFi2", "divide"), fi("mFi3", "multiply"));
		fi fi3 = metaFi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide"));
		fi fi4 = metaFi("sysFi4", fi("mFi3", "average"));

		sig add = sig("add", AdderImpl.class,
			result("result/y1", inPaths("arg/x1", "arg/x2")));
		sig subtract = sig("subtract", SubtractorImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));
		sig average = sig("average", AveragerImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));
		sig multiply = sig("multiply", MultiplierImpl.class,
			result("result/y1", inPaths("arg/x1", "arg/x2")));
		sig divide = sig("divide", DividerImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));

		// five entry multifidelity model with morphers
		mog mod = model(inVal("arg/x1", 90.0), inVal("arg/x2", 10.0),
			ent("arg/y1", entFi(inVal("arg/y1/fi1", 10.0), inVal("arg/y1/fi2", 11.0))),
			ent("arg/y2", entFi(inVal("arg/y2/fi1", 90.0), inVal("arg/y2/fi2", 91.0))),
			ent("mFi1", mphFi(morpher1, add, multiply)),
			ent("mFi2", mphFi(morpher2, average, divide, subtract)),
			ent("mFi3", mphFi(average, divide, multiply)),
			// metafidelities
			fi2, fi3, fi4,
			Strategy.FidelityManagement.YES,
			response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

		cxt out = response(mod);
		logger.info("out: " + out);
		assertTrue(value(out, "mFi1").equals(100.0));
		assertTrue(value(out, "mFi2").equals(9.0));
		assertTrue(value(out, "mFi3").equals(50.0));

		// closing the fidelity for mFi1
		out = response(mod , fi("mFi1", "multiply"));
		logger.info("out: " + out);
		assertTrue(value(out, "mFi1").equals(900.0));
		assertTrue(value(out, "mFi2").equals(50.0));
		assertTrue(value(out, "mFi3").equals(9.0));
	}


	@Test
	public void morphingMultiFidelityModel1() throws Exception {
		logger.info("" + (new ServiceContext("NN") instanceof Arg));
	}
}
