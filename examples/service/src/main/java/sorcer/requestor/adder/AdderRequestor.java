package sorcer.requestor.adder;

import net.jini.core.transaction.TransactionException;
import sorcer.core.requestor.ExertRequestor;
import sorcer.provider.adder.Adder;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;

public class AdderRequestor extends ExertRequestor {

    public Mogram getMogram(String... args) throws MogramException {

        String option = "exertion";
        if (args != null && args.length == 2) {
            option = args[1];
        } else if (this.args != null) {
            option = this.args[0];
        } else {
            throw new MogramException("wrong arguments for: ExertRequestor type, mogram type");
        }
        try {
            if (option.equals("netlet")) {
                return (Exertion) evaluate(new File("src/main/netlets/adder.ntl"));
            } else if (option.equals("dynamic")) {
                return (Exertion) evaluate(new File("src/main/netlets/adder-sbp.ntl"));
            } else if (option.equals("model")) {
                return createModel();
            } else if (option.equals("exertion")) {
                return createExertion();
            }
        } catch (Exception e) {
            throw new MogramException(e);
        }
        return null;
    }

    private Exertion createExertion() throws ContextException, SignatureException, ExertionException {
        Double v1 = new Double(getProperty("arg/x1"));
        Double v2 = new Double(getProperty("arg/x2"));

        return exertion("hello adder", sig("add", Adder.class),
                context("adder", inEnt("arg/x1", v1), inEnt("arg/x2", v2),
                        result("out/y")));
    }

    private Model createModel() throws Exception {
        Double v1 = new Double(getProperty("arg/x1"));
        Double v2 = new Double(getProperty("arg/x2"));

        // model three entries
        return model(inEnt("arg/x1", v1), inEnt("arg/x2", v2),
                srv(sig("add", Adder.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                response("add", "arg/x1", "arg/x2"));
    }

}