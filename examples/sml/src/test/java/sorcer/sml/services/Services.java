package sorcer.sml.services;

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
import sorcer.service.*;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.inputs;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.invoker;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Services {
    private final static Logger logger = LoggerFactory.getLogger(Services.class);

    @Test
    public void evaluateModel() throws Exception  {

        Model context = model(ent("x1", 20.0), ent("x2", 80.0),
                ent("result/y", invoker("x1 + x2", ents("x1", "x2"))));

        // declare response paths
        responseUp(context, "result/y");

        assertTrue(get(context, "x1").equals(20.0));
        assertTrue(get(context, "x2").equals(80.0));

        Model out = response(context);
        assertEquals(1, size(out));
        assertTrue(get(out, "result/y").equals(100.0));

    }

    @Test
    public void modelInsOutsRsp() throws Exception  {

        Model context = model(inEnt("x1", 20.0), inEnt("x2", 80.0),
                outEnt("result/y", invoker("x1 + x2", ents("x1", "x2"))));

        Model inputs = inputs(context);
        logger.info("inputs : " + inputs(context));
        assertEquals(2, size(inputs));
        Model outputs = outputs(context);
        assertEquals(1, size(outputs));
        logger.info("outputs : " + outputs(context));

        // declare response paths
        responseUp(context, "result/y");
        Model out = response(context);
        assertEquals(1, size(out));
        assertTrue(get(out, "result/y").equals(100.0));

        // more response paths
        responseUp(context, "x1");
        out = response(context);
        assertEquals(2, size(out));
        assertTrue(get(out, "x1").equals(20.0));

    }

    @Test
    public void exertModel() throws Exception  {

        Model model = model(sig("add", AdderImpl.class),
                inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                outEnt("result/y"));

        Model out = exert(model);
        assertEquals(6, size(out));

        logger.info("out : " + out);
        logger.info("out @ arg/x1: " + get(out, "arg/x1"));
        logger.info("out @ arg/x2: " + value(out, "arg/x2"));
        logger.info("out @ result/y: " + value(out, "result/y"));

        assertEquals(100.0, value(out, "result/y"));

    }

    @Test
    public void exertSrvModel() throws Exception  {

        Model m = model(
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                srv(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                srv(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                srv(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                aka("y1", "multiply/x1"),
                response("subtract"));

        dependsOn(m, ent("subtract", paths("multiply", "add")));
        logger.info("response: " + response(m));
        Context out = response(m);

        assertTrue(get(out, "subtract").equals(400.0));

    }

    @Test
    public void exertMogram() throws Exception  {

        Mogram mogram = mogram(sig("add", AdderImpl.class),
                            cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                            outEnt("result/y")));

        Mogram out = exert(mogram);
        Context cxt = context(out);
        logger.info("out context: " + cxt);
        logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
        logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
        logger.info("context @ result/y: " + value(cxt, "result/y"));

        assertEquals(100.0, value(cxt, "result/y"));

    }

    @Test
    public void exertTask() throws Exception {

        Mogram t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));

        Mogram out = exert(t5);
        Context cxt = context(out);
        logger.info("out context: " + cxt);
        logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
        logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
        logger.info("context @ result/y: " + value(cxt, "result/y"));

        assertEquals(100.0, value(cxt, "result/y"));

    }


    @Test
    public void evaluateTask() throws Exception {

        Service t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        result("result/y")));

        // get a single context argument at the result path
        assertEquals(100.0, exec(t5));

        // get the subcontext output from the the result path
        assertTrue(context(ent("arg/x1", 20.0), ent("result/z", 100.0)).equals(
                exec(t5, result("result/z", outPaths("arg/x1", "result/z")))));
    }


    @Test
    public void exertJob() throws Exception {

        Mogram t3 = task("t3", sig("subtract", SubtractorImpl.class),
                cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"), outEnt("result/y")));

        Mogram t4 = task("t4", sig("multiply", MultiplierImpl.class),
                // cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
                cxt("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y")));

        Mogram t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));

        Mogram job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
                job("j1", sig(ServiceJobber.class),
                        cxt(inEnt("arg/x1", 10.0),
                                result("job/result", outPaths("j1/t3/result/y"))),
                        job("j2", sig(ServiceJobber.class), t4, t5),
                        t3,
                        pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                        pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

        logger.info("srv job context: " + upcontext(job));
        logger.info("srv j1/t3 context: " + context(job, "j1/t3"));
        logger.info("srv j1/j2/t4 context: " + context(job, "j1/j2/t4"));
        logger.info("srv j1/j2/t5 context: " + context(job, "j1/j2/t5"));

        Mogram exertion = exert(job);
        logger.info("srv job context: " + upcontext(exertion));
        logger.info("exertion value @ j1/t3/arg/x2 = " + get(exertion, "j1/t3/arg/x2"));
        assertEquals(100.0, get(exertion, "j1/t3/arg/x2"));

    }


    @Test
    public void evaluateJob() throws Exception {

        Mogram t3 = task("t3", sig("subtract", SubtractorImpl.class),
                cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"), result("result/y")));

        Mogram t4 = task("t4", sig("multiply", MultiplierImpl.class),
                cxt("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0), result("result/y")));

        Mogram t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0), result("result/y")));

        //TODO: CHECK Access.PULL doesn't work with ServiceJobber!!!
        Mogram job = //j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
                job("j1", sig(ServiceJobber.class), result("job/result", outPaths("j1/t3/result/y")),
                        job("j2", sig(ServiceJobber.class), t4, t5, strategy(Strategy.Flow.PAR, Strategy.Access.PUSH)),
                        t3,
                        pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                        pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

        // get the result value
        assertEquals(400.0, exec(job));

        // get the subcontext output from the exertion
        assertTrue(context(ent("j1/j2/t4/result/y", 500.0),
                ent("j1/j2/t5/result/y", 100.0),
                ent("j1/t3/result/y", 400.0)).equals(
                exec(job, result("result/z",
                        outPaths("j1/j2/t4/result/y", "j1/j2/t5/result/y", "j1/t3/result/y")))));


    }
}
