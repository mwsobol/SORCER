package sorcer.dispatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.DividerImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.service.Context;
import sorcer.service.Signature;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.printDeps;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.*;
import static sorcer.po.operator.ent;

/**
 * Created by pol on 24.11.15.
 */
public class SrvModelAutoDepsTest {

    private final static Logger logger = LoggerFactory.getLogger(SrvModelAutoDepsTest.class);

    @Test
    public void sigLocalModel() throws Exception {
        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                inVal("addfinal/x1", 1000.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class,
                        result(path("model/response", Signature.Direction.INOUT),
                            inPaths("multiply/out", "add/out")))),
                ent(sig("divide", DividerImpl.class, result("divider/out",
                        inPaths("model/response", "multiply/x1")))),
                response("divide"));

        logger.info("Map of dependents: " + dependencies(m));
        logger.info("Dependencies: " + printDeps(m));
        Context out = response(m);
        logger.info("response: " + out);
        logger.info("result: " + get(out, "divide"));
        assertTrue(get(out, "divide").equals(40.0));
    }

    @Test
    public void sigLocalComplexModel() throws Exception {
        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                inVal("addfinal/x1", 1000.0), inVal("divider/out"),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent(sig("subtract", SubtractorImpl.class, result("model/response",
                        inPaths("multiply/out", "add/out")))),
                ent(sig("divide", DividerImpl.class, result("divider/out",
                        inPaths("model/response", "multiply/x1")))),
                ent("addfinal", sig("add", AdderImpl.class, result("addfinal/out",
                        inPaths("addfinal/x1", "divider/out")))),
                response("addfinal"));

        logger.info("Map of dependents: " + dependencies(m));
        logger.info("Dependencies: " + printDeps(m));
        Context out = response(m);
        logger.info("response: " + out);
        logger.info("result: " + get(out, "addfinal"));
        assertTrue(get(out, "addfinal").equals(1040.0));
    }
}
