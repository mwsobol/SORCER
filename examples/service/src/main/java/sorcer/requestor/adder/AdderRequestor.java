package sorcer.requestor.adder;

import sorcer.core.requestor.ServiceRequestor;
import sorcer.mo.operator;
import sorcer.provider.adder.Adder;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import java.io.File;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.response;
import static sorcer.po.operator.ent;

public class AdderRequestor extends ServiceRequestor {

    public Mogram getMogram(String... args) throws MogramException {

        String option = "exertion";
        if (args != null && args.length == 2) {
            option = args[1];
        } else if (this.args != null) {
            option = this.args[0];
        } else {
            throw new MogramException("wrong arguments for: ExertRequestor fiType, mogram fiType");
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
        } catch (Throwable e) {
            throw new MogramException(e);
        }
        return null;
    }

    private Exertion createExertion() throws ContextException, SignatureException, ExertionException {
        Double v1 = new Double(getProperty("arg/x1"));
        Double v2 = new Double(getProperty("arg/x2"));

        return xrt("hello adder", sig("add", Adder.class),
                context("adder", inVal("arg/x1", v1), inVal("arg/x2", v2),
                        result("out/y")));
    }

    private Model createModel() throws Exception {
        Double v1 = new Double(getProperty("arg/x1"));
        Double v2 = new Double(getProperty("arg/x2"));

        // model three args
        return model(inVal("arg/x1", v1), inVal("arg/x2", v2),
                ent(sig("add", Adder.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
                operator.response("add", "arg/x1", "arg/x2"));
    }

}