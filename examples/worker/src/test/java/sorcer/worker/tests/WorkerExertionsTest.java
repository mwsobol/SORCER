package sorcer.worker.tests;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.ent;
import static sorcer.co.operator.inEnt;
import static sorcer.co.operator.outEnt;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.prvName;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.requestTime;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.type;
import static sorcer.eo.operator.value;

import java.net.InetAddress;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Strategy;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Task;
import sorcer.util.Log;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.Worker;
import sorcer.worker.provider.impl.WorkerProvider;
import sorcer.worker.requestor.Works;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkerExertionsTest {
	private static Logger logger = Log.getTestLog();
	
	private Context context;
	private String hostname;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		hostname = InetAddress.getLocalHost().getHostName();

		Work work = new Work() {
			private static final long serialVersionUID = 1L;

			public Context<Integer> exec(Context cxt) throws InvalidWork, ContextException {
				int arg1 = (int)value(cxt, "req/arg/1");
				int arg2 = (int)value(cxt, "req/arg/2");
				int result =  arg1 * arg2;
				put(cxt, "prv/result", result);
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		context = context("work", 
				ent("req/name", hostname),
				ent("req/arg/1", 11),
				ent("req/arg/2", 101),
				ent("req/work", work),
				ent("to/prv/name", "Worker Provider"));
	}

	@Test
	public void localProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerProvider.class), context);
		
		Task et = exert(pt);
		
		logger.info("context: " + context(et));
		
		assertEquals(get(et, "prv/result"), 1111);
		
		assertEquals(get(et, "prv/message"), 
				"Done work by: class sorcer.worker.provider.impl.WorkerProvider");
		
		assertEquals(get(et, "prv/host/name"), hostname);
		
	}

	
	@Test
	public void remoteProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", Worker.class), context, prvName("Worker1"));
		
		Task et = exert(pt);
		
		logger.info("context: " + context(et));
		
		assertEquals(get(et, "prv/result"), 1111);
		
		assertEquals(get(et, "prv/message"), 
				"Done work by: Worker1");
		
		assertEquals(get(et, "prv/host/name"), hostname);
		
	}
	
	
	@Test
	public void exertionStrategy() throws Exception {
//		String requestorName = getProperty("requestor.name");
//		String pn1, pn2, pn3;
//		pn1 = Sorcer.getSuffixedName(getProperty("provider.name.1"));
//		pn2 = Sorcer.getSuffixedName(getProperty("provider.name.2"));
//		pn3 = Sorcer.getSuffixedName(getProperty("provider.name.3"));
//		Job job = null;
//		try {
//			Context context1 = new ServiceContext("work1");
//			context1.putValue("requestor/name", requestorName);
//			context1.putValue("requestor/operand/1", 20);
//			context1.putValue("requestor/operand/2", 80);
//			context1.putValue("requestor/work", Works.work1);
//			context1.putValue("to/provider/name", pn1);
//
//			Context context2 = new ServiceContext("work2");
//			context2.putValue("requestor/name", requestorName);
//			context2.putValue("requestor/operand/1", 10);
//			context2.putValue("requestor/operand/2", 50);
//			context2.putValue("requestor/work", Works.work2);
//			context2.putValue("to/provider/name", pn2);
//
//			Context context3 = new ServiceContext("work3");
//			context3.putValue("requestor/name", requestorName);
//			context3.putValue("requestor/operand/1", 100);
//			context3.putValue("requestor/operand/2", 100);
//			context3.putValue("requestor/work", Works.work3);
//			context3.putValue("to/provider/name", pn3);

			Context cxt1 = context(ent("req/name", "workoholic"), 
					ent("req/arg/1", 20),  ent("req/arg/2", 80),
					ent("req/work", Works.work1),  ent("tp/prv/name", "Worker1"));
		
			Context cxt2 = context(ent("req/name", "workoholic"), 
					ent("req/arg/1", 10),  ent("req/arg/2", 50),
					ent("req/work", Works.work2),  ent("tp/prv/name", "Worker2"));
			
			Context cxt3 = context(ent("req/name", "workoholic"), 
					ent("req/arg/1", 100),  ent("req/arg/2", 100),
					ent("req/work", Works.work3),  ent("tp/prv/name", "Worker3"));
			
//			NetSignature signature1 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class, pn1);
//			NetSignature signature2 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class, pn2);
//			NetSignature signature3 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class, pn3);
//
//			Task task1 = new NetTask("work1", signature1, context1);
//			task1.setExecTimeRequested(true);
//			Task task2 = new NetTask("work2", signature2, context2);
//			Task task3 = new NetTask("work3", signature3, context3);
//			job = new NetJob("flow");
//			job.setExecTimeRequested(true);
//			job.addExertion(task1);
//			job.addExertion(task2);
//			job.addExertion(task3);

			Job job = job("strategy", 
					task("work1", sig("doWork", Worker.class, "Worker1"), cxt1),
					task("work2", sig("doWork", Worker.class, "Worker2"), cxt2),
					task("work3", sig("doWork", Worker.class, "Worker3"), cxt3));
			
			
			Strategy control1 = strategy(Flow.SEQ, Access.PUSH);
			Strategy control2 = strategy(Flow.PAR, Access.PUSH);
			Strategy control3 = strategy(Flow.SEQ, Access.PULL);
			Strategy control4 = strategy(Flow.PAR, Access.PULL);

			Job out = exert(job, control1);
			
//			 out = exert(job, control2);
//			
//			 out = exert(job, control3);
//			
//			 out = exert(job, control4);
			
			
//			// PUSH or PULL provider access
//			boolean isPushAccess = getProperty("provider.access.type", "PUSH").equals("PUSH");
//			if (isPushAccess)
//				job.setAccessType(Access.PUSH);
//			else
//				job.setAccessType(Access.PULL);
//
//			// Exertion control flow PARallel or SEQential
//			boolean iSequential = getProperty("provider.control.flow", "SEQUENTIAL").equals("SEQUENTIAL");
//			if (iSequential)
//				job.setFlowType(Flow.SEQ);
//			else
//				job.setFlowType(Flow.PAR);

//			logger.info("isPushAccess: " + isPushAccess + " iSequential: " + iSequential);
//		} catch (Exception e) {
//			throw new ExertionException("Failed to create exertion", e);
//		}
//		return job;
	}
//	
//	
	@Test
	public void contectPipss() throws Exception {
//		String requestorName = getProperty("requestor.name");
//
//		// define requestor data
//		Job job = null;
//		try {
//			Context context1 = new ServiceContext("work1");
//			context1.putValue("requestor/name", requestorName);
//			context1.putValue("requestor/operand/1", 20);
//			context1.putValue("requestor/operand/2", 80);
//			context1.putValue("requestor/work", Works.work1);
//			context1.putOutValue("provider/result", Context.none);
//
//			Context context2 = new ServiceContext("work2");
//			context2.putValue("requestor/name", requestorName);
//			context2.putValue("requestor/operand/1", 10);
//			context2.putValue("requestor/operand/2", 50);
//			context2.putValue("requestor/work", Works.work2);
//			context2.putOutValue("provider/result", Context.none);
//
//			Context context3 = new ServiceContext("work3");
//			context3.putValue("requestor/name", requestorName);
//			context3.putInValue("requestor/operand/1", Context.none);
//			context3.putInValue("requestor/operand/2", Context.none);
//			context3.putValue("requestor/work", Works.work3);

		Context cxt1 = context(ent("req/name", "workoholic"), 
				ent("req/arg/1", 20),  ent("req/arg/2", 80),
				ent("req/work", Works.work1),  ent("tp/prv/name", "Worker1"),
				outEnt("prv/result"));
	
		Context cxt2 = context(ent("req/name", "workoholic"), 
				ent("req/arg/1", 10),  ent("req/arg/2", 50),
				ent("req/work", Works.work2),  ent("tp/prv/name", "Worker2"),
				outEnt("prv/result"));
		
		Context cxt3 = context(ent("req/name", "workoholic"), 
				inEnt("req/arg/1"),  inEnt("req/arg/2"),
				ent("req/work", Works.work3),  ent("tp/prv/name", "Worker3"));
		
//			// pass the parameters from one context to the next context
//			// piping parameters should be annotated via in, out, or inout paths
//			context1.connect("provider/result", "requestor/operand/1", context3);
//			context2.connect("provider/result", "requestor/operand/2", context3);

//			// define required services
//			NetSignature signature1 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class);
//			NetSignature signature2 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class);
//			NetSignature signature3 = new NetSignature("doWork",
//					sorcer.ex2.provider.Worker.class);
//
//			// define tasks
//			Task task1 = new NetTask("work1", signature1, context1);
//			Task task2 = new NetTask("work2", signature2, context2);
//			Task task3 = new NetTask("work3", signature3, context3);
//
//			// define a job
//			job = new NetJob("piped");
//			job.addExertion(task1);
//			job.addExertion(task2);
//			job.addExertion(task3);
		
			Job job = job("strategy", 
					task("work1", sig("doWork", Worker.class), cxt1),
					task("work2", sig("doWork", Worker.class), cxt2),
					task("work3", sig("doWork", Worker.class), cxt3),
					pipe(out("strategy/work1", "prv/result"), in("strategy/work3", "req/arg/1")),
					pipe(out("strtegy/work2", "prv/result"), in("strategy/work3", "req/arg/2")));
			
			requestTime(job);
			
//		// define a job control strategy
//		// use the catalog to delegate the tasks
//		job.setAccessType(Access.PUSH);
//		// either parallel or sequential
//		job.setFlowType(Flow.SEQ);
//		// time the job execution
//		job.setExecTimeRequested(true);
//
//		return job;
	}

	@Test
	public void sharedTaskContect() throws Exception {

//		String requestorName = getProperty("requestor.name");
//		String prefix1 = getProperty("value.prefix.1");
//		String prefix2 = getProperty("value.prefix.2");
//
//		// define requestor data
//		Task batch = null;
//		try {
//			context.putValue("requestor/name", requestorName);
//
//			context.putInValue(prefix1 + "w1/req/arg/1", 20);
//			context.putInValue(prefix1 + "w1/req/arg/2", 80);
//			context.putInValue(prefix1 + "w1/req/work", Works.work1);
//			context.putOutValue("requestor/operand/1", Context.none, "par|"+ prefix1);
//
//			context.putInValue(prefix2 + "/requestor/operand/1", 10);
//			context.putInValue(prefix2 + "/requestor/operand/2", 50);
//			context.putInValue(prefix2 + "/requestor/work", Works.work2);
//			context.putOutValue("requestor/operand/2", Context.none, "par|"+ prefix2);
//
//			context.putInValue("requestor/work", Works.work3);
//			context.putOutValue("provider/result", Context.none);

			context = context("pBatch",
				inEnt("w1/req/arg/1", 20), inEnt("w1/req/arg/2", 80), 
				inEnt("w1/req/work", Works.work1), outEnt("req/arg/1", Context.none, "par|w1"),
			
				inEnt("w2/req/arg/1", 10), inEnt("w2/req/arg/2", 50), 
				inEnt("w2/req/work", Works.work2), outEnt("req/arg/2", Context.none, "par|w2"),
			
				inEnt("w3/req/work", Works.work3), outEnt("prv/result"));
			
			
//			// define required services
//			NetSignature signature1 = new NetSignature("doWork#" + prefix1,
//					sorcer.ex2.provider.Worker.class, Sorcer.getActualName(doWork#));
//			signature1.setType(Signature.Type.PRE);
//			NetSignature signature2 = new NetSignature("doWork#" + prefix2,
//					sorcer.ex2.provider.Worker.class, Sorcer.getActualName("Worker2"));
//			signature2.setType(Signature.Type.PRE);
//			NetSignature signature3 = new NetSignature("doWork", 
//					sorcer.ex2.provider.Worker.class, Sorcer.getActualName("Worker3"));

			
//			// define tasks
//			batch = new NetTask("batch work", signature1, context);
//			batch.addSignature(signature2);
//			batch.addSignature(signature3);
			
			Task bt = task("pBatch", 
					type(sig("doWork#w1", Worker.class, prvName("Worker1"),
						result("req/arg/1")), Signature.PRE),
					type(sig("doWork#w2", Worker.class, prvName("Worker2"),
						result("req/arg/2")), Signature.PRE),
					type(sig("doWork", Worker.class, prvName("Worker3"),
						result("prv/result"))),
					context);
			
			requestTime(bt);		
	}
}
