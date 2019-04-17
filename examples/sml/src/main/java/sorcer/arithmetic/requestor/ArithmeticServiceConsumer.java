package sorcer.arithmetic.requestor;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Averager;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.AveragerImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.co.operator;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.requestor.ServiceConsumer;
import sorcer.service.*;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

public class ArithmeticServiceConsumer extends ServiceConsumer {

	/* (non-Javadoc)
         * @see sorcer.core.requestor.ServiceConsumer#getMogram(java.lang.String[])
         */
	@Override
	public Mogram getMogram(String... args) throws MogramException {

		Job job = null;
		try {
			Task t3 = task("t3", sigFi("object/subtract", sig("subtract", SubtractorImpl.class)),
					sigFi("object/average", sig("average", AveragerImpl.class)),
					sigFi("net/subtract", sig("subtract", Subtractor.class)),
					sigFi("net/average", sig("average", Averager.class)),
					context("t3-cxt", operator.inVal("arg/x1"), operator.inVal("arg/x2"),
							outVal("result/y")));

			Task t4 = task("t4", sigFi("object", sig("multiply", MultiplierImpl.class)),
					sigFi("net", sig("multiply", Multiplier.class)),
					context("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
							outVal("result/y")));

			Task t5 = task("t5", sigFi("object", sig("add", AdderImpl.class)),
					sigFi("net", sig("add", Adder.class)),
					context("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0),
							outVal("result/y")));
			job = job("j1", sigFi("object", sig("service", ServiceJobber.class)),
					sigFi("net", sig("exert", Jobber.class)),
					job("j2", sig("exert", ServiceJobber.class), t4, t5),
					t3,
					pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
					pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")),
					metaFi("job1", fi("net", "j1"), fi("net", "j1/j2/t4")),
					metaFi("job2", fi("net", "j1"), fi("net", "j1/j2/t4"), fi("net", "j1/j2/t5")));

		} catch (Exception e) {
			throw new MogramException(e);
		}
		return job;
	}

	@Override
	public void postprocess(String... args) throws RoutineException, ContextException {
		super.postprocess();
		logger.info("<<<<<<<<<< f5 context: \n" + upcontext((Routine)mogram));
	}
}