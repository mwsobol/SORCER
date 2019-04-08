package sorcer.sml.mograms;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.*;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.provider.Modeler;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.provider.rendezvous.ServiceModeler;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.loop;
import static sorcer.eo.operator.result;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.result;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * Created by Mike Sobolewski on 10/21/15.
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ServiceMograms {

    private final static Logger logger = LoggerFactory.getLogger(ServiceMograms.class);

    @Test
    public void blockWithExertionAndModel() throws Exception {

        // usage of in and outGovernance connectors associated with model
        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        outVal("multiply/outDispatcher/y")));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        outVal("add/outDispatcher/y")));

        // in connector from exertion to model
        Context taskOutConnector = outConn(inVal("add/x1", "j2/t4/multiply/outDispatcher/y"),
                inVal("multiply/x1", "j2/t5/add/outDispatcher/y"));

        Job j2 = job("j2", sig("exert", ServiceJobber.class),
                t4, t5, strategy(Flow.PAR),
                taskOutConnector);

        // outGovernance connector from model
        Context modelOutConnector = outConn(inVal("y1", "add"), inVal("y2", "multiply"), inVal("y3", "subtract"));

        Model model = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/outGovernance",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/outGovernance",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/outGovernance",
                        inPaths("multiply/outGovernance", "add/outGovernance")))));

        responseUp(model, "add", "multiply", "subtract");
        //dependsOn(model, call("subtract", paths("multiply", "add")));
        // specify how model connects to exertion
        outConn(model, modelOutConnector);

        Block block = block("mogram", j2, model);

        Context result = context(exert(block));

        logger.info("outDispatcher: " + result);

        assertTrue(value(result, "add").equals(580.0));
        assertTrue(value(result, "multiply").equals(5000.0));
        assertTrue(value(result, "y1").equals(580.0));
        assertTrue(value(result, "y2").equals(5000.0));
        assertTrue(value(result, "y3").equals(4420.0));
    }

    @Test
    public void incrementer() throws Exception {

        Incrementer incrementer = new IncrementerImpl(100.0);

        Model model = model(
                inVal("by", 10.0),
                ent(sig("increment", incrementer, result("outGovernance",
                        inPaths("by")))));

        responseUp(model, "increment", "outGovernance");

        Model exerted = exert(model);
        logger.info("outGovernance context: " + exerted);
        assertTrue(exec(exerted, "outGovernance").equals(110.0));

        exerted = exert(model);
        logger.info("outGovernance context: " + exerted);
        assertTrue(exec(exerted, "outGovernance").equals(120.0));
    }

    @Test
    public void loopWithModel() throws Exception {

        Incrementer incrementer = new IncrementerImpl(100.0);

        Model mdl = model(
                inVal("by", entFi(inVal("by-10", 10.0), inVal("by-20", 20.0))), inVal("outGovernance", 0.0),
                ent(sig("increment", incrementer, result("outGovernance", inPaths("by", "template")))),
                ent("multiply", invoker("add * outGovernance", ents("add", "outGovernance"))));

        responseUp(mdl, "increment", "outGovernance", "multiply", "by");
//        Context outGovernance = response(mdl, val("add", 100.0));
//        Model exerted = exert(mdl);
//        logger.info("outGovernance context: " + exerted);
//        assertTrue(eval(exerted, "multiply").equals(11000));
//        assertTrue(eval(exerted, "outGovernance").equals(110.0));

        logger.info("DEPS: " + printDeps(mdl));

        Block looping = block(
                context(inVal("template", "URL")),
                task(sig("add", AdderImpl.class),
                        context(inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("add"))),
                loop(condition(cxt ->
                                (double) value(cxt, "outGovernance") < 1000.0), mdl));

        looping = exert(looping, fi("by-20", "by"));
        logger.info("block context: " + context(looping));
        logger.info("outDispatcher: " + value(context(looping), "outGovernance"));
        logger.info("model outDispatcher: " + value(result(mdl), "outGovernance"));
        logger.info("multiply outDispatcher: " + value(result(mdl), "multiply"));
        // outGovernance variable in block
        assertTrue(value(context(looping), "outGovernance").equals(1000.0));
        // outGovernance variable in model
        assertTrue(value(result(mdl), "outGovernance").equals(1000.0));
        assertTrue(value(result(mdl), "multiply").equals(100000.0));
        assertTrue(value(result(mdl), "by").equals(20.0));
    }

    @Test
    public void modelWithInnerTask() throws Exception {

        // usage of in and outGovernance connectors associated with model
        Task innerTask = task(
                "task/multiply",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        result("multiply/outDispatcher")));

        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/outGovernance",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/outGovernance",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/outGovernance",
                        inPaths("multiply/outGovernance", "add/outGovernance")))),
                ent(sig("outGovernance", "average", AveragerImpl.class, result("model/response",
                        inPaths("task/multiply", "subtract")))),
                response("task/multiply", "subtract", "outGovernance"));

        // dependsOn(m, call("subtract", paths("multiply", "add")));

        add(m, innerTask);

        Context out = response(m);
        logger.info("response: " + out);
        assertTrue(get(out, "task/multiply").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));
        assertTrue(get(out, "outGovernance").equals(450.0));
    }

    @Test
    public void modelWithInnerModel() throws Exception {
        // get response from a service model with inner model

        Model innerModel = model("inner/multiply",
                ent(sig("inner/multiply/outGovernance", "multiply", MultiplierImpl.class,
                        result("multiply/outGovernance", inPaths("arg/x1", "arg/x2")))),
                response("inner/multiply/outGovernance"));

        Model outerModel = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/outGovernance",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/outGovernance",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/outGovernance",
                        inPaths("multiply/outGovernance", "add/outGovernance")))),
                ent(sig("outGovernance", "average", AveragerImpl.class, result("model/response",
                        inPaths("inner/multiply/outGovernance", "subtract")))),
                response("inner/multiply", "subtract", "outGovernance"));

        // dependsOn(outerModel, call("subtract", paths("multiply", "add")));

        add(outerModel, innerModel, inVal("arg/x1", 10.0), inVal("arg/x2", 50.0));

        Context out = response(outerModel);
        logger.info("response: " + out);
        assertTrue(get(out, "inner/multiply/outGovernance").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));
        assertTrue(get(out, "outGovernance").equals(450.0));
    }

    @Test
    public void localModeler() throws Exception {
        // get response from a service model with inner model

        Model innerMdl = model("inner/multiply",
                ent(sig("inner/multiply/outGovernance", "multiply", MultiplierImpl.class,
                        result("multiply/outGovernance", inPaths("arg/x1", "arg/x2")))),
                response("inner/multiply/outGovernance"));

        Model outerMdl = model("outer/model",
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/outGovernance",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/outGovernance",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("subtract/outGovernance",
                        inPaths("multiply/outGovernance", "add/outGovernance")))),
                ent(sig("outGovernance", "average", AveragerImpl.class, result("model/response",
                        inPaths("inner/multiply/outGovernance", "subtract")))),
                response("inner/multiply", "subtract", "outGovernance"));

         dependsOn(outerMdl, dep("subtract", paths("multiply", "add")));

        add(outerMdl, innerMdl, inVal("arg/x1", 10.0), inVal("arg/x2", 50.0));

        Task mt = task("modelTask", sig("exert", ServiceModeler.class,
                outPaths("subtract", "outGovernance")), outerMdl);


        Context out = context(exert(mt));
        logger.info("response: " + out);
        assertTrue(get(out, "inner/multiply/outGovernance").equals(500.0));
        assertTrue(get(out, "subtract/outGovernance").equals(400.0));
        assertTrue(get(out, "model/response").equals(450.0));
    }

    @Test
    public void remoteModeler() throws Exception {
        // get response from a service model with inner model

        Domain innerMdl = model("inner/multiply",
                ent(sig("inner/multiply/outGovernance", "multiply", Multiplier.class,
                        result("multiply/outGovernance", inPaths("arg/x1", "arg/x2")))),
                response("inner/multiply/outGovernance"));

        Domain outerMdl = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", Multiplier.class, result("multiply/outGovernance",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(space(sig("add", Adder.class, result("add/outGovernance",
                        inPaths("add/x1", "add/x2"))))),
                ent(sig("subtract", Subtractor.class, result("subtract/outGovernance",
                        inPaths("multiply/outGovernance", "add/outGovernance")))),
                ent(sig("outGovernance", "average", Averager.class, result("model/response",
                        inPaths("inner/multiply/outGovernance", "subtract")))),
                response("inner/multiply", "subtract", "outGovernance"));

         dependsOn(outerMdl, dep("subtract", paths("multiply", "add")));

        add(outerMdl, innerMdl, inVal("arg/x1", 10.0), inVal("arg/x2", 50.0));

        Task mt = task("modelTask", sig("exert", Modeler.class,
                outPaths("subtract", "outGovernance")), outerMdl);

        Context out = context(exert(mt));
        logger.info("response: " + out);
        assertTrue(get(out, "inner/multiply/outGovernance").equals(500.0));
        assertTrue(get(out, "outGovernance").equals(450.0));
        assertTrue(get(out, "subtract").equals(400.0));
    }

}
