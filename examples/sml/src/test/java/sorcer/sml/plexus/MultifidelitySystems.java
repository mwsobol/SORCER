package sorcer.sml.plexus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.plexus.MultiFidelityService;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Fidelity;
import sorcer.service.Job;
import sorcer.service.Task;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class MultifidelitySystems {
    private final static Logger logger = LoggerFactory.getLogger(MultifidelitySystems.class);

    @Test
    public void multiFideltySystem() throws Exception {

        MultiFidelityService mfs = new MultiFidelityService("plexus");

        mfs.put(getTask4());
        mfs.put(getTask5());
        mfs.put(getJob());

        FidelityManager fm = mfs.getFidelityManager();
        fm.addFidelity(fi("f1", "t4", "t5"));
        fm.addFidelity(new Fidelity("f2", "j1"));
        fm.setSelectedFidelity("f1");

        logger.info("manager: " + fm.getFidelities());
    }


    public Task getTask4() throws Exception {
        Task t4 = task(
                "t4",
                sig("multiply", Multiplier.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y", null)));

        return t4;
    }

    public Task getTask5() throws Exception {
        Task t5 = task(
                "t5",
                sig("add", Adder.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y", null)));

        return t5;
    }

    public Job getJob() throws Exception {
        Task t3 = task(
                "t3",
                sig("subtract", Subtractor.class),
                context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
                        outEnt("result/y", null)));

        Task t4 = task(
                "t4",
                sig("multiply", Multiplier.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y", null)));

        Task t5 = task(
                "t5",
                sig("add", Adder.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y", null)));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        Job job = job(
                "j1", sig("service", ServiceJobber.class),
                job("j2", t4, t5), t3,
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

        return job;
    }
}
