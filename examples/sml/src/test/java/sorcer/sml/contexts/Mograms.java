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
import sorcer.core.context.model.par.Par;
import sorcer.core.invoker.InvokeIncrementor;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.*;

/**
 * Created by Mike Sobolewski on 4/15/15.
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
    public void par2() throws Exception {

        InvokeIncrementor i = inc("x1", 200.0);
         Par p = par("x1", next(i));

        logger.info("value: " + value(p));
        logger.info("value: " + value(p));

//        logger.info("value: " + next(i));
//        logger.info("value: " + next(i));
//        logger.info("value: " + next(i));
    }

    @Test
    public void exertMstcGateSchema() throws Exception {

        Model model = srvModel(
                inEnt("x", 10.0),
                outEnt("y", next(inc("x", 20.0))));

        responseUp(model, "y");
        Context result = response(model);
        logger.info("result1: " + result);

//        Context result = response(model);
//        logger.info("result1: " + result);
//        assertTrue(value(result, "incBy200").equals(250.0));

//        Block looping = block(
//                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
//                loop(condition("{ incBy200 -> incBy200 < 1000 }", "incBy200"),
//                        model));

//        Block looping = block(
//                context(ent("x1", 10.0), ent("x2", 20.0), ent("z", 100.0)),
//                loop(condition("{ x1, x2, z -> x1 + x2 < z }", "x1", "x2", "z"),
//                        task(par("x1", invoker("x1 + 3", pars("x1"))))));

//        looping = exert(looping);
//        logger.info("block context: " + context(looping));
//        logger.info("result: " + value(context(looping), "x1"));
//        assertEquals(value(context(looping), "x1"), 82.00);
    }

    @Test
    public void exertMstcGateMogram() throws Exception {

        Model airCycleModel = srvModel("airCycleModel",
                srv(sig("getAc2HexOut", Class.class, result("ac2HexOut1", inPaths("offDesignCases")))),

                outEnt("fullEngineDeck"),
                srv(sig("mstcGate", Class.class, result("fullEngineDeck", inPaths("ac2HexOut1")))),

                outEnt("acmFile"),
                srv(sig("parseEngineDeck", Class.class, result("acmFile", inPaths("fullEngineDeck")))),

                srv(sig("executeAirCycleMachine", Class.class, result("ac2HexOut2",
                        inPaths("acmFile")))));

        responseUp(airCycleModel, "executeAirCycleMachine");
        dependsOn(airCycleModel, ent("execute", paths("mstcGate", "offDesignCases")));

        Block airCycleMachineMogram = block(
                context(ent("offDesignCases", "myURL")),
                loop(condition("{ ac2HexOut1, ac2HexOut2 -> ac2HexOut1.equals(ac2HexOut2) }",
                                "ac2HexOut1", "ac2HexOut2"),
                        airCycleModel));

        airCycleMachineMogram = exert(airCycleMachineMogram);
        logger.info("block context: " + context(airCycleMachineMogram));
    }
}
