package sorcer.arithmetic.requestor;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Averager;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.requestor.SorcerRequestor;
import sorcer.service.*;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

public class ArithmeticSorcerRequestor extends SorcerRequestor {

    public Exertion getExertion(String... args) throws ExertionException, ContextException, SignatureException {

        Task t3 = task("t3",
                srvFi("object", sig("subtract", SubtractorImpl.class), sig("average", AveragerImpl.class)),
                srvFi("net", sig("subtract", Subtractor.class), sig("average", Averager.class)),
                context("t3-cxt", inEnt("arg/x1", null), inEnt("arg/x2", null),
                        outEnt("result/y", null)));

        Task t4 = task("t4", srvFi("object", sig("multiply", MultiplierImpl.class)),
                srvFi("net", sig("multiply", Multiplier.class)),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y", null)));

        Task t5 = task("t5", srvFi("object", sig("add", AdderImpl.class)),
                srvFi("net", sig("add", Adder.class)),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));

        Job job = job("j1", srvFi("object", sig("service", ServiceJobber.class)),
                srvFi("net", sig("service", Jobber.class)),
                job("j2", sig("service", ServiceJobber.class), t4, t5),
                t3,
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
                fiContext("mix1", fi("j1", "net"), fi("j1/j2/t4", "net")),
                fiContext("mix2", fi("j1", "net"), fi("j1/j2/t4", "net"), fi("j1/j2/t5", "net")));

        return job;

    }

    @Override
    public void run(String... args) throws Exception {
        Exertion exertion = exert(getExertion());
        logger.info("<<<<<<<<<< f5 context: \n" + serviceContext(exertion));

    }
}