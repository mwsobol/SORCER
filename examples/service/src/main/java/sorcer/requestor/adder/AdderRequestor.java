package sorcer.requestor.adder;

import sorcer.core.requestor.ExertRequestor;
import sorcer.provider.adder.Adder;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.io.File;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;

public class AdderRequestor extends ExertRequestor {

    public Mogram getMogram(String... args) throws Exception {

        if (args != null && args.length == 2) {
            if (args[1].equals("netlet")) {
                return (Exertion) evaluate(new File("src/main/netlets/adder.ntl"));
            } else if (args[1].equals("dynamic")) {
                return (Exertion) evaluate(new File("src/main/netlets/adder-sbp.ntl"));
            } else if (args[1].equals("model")) {
                return createModel();
            } else if (args[1].equals("exertion")) {
                return createExertion();
            }
        }
        throw new MogramException("wrong arguments for: ExertRequestor type, mogram type");
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