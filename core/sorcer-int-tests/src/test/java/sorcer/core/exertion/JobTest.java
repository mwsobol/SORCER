package junit.sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import static org.junit.Assert.assertEquals;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.*;

import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import junit.sorcer.core.provider.Adder;
import junit.sorcer.core.provider.Multiplier;
import junit.sorcer.core.provider.Subtractor;

import org.junit.Test;

import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.Jobber;
import sorcer.service.Job;
import sorcer.service.Strategy;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */

public class JobTest {
	private final static Logger logger = Logger.getLogger(TaskTest.class
			.getName());

	static {
		System.setProperty("java.util.logging.config.file", Sorcer.getHome()
				+ "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new RMISecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-beans.jar" });
	}

	@Test
	public void batchTask1aTest() throws Exception {
		Task f4 = task(
				"f4",
				sig("multiply",
						Multiplier.class,
						deploy(configuration("bin/sorcer/test/arithmetic/configs/multiplier-prv.config"),
								idle(1), ServiceDeployment.Type.SELF)),
				context("multiply", input("arg/x1", 10.0d),
						input("arg/x2", 50.0d), out("result/y1", null)));

		Task f5 = task(
				"f5",
				sig("add",
						Adder.class,
						deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
				context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
						output("result/y2", null)));

		Task f3 = task(
				"f3",
				sig("subtract",
						Subtractor.class,
						deploy(maintain(2, perNode(2)),
								idle(1),
								configuration("bin/sorcer/test/arithmetic/configs/subtractor-prv.config"))),
				context("subtract", input("arg/x5", null),
						input("arg/x6", null), output("result/y3", null)));

		Job f1 = job("f1", sig("service", Jobber.class, "Jobber"),
				job(sig("service", Jobber.class, "Jobber"), "f2", f4, f5), f3,
				strategy(Strategy.Provision.YES),
				pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
				pipe(out(f5, "result/y2"), input(f3, "arg/x6")));

		logger.info("f1 signature : " + f1.getProcessSignature());
	}

}
