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
import sorcer.core.plexus.Morpher;
import sorcer.core.plexus.MorphedFidelity;
import sorcer.core.plexus.MultifidelityService;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.Strategy.*;

import java.rmi.RemoteException;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.mo.operator.printDeps;
import static sorcer.mo.operator.response;

/**
 * Created by Mike Sobolewski on 10/26/15.
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class MultiFidelities {

    private final static Logger logger = LoggerFactory.getLogger(MultiFidelities.class);

    @Test
    public void sigMultiFidelityModel() throws Exception {

        // three entry model
        Model mod = model(inEnt("arg/x1", 10.0), inEnt("arg/x2", 90.0),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("mFi", "arg/x1", "arg/x2"));

        Context out = response(mod, fi("mFi", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi").equals(900.0));
        assertTrue(get(mod, "result/y").equals(900.0));
    }

    @Test
    public void entMultiFidelityModel() throws Exception {

        // three entry model
        Model mdl = model(
                ent("arg/x1", eFi(inEnt("arg/x1/fi1", 10.0), inEnt("arg/x1/fi2", 11.0))),
                ent("arg/x2", eFi(inEnt("arg/x2/fi1", 90.0), inEnt("arg/x2/fi2", 91.0))),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("mFi", "arg/x1", "arg/x2"));

        logger.info("DEPS: " + printDeps(mdl));

//        Context out = response(mdl, fi("arg/x1", "arg/x1/fi2"), fi("arg/x2", "arg/x2/fi2"), fi("mFi", "multiply"));
        Context out = response(mdl, fi("arg/x1", "arg/x1/fi2"), fis( fi("arg/x2", "arg/x2/fi2"), fi("mFi", "multiply")));
        logger.info("out: " + out);
        assertTrue(get(out, "arg/x1").equals(11.0));
        assertTrue(get(out, "arg/x2").equals(91.0));
        assertTrue(get(out, "mFi").equals(1001.0));
        assertTrue(get(mdl, "result/y").equals(1001.0));
    }

    @Test
    public void entMultiFidelityModeWithFM() throws Exception {

        // three entry model
        Model mdl = model(
                ent("arg/x1", eFi(inEnt("arg/x1/fi1", 10.0), inEnt("arg/x1/fi2", 11.0))),
                ent("arg/x2", eFi(inEnt("arg/x2/fi1", 90.0), inEnt("arg/x2/fi2", 91.0))),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                FidelityMangement.YES,
                response("mFi", "arg/x1", "arg/x2"));

        logger.info("DEPS: " + printDeps(mdl));

        reconfigure(mdl, fi("arg/x1", "arg/x1/fi2"), fi("arg/x2", "arg/x2/fi2"), fi("mFi", "multiply"));
        Context out = response(mdl);
        logger.info("out: " + out);
        assertTrue(get(out, "arg/x1").equals(11.0));
        assertTrue(get(out, "arg/x2").equals(91.0));
        assertTrue(get(out, "mFi").equals(1001.0));
        assertTrue(get(mdl, "result/y").equals(1001.0));
    }

    @Test
    public void sigMultiFidelityModel2() throws Exception {

        // three entry model
        Model mod = model(inEnt("arg/x1", 10.0), inEnt("arg/x2", 90.0),
                ent("mFi", sFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                response("mFi", "arg/x1", "arg/x2"));

        Context out = response(mod, fi("mFi", "add"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi").equals(100.0));
        assertTrue(get(mod, "result/y").equals(100.0));
    }

    @Test
    public void sigMultiFidelityAmorphousModel() throws Exception {

        FidelityManager manager = new FidelityManager() {
            @Override
            public void initialize() {
                // define model metafidelities Fidelity<Fidelity>
                add(fi("sysFi2", fi("mFi2", "divide"), fi("mFi3", "multiply")));
                add(fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide")));
            }

            @Override
            public void update(Observable mFi, Object value) throws EvaluationException, RemoteException {
                if (mFi instanceof MorphedFidelity) {
                    ServiceFidelity<Signature> fi = ((MorphedFidelity) mFi).getFidelity();
                    if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("add")) {
                        if (((Double) value) <= 200.0) {
                            morph("sysFi2");
                        } else {
                            morph("sysFi3");
                        }
                    } else if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("multiply")) {
                        morph("sysFi3");
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

        // fidelities morphed by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(900.0));

        // first closing the fidelity for mFi1
        // then fidelities morphed by the model's fidelity manager accordingly
        out = response(mod , fi("mFi1", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }

    @Test
    public void notInitializedFidelityManager() throws Exception {

        FidelityManager manager = new FidelityManager() {

            @Override
            public void update(Observable mFi, Object value) throws EvaluationException, RemoteException {
                if (mFi instanceof MorphedFidelity) {
                    ServiceFidelity<Signature> fi = ((MorphedFidelity) mFi).getFidelity();
                    if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("add")) {
                        if (((Double) value) <= 200.0) {
                            morph("sysFi2");
                        } else {
                            morph("sysFi3");
                        }
                    } else if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("multiply")) {
                        morph("sysFi3");
                    }
                }
            }
        };

        ServiceFidelity<ServiceFidelity> fi2 = fi("sysFi2",fi("mFi2", "divide"), fi("mFi3", "multiply"));
        ServiceFidelity<ServiceFidelity> fi3 = fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide"));

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
                manager, fi2, fi3,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

        // fidelities morphed by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(900.0));

        // first closing the fidelity for mFi1
        // then fidelities morphed by the model's fidelity manager accordingly
        out = response(mod , fi("mFi1", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }

    @Test
    public void entMultiFidelityAmorphousModel() throws Exception {

        FidelityManager manager = new FidelityManager() {
            @Override
            public void initialize() {
                // define model metafidelities Fidelity<Fidelity>
                add(fi("sysFi2", fi("mFi2", "divide"), fi("mFi3", "multiply")));
                add(fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide")));
            }

            @Override
            public void update(Observable mFi, Object value) throws EvaluationException, RemoteException {
                if (mFi instanceof MorphedFidelity) {
                    ServiceFidelity<Signature> fi = ((MorphedFidelity) mFi).getFidelity();
                    if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("add")) {
                        if (((Double) value) <= 200.0) {
                            morph("sysFi2");
                        } else {
                            morph("sysFi3");
                        }
                    } else if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("multiply")) {
                        morph("sysFi3");
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

        // fidelities morphed by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(900.0));

        // first closing the fidelity for mFi1
        // then fidelities morphed by the model's fidelity manager accordingly
        out = response(mod , fi("mFi1", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }

    @Test
    public void morphingMultiFidelityModel() throws Exception {

        Morpher morpher1 = (mgr, mFi, value) -> {
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("add")) {
                if (((Double) value) <= 200.0) {
                    mgr.morph("sysFi2");
                } else {
                    mgr.morph("sysFi3");
                }
            } else if (fi.getPath().equals("mFi1") && fi.getSelectName().equals("multiply")) {
                mgr.morph("sysFi3");
            }
        };

        Morpher morpher2 = (mgr, mFi, value) -> {
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("divide")) {
                if (((Double) value) <= 9.0) {
                    mgr.morph("sysFi4");
                } else {
                    mgr.morph("sysFi3");
                }
            }
        };

        ServiceFidelity<ServiceFidelity> fi2 = fi("sysFi2",fi("mFi2", "divide"), fi("mFi3", "multiply"));
        ServiceFidelity<ServiceFidelity> fi3 = fi("sysFi3", fi("mFi2", "average"), fi("mFi3", "divide"));
        ServiceFidelity<ServiceFidelity> fi4 = fi("sysFi4", fi("mFi3", "average"));

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

        // three entry multifidelity model with morphers
        Model mod = model(inEnt("arg/x1", 90.0), inEnt("arg/x2", 10.0),
                ent("mFi1", mFi(morpher1, add, multiply)),
                ent("mFi2", mFi(morpher2, average, divide, subtract)),
                ent("mFi3", mFi(average, divide, multiply)),
                fi2, fi3, fi4,
                response("mFi1", "mFi2", "mFi3", "arg/x1", "arg/x2"));

        // fidelities morphed by the model's fidelity manager
        Context out = response(mod);
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(100.0));
        assertTrue(get(out, "mFi2").equals(9.0));
        assertTrue(get(out, "mFi3").equals(50.0));

        // first closing the fidelity for mFi1
        // then fidelities morphed by the model's fidelity manager accordingly
        out = response(mod , fi("mFi1", "multiply"));
        logger.info("out: " + out);
        assertTrue(get(out, "mFi1").equals(900.0));
        assertTrue(get(out, "mFi2").equals(50.0));
        assertTrue(get(out, "mFi3").equals(9.0));
    }

    @Test
    public void selectMultifidelityEntries() throws Exception {
        Entry e1 = ent("x1", 5.0);
        Entry e2 = ent("x2", 6.0);
        Entry e3 = ent("x3", 7.0);

        MultifidelityService mfs = multiFiService("entries", fi(e1, e2, e3));

        Object out = exec(mfs);
        logger.info("out: " + out);
        assertTrue(out.equals(5.0));

        selectFi(mfs, "x2");
        out = exec(mfs);
        logger.info("out: " + out);
        assertTrue(out.equals(6.0));

        selectFi(mfs, "x3");
        out = exec(mfs);
        logger.info("out: " + out);
        assertTrue(out.equals(7.0));
    }

    @Test
    public void morphMultifidelityEntries() throws Exception {
        Entry e1 = ent("x1", 5.0);
        Entry e2 = ent("x2", 6.0);
        Entry e3 = ent("x3", 7.0);

        Morpher morpher = (mgr, mFi, value) -> {
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("x1")) {
                if (((double)value) <= 5.0) {
                    mgr.reconfigure("x3");
                }
            }
        };

        MultifidelityService mfs = multiFiService("entries", mFi(morpher, e1, e2, e3));

        Object out = exec(mfs);
        logger.info("out: " + out);
        assertTrue(out.equals(5.0));

        out = exec(mfs);
        logger.info("out: " + out);
        assertTrue(out.equals(7.0));
    }

    @Test
    public void selectMultifidelitySignatures() throws Exception {

        Context cxt = context(inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                outEnt("result/y"));
        Signature ms = sig("multiply", MultiplierImpl.class);
        Signature as = sig("add", AdderImpl.class);

        MultifidelityService mfs = multiFiService(fi(ms, as), cxt);

        Context out = (Context) exec(mfs);
        logger.info("out: " + out);
        assertTrue(value(context(out), "result/y").equals(500.0));

        selectFi(mfs, "add");
        out = (Context) exec(mfs);
        assertTrue(value(context(out), "result/y").equals(60.0));
    }

    @Test
    public void morphMultifidelitySignatures() throws Exception {

        Context cxt = context(inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                outEnt("result/y"));
        Signature ms = sig("multiply", MultiplierImpl.class);
        Signature as = sig("add", AdderImpl.class);

        Morpher morpher = (mgr, mFi, value) -> {
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("multiply")) {
                try {
                    if (((Double) value(context(value), "result/y")) >= 500.0) {
                        mgr.reconfigure("add");
                    }
                } catch (ContextException e) {
                    e.printStackTrace();
                }
            }
        };

        MultifidelityService mfs = multiFiService(mFi(morpher, ms, as), cxt);

        Context out = (Context) exec(mfs);
        logger.info("out: " + out);
        assertTrue(value(context(out), "result/y").equals(500.0));

        out = (Context) exec(mfs);
        assertTrue(value(context(out), "result/y").equals(60.0));
    }

    @Test
    public void selectMultifidelityMogram() throws Exception {

        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y")));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));


        MultifidelityService mfs = multiFiService(mFi(t5, t4));
        Mogram mog = exert(mfs);
        logger.info("out: " + mog.getContext());
        assertTrue(value(context(mog), "result/y").equals(100.0));

        selectFi(mfs, "t4");
        mog = exert(mfs);
        logger.info("out: " + mog.getContext());
        assertTrue(value(context(mog), "result/y").equals(500.0));
    }

    @Test
    public void morphMultifidelityMogram() throws Exception {

        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y")));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));


        Morpher morpher = (mgr, mFi, value) -> {
            ServiceFidelity<Signature> fi =  mFi.getFidelity();
            if (fi.getSelectName().equals("t5")) {
                try {
                    if (((Double) value(context(value), "result/y")) <= 200.0) {
                        mgr.reconfigure("t4");
                    }
                } catch (ContextException e) {
                    e.printStackTrace();
                }
            }
        };

        MultifidelityService mfs = multiFiService(mFi(morpher, t5, t4));
        Mogram mog = exert(mfs);
        logger.info("out: " + mog.getContext());
        assertTrue(value(context(mog), "result/y").equals(100.0));

        mog = exert(mfs);
        logger.info("out: " + mog.getContext());
        assertTrue(value(context(mog), "result/y").equals(500.0));
    }

}
