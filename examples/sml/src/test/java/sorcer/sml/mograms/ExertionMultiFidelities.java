package sorcer.sml.mograms;

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
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.putValue;
import static sorcer.so.operator.*;

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
            context("subtract", inVal("arg/x1"), inVal("arg/x2"),
                outVal("result/y")));

        Task t4 = task("t4",
            sFi("object", sig("multiply", MultiplierImpl.class)),
            sFi("net", sig("multiply", Multiplier.class)),
            context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
                outVal("result/y")));

        Task t5 = task("t5",
            sFi("object", sig("add", AdderImpl.class)),
            sFi("net", sig("add", Adder.class)),
            context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                outVal("result/y")));

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
            fi("job1", fi("object", "j1/j2/t4"), fi("net", "j1/j2/t5")),
            fi("job2",  fi("net", "j1/j2"),
                fi("net", "j1/t3"), fi("net", "j1/j2/t4"), fi("net", "j1/j2/t5")),
            fi("job3",  fi("j1", "net"), fi("j1/j2", "net"),
                fi("net", "j1/t3"), fi("net", "j1/j2/t4"), fi("net", "j1/j2/t5")));

        return (Job)tracable(job);
    }

    @Test
    public void multiFiSigJobTest() throws Exception {

        Job job = getMultiFiJob();

        logger.info("j1 fi: " + fi(job));
        logger.info("j1 fis: " + fis(job));
        logger.info("j2 fi: " + fi(xrt(job, "j1/j2")));
        logger.info("j2 fis: " + fis(xrt(job, "j1/tj2")));
        logger.info("t3 fi: " + fi(xrt(job, "j1/t3")));
        logger.info("t4 fi: " + fi(xrt(job, "j1/j2/t4")));
        logger.info("t5 fi: " + fi(xrt(job, "j1/j2/t5")));
        logger.info("job context: " + upcontext(job));
        Context out = null;
        // Jobbers and  all tasks are local
        out = upcontext(exert(job));
        logger.info("job context: " + out);
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));

        //Local Jobbers with remote Multiplier nad Adder
        job = getMultiFiJob();
        job = exert(job, fi("object"), fi("j1/j2/t4", "net"), fi("j1/j2/t5", "net"));
        out = upcontext(job);
        logger.info("job context: " + out);
        logger.info("job trace: " + trace(job));
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));

        // Local Jobbers, Adder, and Multiplier with remote Subtractor
        job = getMultiFiJob();
        job = exert(job, fi("object"), fi("j1/t3", "net"));
        out = upcontext(job);
        logger.info("job context: " + out);
        logger.info("job trace: " + trace(job));
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));
    }

	@Test
	public void morphFiTask() throws Exception {

		Morpher t4mrp = (mgr, mFi, context) -> {
			if (mFi.getPath().equals("t4")) {
				if (((Double) value((Context)context, "result/y")) >= 200.0) {
					putValue((Context)context, "result/y", 300.0);
				}
			}
		};

		Task t4 = task("t4",
				mrpFi(t4mrp, sFi("object", sig("multiply", MultiplierImpl.class)),
						sFi("net", sig("multiply", Multiplier.class))),
				context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
						outVal("result/y")));


		assertEquals("object", fiName(t4));
		// select fidelity and check the selection
		assertEquals("net", name(fi(t4, "net")));
		assertEquals("object", name(fi(t4, "object")));

		t4 = exert(t4);
		Context out = context(t4);
		logger.info("out: " + out);
		assertTrue(value(out, "result/y").equals(300.0));
	}


    private Job getMorphFiJob() throws Exception {

		Morpher t4mrp = (mgr, mFi, context) -> {
			if (mFi.getPath().equals("t4")) {
				if (((Double) value((Context)context, "result/y")) >= 200.0) {
					mgr.reconfigure(fi("t4", "object2"));
				}
			}
		};

		Morpher t5mrp = (mgr, mFi, context) -> {
			if (mFi.getPath().equals("t5")) {
				if (((Double) value((Context)context, "result/y")) <= 200.0) {
					mgr.reconfigure(fi("t5", "object2"));
				}
			}
		};

		Task t3 = task("t3",
			sFi("object", sig("subtract", SubtractorImpl.class)),
			sFi("net", sig("subtract", Subtractor.class)),
			context("subtract", inVal("arg/x1"), inVal("arg/x2"),
				outVal("result/y")));

		Task t4 = task("t4",
			mrpFi(t4mrp, sFi("object1", sig("multiply", MultiplierImpl.class)),
				sFi("object2", sig("add", AdderImpl.class))),
			context("multiply", inVal("arg/x1", 10.0), inVal("arg/x2", 50.0),
				outVal("result/y")));

		Task t5 = task("t5",
			mrpFi(t5mrp, sFi("object1", sig("add", AdderImpl.class)),
				sFi("object2", sig("multiply", MultiplierImpl.class))),
			context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
				outVal("result/y")));

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
			fi("job1", fi("object1", "j1/j2/t4"), fi("object2", "j1/j2/t5")),
			fi("job2", fi("net", "j1/j2"),
				fi("object2", "j1/t3"), fi("object2", "j1/j2/t4"), fi("object2", "j1/j2/t5")),
			fi("job3", fi("object2", "j1"), fi("object2", "j1/j2"),
				fi("object2", "j1/t3"), fi("object2", "j1/j2/t4"), fi("object2", "j1/j2/t5")));

		return (Job) tracable(job);
	}

    @Test
    public void morphFiJobTest() throws Exception {

        Job job = getMorphFiJob();

        logger.info("j1 fi: " + fi(job));
        logger.info("j1 fis: " + fis(job));
        logger.info("j2 fi: " + fi(xrt(job, "j1/j2")));
        logger.info("j2 fis: " + fis(xrt(job, "j1/tj2")));
        logger.info("t3 fi: " + fi(xrt(job, "j1/t3")));
        logger.info("t4 fi: " + fi(xrt(job, "j1/j2/t4")));
        logger.info("t5 fi: " + fi(xrt(job, "j1/j2/t5")));
        logger.info("job context: " + upcontext(job));

        logger.info("job context: " + context(job));

        Context out = null;

        // Jobbers and  all tasks are local
        job = exert(job);
        out = upcontext(job);
        logger.info("job context: " + out);
        assertTrue(value(out, "j1/t3/result/y").equals(400.0));

        job = exert(job);
        out = upcontext(job);
        logger.info("job context: " + out);
        assertTrue(value(out, "j1/t3/result/y").equals(-1540.0));
    }

}
