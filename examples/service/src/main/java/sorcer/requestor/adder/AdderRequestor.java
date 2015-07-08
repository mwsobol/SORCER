package sorcer.requestor.adder;

import sorcer.core.requestor.ServiceRequestor;
import sorcer.provider.adder.Adder;
import sorcer.service.*;

import java.io.File;
import java.io.IOException;

import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;

public class AdderRequestor extends ServiceRequestor {

    public Exertion getExertion(String... args)
            throws ExertionException, ContextException, SignatureException, IOException {


        if (args[1].equals("netlet")) {
            return (Exertion) evaluate(new File("src/main/netlets/adder.ntl"));
        } else if (args[1].equals("dynamic")) {
            return (Exertion) evaluate(new File("src/main/netlets/adder-sbp.ntl"));
        }
        Class serviceType =  Adder.class;

        Double v1 = new Double(getProperty("arg/x1"));
        Double v2 = new Double(getProperty("arg/x2"));

        return task("hello adder", sig("add", serviceType),
                context("adder", inEnt("arg/x1", v1), inEnt("arg/x2", v2),
                        result("out/y")));
    }

    @Override
    public void postprocess(String... args) throws ExertionException, ContextException {
        super.postprocess();
        logger.info("<<<<<<<<<< add task: \n" + exertion);
    }
}