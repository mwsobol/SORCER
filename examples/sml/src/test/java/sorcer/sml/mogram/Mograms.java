package sorcer.sml.mogram;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.result;
import static sorcer.po.operator.invoker;

/**
 * Created by Mike Sobolewski on 10/21/15.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Mograms {

    private final static Logger logger = LoggerFactory.getLogger(Mograms.class);

    @Test
    public void exertExertionToModelMogram() throws Exception {

        // usage of in and out connectors associated with model
        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("multiply/result/y")));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("add/result/y")));

        // in connector from exertion to model
        Context taskOutConnector = outConn(inEnt("add/x1", "j2/t4/multiply/result/y"),
                inEnt("multiply/x1", "j2/t5/add/result/y"));

        Job j2 = job("j2", sig("service", ServiceJobber.class),
                t4, t5, strategy(Flow.PAR),
                taskOutConnector);

        // out connector from model
        Context modelOutConnector = outConn(inEnt("y1", "add"), inEnt("y2", "multiply"), inEnt("y3", "subtract"));

        Model model = model(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/out",
                        inPaths("multiply/out", "add/out")))));

        responseUp(model, "add", "multiply", "subtract");
        dependsOn(model, ent("subtract", paths("multiply", "add")));
        // specify how model connects to exertion
        outConn(model, modelOutConnector);

        Block block = block("mogram", j2, model);

        Context result = context(exert(block));

        logger.info("result: " + result);

        assertTrue(value(result, "add").equals(580.0));
        assertTrue(value(result, "multiply").equals(5000.0));
        assertTrue(value(result, "y1").equals(580.0));
        assertTrue(value(result, "y2").equals(5000.0));
        assertTrue(value(result, "y3").equals(4420.0));
    }

    @Test
    public void incrementer() throws Exception {

        IncrementerImpl incrementer = new IncrementerImpl(100.0);

        Model model = model(
                inEnt("by", 10.0),
                ent(sig("increment", incrementer, result("out",
                        inPaths("by")))));

        responseUp(model, "increment", "out");

        Model exerted = exert(model);
        logger.info("out context: " + exerted);
        assertTrue(value(exerted, "out").equals(110.0));

        ((SrvModel)exerted).clearOutputs();

        exerted = exert(model);
        logger.info("out context: " + exerted);
        assertTrue(value(exerted, "out").equals(120.0));
    }

    @Test
    public void exertMstcGateSchema() throws Exception {

        IncrementerImpl incrementer = new IncrementerImpl(100.0);

        Model model = model(
                inEnt("by", 10.0), inEnt("out", 0.0),
                ent(sig("increment", incrementer, result("out", inPaths("by")))),
                ent("multiply", invoker("add * out", ents("add", "out"))));


        responseUp(model, "increment", "out", "multiply");
//        Model exerted = exert(model);
//        logger.info("out context: " + exerted);
//        assertTrue(value(exerted, "out").equals(110.0));

        Block looping = block(
                context(inEnt("offDesignCasesTemplate", "URL")),
                task(sig("add", AdderImpl.class),
                        context(inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("add"))),
                loop(condition(cxt -> (double)value(cxt, "out") < 1000.0),
                        model));

        looping = exert(looping);
        logger.info("block context: " + context(looping));
        logger.info("result: " + value(context(looping), "out"));
        logger.info("model result: " + value(result(model), "out"));
        logger.info("multiply result: " + value(result(model), "multiply"));
        // out variable in blosck
        assertTrue(value(context(looping), "out").equals(1000.0));
        // out variable in model
        assertTrue(value(result(model), "out").equals(1000.0));
        assertTrue(value(result(model), "multiply").equals(100000.0));
    }

    @Test
     public void modelWithInnerTask() throws Exception {

        // usage of in and out connectors associated with model
        Task innerTask = task(
                "task/multiply",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        result("multiply/result")));

        Model m = model(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                ent(sig("out", "average", AveragerImpl.class, result("model/response",
                        inPaths("task/multiply", "subtract")))),
                response("task/multiply", "subtract", "out"));

        dependsOn(m, ent("subtract", paths("multiply", "add")));

        add(m, innerTask);

        Context out = response(m);
        logger.info("response: " + out);
        assertTrue(get(out, "task/multiply").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));
        assertTrue(get(out, "out").equals(450.0));
    }

    @Test
    public void modelWithInnerModel() throws Exception {
        // get responses from a service model with inner model

        Model innerModel = model("inner/multiply",
                ent(sig("inner/multiply/out", "multiply", MultiplierImpl.class,
                        result("multiply/out", inPaths("arg/x1", "arg/x2")))),
                response("inner/multiply/out"));

        Model outerModel = model(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/out",
                        inPaths("multiply/out", "add/out")))),
                ent(sig("out", "average", AveragerImpl.class, result("model/response",
                        inPaths("inner/multiply/out", "subtract")))),
                response("inner/multiply", "subtract", "out"));

        dependsOn(outerModel, ent("subtract", paths("multiply", "add")));

        add(outerModel, innerModel, inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0));

        Context out = response(outerModel);
        logger.info("response: " + out);
        assertTrue(get(out, "inner/multiply/out").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));
        assertTrue(get(out, "out").equals(450.0));
    }

}
