package sorcer.arithmetic.tester.requestor;


import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Averager;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.AveragerImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.service.*;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

public class ArithmeticServiceRequestor extends ServiceRequestor {

	/* (non-Javadoc)
	 * @see sorcer.core.requestor.ExertionRunner#getMogram(java.lang.String[])
	 */
	@Override
	public Exertion getExertion(String... args) throws ExertionException, ContextException, SignatureException {

			Task t3 = task("t3",
					sFi("object", sig("subtract", SubtractorImpl.class), sig("average", AveragerImpl.class)),
					sFi("net", sig("subtract", Subtractor.class), sig("average", Averager.class)),
					context("t3-cxt", inEnt("arg/x1", null), inEnt("arg/x2", null),
							outEnt("result/y", null)));

			Task t4 = task("t4", sFi("object", sig("multiply", MultiplierImpl.class)),
					sFi("net", sig("multiply", Multiplier.class)),
					context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
							outEnt("result/y", null)));

			Task t5 = task("t5", sFi("object", sig("add", AdderImpl.class)),
					sFi("net", sig("add", Adder.class)),
					context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
							outEnt("result/y")));

			Job job = job("j1", sFi("object", sig("service", ServiceJobber.class)),
					sFi("net", sig("service", Jobber.class)),
					job("j2", sig("service", ServiceJobber.class), t4, t5),
					t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					fiContext("mix1", fi("j1", "net"), fi("j1/j2/t4", "net")),
					fiContext("mix2", fi("j1", "net"), fi("j1/j2/t4", "net"), fi("j1/j2/t5", "net")));

			return job;

	}

	@Override
	public void postprocess(String... args) throws ExertionException, ContextException {
		super.postprocess();
		logger.info("<<<<<<<<<< f5 context: \n" + serviceContext(exertion));
	}
}