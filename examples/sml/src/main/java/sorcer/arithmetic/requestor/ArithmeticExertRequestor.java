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
import sorcer.core.requestor.ExertRequestor;
import sorcer.service.*;

import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.*;

public class ArithmeticExertRequestor extends ExertRequestor {

	/* (non-Javadoc)
	 * @see sorcer.core.requestor.ServiceRequestor#getMogram(java.lang.String[])
	 */
	@Override
	public Mogram getMogram(String... args) throws MogramException {

		Job job = null;
		try {
			Task t3 = task("t3", sFi("object/subtract", sig("subtract", SubtractorImpl.class)),
					sFi("object/average", sig("average", AveragerImpl.class)),
					sFi("net/subtract", sig("subtract", Subtractor.class)),
					sFi("net/average", sig("average", Averager.class)),
					context("t3-cxt", inEnt("arg/x1"), inEnt("arg/x2"),
							outEnt("result/y")));

			Task t4 = task("t4", sFi("object", sig("multiply", MultiplierImpl.class)),
					sFi("net", sig("multiply", Multiplier.class)),
					context("multiply", inEnt("arg/x1", 10.0), inEnt("arg/x2", 50.0),
							outEnt("result/y")));

			Task t5 = task("t5", sFi("object", sig("add", AdderImpl.class)),
					sFi("net", sig("add", Adder.class)),
					context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0),
							outEnt("result/y")));
			job = job("j1", sFi("object", sig("service", ServiceJobber.class)),
					sFi("net", sig("exert", Jobber.class)),
					job("j2", sig("exert", ServiceJobber.class), t4, t5),
					t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					sFi("job1", cFi("j1", "net"), cFi("j1/j2/t4", "net")),
					sFi("job2", cFi("j1", "net"), cFi("j1/j2/t4", "net"), cFi("j1/j2/t5", "net")));

		} catch (Exception e) {
			throw new MogramException(e);
		}
		return job;
	}

	@Override
	public void postprocess(String... args) throws ExertionException, ContextException {
		super.postprocess();
		logger.info("<<<<<<<<<< f5 context: \n" + upcontext((Exertion)mogram));
	}
}