package sorcer.sml.contexts;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.inPaths;
import static sorcer.co.operator.srv;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.invoker;
/**
 * Created by Mike Sobolewski on 4/15/15.
 */
public class SrvModels {

    private final static Logger logger = LoggerFactory.getLogger(SrvModels.class);

    @Test
    public void exertServiceModel() throws Exception {

        // get a context from a subject provider
        // exerting a model with the subject provider as its service context

        Model m = srvModel(sig("add", AdderImpl.class),
                inEnt("arg/x1", 1.0), inEnt("arg/x2", 2.0),
                ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

        add(m, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

        add(m, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

        // two response paths declared
        stepupResponse(m, "add", "multiply");
        // exert the model
        Model model = exert(m);
        logger.info("model: " + model);

        assertTrue(response(model, "add").equals(4.0));
        System.out.println("responses: " + response(model));

        assertTrue(response(model).equals(context(ent("add", 4.0), ent("multiply", 20.0))));
//                context(ent("add", 4.0), ent("multiply", 20.0), ent("result/value", 3.0))));

    }

    @Test
    public void serviceResponses() throws Exception {

        // get responses from a service model

        Model m = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                srv("y1", "multiply/x1"),
                response("subtract"));

        dependsOn(m, ent("subtract", paths("multiply", "add")));
        logger.info("response: " + response(m));
        Context out = response(m);

        assertTrue(get(out, "subtract").equals(400.0));

    }

    @Test
    public void evaluateServiceModel() throws Exception {

        // get responses from a service model

        Model m = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                srv("y1", "multiply/x1"));


        // get a scalar response
        stepupResponse(m, "subtract");
        dependsOn(m, ent("subtract", paths("multiply", "add")));
		logger.info("response: " + response(m));
        Context out = response(m);

        assertTrue(get(out, "subtract").equals(400.0));

        // stepup a response context
        stepupResponse(m, "add", "multiply", "y1");
        out = response(m);
        logger.info("out: " + out);
        assertTrue(get(out, "add").equals(100.0));
        assertTrue(get(out, "multiply").equals(500.0));
        assertTrue(get(out, "subtract").equals(400.0));

        assertTrue(response(out, "y1").equals(10.0));

        logger.info("model: " + m);

    }


    @Test
    public void exertModelToTaskMogram() throws Exception {

        // output connector from model to exertion
        Context outConnector = outConn(inEnt("y1", "add"), inEnt("y2", "multiply"), inEnt("y3", "subtract"));

        Model model = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("subtract/response",
                        inPaths("multiply/out", "add/out")))),
                srv("y1", "multiply/x1"), srv("y2", "add/x2"), srv("y3", "subtract/response"));

        stepupResponse(model, "add", "multiply", "subtract");
        dependsOn(model, ent("subtract", paths("multiply", "add")));
        // specify how model connects to exertion
        outConn(model, outConnector);

//        Context out = response(model);
//        logger.info("out: " + out);

        Block block = block("mogram",
                model,
                task(sig("average", AveragerImpl.class,
                        result("average/response", inPaths("y1", "y2", "y3")))));

//        logger.info("block context: " + block.getContext());
        Context result = context(exert(block));
//        logger.info("result: " + result);

        assertTrue(value(result, "y1").equals(100.0));
        assertTrue(value(result, "y2").equals(500.0));
        assertTrue(value(result, "y3").equals(400.0));
        assertTrue(value(result, "average/response").equals(333.3333333333333));
    }


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

        Model model = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("subtract/out",
                        inPaths("multiply/out", "add/out")))));

//                srv("z1", "multiply/x1"), srv("z2", "add/x2"), srv("z3", "subtract/out"));

        stepupResponse(model, "add", "multiply", "subtract");
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
}
