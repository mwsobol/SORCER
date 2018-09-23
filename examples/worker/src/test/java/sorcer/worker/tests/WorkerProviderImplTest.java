package sorcer.worker.tests;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.po.operator;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.impl.WorkerProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static sorcer.co.operator.get;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.value;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkerProviderImplTest {
	private final static Logger logger = LoggerFactory.getLogger(WorkerProviderImplTest.class);
	
	String hostname;
	Context context;
	WorkerProvider provider;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		hostname = InetAddress.getLocalHost().getHostName();
		provider = new WorkerProvider();

		Work work = new Work() {
			private static final long serialVersionUID = 1L;

			public Context<Integer> exec(Context cxt) throws InvalidWork, ContextException {
				int arg1 = (Integer)value(cxt, "req/arg/1");
				int arg2 = (Integer)value(cxt, "req/arg/2");
				int result =  arg1 * arg2;
				put(cxt, "prv/result", result);
				cxt.setReturnValue(result);
				return cxt;
			}
		};

		context = context("work", 
				operator.ent("req/key", hostname),
				operator.ent("req/arg/1", 11),
				operator.ent("req/arg/2", 101),
				operator.ent("req/work", work),
				operator.ent("to/prv/key", "Worker Provider"));
	}


	/**
	 * Test method for {@link sorcer.worker.provider.impl.WorkerProvider#sayHi(sorcer.service.Context)}.
	 * @throws IOException 
	 */
	@Test
	public void testSayHi() throws ContextException, IOException {
		Context result = provider.sayHi(context);
		logger.info("result: " + result);
		assertTrue(result.getValue("prv/message").equals("Hi " + hostname + "!"));
	}

	/**
	 * Test method for {@link sorcer.worker.provider.impl.WorkerProvider#sayBye(sorcer.service.Context)}.
	 */
	@Test
	public void testSayBye() throws RemoteException, ContextException {
		Context result = provider.sayBye(context);
		logger.info("result: " + result);
		assertEquals(result.getValue("prv/message"), "Bye " + hostname + "!");

	}

	/**
	 * Test method for {@link sorcer.worker.provider.impl.WorkerProvider#doWork(sorcer.service.Context)}.
	 */
	@Test
	public void testDoWork() throws RemoteException, InvalidWork, ContextException {
		Context result = provider.doWork(context);
		logger.info("result: " + result);
		assertEquals(result.getValue("prv/result"), 1111);
	}

}
