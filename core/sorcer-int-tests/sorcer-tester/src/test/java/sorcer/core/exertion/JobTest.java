package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.Jobber;
import sorcer.service.Job;
import sorcer.service.Strategy;
import sorcer.service.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class JobTest {
	private final static Logger logger = LoggerFactory.getLogger(TaskTest.class
			.getName());

	@Test
	public void batchTask1aTest() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply",
						Multiplier.class,
						deploy(configuration("bin/sorcer/test/arithmetic/configs/multiplier-prv.config"),
								idle(1), ServiceDeployment.Type.SELF)),
				context("multiply", inEnt("arg/x1", 10.0d),
						inEnt("arg/x2", 50.0d), outEnt("result/y1", null)));

		Task f5 = task(
				"f5",
				sig("add",
						Adder.class,
						deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
				context("add", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
						outEnt("result/y2", null)));

		Task f3 = task(
				"f3",
				sig("subtract",
						Subtractor.class,
						deploy(maintain(2, perNode(2)),
								idle(1),
								configuration("bin/sorcer/test/arithmetic/configs/subtractor-prv.config"))),
				context("subtract", inEnt("arg/x5", null),
						inEnt("arg/x6"), outEnt("result/y3")));

		Job f1 = job("f1", sig("service", Jobber.class, "Jobber"),
				job(sig("service", Jobber.class, "Jobber"), "f2", f4, f5), f3,
				strategy(Strategy.Provision.YES),
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));

		logger.info("f1 signature : " + f1.getProcessSignature());
	}

}
