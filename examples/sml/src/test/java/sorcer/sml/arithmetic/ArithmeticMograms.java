package sorcer.sml.arithmetic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.plexus.Morpher;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.po.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ContextModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.invoker;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ArithmeticMograms {
	private final static Logger logger = LoggerFactory.getLogger(ArithmeticMograms.class);
	private Morpher mFi1Morpher;

	@Test
	public void lambdaEntryModel() throws Exception {
		// all model args as functions - Java lambda expressions

		ContextModel mo = model(operator.ent("multiply/x1", 10.0), operator.ent("multiply/x2", 50.0),
				operator.ent("add/x1", 20.0), operator.ent("add/x2", 80.0),
				operator.lambda("add", (Context <Double> model) ->
						value(model, "add/x1") + value(model, "add/x2"), args("add/x1", "add/x2")),
				operator.lambda("multiply", (Context <Double> model) ->
						value(model, "multiply/x1") * value(model, "multiply/x2"), args("multiply/x1", "multiply/x2")),
				operator.lambda("subtract", (Context <Double> model) ->
						value(model, "multiply") - value(model, "add"), result("add/out",
                        inPaths("multiply", "add"))),
				response("subtract", "multiply", "add"));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "subtract").equals(400.0));
		assertTrue(get(out, "multiply").equals(500.0));
		assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void dynamicLambdaModel() throws Exception {
		// change scope at runtime for a selected entry ("multiply") in the model

		Model mo = model(operator.ent("multiply/x1", 10.0), operator.ent("multiply/x2", 50.0),
				operator.ent("add/x1", 20.0), operator.ent("add/x2", 80.0),
				operator.lambda("add", (Context <Double> model) ->
						value(model, "add/x1") + value(model, "add/x2")),
				operator.lambda("multiply", (Context <Double> model) ->
						value(model, "multiply/x1") * value(model, "multiply/x2")),
				operator.lambda("subtract", (Context <Double> model) ->
						value(model, "multiply") - value(model, "add")),
				operator.lambda("multiply2", "multiply", (Service entry, Context scope, Arg[] args) -> {
					double out = (double)exec(entry, scope);
					if (out > 400) {
						putValue(scope, "multiply/x1", 20.0);
						putValue(scope, "multiply/x2", 50.0);
						out = (double)exec(entry, scope);
					}
					return context(operator.ent("multiply2", out));
				} ),
				response("subtract", "multiply2", "add"));

		dependsOn(mo, operator.ent("subtract", paths("multiply2", "add")));
		Object val = asis(mo, "subtract");
		if (val instanceof Srv) {
			Srv srv = ((Srv)val);
			if (srv.value() instanceof ContextCallable) {
				ContextCallable ctx = (ContextCallable) srv.value();
				logger.info("class: " + ctx.getClass());
			}
		}
		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "subtract").equals(900.0));
		assertTrue(get(out, "multiply2").equals(1000.0));
		assertTrue(get(out, "add").equals(100.0));
	}

	@Test
	public void lambdaModelWithReturnPath() throws Exception {

		ContextModel mo = model(operator.ent("multiply/x1", 10.0), operator.ent("multiply/x2", 50.0),
				operator.ent("add/x1", 20.0), operator.ent("add/x2", 80.0),
				operator.ent("arg/x1", 30.0), operator.ent("arg/x2", 90.0),
				operator.lambda("add", (Context <Double> model) ->
						value(model, "add/x1") + value(model, "add/x2"),
						result("add/out",
								inPaths("add/x1", "add/x2"))),
				operator.lambda("multiply", (Context <Double> model) ->
								value(model, "multiply/x1") * value(model, "multiply/x2"),
						result("multiply/out",
								inPaths("multiply/x1", "multiply/x2"))),
				operator.lambda("subtract", (Context <Double> model) ->
						value(model, "multiply/out") - value(model, "add/out"),
						result("model/response", inPaths("multiply/out", "add/out"))),
				response("subtract", "multiply/out", "add/out", "model/response"));

		Context out = response(mo);
		logger.info("model response: " + out);
		assertTrue(get(out, "model/response").equals(400.0));
		assertTrue(get(out, "multiply/out").equals(500.0));
		assertTrue(get(out, "add/out").equals(100.0));
	}

	@Test
	public void sigLocalModel() throws Exception {
		// get response from a local service model and resolve dependencies

		ContextModel m = model(
				inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
				inVal("add/x1", 20.0), inVal("add/x2", 80.0),
				operator.ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				operator.ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				operator.ent(sig("subtract", SubtractorImpl.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		logger.info("dependencies: " + dependencies(m));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

	@Test
	public void sigRemoteModel() throws Exception {
		// get response from a remote service model and resolve dependencies

		ContextModel m = model(
				inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
				inVal("add/x1", 20.0), inVal("add/x2", 80.0),
				operator.ent(sig("multiply", Multiplier.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				operator.ent(sig("add", Adder.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				operator.ent(sig("subtract", Subtractor.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		logger.info("dependencies: " + dependencies(m));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

	@Test
	public void sigMixedModel() throws Exception {
		// get response from a remote service model and resolve dependencies

		ContextModel m = model(
				inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
				inVal("add/x1", 20.0), inVal("add/x2", 80.0),
				operator.ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
						inPaths("multiply/x1", "multiply/x2")))),
				operator.ent(sig("add", AdderImpl.class, result("add/out",
						inPaths("add/x1", "add/x2")))),
				operator.ent(sig("subtract", Subtractor.class, result("model/response",
						inPaths("multiply/out", "add/out")))),
				response("subtract"));

		logger.info("dependencies: " + dependencies(m));
//        logger.info("response: " + response(m));
		Context out = response(m);

		assertTrue(get(out, "subtract").equals(400.0));
	}

    @Test
    public void batchTask() throws Exception {
        // batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
        // shared context with named paths
        Task batch3 = task("batch3",
                type(sig("multiply", MultiplierImpl.class,
						result("subtract/x1", Signature.Direction.IN)), Signature.PRE),
                type(sig("add", AdderImpl.class,
						result("subtract/x2", Signature.Direction.IN)), Signature.PRE),
                sig("subtract", SubtractorImpl.class,
						result("result/y", inPaths("subtract/x1", "subtract/x2"))),
                context(inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                        inVal("add/x1", 20.0), inVal("add/x2", 80.0)));

        logger.info("task getSelects:" + fi(batch3));

        batch3 = exert(batch3);
		//logger.info("task result/y: " + get(batch3, "result/y"));
		assertEquals(get(batch3, "result/y"), 400.0);
    }

	@Test
	public void arithmeticBlock() throws Exception {

		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/t4"), inVal("arg/t5"),
						result("block/result", Signature.Direction.OUT)));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1"), inVal("arg/x2"),
						result("arg/t4", Signature.Direction.IN)));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", inVal("arg/x3"), inVal("arg/x4"),
						result("arg/t5", Signature.Direction.IN)));

		Block block = block("block", t4, t5, t3, context(
				inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
				inVal("arg/x3", 20.0), inVal("arg/x4", 80.0)));

		Block result = exert(block);
		assertEquals(value(context(result), "block/result"), 400.00);
	}

    @Test
    public void altBlock() throws Exception {
        Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                context("subtract", inVal("arg/t4"), inVal("arg/t5"),
                        result("block/result", Signature.Direction.OUT)));

        Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        result("arg/t4", Signature.Direction.IN)));

        Task t5 = task("t5", sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        result("arg/t5", Signature.Direction.IN)));

        Task t6 = task("t6", sig("average", AveragerImpl.class),
                context("average", inVal("arg/t4"), inVal("arg/t5"),
                        result("block/result", Signature.Direction.OUT)));

        Block block = block("block", t4, t5,
                alt(opt(condition((Context<Double> cxt) -> value(cxt, "t4") > value(cxt, "t5")), t3),
                        opt(condition((Context<Double> cxt) -> value(cxt, "t4") <= value(cxt, "t5")), t6)));


        Block result = exert(block);
        assertEquals(value(context(result), "block/result"), 400.00);
    }

    @Test
    public void loopBlock() throws Exception {
        Block block = block("block",
                context(operator.ent("x1", 10.0), operator.ent("x2", 20.0), operator.ent("z", 100.0)),
                loop(condition((Context<Double> scope) -> value(scope, "x1") + value(scope, "x2")
                                < value(scope, "z")),
                        task(operator.ent("x1", invoker("x1 + 3", args("x1"))))));

        block = exert(block);
        assertEquals(value(context(block), "x1"), 82.00);
    }

	@Test
	public void jobPipeline() throws Exception {

		Task t3 = task("t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
						outVal("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						outVal("result/y")));

		Job job = job(sig("exert", ServiceJobber.class),
				"j1", t4, t5, t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertTrue(value(context, "j1/t3/result/y").equals(400.0));
	}

	@Test
	public void nestedJob() throws Exception {

		Task t3 = task("t3",
				sig("subtract", SubtractorImpl.class),
				context("subtract", inVal("arg/x1"), inVal("arg/x2"),
						result("result/y")));

		Task t4 = task("t4",
				sig("multiply", MultiplierImpl.class),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						result("result/y")));

		Task t5 = task("t5",
				sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
						result("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job(
				"j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5), t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		Context context = upcontext(exert(job));
		logger.info("job context: " + context);
		assertTrue(value(context, "j1/t3/result/y").equals(400.0));
	}

    @Test
    public void amorphousModel() throws Exception {

		Morpher mFi1Morpher =  (mgr, mFi, value) -> {
			ServiceFidelity<Signature> fi =  mFi.getFidelity();
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
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("divide")) {
                if (((Double) value) <= 9.0) {
                    mgr.morph("sysFi4");
                } else {
                    mgr.morph("sysFi3");
                }
            }
        };

        ServiceFidelity<Fidelity> fi2 = fi("sysFi2",fi("mFi2", "divide"), fi("mFi3", "multiply"));
		ServiceFidelity<Fidelity> fi3 = fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide"));
		ServiceFidelity<Fidelity> fi4 = fi("sysFi4", fi("mFi3", "average"));

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

        // multifidelity model with morphers
        ContextModel mod = model(inVal("arg/x1", 90.0), inVal("arg/x2", 10.0),
                operator.ent("mFi1", mFi(mFi1Morpher, add, multiply)),
                operator.ent("mFi2", mFi(mFi2Morpher, average, divide, subtract)),
                operator.ent("mFi3", mFi(average, divide, multiply)),
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
	
	
