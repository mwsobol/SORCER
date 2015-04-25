package sorcer.sml.contexts;

import org.junit.Test;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.modeling.Model;

import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.inPaths;
import static sorcer.co.operator.srv;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoker;

/**
 * Created by Mike Sobolewski on 4/15/15.
 */
public class SrvModels {

    private final static Logger logger = Logger.getLogger(SrvModels.class.getName());

    @Test
    public void exertServiceModel() throws Exception {

        // get a context from a subject provider
        // exerting a model with the subject provider as its service context

        Model m = srvModel(sig("add", AdderImpl.class),
                inEnt("arg/x1", 1.0), inEnt("arg/x2", 2.0),
                ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

        add(m, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

        add(m, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

        // two responses declared
        addResponse(m, "add", "multiply", "result/value");
        // exert the model
        Model model = exert(m);
        logger.info("model: " + model);

        assertTrue(response(model, "add").equals(4.0));

        assertTrue(responses(model).equals(
                context(ent("add", 4.0), ent("multiply", 20.0), ent("result/value", 3.0))));

    }


    @Test
    public void queryResponseServiceModel() throws Exception {

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
        addResponse(m, "subtract");
        dependsOn(m, "subtract", paths("multiply", "add"));
//		logger.info("response: " + response(m));

        assertTrue(response(m).equals(400.0));

        // get a response context
        addResponse(m, "add", "multiply", "y1");
        Context out = responses(m);
        logger.info("out: " + out);
        assertTrue(response(out, "add").equals(100.0));
        assertTrue(response(out, "multiply").equals(500.0));
        assertTrue(response(out, "subtract").equals(400.0));

        assertTrue(response(out, "y1").equals(10.0));

        logger.info("model: " + m);

    }


    @Test
    public void modelToTaskMogram() throws Exception {

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

        addResponse(model, "add", "multiply", "subtract");
        dependsOn(model, "subtract", paths("multiply", "add"));
        // specify how model connects to exertion
        outConn(model, outConnector);

//        Context out = responses(model);
//        logger.info("out: " + out);

        Block block = block(sig(ServiceConcatenator.class),
                model,
                task(sig("average", AveragerImpl.class,
                        result("average/response", inPaths("y1", "y2", "y3")))));

        Context result = context(exert(block));

        logger.info("result: " + result);

        assertTrue(value(result, "y1").equals(100.0));
        assertTrue(value(result, "y2").equals(500.0));
        assertTrue(value(result, "y3").equals(400.0));
        assertTrue(value(result, "average/response").equals(333.3333333333333));
    }

}
