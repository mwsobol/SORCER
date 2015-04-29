package sorcer.worker.tests;

import org.junit.Before;
import org.junit.Test;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.util.Log;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.impl.WorkerProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.ent;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkerProviderImplTest {
	private static Logger logger = Log.getTestLog();
	
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
				ent("req/name", hostname),
				ent("req/arg/1", 11),
				ent("req/arg/2", 101),
				ent("req/work", work),
				ent("to/prv/name", "Worker Provider"));
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
