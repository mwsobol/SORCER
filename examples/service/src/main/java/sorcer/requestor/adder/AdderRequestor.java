package sorcer.requestor.adder;

import sorcer.core.requestor.ServiceRequestor;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.provider.adder.impl.AdderImpl;

import java.util.Arrays;

import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;

public class AdderRequestor extends ServiceRequestor {

    public Exertion getExertion(String... args) throws ExertionException, ContextException, SignatureException {

        logger.info("args: " + Arrays.toString(args));
        
        return task("hello adder", sig("add", AdderImpl.class),
                context("adder", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        result("out/y")));
    }

    @Override
    public void postprocess(String... args) throws ExertionException, ContextException {
        super.postprocess();
        logger.info("<<<<<<<<<< add task: \n" + exertion);
    }
}