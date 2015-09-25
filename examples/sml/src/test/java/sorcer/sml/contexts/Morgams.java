package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
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
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.inc;
import static sorcer.po.operator.par;

/**
 * Created by Mike Sobolewski on 4/15/15.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Morgams {

    private final static Logger logger = LoggerFactory.getLogger(Morgams.class);


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
    public void exertMstcGateSchema() throws Exception {

        Model model = srvModel(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                par("incBy200", inc("multiply/out", 200.0)));

        responseUp(model, "multiply", "incBy200");
        dependsOn(model, ent("incBy200", paths("multiply")));

        Context result = response(model);
        logger.info("result: " + result);
        assertTrue(value(result, "incBy200").equals(700.0));

//        Block looping = block(
//                loop(condition("{ incBy200 -> incBy200 < 1000 }", "incBy200"),
//                        model));
//
//        looping = exert(looping);
//        logger.info("block context: " + context(looping));
    }

//    @Test
//    public void exertMstcGateMogram() throws Exception {
//
//        Model airCycleModel = srvModel(
//                inEnt("offDesignCases"), outEnt("fullEngineDeck"),
//                srv(sig("mstcGate", Class.class, result("fullEngineDeck", inPaths("offDesignCases")))),
//
//                inEnt("offDesignCases"), outEnt("acmFile"),
//                srv(sig("parsing", Class.class, result("ac2HexOut1", inPaths("fullEngineDeck")))),
//
//                srv(sig("execute", Class.class, result("ac2HexOut2",
//                        inPaths("ac2HexOut1")))));
//
//        responseUp(airCycleModel, "execute");
//        dependsOn(airCycleModel, ent("execute", paths("mstcGate", "offDesignCases")));
//
//        Block airCycleMachine = block(
//                context(ent("offDesignCases", "myURL")),
//                loop(condition("{ ac2HexOut1, ac2HexOut2 -> ac2HexOut1.equals(ac2HexOut2) }",
//                                "ac2HexOut1", "ac2HexOut2"),
//                        airCycleModel));
//
//        airCycleMachine = exert(airCycleMachine);
//        logger.info("block context: " + context(airCycleMachine));
//    }
}
