package sorcer.sml.mogram;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.plexus.Morpher;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.*;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

/**
 * Created by Mike Sobolewski on 06/24/16.
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ExertionMultiFidelities {

    private final static Logger logger = LoggerFactory.getLogger(ExertionMultiFidelities.class);

    private Job getMultiFiJob() throws Exception {

        Task t3 = task("t3",
            sFi("object", sig("subtract", SubtractorImpl.class)),
            sFi("net", sig("subtract", Subtractor.class)),
            context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
                outEnt("result/y")));

        Task t4 = task("t4",
            sFi("object", sig("multiply", MultiplierImpl.class)),
            sFi("net", sig("multiply", Multiplier.class)),
            context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                outEnt("result/y")));

        Task t5 = task("t5",
            sFi("object", sig("add", AdderImpl.class)),
            sFi("net", sig("add", Adder.class)),
            context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
                outEnt("result/y")));

        Job job = job("j1",
            sFi("object", sig("exert", ServiceJobber.class)),
            sFi("net", sig("exert", Jobber.class)),
            job("j2",
                sFi("object", sig("exert", ServiceJobber.class)),
                sFi("net", sig("exert", Jobber.class)),
                t4, t5),
            t3,
            pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
            pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
            fi("job1", cFi("j1/j2/t4", "object"), cFi("j1/j2/t5", "net")),
            fi("job2",  cFi("j1/j2", "net"),
                cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")),
            fi("job3",  cFi("j1", "net"), cFi("j1/j2", "net"),
                cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")));

        return (Job)tracable(job);
    }

    @Test
    public void multiFiSigJobTest() throws Exception {

        Job job = getMultiFiJob();

        logger.info("j1 fi: " + fi(job));
        logger.info("j1 fis: " + fis(job));
        logger.info("j2 fi: " + fi(exertion(job, "j1/j2")));
        logger.info("j2 fis: " + fis(exertion(job, "j1/tj2")));
        logger.info("t3 fi: " + fi(exertion(job, "j1/t3")));
        logger.info("t4 fi: " + fi(exertion(job, "j1/j2/t4")));
        logger.info("t5 fi: " + fi(exertion(job, "j1/j2/t5")));
        logger.info("job context: " + upcontext(job));
        Context out = null;
        // Jobbers and  all tasks are local
        out = upcontext(exert(job));
        logger.info("job context: " + out);
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));

        //Local Jobbers with remote Multiplier nad Adder
        job = getMultiFiJob();
        job = exert(job, fi("object"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net"));
        out = upcontext(job);
        logger.info("job context: " + out);
        logger.info("job trace: " + trace(job));
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));

        // Local Jobbers, Adder, and Multiplier with remote Subtractor
        job = getMultiFiJob();
        job = exert(job, fi("object"), cFi("j1/t3", "net"));
        out = upcontext(job);
        logger.info("job context: " + out);
        logger.info("job trace: " + trace(job));
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));
    }

    private Job getMorphMultiFiJob() throws Exception {

		Morpher t4mrp = (mgr, mFi, context) -> {
			ServiceFidelity<Signature> fi = mFi.getFidelity();
			if (mFi.getPath().equals("t4")) {
				if (((Double) value((Context)context, "result/y")) >= 200.0) {
					mgr.reconfigure(fi("t5", "net"));
				}
			}
		};

		Morpher t5mrp = (mgr, mFi, context) -> {
			ServiceFidelity<Signature> fi = mFi.getFidelity();
			if (mFi.getPath().equals("t5")) {
				if (((Double) value((Context)context, "result/y")) <= 200.0) {
					mgr.reconfigure(fi("t4", "net"));
				}
			}
		};

		Task t3 = task("t3",
			sFi("object", sig("subtract", SubtractorImpl.class)),
			sFi("net", sig("subtract", Subtractor.class)),
			context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
				outEnt("result/y")));

		Task t4 = task("t4",
			mFi(t4mrp, sFi("object", sig("multiply", MultiplierImpl.class)),
				sFi("net", sig("multiply", Multiplier.class))),
			context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
				outEnt("result/y")));

		Task t5 = task("t5",
			mFi(t5mrp, sFi("object", sig("add", AdderImpl.class)),
				sFi("net", sig("add", Adder.class))),
			context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
				outEnt("result/y")));

		Job job = job("j1",
			sFi("object", sig("exert", ServiceJobber.class)),
			sFi("net", sig("exert", Jobber.class)),
			job("j2",
				sFi("object", sig("exert", ServiceJobber.class)),
				sFi("net", sig("exert", Jobber.class)),
				t4, t5),
			t3,
			pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
			pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
			fi("job1", cFi("j1/j2/t4", "object"), cFi("j1/j2/t5", "net")),
			fi("job2", cFi("j1/j2", "net"),
				cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")),
			fi("job3", cFi("j1", "net"), cFi("j1/j2", "net"),
				cFi("j1/t3", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")));

		return (Job) tracable(job);
	}

		@Test
		public void morphFiSigJobTest() throws Exception {

			Job job = getMorphMultiFiJob();

			logger.info("j1 fi: " + fi(job));
			logger.info("j1 fis: " + fis(job));
			logger.info("j2 fi: " + fi(exertion(job, "j1/j2")));
			logger.info("j2 fis: " + fis(exertion(job, "j1/tj2")));
			logger.info("t3 fi: " + fi(exertion(job, "j1/t3")));
			logger.info("t4 fi: " + fi(exertion(job, "j1/j2/t4")));
			logger.info("t5 fi: " + fi(exertion(job, "j1/j2/t5")));
			logger.info("job context: " + upcontext(job));
			Context out = null;
			// Jobbers and  all tasks are local
			out = upcontext(exert(job));
//			logger.info("job context: " + out);
//			assertTrue(value(out, "j1/t3/result/y").equals(400.0));
    }

	@Test
    public void task1() throws Exception {
        Task t3 = task("t3",
                sFi("object", sig("subtract", SubtractorImpl.class)),
                sFi("net", sig("subtract", Subtractor.class)),
                context("subtract", inEnt("arg/x1"), inEnt("arg/x2"),
                        outEnt("result/y")));

        logger.info("FI: " + t3.selectFidelity());
        logger.info("CONTEXT: " + context (t3));
    }


    @Test
    public void task2() throws Exception {

        Morpher t4mrp = (mgr, mFi, context) -> {
            ServiceFidelity<Signature> fi = mFi.getFidelity();
            if (mFi.getPath().equals("t4")) {
                if (((Double) value((Context)context, "result/y")) >= 200.0) {
                    mgr.reconfigure(fi("t5", "net"));
                }
            }
        };

        Task t4 = task("t4",
                mFi(t4mrp, sFi("object", sig("multiply", MultiplierImpl.class)),
                        sFi("net", sig("multiply", Multiplier.class))),
                context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
                        outEnt("result/y")));


        logger.info("FI: " + t4.selectFidelity("net"));
        logger.info("FI: " + t4.selectFidelity("object"));
//        logger.info("FI: " + t4.selectFidelity(fi("t4", "net")));
        logger.info("CONTEXT: " + context (t4));

    }
}
