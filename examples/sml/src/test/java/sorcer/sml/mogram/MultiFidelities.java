package sorcer.sml.mogram;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.Observable;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MultiFidelity;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;

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
    public void fidelityManagerWithMultiSigFidelities() throws Exception {

        FidelityManager manager = new FidelityManager() {
            @Override
            public void initialize() {
                Fidelity<Fidelity> fi2 = fi(fi("divide", "mFi2"), fi("multiply", "mFi3"));
                Fidelity<Fidelity> fi3 = fi(fi("average", "mFi2"), fi("divide", "mFi3"));
                put(ent("sysFi2", fi2), ent("sysFi3", fi3));
            }

            @Override
            public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
                if (observable instanceof MultiFidelity) {
                    Fidelity<Signature> mFi = ((MultiFidelity) observable).getMultiFidelity();
                    if (mFi.getPath().equals("mFi1") && mFi.getSelectedName().equals("add")) {
                        if (((Double) obj) <= 200.0) {
                            morph("sysFi2");
                        } else {
                            morph("sysFi3");
                        }
                    }
                }
            }
        };

        Signature add = sig("add", AdderImpl.class,
                result("result/y1", inPaths("arg/x1", "arg/x2")));
        Signature subtract = sig("subtract", SubtractorImpl.class,
                result("result/y2", inPaths("arg/x1", "arg/x2")));
        Signature average = sig("average", AveragerImpl.class,
                result("result/y2", inPaths("arg/x1", "arg/x2")));
        Signature multiply = sig("multiply", MultiplierImpl.class,
                result("result/y1", inPaths("arg/x1", "arg/x2")));
        Signature divide = sig("divide", DividerImpl.class,
                result("result/y2", inPaths("arg/x1", "arg/x2")));

        // three entry multifidelity model
        Model mod = model(inEnt("arg/x1", 90.0), inEnt("arg/x2", 10.0),
                ent("mFi1", mFi(add, multiply)),
                ent("mFi2", mFi(average, divide, subtract)),
                ent("mFi3", mFi(average, divide, multiply)),
                manager,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

        // fidelities updated by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(900.0));

        // first closing the fidelity for mFi1
        // then fidelities updated by the model's fidelity manager accordingly
        out = response(mod , fi("multiply", "mFi1"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }

    @Test
    public void fidelityManagerWithMultiEntFidelities() throws Exception {

        FidelityManager manager = new FidelityManager() {
            @Override
            public void initialize() {
                Fidelity<Fidelity> fi2 = fi(fi("divide", "mFi2"), fi("multiply", "mFi3"));
                Fidelity<Fidelity> fi3 = fi(fi("average", "mFi2"), fi("divide", "mFi3"));
                put(ent("sysFi2", fi2), ent("sysFi3", fi3));
            }

            @Override
            public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
                if (observable instanceof MultiFidelity) {
                    Fidelity<Signature> mFi = ((MultiFidelity) observable).getMultiFidelity();
                    if (mFi.getPath().equals("mFi1") && mFi.getSelectedName().equals("add")) {
                        if (((Double) obj) <= 200.0) {
                            morph("sysFi2");
                        } else {
                            morph("sysFi3");
                        }
                    }
                }
            }
        };

        Entry addEnt = ent(sig("add", AdderImpl.class,
                result("result/y1", inPaths("arg/x1", "arg/x2"))));
        Entry subtractEnt = ent(sig("subtract", SubtractorImpl.class,
                result("result/y1", inPaths("arg/x1", "arg/x2"))));
        Entry multiplyEnt = ent(sig("multiply", MultiplierImpl.class,
                result("result/y1", inPaths("arg/x1", "arg/x2"))));
        Entry divideEnt = ent(sig("divide", DividerImpl.class,
                result("result/y2", inPaths("arg/x1", "arg/x2"))));
        Entry averageEnt = ent(sig("average", AveragerImpl.class,
                result("result/y2", inPaths("arg/x1", "arg/x2"))));

        Model mod = model(inEnt("arg/x1", 90.0), inEnt("arg/x2", 10.0),
                addEnt, multiplyEnt, divideEnt, averageEnt,
                ent("mFi1", mFi(addEnt, multiplyEnt)),
                ent("mFi2", mFi(averageEnt, divideEnt, subtractEnt)),
                ent("mFi3", mFi(averageEnt, divideEnt, multiplyEnt)),
                manager,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

        // fidelities updated by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(900.0));

        // first closing the fidelity for mFi1
        // then fidelities updated by the model's fidelity manager accordingly
        out = response(mod , fi("multiply", "mFi1"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }
}
