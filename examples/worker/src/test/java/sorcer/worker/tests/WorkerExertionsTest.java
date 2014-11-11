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
import static sorcer.eo.operator.serviceContext;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.store;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.type;
import static sorcer.eo.operator.value;

import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;

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
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/worker")
public class WorkerExertionsTest {
	private static Logger logger = Log.getTestLog();

	private Context context;
	private String hostname;
	private URL contextUrl;
	
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
		
		contextUrl = store(context);
	}

	@Test
	public void localProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerProvider.class), 
				context);
		Task et = exert(pt);
	
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/message"), 
				"Done work by: class sorcer.worker.provider.impl.WorkerProvider");
		assertEquals(get(et, "prv/host/name"), hostname);
		
	}

	@Test
	public void dbContextTest() throws Exception {

		Task pt = task("work", sig("doWork", WorkerProvider.class), 
				context("work", contextUrl));
		Task et = exert(pt);
	
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/message"), 
				"Done work by: class sorcer.worker.provider.impl.WorkerProvider");
		assertEquals(get(et, "prv/host/name"), hostname);
		
	}
	
	@Test
	public void remoteProviderTest() throws Exception {

		Task pt = task("work", sig("doWork", Worker.class, prvName("Worker1")), 
				context);
		Task et = exert(pt);
		
		logger.info("context: " + context(et));
		assertEquals(get(et, "prv/result"), 1111);
		assertEquals(get(et, "prv/message"), "Done work by: Worker1");
		assertEquals(get(et, "prv/host/name"), hostname);
		
	}
	
	
	@Test
	public void sharedTaskContext() throws Exception {

		context = context("pBatch",
					inEnt("w1/req/arg/1", 20), inEnt("w1/req/arg/2", 80), 
					inEnt("w1/req/work", Works.work1), outEnt("req/arg/1", "tag|w1"),

					inEnt("w2/req/arg/1", 10), inEnt("w2/req/arg/2", 50), 
					inEnt("w2/req/work", Works.work2), outEnt("req/arg/2", "tag|w2"),

					inEnt("w3/req/work", Works.work3), outEnt("prv/result"));


		Task bt = task("pBatch", 
					type(sig("doWork#w1", Worker.class, prvName("Worker1"), result("req/arg/1")), 
							Signature.PRE),
					type(sig("doWork#w2", Worker.class, prvName("Worker2"), result("req/arg/2")), 
							Signature.PRE),
					sig("doWork", Worker.class, prvName("Worker3"), result("prv/result")),
					context);

		requestTime(bt);
		Task out = exert(bt);
		logger.info("task context: " + context(out));
	}
	
	
	@Test
	public void exertionStrategy() throws Exception {

		Context cxt1 = context(ent("req/name", "workaholic"), 
				ent("req/arg/1", 20),  ent("req/arg/2", 80),
				ent("req/work", Works.work1),  ent("tp/prv/name", "Worker1"));

		Context cxt2 = context(ent("req/name", "workaholic"), 
				ent("req/arg/1", 10),  ent("req/arg/2", 50),
				ent("req/work", Works.work2),  ent("tp/prv/name", "Worker2"));

		Context cxt3 = context(ent("req/name", "workaholic"), 
				ent("req/arg/1", 100),  ent("req/arg/2", 100),
				ent("req/work", Works.work3),  ent("tp/prv/name", "Worker3"));

		Job job = job("strategy", 
				task("work1", sig("doWork", Worker.class, prvName("Worker1")), cxt1),
				task("work2", sig("doWork", Worker.class, prvName("Worker2")), cxt2),
				task("work3", sig("doWork", Worker.class, prvName("Worker3")), cxt3));

		Strategy control1 = strategy(Flow.SEQ, Access.PUSH);
		Strategy control2 = strategy(Flow.PAR, Access.PUSH);
		Strategy control3 = strategy(Flow.SEQ, Access.PULL);
		Strategy control4 = strategy(Flow.PAR, Access.PULL);

		Job out = exert(job, control1);
		logger.info("job context: " + serviceContext(out));

//		out = exert(job, control2);
//		logger.info("job context: " + serviceContext(out));
//		out = exert(job, control3);
//		logger.info("job context: " + serviceContext(out));
//		out = exert(job, control4);
//		logger.info("job context: " + serviceContext(out));

	}


	@Test
	public void contextPipes() throws Exception {
		
		Context cxt1 = context(ent("req/name", "workaholic"), 
				ent("req/arg/1", 20),  ent("req/arg/2", 80),
				ent("req/work", Works.work1),  ent("tp/prv/name", "Worker1"),
				outEnt("prv/result"));

		Context cxt2 = context(ent("req/name", "workaholic"), 
				ent("req/arg/1", 10),  ent("req/arg/2", 50),
				ent("req/work", Works.work2),  ent("tp/prv/name", "Worker2"),
				outEnt("prv/result"));

		Context cxt3 = context(ent("req/name", "workaholic"), 
				inEnt("req/arg/1"),  inEnt("req/arg/2"),
				ent("req/work", Works.work3),  ent("tp/prv/name", "Worker3"));

		Job job = job("strategy", 
				task("work1", sig("doWork", Worker.class), cxt1),
				task("work2", sig("doWork", Worker.class), cxt2),
				task("work3", sig("doWork", Worker.class), cxt3),
				pipe(out("strategy/work1", "prv/result"), in("strategy/work3", "req/arg/1")),
				pipe(out("strtegy/work2", "prv/result"), in("strategy/work3", "req/arg/2")));

		requestTime(job);
		Job out = exert(job);
		logger.info("job context: " + serviceContext(out));

	}

}
