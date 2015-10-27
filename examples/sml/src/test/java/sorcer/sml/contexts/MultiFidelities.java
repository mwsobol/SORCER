package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.invoker.Observable;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MultiFidelity;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.invoker;

/**
 * Created by Mike Sobolewski on 10/26/15.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class MultiFidelities {

    private final static Logger logger = LoggerFactory.getLogger(MultiFidelities.class);

    @Test
    public void evalauteSignatureFidelityModel1() throws Exception {

        // three entry model
        Model mod = model(inEnt("arg/x1", 10.0), inEnt("arg/x2", 90.0),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("mFi", "arg/x1", "arg/x2"));

        Context out = response(mod, fi("multiply", "mFi"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi").equals(900.0));
        assertTrue(get(mod, "result/y").equals(900.0));
    }

    @Test
    public void evalauteSignaureFidelityModel2() throws Exception {

        // three entry model
        Model mod = model(inEnt("arg/x1", 10.0), inEnt("arg/x2", 90.0),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("mFi", "arg/x1", "arg/x2"));

        Context out = response(mod, fi("add", "mFi"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi").equals(100.0));
        assertTrue(get(mod, "result/y").equals(100.0));
    }

    @Test
    public void evalauteMultiFidelityModel1() throws Exception {

        FidelityManager manager = new FidelityManager() {
            @Override
            public void initialize() {
                Fidelity<Fidelity> fi2 = fi(fi("divide", "mFi2"), fi("add", "mFi3"));
                Fidelity<Fidelity> fi3 = fi(fi("average", "mFi2"));
                put(ent("sysFi2", fi2), ent("sysFi3", fi3));
            }

            @Override
            public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
                if (observable instanceof MultiFidelity) {
                    Fidelity<Signature> mFi = ((MultiFidelity) observable).getMultiFidelity();
                    if (mFi.getPath().equals("mFi1") && mFi.getSelectedName().equals("add")) {
                        if (((Double) obj) >= 100.0) {
                            morph("sysFi2", "sysFi3");
                        } else {
                            morph("sysFi3");
                        }
                    }
                }
            }
        };

        // three entry multifidelity model
        Model mod = model(inEnt("arg/x1", 90.0), inEnt("arg/x2", 10.0),
                ent("mFi1", multiFi(sig("add", AdderImpl.class, result("result/y1", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y1", inPaths("arg/x1", "arg/x2"))))),
                ent("mFi2", multiFi(sig("average", AveragerImpl.class, result("result/y2", inPaths("arg/x1", "arg/x2"))),
                        sig("divide", DividerImpl.class, result("result/y2", inPaths("arg/x1", "arg/x2"))),
                        sig("subtract", SubtractorImpl.class, result("result/y2", inPaths("arg/x1", "arg/x2"))))),
                ent("mFi3", multiFi(sig("average", AveragerImpl.class, result("result/y3", inPaths("arg/x1", "arg/x2"))),
                        sig("add", AdderImpl.class, result("result/y3", inPaths("arg/x1", "arg/x2"))))),
                manager,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

//        Context out = response(mod, fi("divide", "mFi1"));
        Context out = response(mod);

        logger.info("out: " + out);
//        logger.info("result: " + mod.getResult());
//        assertTrue(get(out, "mFi").equals(900.0));
//        assertTrue(get(mod, "result/y").equals(900.0));
    }

}
