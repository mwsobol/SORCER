package sorcer.dispatch;

import org.junit.Assert;
import org.junit.Test;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.dispatch.ExertionSorter;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;
import sorcer.service.Strategy.*;

import java.util.ArrayList;
import java.util.List;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.10.13
 */
public class ExertionSorterTest {

    private static void printExertions(List<Routine> exertions) {
        int i = 0;
        for (Routine xrt : exertions) {
            System.out.println("Exertion: " + i + " " + xrt.getName());
            i++;
        }
    }

    private static void printAllExertions(Routine topXrt) {
        if (topXrt.isTask())
            System.out.print("T " + topXrt.getName() + " ");
        else {
            System.out.println("J " + topXrt.getName() + " {");
            for (Mogram xrt : topXrt.getMograms()) {
                printAllExertions((Routine)xrt);
            }
            System.out.println(" }");
        }
    }

    // two level job composition with PULL and PAR execution
    private static Job createJob(Strategy.Flow flow, Strategy.Access access) throws Exception {
        Task t3 = task(
                "t3",
                sig("subtract", Subtractor.class),
                context("subtract", inVal("arg/x1", null), inVal("arg/x2", null),
                        outVal("result/y", null)));
        Task t4 = task("t4",
                sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        outVal("result/y", null)));
        Task t5 = task("t5",
                sig("add", Adder.class),
                context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        outVal("result/y", null)));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        return job("j1", t3, // sig("exert", Jobber.class),
                job("j2", t5, t4, strategy(flow, access)),
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
    }


    private static Job createSrv() throws Exception {
        Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                cxt("subtract", inVal("arg/x1"), inVal("arg/x2"),
                        outVal("result/y")));

        Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                //cxt("multiply", inVal("super/arg/x1"), inVal("arg/x2", 50.0),
                cxt("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                        outVal("result/y")));

        Task t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        outVal("result/y")));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        //Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
        return job("j1", sig("exert", ServiceJobber.class),
                cxt(inVal("arg/x1", 10.0), result("job/result", outPaths("j1/t3/result/y"))),
                job("j2", sig("exert", ServiceJobber.class), t4, t5),
                t3,
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
    }


    private static Job createComplexJob() throws Exception {

        Task f4 = task("Task_f4", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x1", 2), inVal("arg/x2", 25 * 2),
                        outVal("result/y1")),
                strategy(Access.PUSH, Flow.SEQ, Monitor.NOTIFY_ALL, Provision.TRUE, Wait.TRUE));

        Task f44 = task("Task_f44", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x41", 10.0d), inVal("arg/x42", 50.0d),
                        outVal("result/y41", null)));

        Task f5 = task("Task_f5", sig("add", Adder.class),
                context("add", inVal("arg/x3", 20.0d), inVal("arg/x4", 80.0d),
                        outVal("result/y2")));

        Task f6 = task("Task_f6", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x7", 11.0d), inVal("arg/x8", 51.0d),
                        outVal("result/y4")));

        Task f7 = task("Task_f7", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x9", 12.0d), inVal("arg/x10", 52.0d),
                        outVal("result/y5")));

        Task f9 = task("Task_f9", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x11", 13.0d), inVal("arg/x12", 53.0d),
                        outVal("result/y6")));

        Task f10 = task("Task_f10", sig("multiply", Multiplier.class),
                context("multiply", inVal("arg/x13", 14.0d), inVal("arg/x14", 54.0d),
                        outVal("result/y7")));

        Task f3 = task("Task_f3", sig("subtract", Subtractor.class),
                context("subtract", inVal("arg/x5"), inVal("arg/x6"),
                        outVal("result/y3")));

        Task f55 = task("Task_f55", sig("add", Adder.class),
                context("add", inVal("arg/x53", 20.0d), inVal("arg/x54", 80.0d), outVal("result/y52")));

        Task f21 = task("Task_f21", sig("multiply", Multiplier.class),
                context("Task_f21", inVal("arg2", 50.5d), inVal("arg1", 20.0d), outVal("fillMeOut")),
                strategy(Access.PUSH, Flow.SEQ, Monitor.NOTIFY_ALL, Provision.FALSE, Wait.TRUE));

        Task f22 = task("Task_f22", sig("add", Adder.class),
                context("Task_f22", inVal("arg4", 23d), inVal("arg3", 43d), outVal("fillMeOut")),
                strategy(Access.PUSH, Flow.SEQ, Monitor.NOTIFY_ALL, Provision.FALSE, Wait.TRUE));

        Job f20 = job("Job_f20", f22 , f21 );

        Job j8 = job("Job_f8", pipe(outPoint(f10, "result/y7"),
                inPoint(f55, "arg/x54")), pipe(outPoint(f7, "result/y5"),
                inPoint(f55, "arg/x53")), f55, f10, f9,
                pipe(outPoint(f9, "result/y6"), inPoint(f10, "arg/x13")));

        Pipe p1 = pipe(outPoint(f4, "result/y1"), inPoint(f7, "arg/x9"));

        return job("Job_f1", f3, j8, f20, job("Job_f2", f5, f7, f6, f4),
                pipe(outPoint(f6, "result/y4"), inPoint(f5, "arg/x3")),
                pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
                pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")), p1);
    }

    @Test
    public void testSorterSimple() throws Exception {
        System.out.println("Before sorting");
        Job job = createSrv(); //createComplexJob();
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        Assert.assertEquals(Strategy.Flow.SEQ, es.getSortedJob().getFlowType());
        Assert.assertEquals(Strategy.Flow.PAR, ((Routine)es.getSortedJob().getMogram("j2")).getFlowType());

    }

    @Test
    public void testSorterSimple2() throws Exception {
        System.out.println("Before sorting");
        Job job = createJob(Strategy.Flow.AUTO, Strategy.Access.PULL);
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        Assert.assertEquals(Strategy.Flow.PAR, ((Routine)es.getSortedJob().getMogram("j2")).getFlowType());
    }

    @Test
    public void testSorterComplex() throws Exception {
        System.out.println("Before sorting");
        Job job = createComplexJob();
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        final Mogram f3 = job.getMogram("Task_f3");
        final Mogram j2 = job.getMogram("Job_f2");
        final Mogram j8 = job.getMogram("Job_f8");
        final Mogram j20 = job.getMogram("Job_f20");
        List<Mogram> expList = new ArrayList<Mogram>();
        expList.add(j2);
        expList.add(f3);
        expList.add(j8);
        expList.add(j20);
        Assert.assertArrayEquals(expList.toArray(), es.getSortedJob().getMograms().toArray());
    }

}
