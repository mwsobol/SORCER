package sorcer.arithmetic.provider.impl;

import static sorcer.co.operator.inEntry;
import static sorcer.co.operator.outEntry;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;

import java.util.logging.Logger;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.core.SorcerConstants;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ArithmeticUtil implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ArithmeticUtil.class.getName());
	
	// two level job composition with PULL and PAR execution
	private static Job createJob(Flow flow, Access access) throws Exception {
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", inEntry("arg/x1"), inEntry("arg/x2"),
						outEntry("result/y")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", inEntry("arg/x1", 10.0), inEntry("arg/x2", 50.0),
						outEntry("result/y")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0),
						outEntry("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1", 
				job("j2", t4, t5, strategy(flow, access)), 
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
				
		return job;
	}
	
	public static Exertion createJob() throws Exception {
		return createJob(Flow.SEQ, Access.PUSH);
	}

	
		
	public static Context createContext() throws Exception {
		return context("add", inEntry("arg/x1", 20.0), inEntry("arg/x2", 80.0));
	}

}
