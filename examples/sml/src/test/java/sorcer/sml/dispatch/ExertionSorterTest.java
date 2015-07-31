package sorcer.sml.dispatch;

import org.junit.Assert;
import org.junit.Test;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.dispatch.ExertionSorter;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;

import java.util.ArrayList;
import java.util.List;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.co.operator.outPaths;
import static sorcer.eo.operator.*;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.10.13
 */
public class ExertionSorterTest {

    private static void printExertions(List<Exertion> exertions) {
        int i = 0;
        for (Exertion xrt : exertions) {
            System.out.println("Exertion: " + i + " " + xrt.getName());
            i++;
        }
    }

    private static void printAllExertions(Exertion topXrt) {
        if (topXrt.isTask())
            System.out.print("T " + topXrt.getName() + " ");
        else {
            System.out.println("J " + topXrt.getName() + " {");
            for (Mogram xrt : topXrt.getMograms()) {
                printAllExertions((Exertion)xrt);
            }
            System.out.println(" }");
        }
    }

    // two level job composition with PULL and PAR execution
    private static Job createJob(Strategy.Flow flow, Strategy.Access access) throws Exception {
        Task t3 = task(
                "t3",
                sig("subtract", Subtractor.class),
                context("subtract", inEnt("arg/x1", null), inEnt("arg/x2", null),
                        outEnt("result/y", null)));
        Task t4 = task("t4",
                sig("multiply", Multiplier.class),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y", null)));
        Task t5 = task("t5",
                sig("add", Adder.class),
                context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y", null)));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        return job("j1", t3, // sig("service", Jobber.class),
                job("j2", t5, t4, strategy(flow, access)),
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
    }


    private static Job createSrv() throws Exception {
        Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
                cxt("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
                        outEnt("result/y")));

        Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
                //cxt("multiply", inEnt("super/arg/x1"), inEnt("arg/x2", 50.0),
                cxt("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y")));

        Task t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                        outEnt("result/y")));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        //Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
        return job("j1", sig("execute", ServiceJobber.class),
                cxt(inEnt("arg/x1", 10.0), result("job/result", outPaths("j1/t3/result/y"))),
                job("j2", sig("execute", ServiceJobber.class), t4, t5),
                t3,
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
    }


    private static Job createComplexJob() throws Exception {

        Task f4 = task("Task_f4", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x1"), 2), inEnt(path("arg/x2"), 25 * 2),
                        outEnt(path("result/y1"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.TRUE, Strategy.Wait.TRUE));

        Task f44 = task("Task_f44", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x41"), 10.0d), inEnt(path("arg/x42"), 50.0d),
                        outEnt(path("result/y41"), null)));

        Task f5 = task("Task_f5", sig("add", Adder.class),
                context("add", inEnt(path("arg/x3"), 20.0d), inEnt(path("arg/x4"), 80.0d),
                        outEnt(path("result/y2"), null)));

        Task f6 = task("Task_f6", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x7"), 11.0d), inEnt(path("arg/x8"), 51.0d),
                        outEnt(path("result/y4"), null)));

        Task f7 = task("Task_f7", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x9"), 12.0d), inEnt(path("arg/x10"), 52.0d),
                        outEnt(path("result/y5"), null)));

        Task f9 = task("Task_f9", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x11"), 13.0d), inEnt(path("arg/x12"), 53.0d),
                        outEnt(path("result/y6"), null)));

        Task f10 = task("Task_f10", sig("multiply", Multiplier.class),
                context("multiply", inEnt(path("arg/x13"), 14.0d), inEnt(path("arg/x14"), 54.0d),
                        outEnt(path("result/y7"), null)));

        Task f3 = task("Task_f3", sig("subtract", Subtractor.class),
                context("subtract", inEnt(path("arg/x5"), null), inEnt(path("arg/x6"), null),
                        outEnt(path("result/y3"), null)));

        Task f55 = task("Task_f55", sig("add", Adder.class),
                context("add", inEnt(path("arg/x53"), 20.0d), inEnt(path("arg/x54"), 80.0d), outEnt(path("result/y52"), null)));

        Task f21 = task("Task_f21", sig("multiply", Multiplier.class), context("Task_f21", inEnt(path("arg2"), 50.5d), inEnt(path("arg1"), 20.0d), outEnt(path("fillMeOut"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.FALSE, Strategy.Wait.TRUE));

        Task f22 = task("Task_f22", sig("add", Adder.class), context("Task_f22", inEnt(path("arg4"), 23d), inEnt(path("arg3"), 43d), outEnt(path("fillMeOut"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.FALSE, Strategy.Wait.TRUE));

        Job f20 = job("Job_f20", f22 , f21 );

        Job j8 = job("Job_f8", pipe(outPoint(f10, path("result/y7")), inPoint(f55, path("arg/x54"))), pipe(outPoint(f7, path("result/y5")), inPoint(f55, path("arg/x53"))), f55, f10, f9,
                pipe(outPoint(f9, path("result/y6")), inPoint(f10, path("arg/x13"))));

        Pipe p1 = pipe(outPoint(f4, path("result/y1")), inPoint(f7, path("arg/x9")));

        return job("Job_f1", f3, j8, f20, job("Job_f2", f5, f7, f6, f4),
                pipe(outPoint(f6, path("result/y4")), inPoint(f5, path("arg/x3"))),
                pipe(outPoint(f4, path("result/y1")), inPoint(f3, path("arg/x5"))),
                pipe(outPoint(f5, path("result/y2")), inPoint(f3, path("arg/x6"))), p1);
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
        Assert.assertEquals(Strategy.Flow.PAR, ((Exertion)es.getSortedJob().getMogram("j2")).getFlowType());

    }

    @Test
    public void testSorterSimple2() throws Exception {
        System.out.println("Before sorting");
        Job job = createJob(Strategy.Flow.AUTO, Strategy.Access.PULL);
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        Assert.assertEquals(Strategy.Flow.PAR, ((Exertion)es.getSortedJob().getMogram("j2")).getFlowType());
    }

    @Test
    public void testSorterComplex() throws Exception {
        System.out.println("Before sorting");
        Job job = createComplexJob();
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        final Exertion f3 = (Exertion)job.getMogram("Task_f3");
        final Exertion j2 = (Exertion)job.getMogram("Job_f2");
        final Exertion j8 = (Exertion)job.getMogram("Job_f8");
        final Exertion j20 = (Exertion)job.getMogram("Job_f20");
        List<Exertion> expList = new ArrayList<Exertion>();
        expList.add(j2);
        expList.add(f3);
        expList.add(j8);
        expList.add(j20);
        Assert.assertArrayEquals(expList.toArray(), es.getSortedJob().getMograms().toArray());
    }

}
