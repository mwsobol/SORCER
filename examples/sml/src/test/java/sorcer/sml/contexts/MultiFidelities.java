package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.impl.*;
import sorcer.core.invoker.Observable;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MetaFidelity;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.Flow;
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
            public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
                if (observable instanceof MetaFidelity) {
                    MetaFidelity mFi = (MetaFidelity)observable;
                     if (mFi.getPath().equals("mF1") && mFi.getName().equals("add")) {
                         try {
                             MetaFidelity fi = get(mogram, "mFi2");
                             fi.setSelection(fi.getSelect("average"));
                         } catch (ContextException e) {
                             throw new EvaluationException(e);
                         }
                     }
                }
            }
        };
        // three entry model
        Model mod = model(inEnt("arg/x1", 90.0), inEnt("arg/x2", 10.0),
                ent("mFi1", mFi(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))),
                        sig("multiply", MultiplierImpl.class, result("result/y", inPaths("arg/x1", "arg/x2"))))),
                ent("mFi2", mFi(sig("average", AveragerImpl.class, result("result/z", inPaths("arg/x1", "arg/x2"))),
                        sig("divide", DividerImpl.class, result("result/z", inPaths("arg/x1", "arg/x2"))))),
                manager,
                response("mFi1", "mFi2", "arg/x1", "arg/x2"));

        Context out = response(mod, fi("add", "mFi1"));
        logger.info("out: " + out);
//        assertTrue(get(out, "mFi").equals(900.0));
//        assertTrue(get(mod, "result/y").equals(900.0));
    }

}
