package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.co.operator;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.service.Context;
import sorcer.service.Routine;
import sorcer.service.Job;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ArithmeticUtil implements SorcerConstants {

	private final static Logger logger = LoggerFactory.getLogger(ArithmeticUtil.class.getName());
	
	// two level job composition with PULL and PAR execution
	private static Job createJob(Flow flow, Access access) throws Exception {
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", operator.inVal("arg/x1"), operator.inVal("arg/x2"),
						outVal("result/y")));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1", 
				job("j2", t4, t5, strategy(flow, access)), 
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));
				
		return job;
	}
	
	public static Routine createJob() throws Exception {
		return createJob(Flow.SEQ, Access.PUSH);
	}

	public static Job createLocalJob() throws Exception {
		Task t3 = task("t3", sig("subtract", SubtractorImpl.class),
				context("subtract", operator.inVal("arg/x1"), operator.inVal("arg/x2"),
						outVal("result/y")));

		Task t4 = task("t4", sig("multiply", MultiplierImpl.class),
				context("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
						outVal("result/y")));

		Task t5 = task("t5", sig("add", AdderImpl.class),
				context("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0),
						outVal("result/y")));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		Job job = job("j1", sig("exert", ServiceJobber.class),
				job("j2", t4, t5, sig("exert", ServiceJobber.class)),
				t3,
				pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
				pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

		return job;
	}
		
	public static Context createContext() throws Exception {
		return context("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0));
	}

}
