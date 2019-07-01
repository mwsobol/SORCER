package sorcer.sml.lambdas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.*;
import sorcer.service.Morpher;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ent;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.result;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.inc;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Models {
	private final static Logger logger = LoggerFactory.getLogger(Models.class);

	@Test
	public void lambdaModel() throws Exception {

		Model mdl = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
				ent("add/x1", 20.0), ent("add/x2", 80.0),
				lambda("add", (Context<Double> model) ->
						v(model, "add/x1") + v(model, "add/x2")),
				lambda("multiply", (Context<Double> model) ->
						v(model, "multiply/x1") * v(model, "multiply/x2")),
				lambda("subtract", (Context<Double> model) ->
						v(model, "multiply") - v(model, "add")),
				response("subtract", "multiply", "add"));

		logger.info("DEPS: " + printDeps(mdl));
		Context out = response(mdl);
		logger.info("model response: " + out);
		assertTrue(get(out, "subtract").equals(400.0));
		assertTrue(get(out, "multiply").equals(500.0));
		assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void settingLambdaModel() throws Exception {

		Model mdl = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
			    ent("add/x1", 20.0), ent("add/x2", 80.0),
				lambda("add", (Context<Double> model) ->
						v(model, "add/x1") + v(model, "add/x2")),
				lambda("multiply", (Context<Double> model) ->
						v(model, "multiply/x1") * v(model, "multiply/x2")),
				lambda("subtract", (Context<Double> model) ->
						v(model, "multiply") - v(model, "add")),
				response("subtract", "multiply", "add"));

		logger.info("DEPS: " + printDeps(mdl));
		Context out = response(mdl, ent("multiply/x1", 20.0), ent("multiply/x2", 100.0));
		logger.info("model response: " + out);
		assertTrue(get(out, "subtract").equals(1900.0));
		assertTrue(get(out, "multiply").equals(2000.0));
		assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void lazyLambdaModel() throws Exception {
		// compute multiply only once

		Model mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
			    ent("add/x1", 20.0), ent("add/x2", 80.0),
			    ent("multiply/done", false),
				lambda("add", (Context<Double> model) ->
						v(model, "add/x1") + v(model, "add/x2")),
				lambda("multiply", (Context<Double> model) ->
						v(model, "multiply/x1") * v(model, "multiply/x2")),
				lambda("subtract", (Context<Double> model) ->
						v(model, "multiply") - v(model, "add")),
				lambda("multiply2", (Context<Object> cxt) -> {
					ent multiply = (ent) get(cxt, "multiply");
					double out = 0;
					if (value(cxt, "multiply/done").equals(false)) {
						out = (double) exec(multiply, cxt) + 10.0;
						set(cxt, "multiply/done", true);
					} else {
						out = (double)value(cxt, "multiply");
					}
					return out;
				}),
				lambda("multiply3", (Context<Object> cxt) -> {
					ent multiply = (ent) get(cxt, "multiply");
					double out = 0;
					if (value(cxt, "multiply/done").equals(false)) {
						out = (double) exec(multiply, cxt);
						set(cxt, "multiply/done", true);
					} else {
						out = (double)value(cxt, "multiply");
					}
					return out;
				}),
				response("multiply2", "multiply3"));

		dependsOn(mo, dep("subtract", paths("multiply2", "add")));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "multiply2").equals(510.0));
		assertTrue(get(out, "multiply3").equals(500.0));
	}


	@Test
	public void dynamicLambdaModel() throws Exception {
		// change scope at runtime for a selected entry ("multiply") in the model

		Model mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
			    ent("add/x1", 20.0), ent("add/x2", 80.0),
				lambda("add", (Context <Double> model) ->
						v(model, "add/x1") + v(model, "add/x2")),
				lambda("multiply", (Context <Double> model) ->
						v(model, "multiply/x1") * v(model, "multiply/x2")),
				lambda("subtract", (Context <Double> model) ->
						v(model, "multiply") - v(model, "add")),
				lambda("multiply2", "multiply", (Service entry, Context scope, Arg[] args) -> {
					double out = (double) exec(entry, scope);
					// out is result of multiply
					if (out > 400) {
						putValue(scope, "multiply/x1", 20.0);
						putValue(scope, "multiply/x2", 50.0);
						out = (double)exec(entry, scope);
					}
					return context(val("multiply2", out));
                } ),
				response("subtract", "multiply", "multiply2", "add"));

		dependsOn(mo, dep("subtract", paths("multiply", "add")));

        Context out = response(mo);
        logger.info("model response: " + out);
        assertTrue(get(out, "subtract").equals(400.0));
        assertTrue(get(out, "multiply2").equals(1000.0));
        assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void lambdaModelWithReturnPath() throws Exception {

		Domain mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
			    ent("add/x1", 20.0), ent("add/x2", 80.0),
			    ent("arg/x1", 30.0), ent("arg/x2", 90.0),
				lambda("add", (Context <Double> model) ->
								v(model, "add/x1") + v(model, "add/x2"),
						result("add/out",
								inPaths("add/x1", "add/x2"))),
				lambda("multiply", (Context <Double> model) ->
								v(model, "multiply/x1") * v(model, "multiply/x2"),
						result("multiply/out",
								inPaths("multiply/x1", "multiply/x2"))),
				lambda("subtract", (Context <Double> model) ->
								v(model, "multiply/out") - v(model, "add/out"),
						result("model/response")),
				response("subtract", "multiply/out", "add/out", "model/response"));

		logger.info("DEPS: " + printDeps(mo));
		dependsOn(mo, dep("subtract", paths("multiply", "add")));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "model/response").equals(400.0));
		assertTrue(get(out, "multiply/out").equals(500.0));
		assertTrue(get(out, "add/out").equals(100.0));
	}

	@Test
	public void addLambdaEntry() throws Exception {

		Double delta = 0.5;

		EntryCollable entFunction = (Model mdl) -> {
			double out = (double) exec(mdl, "multiply");
			out = out + 1000.0 + delta;
			return ent("out", out);
		};

		Model mo = model(
				inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
				inVal("add/x1", 20.0), inVal("add/x2", 80.0),
				ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				ent(sig("subtract", SubtractorImpl.class, result("subtract/out",
						inPaths("multiply/out", "add/out")))),
				response("subtract", "lambda", "out"));

	//	dependsOn(mo, dep("subtract", paths("multiply", "add")));

		add(mo, lambda("lambda", entFunction));

		Context out = response(mo);
		logger.info("response: " + out);
		assertTrue(get(out, "subtract").equals(400.0));
		assertTrue(get(out, "out").equals(1500.5));
		assertTrue(get(out, "lambda").equals(1500.5));
	}

	@Test
	public void entryReturnValueWithSubstitution() throws Exception {

		EntryCollable lambdaTask = (Model mdl) -> {
			Context out = null;
			Double value = null;
			Task task = (Task) asis(mdl, "task/multiply");
			put(context(task), "arg/x1", 20.0);
			put(context(task), "arg/x2", 100.0);
			out = context(exert(task));
			value = (Double)get(out, "multiply/result");
			// overwrite the original eval with a new task
			return val("multiply/out", value);
		};

		// usage of in and out connectors associated with model
		Task modelTask = task(
				"task/multiply",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("multiply/result")));

		Domain mdl = model(
				inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
				inVal("add/x1", 20.0), inVal("add/x2", 80.0),
				ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				ent(sig("subtract", SubtractorImpl.class, result("subtract/out",
						inPaths("multiply/out", "add/out")))),
				response("subtract", "multiply"));
//				response("lambda"));

		dependsOn(mdl, dep("subtract", paths("lambda", "add")));

		add(mdl, modelTask);
		add(mdl, lambda("lambda", lambdaTask));
//		responseDown(mdl, "multiply");
		responseUp(mdl, "lambda");

		Context out = response(mdl);
		logger.info("response: " + out);
//		assertTrue(getValue(out, "multiply").equals(500.0));
//		assertTrue(getValue(out, "lambda").equals(2000.0));
//		assertTrue(getValue(out, "subtract").equals(1900.0));
	}


	@Test
	public void lambdaTaskLoop() throws Exception {
		Task ti = task(
				sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", inc("arg/x2", 2.0)),
						inVal("arg/x2", 80.0), result("task/result")));

		Block lb = block(sig(ServiceConcatenator.class),
				context(ent("sum", 0.0)),
				loop(0, 100, task(lambda("sum", (Context<Double> cxt) -> {
					Double out = value(cxt, "sum") + (Double) exec(ti);
					putValue(context(ti), "arg/x2", (Double)value(context(ti), "arg/x2") + 1.5);
					return out; }))));

		lb = exert(lb);

		assertTrue(value(context(lb), "sum").equals(31050.0));
	}

	@Test
	public void canditionalEntryRangeLoop() throws Exception {
		Task ti = task(
				sig("add", AdderImpl.class),
				model("add", inVal("arg/x1", inc("arg/x2", 2.0)),
						inVal("arg/x2", 80.0), result("task/result")));

		Block lb = block(sig(ServiceConcatenator.class),
				context(ent("sum", 0.0),
					ent("from", 320.0), ent("to", 420.0)),
				loop(0, 100, task(lambda("sum", (Context<Double> cxt) -> {
					Double from = value(cxt, "from");
					Double to = value(cxt, "to");
					Double out = value(cxt, "sum") + (Double) exec(ti);
					putValue(context(ti), "arg/x2", (Double)value(context(ti), "arg/x2") + 1.5);

					// skip eval 333 but with increase by 100
					if (out > from && out < to) {
						out = value(cxt, "sum") + 100.0;
					}
					return out; }))));

		lb = exert(lb);
		logger.info("block context: " + context(lb));
		assertTrue(value(context(lb), "sum").equals(30985.0));
	}

    @Test
    public void amorphousModel() throws Exception {

		Morpher mFi1Morpher = (mgr, mFi, value) -> {
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

        Morpher mFi2Morpher = (mgr, mFi, value) -> {
            Fidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("divide")) {
                if (((Double) value) <= 9.0) {
                    mgr.morph("sysFi4");
                } else {
                    mgr.morph("sysFi3");
                }
            }
        };

        Metafidelity fi2 = metaFi("sysFi2",fi("divide", "mFi2"), fi("multiply", "mFi3"));
		Metafidelity fi3 = metaFi("sysFi3", fi("average", "mFi2"), fi("divide", "mFi3"));
		Metafidelity fi4 = metaFi("sysFi4", fi("average", "mFi3"));

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

        // three entry multifidelity model with morphers
        Model mod = model(inVal("arg/x1", 90.0), inVal("arg/x2", 10.0),
                ent("mFi1", mphFi(mFi1Morpher, add, multiply)),
                ent("mFi2", mphFi(mFi2Morpher, average, divide, subtract)),
                ent("mFi3", mphFi(average, divide, multiply)),
                fi2, fi3, fi4,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(50.0));

        // first closing the fidelity for mFi1
        out = response(mod , fi("mFi1", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }
}
	
	
