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

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.inVal;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.job;
import static sorcer.mo.operator.model;
import static sorcer.mo.operator.response;
import static sorcer.po.operator.*;
import static sorcer.po.operator.srv;
import static sorcer.so.operator.*;
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
		sig s1 = sig("s1", Class.class);
		sig s2 = sig("s2", Class.class);
		Object o1 = exec(s1);

		// Entries
		val v1 = val("x1", 10.8);
		func f1 = proc("x2", 20.0);
		func f2 = func("v1 + f1", args("v1", "f1"));
		func f3 = lmbd("s1", args("v1", "f1"));
		func f4 = ane("x3", 1.0);
		func f5 = srv(sig("s1", Class.class));
		ent e1 = ent("x1", 10.0);
		ent f5b = srv(s1);
		ent v1b = val("x1", 10.8);

		// Data Contexts
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
		Object o5 = eval(t1);
		Object o6 = eval(block());
		Object o7 = eval(job());
		Object o8 = exec(m1);
		Object o9 = value(context(), "path");
		Object o10 = response(model(), "path");

		// returning entries
		ent e2 = act(v1);
		ent e3 = act(f1);
		ent e4 = act(context(), "path");

		// exerting mograms
		mog m3 = exert(task());
		mog m4 = exert(job());
		mog m5 = exert(model());
		cxt c3 = response(model());
		cxt c4 = context(job());
		cxt c5 = context(exert(job()));
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

		Metafidelity fi2 = fi("sysFi2",fi("mFi2", "divide"), fi("mFi3", "multiply"));
		Metafidelity fi3 = fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide"));
		Metafidelity fi4 = fi("sysFi4", fi("mFi3", "average"));

		Signature add = sig("add", AdderImpl.class,
			result("result/y1", inPaths("arg/x1", "arg/x2")));
		Signature subtract = sig("subtract", SubtractorImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));
		Signature average = sig("average", AveragerImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));
		Signature multiply = sig("multiply", MultiplierImpl.class,
			result("result/y1", inPaths("arg/x1", "arg/x2")));
		Signature divide = sig("divide", DividerImpl.class,
			result("result/y2", inPaths("arg/x1", "arg/x2")));

		// five entry multifidelity model with morphers
		Model mod = model(inVal("arg/x1", 90.0), inVal("arg/x2", 10.0),
			ent("arg/y1", entFi(inVal("arg/y1/fi1", 10.0), inVal("arg/y1/fi2", 11.0))),
			ent("arg/y2", entFi(inVal("arg/y2/fi1", 90.0), inVal("arg/y2/fi2", 91.0))),
			ent("mFi1", mrpFi(morpher1, add, multiply)),
			ent("mFi2", mrpFi(morpher2, average, divide, subtract)),
			ent("mFi3", mrpFi(average, divide, multiply)),
			// metafidelities
			fi2, fi3, fi4,
			Strategy.FidelityManagement.YES,
			response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

		Context out = response(mod);
		logger.info("out: " + out);
		assertTrue(get(out, "mFi1").equals(100.0));
		assertTrue(get(out, "mFi2").equals(9.0));
		assertTrue(get(out, "mFi3").equals(50.0));

		// closing the fidelity for mFi1
		out = response(mod , fi("mFi1", "multiply"));
		logger.info("out: " + out);
		assertTrue(get(out, "mFi1").equals(900.0));
		assertTrue(get(out, "mFi2").equals(50.0));
		assertTrue(get(out, "mFi3").equals(9.0));
	}


	@Test
	public void morphingMultiFidelityModel1() throws Exception {
		logger.info("" + (new ServiceContext("NN") instanceof Arg));
	}
}
