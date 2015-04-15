package sorcer.sml.contexts;

import org.junit.Test;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.service.Context;
import sorcer.service.Mogram;
import sorcer.service.modeling.Model;

import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.paths;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.sig;
import static sorcer.po.operator.invoker;

/**
 * Created by Mike Sobolewski on 4/15/15.
 */
public class SrvModels {

    private final static Logger logger = Logger.getLogger(SrvModels.class.getName());

    @Test
    public void exertContextServiceModel() throws Exception {

        // get a context from a subject provider

        Model m = srvModel(sig("add", AdderImpl.class),
                inEnt("arg/x1", 1.0), inEnt("arg/x2", 2.0),
                ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

        add(m, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

        add(m, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

        // two responses declared
        addResponse(m, "add", "multiply", "result/value");
        // exert the model
        Model model = exert(m);
        // logger.info("model: " + model);
        logger.info("result: " + responses(model));

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
    public void modelOutMap() throws Exception {

        Context outMap = context(inEnt("add", "x1"), inEnt("multiply", "x3"), inEnt("multiply", "x3"));

        Model m = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                srv("y1", "multiply/x1"), srv("y2", "add/x2"));

        addResponse(m, "add", "multiply", "subtract");
        dependsOn(m, "subtract", paths("multiply", "add"));
        mapContext(m, outMap);

        Mogram block = block(m, task(sig("multiply", MultiplierImpl.class)));
    }

}
